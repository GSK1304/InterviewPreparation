# 📚 System Design — Web Crawler

---

## 🎯 Problem Statement
Design a scalable web crawler that systematically browses the internet, downloads web pages, extracts links, and stores content for indexing by a search engine.

---

## Step 1: Clarify Requirements

### Functional
- Crawl billions of web pages starting from seed URLs
- Follow links to discover new pages
- Respect robots.txt (crawl rules per site)
- Handle duplicate content (don't store same page twice)
- Handle different content types (HTML, PDF, images — focus on HTML)
- Re-crawl pages periodically (freshness)
- Politeness: don't overwhelm any single site

### Non-Functional
- **Scale**: 1B pages total, 1B new/updated pages/month
- **Throughput**: ~400 pages/sec sustained (to crawl 1B pages/month)
- **Storage**: 1B pages × avg 100KB = ~100TB HTML content
- **Freshness**: Popular pages re-crawled daily; rare pages monthly
- **Politeness**: Max 1 request/10 seconds per domain

---

## Step 2: Estimation

```
Crawl target: 1B pages/month = 385 pages/sec = ~400 pages/sec

Per page:
  Download HTML: avg 100KB
  Parse + extract links: avg 20 links per page
  Store content: 100KB × 1B = 100TB

Network:
  400 pages/sec × 100KB = 40 MB/sec inbound bandwidth

URL frontier:
  1B URLs × 100 bytes = 100GB (fits in distributed queue)

Seen URLs (dedup):
  1B URLs × 8 bytes (hash) = 8GB (fits in Redis/Bloom filter)
```

---

## Step 3: High-Level Architecture

```
            Seed URLs
                │
    ┌───────────▼────────────────────────────────┐
    │            URL Frontier                     │
    │   Priority Queue + Politeness Scheduler    │
    │   (Kafka / Redis sorted set)                │
    └───────────┬────────────────────────────────┘
                │ URLs to crawl (batched by domain)
    ┌───────────▼────────────────────────────────┐
    │         Crawler Workers (stateless)         │
    │   - Fetch HTML                             │
    │   - Parse robots.txt                       │
    │   - Extract links                          │
    │   - Detect duplicates (Bloom filter)       │
    └──────┬──────────────────┬──────────────────┘
           │                  │
  ┌────────▼──────┐  ┌────────▼──────────────┐
  │ Content Store │  │  Link Extractor /      │
  │ (S3 / HDFS)   │  │  URL Normalizer        │
  │  Raw HTML     │  │                        │
  └───────────────┘  └────────┬───────────────┘
                              │ New URLs
                    ┌─────────▼──────────────┐
                    │   Seen URL Filter       │
                    │  (Bloom Filter + Redis) │
                    └─────────┬──────────────┘
                              │ Unseen URLs only
                    ┌─────────▼──────────────┐
                    │   URL Frontier          │
                    │   (back to top)         │
                    └────────────────────────┘
                              │
                    ┌─────────▼──────────────┐
                    │   Search Index          │
                    │   (Elasticsearch /      │
                    │    custom inverted idx) │
                    └────────────────────────┘
```

---

## Step 4: URL Frontier — The Core Component

The URL frontier manages which URLs to crawl next, in what order, and how to be polite to servers.

### Requirements for URL Frontier
```
1. Prioritization: crawl important URLs first (high PageRank, fresh news)
2. Politeness: don't send more than 1 req/10s to same domain
3. Freshness: re-crawl pages based on change frequency
4. Scale: handle billions of URLs efficiently
```

### Two-Queue Architecture
```
Front Queue (Prioritizer):
  Multiple queues with different priorities
    High priority: news sites, popular pages, recently updated
    Medium: normal web pages
    Low: rarely updated pages

  Priority assignment:
    PageRank score (how many other pages link to this?)
    Domain popularity (google.com > obscure blog)
    Recency (news from last hour > 2-year-old article)
    Content freshness signal (was it different last time?)

Back Queue (Politeness Scheduler):
  One queue per active domain
    queue:google.com: [url1, url2, url3]
    queue:github.com: [url4, url5]
    queue:stackoverflow.com: [url6]
  
  Worker picks from back queue respecting:
    Last crawl time per domain
    robots.txt crawl-delay directive
    Minimum 10s between requests to same domain

Implementation with Redis:
  ZADD domain_schedule {next_crawl_timestamp} {domain}
  Worker: ZRANGEBYSCORE domain_schedule 0 {now} LIMIT 1 → get ready domains
  After crawl: ZADD domain_schedule {now+delay} {domain}
```

### Prioritization Formula
```
score = (0.4 × pagerank_normalized)
      + (0.3 × domain_popularity)
      + (0.2 × recency_score)
      + (0.1 × change_frequency)

recency_score: 1.0 for < 1hr old, 0.5 for < 1day, 0.1 for > 1week
change_frequency: historical probability this URL changes on re-crawl
```

---

## Step 5: Duplicate Detection

### URL Deduplication
```
Problem: Same URL discovered from 1000 different pages → crawl once

Solution 1: HashSet in memory
  Set<String> seenUrls = new HashSet<>(); → doesn't scale to 1B URLs

Solution 2: Bloom Filter
  Probabilistic: 0.1% false positive rate (sometimes skip unseen URL — OK)
  Memory: 1B URLs × 10 bits = 1.25GB → fits comfortably in memory
  O(1) check and insert
  
  fp = false positive rate (0.001), n = items (1B)
  Optimal bits = -n × ln(fp) / (ln2)² = ~15B bits = ~1.9GB
  
Implementation:
  Redis BITSET or Apache Cassandra with CQL BLOB
  Multiple hash functions (k=7 optimal for p=0.001)

Solution 3: Consistent Hashing of URL → shard across nodes
  Hash URL → one of N Redis instances
  SETNX {hash(url)} 1 → returns 0 if already seen
  Exact deduplication, O(1), horizontally scalable
```

### Content Deduplication
```
Problem: Different URLs serve same content
  http://example.com/page vs https://example.com/page vs
  http://www.example.com/page → same content!

Solution: SimHash / MinHash
  Compute fingerprint of page content (64-bit hash)
  Near-duplicate if Hamming distance < 3 bits
  
  Storage: {content_hash} → first URL that had this content
  On crawl: compute hash → if near-duplicate of existing → skip
  
  Canonical URL: pick one URL per content group for indexing
  
Google: "Shingles" algorithm for near-duplicate detection
Facebook: "TLSH" (Trend Micro Locality Sensitive Hash) for similar content
```

---

## Step 6: Crawler Workers

### Stateless, Horizontally Scalable Workers
```java
// Simplified crawler worker loop
while (true) {
    String url = frontier.nextUrl();      // blocking until URL available
    
    // 1. Respect robots.txt
    if (!robotsCache.isAllowed(url)) continue;
    
    // 2. Fetch page
    HttpResponse response = httpClient.get(url, 
        headers: {
            "User-Agent": "Googlebot/2.1 (+http://www.example.com/bot.html)",
            "Accept": "text/html",
        },
        timeout: 5000,
        maxRedirects: 5
    );
    
    // 3. Handle response
    if (response.status == 301 or 302) {
        frontier.add(response.redirectUrl, priority);
        return;
    }
    if (response.status != 200) {
        recordFailure(url, response.status);
        return;
    }
    
    // 4. Detect duplicate content
    String contentHash = simHash(response.body);
    if (contentStore.exists(contentHash)) {
        recordCanonical(url, contentStore.getUrl(contentHash));
        return;
    }
    
    // 5. Store content
    contentStore.save(url, response.body, contentHash);
    
    // 6. Extract and enqueue new links
    List<String> links = htmlParser.extractLinks(response.body, url);
    for (String link : links) {
        String normalized = normalizeUrl(link);
        if (!seenFilter.contains(normalized)) {
            seenFilter.add(normalized);
            frontier.add(normalized, score(normalized));
        }
    }
}
```

### robots.txt Handling
```
robots.txt defines crawl rules per bot:
  User-agent: Googlebot
  Disallow: /private/
  Disallow: /admin/
  Crawl-delay: 10

  User-agent: *
  Disallow: /

Cache robots.txt per domain:
  Redis: SETEX robots:{domain} 86400 {robots_txt_content}
  Re-fetch if > 24 hours old
  If robots.txt fetch fails: be conservative, don't crawl

Respect: crawl-delay, disallow rules, allow rules
Check before every URL: O(1) via cached rules
```

---

## Step 7: URL Normalization

```
Same URL can appear in many forms → normalize to canonical form

Normalizations:
  1. Lowercase scheme and host: HTTP://EXAMPLE.COM → http://example.com
  2. Remove default port: http://example.com:80/ → http://example.com/
  3. Remove fragment: http://example.com/page#section → http://example.com/page
  4. Decode unreserved chars: http://example.com/%7Euser → http://example.com/~user
  5. Sort query params: ?b=2&a=1 → ?a=1&b=2
  6. Remove session IDs: ?sessionid=abc → remove (duplicate content)
  7. Resolve relative URLs: ../page.html → absolute URL
  8. Enforce trailing slash consistency: /about → /about/

After normalization: hash for deduplication
```

---

## Step 8: Scaling the Crawler

### Distributed Workers
```
100 crawler workers:
  Each opens 100 concurrent connections = 10,000 parallel fetches
  At 10 pages/worker/sec = 1,000 pages/sec total (2.6B pages/month)
  
Assign URLs to workers by domain hash:
  worker = hash(domain) % numWorkers
  Ensures same domain always goes to same worker → easy politeness tracking

Auto-scale workers:
  Kubernetes HPA on frontier queue depth
  Scale up if queue > 1M URLs, scale down if < 10K
```

### DNS Pre-fetching
```
DNS resolution is slow (50-200ms) and cached per domain
  Pre-fetch DNS for domains about to be crawled
  Cache: domain → IP (TTL from DNS, min 5 minutes)
  
  At 400 unique domains/sec crawled:
  DNS cache hit ratio: > 99% (most domains crawled multiple pages)
```

### Content Storage
```
S3 / GCS object storage:
  Key: crawl/{domain}/{url_hash}/{crawl_timestamp}
  Stores: raw HTML, HTTP headers, crawl metadata

Compressed: gzip HTML → 60-80% size reduction
  100TB uncompressed → 20-40TB compressed

Partitioned by crawl date:
  s3://bucket/crawl/2025/01/01/... → easy time-based queries
  Lifecycle policy: delete raw HTML after 30 days (keep extracted index data)
```

---

## Step 9: Freshness & Re-crawl Strategy

```
Not all pages need to be re-crawled at the same frequency:
  News sites: re-crawl every 15 minutes
  E-commerce (product prices): daily
  Wikipedia: weekly
  Static documentation: monthly
  Abandoned sites: quarterly

Change frequency estimation:
  Track historical: was page different last N visits?
  change_rate = (changed_visits / total_visits)
  
  Exponential backoff for rarely-changing pages:
    if no change in 3 visits → double crawl interval (up to max)
    if changed → reset to base interval

Sitemaps (sitemap.xml):
  Many sites publish sitemaps with <lastmod> timestamps
  Parse sitemap → prioritize recently modified pages
  Much more efficient than blind re-crawl
```

---

## Interview Q&A

**Q: How do you handle crawler traps (infinite URL spaces)?**
A: (1) URL depth limit — don't crawl beyond 8 levels deep. (2) Path cycle detection — detect `/a/b/a/b/a/...` patterns. (3) Per-domain URL limit — max 100K URLs per domain. (4) Query parameter filtering — remove known session/tracking parameters that generate infinite URLs. (5) Canonical URL hints — respect `<link rel="canonical">` tags.

**Q: How does Google's crawler handle JavaScript-heavy SPAs?**
A: Googlebot has a full Chrome headless browser that executes JavaScript. Pages are added to a rendering queue, JS is executed, final DOM is extracted. This is expensive — Google prioritizes JS rendering for important pages. For less important pages, they crawl the raw HTML. Site owners can use SSR/SSG to make crawling easier and faster.

**Q: How would you detect and handle honeypot traps?**
A: Honeypots are pages designed to identify scrapers — links that are invisible to humans (hidden via CSS) but visible to crawlers. Detection: respect `display:none` and `visibility:hidden` CSS. Flag domains that have many 404s on discovered links. Rate-limit new domain discovery. IP reputation checking. Some traps are detected by observing unusual patterns in crawled content.

**Q: How does the crawler know when to stop and what constitutes "done"?**
A: A web crawler is never truly done — the web changes constantly. Define scope: seed list of domains, depth limit, total URL cap. Crawlers operate continuously: crawl, re-crawl, discover new pages. "Done" per URL = successfully fetched, parsed, and indexed. Monitor frontier size — if shrinking, increase crawl rate; if growing, scale up workers.

**Q: How do you handle crawling behind login/authentication?**
A: Generally, ethical crawlers don't log in to sites — they only crawl publicly accessible content. Exceptions: site owners explicitly grant crawler access (provide credentials). Search engines only index public content by design. For internal use cases (company intranet crawler): maintain session credentials, handle token refresh, respect per-user authorization.
