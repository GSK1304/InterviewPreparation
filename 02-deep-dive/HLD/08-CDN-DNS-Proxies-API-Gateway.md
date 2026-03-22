# 📚 HLD Core Concepts — CDN, DNS, Proxies & API Gateway

---

## 1. CDN (Content Delivery Network)

### What is a CDN?
A globally distributed network of proxy servers (Points of Presence / PoPs) that cache and serve content from locations geographically close to users.

```
Without CDN:                    With CDN:
User (India) ──────────────►  User (India) ──► Mumbai PoP (cache hit)
              8,000+ km              ~50ms           < 5ms
              150ms RTT
US Origin Server               US Origin Server (only on cache miss)
```

### How CDN Works
```
1. User requests image.jpg
2. DNS resolves to nearest CDN PoP (via Anycast or GeoDNS)
3. CDN PoP checks cache:
   HIT  → serve from edge immediately (< 5ms)
   MISS → fetch from origin, cache it, serve user
4. Subsequent requests from same region → cache HIT
```

### Push vs Pull CDN
| | Push CDN | Pull CDN |
|--|----------|----------|
| **How** | You upload content to CDN proactively | CDN fetches from origin on first request |
| **Control** | Full control over what's cached | CDN decides based on requests |
| **Storage cost** | Higher (all content pre-pushed) | Lower (only popular content cached) |
| **Cold start** | No cold start | First user in region gets cache miss |
| **Use when** | Static sites, known assets, small storage | Dynamic sites, unknown traffic patterns |
| **Example** | Uploading product images to S3+CloudFront | News articles cached on first read |

### CDN Cache Invalidation
```
Problem: How to update cached content?
  Option 1: TTL expiry — content expires after N minutes/hours
    + Simple; no invalidation needed
    - Stale content served until TTL expires

  Option 2: Versioned URLs — /static/app.v3.js
    + Instant update (new URL = no cache)
    + Old cached version still served if URL referenced
    - Requires URL management (cache busting)

  Option 3: Explicit invalidation — CDN API call to purge
    + Immediate update
    - Expensive at scale; thundering herd on origin after purge

✅ Best practice: Versioned URLs for static assets + short TTL for dynamic content
```

### CDN Advantages & Tradeoffs
| Advantage | Tradeoff |
|-----------|---------|
| Reduced latency (< 10ms vs 150ms) | Added complexity for cache invalidation |
| Reduced origin load (90%+ cache hit ratio) | Eventual consistency (stale content possible) |
| DDoS mitigation (absorbs at edge) | Cost (pay per GB served from edge) |
| High availability (PoPs worldwide) | Harder debugging (which PoP has stale content?) |

### When to Use CDN
✅ Static assets (images, CSS, JS, fonts, videos)
✅ API responses with low change frequency (product catalogs, reference data)
✅ Global user base needing low latency
✅ High traffic that would overload origin servers

❌ Don't use for: highly personalized content, real-time data, content requiring auth on every request

### 🏭 Industry Examples
- **Netflix**: Uses multiple CDN providers (Akamai, CloudFront, their own Open Connect) to serve 15%+ of internet traffic. Open Connect Appliances sit inside ISP data centers for sub-10ms video delivery.
- **Cloudflare CDN**: Serves 46M HTTP requests/second from 300+ PoPs. Their Workers product lets code run at the edge.
- **GitHub**: Uses Fastly CDN for serving code repositories. Raw file downloads cached at edge.
- **Amazon CloudFront**: Integrates with S3, EC2, ALB. Used by Amazon.com for product images.

---

## 2. DNS (Domain Name System)

### What is DNS?
Hierarchical distributed system that translates human-readable domain names to IP addresses.

### DNS Resolution Flow
```
Browser → OS cache → Router cache → Recursive Resolver (ISP)
    → Root Name Server (.) → TLD Server (.com) → Authoritative Server
                                                  (returns IP)
                              ◄─────────────────────────────────
Result cached at each layer with TTL
```

### DNS Record Types
| Record | Purpose | Example |
|--------|---------|---------|
| A | Domain → IPv4 | `api.example.com → 1.2.3.4` |
| AAAA | Domain → IPv6 | `api.example.com → 2001:db8::1` |
| CNAME | Domain → Domain (alias) | `www → example.com` |
| MX | Mail server | `example.com → mail.example.com` |
| TXT | Arbitrary text (SPF, verification) | `v=spf1 include:...` |
| NS | Name servers for zone | `example.com → ns1.example.com` |
| SOA | Zone authority info | Start of authority record |
| SRV | Service location | `_http._tcp.example.com` |

### DNS Load Balancing
```
Round Robin DNS:
  api.example.com → 1.2.3.4 (server 1)
  api.example.com → 1.2.3.5 (server 2)  ← rotated per request
  api.example.com → 1.2.3.6 (server 3)
  Problem: No health checking; stale cached IPs served if server goes down

GeoDNS (Latency-Based):
  User in India  → api.example.com → Mumbai region IP
  User in US     → api.example.com → Virginia region IP
  Used by: Route 53 latency routing, Cloudflare GeoDNS

Anycast DNS:
  Same IP announced from multiple PoPs worldwide
  Router's BGP protocol routes to "nearest" PoP
  Used by: Cloudflare (1.1.1.1), Google (8.8.8.8)
```

### DNS TTL Strategy
```
High TTL (hours/days):  ✅ Less DNS queries, faster resolution
                        ❌ Slow propagation when IP changes
                        Use for: stable infrastructure

Low TTL (60s):          ✅ Fast failover (Blue-Green deployments)
                        ❌ More DNS queries, higher resolver load
                        Use for: deployments, disaster recovery

Rule: Lower TTL before planned changes, raise after stable
```

### DNS Advantages & Tradeoffs
| Advantage | Tradeoff |
|-----------|---------|
| Enables human-readable names | Propagation delay (TTL must expire) |
| Built-in load distribution | Clients cache IPs — failover not instant |
| GeoDNS for latency optimization | DNS amplification DDoS vulnerability |
| Hierarchical — infinitely scalable | Split-brain DNS in complex multi-region |

### 🏭 Industry Examples
- **Netflix**: Uses Route 53 with health checks + automatic failover between AWS regions.
- **Cloudflare**: Their authoritative DNS is the fastest in the world (< 11ms globally). Protects customers from DNS DDoS.
- **GitHub**: Uses multiple DNS providers for redundancy. GitHub.com has low TTL for fast incident recovery.

---

## 3. Forward Proxy vs Reverse Proxy

```
Forward Proxy (client-side):              Reverse Proxy (server-side):
Client → [Forward Proxy] → Internet       Client → [Reverse Proxy] → Servers

Client knows it's using a proxy.          Client thinks it's talking to server.
Server doesn't know about client.         Server doesn't know about client.

Use cases:                                 Use cases:
  - Corporate internet filtering           - Load balancing
  - VPN / anonymization                    - SSL termination
  - Content filtering (parental controls)  - Caching
  - Bypassing geo-restrictions             - Rate limiting
  - Caching (forward direction)            - Compression
                                           - API Gateway
Examples: Squid, Privoxy, corporate VPNs  Examples: Nginx, HAProxy, Cloudflare
```

---

## 4. API Gateway vs Load Balancer vs Reverse Proxy

### Comparison Table
| Feature | Load Balancer | Reverse Proxy | API Gateway |
|---------|--------------|---------------|-------------|
| **Primary job** | Distribute traffic | Route + cache + SSL | Manage APIs |
| **Layer** | L4 (TCP) or L7 (HTTP) | L7 (HTTP) | L7 (HTTP) |
| **SSL termination** | ✅ (L7 only) | ✅ | ✅ |
| **Authentication** | ❌ | ❌ | ✅ |
| **Rate limiting** | ❌ | Limited | ✅ |
| **Request transformation** | ❌ | Limited | ✅ |
| **Service discovery** | Limited | Limited | ✅ |
| **Analytics/logging** | Basic | Basic | ✅ |
| **Protocol translation** | ❌ | ❌ | ✅ (REST→gRPC) |
| **Examples** | AWS ALB/NLB, HAProxy | Nginx, Cloudflare | Kong, AWS API GW, Spring Cloud GW |

### When to Use Which
```
Load Balancer → When you just need traffic distribution across identical servers
               Use case: Scaling web/app tier horizontally

Reverse Proxy → When you need caching, SSL offloading, compression, basic routing
               Use case: Fronting a web server cluster with caching + SSL

API Gateway   → When you have microservices needing unified auth, rate limiting,
               routing, and protocol translation
               Use case: Microservices platform entry point

In practice: Often stack them:
  Client → CDN → API Gateway → Load Balancer → Service Instances
```

### API Gateway Responsibilities
```
1. Authentication & Authorization — validate JWT, check permissions
2. Rate Limiting — per user/IP/endpoint token bucket
3. Request Routing — /users/* → UserService, /orders/* → OrderService
4. SSL Termination — decrypt HTTPS, forward HTTP internally
5. Request/Response Transformation — add headers, modify payloads
6. Service Discovery — resolve service names to IPs via registry
7. Circuit Breaking — stop forwarding to unhealthy services
8. Observability — centralized logging, metrics, tracing
9. Protocol Translation — REST to gRPC, HTTP/1.1 to HTTP/2
```

### 🏭 Industry Examples
- **Netflix Zuul/Gateway**: Routes 2B+ requests/day to 700+ microservices. Handles auth, rate limiting, A/B routing.
- **AWS API Gateway**: Manages APIs for millions of developers. Handles OAuth2, API keys, throttling.
- **Uber's Frontier**: Custom API gateway handling auth + routing for all Uber microservices.
- **Kong Gateway**: Open-source API gateway used by Airbnb, HelloSign, and thousands of companies.
- **kACE**: Spring Cloud Gateway validates JWT, enforces rate limits, routes to pricing/RFQ/auth services.

---

## 5. Consistent Hashing

### The Problem
Standard hash: `server = hash(key) % N`
- Adding/removing a server remaps ~all keys → massive cache invalidation

### Consistent Hashing Solution
```
Ring-based hash space: 0 → 2^32 (or any large range)

1. Hash each server to a position on the ring:
   Server A: hash("ServerA") = 12
   Server B: hash("ServerB") = 45
   Server C: hash("ServerC") = 78

2. Hash each key to a position:
   key1: hash("key1") = 30 → maps to Server B (next clockwise)
   key2: hash("key2") = 60 → maps to Server C
   key3: hash("key3") = 90 → maps to Server A (wraps around)

3. Add Server D at position 55:
   Only keys between Server B (45) and D (55) move → ~K/N keys
   
4. Remove Server B at position 45:
   Only keys between Server A (12) and C (78) move via D
```

### Virtual Nodes
```
Problem: Uneven distribution with few servers
Solution: Each physical server gets V virtual nodes on the ring

Server A: hash("ServerA#1")=12, hash("ServerA#2")=34, hash("ServerA#3")=67, ...
Server B: hash("ServerB#1")=23, hash("ServerB#2")=56, hash("ServerB#3")=89, ...

Result: Even key distribution regardless of server count
V=100-200 virtual nodes per server is typical
```

### Consistent Hashing Advantages & Tradeoffs
| Advantage | Tradeoff |
|-----------|---------|
| Only K/N keys remapped when N changes | More complex than modular hash |
| Minimal cache disruption on scale-up/down | Virtual nodes need careful tuning |
| Natural load distribution with virtual nodes | Hot spots still possible without vnodes |
| Used in distributed caches, databases, CDNs | Node lookup O(log N) with sorted ring |

### 🏭 Industry Examples
- **Amazon DynamoDB**: Uses consistent hashing with virtual nodes for partition key distribution.
- **Apache Cassandra**: Ring-based consistent hashing with virtual nodes (256 tokens/node default).
- **Memcached (libketama)**: First widely used consistent hash implementation.
- **Nginx**: Uses consistent hashing for upstream server selection in reverse proxy mode.
- **Discord**: Uses consistent hashing to distribute 850M+ messages/day across message broker nodes.

---

## Interview Q&A

**Q: What happens when a CDN PoP goes down?**
A: DNS TTL expires → resolver queries authoritative DNS → returns backup PoP or origin IP. CDNs use Anycast so BGP automatically routes to next nearest PoP. Most enterprise CDNs have <30s failover.

**Q: CDN vs caching at the application layer — when to use which?**
A: CDN for geographically distributed content served to end users (static assets, API responses). App-layer cache (Redis) for data shared between service instances, session storage, and computed results that are not user-facing. They complement each other — CDN at the edge, Redis at the service layer.

**Q: Why can't we just use round-robin DNS instead of a load balancer?**
A: Round-robin DNS has no health checking, clients cache IPs (failover takes TTL time), no session affinity, no SSL termination, and no observability. Load balancers solve all these with sub-second health check intervals.

**Q: API Gateway vs Service Mesh — what's the difference?**
A: API Gateway handles north-south traffic (external clients → services). Service Mesh (Istio, Linkerd) handles east-west traffic (service → service). They complement each other — Gateway for external API management, Mesh for internal service observability and security.

**Q: How does consistent hashing handle hot spots?**
A: Virtual nodes help distribute load. Additionally, some systems use "bounded loads" (a node can take at most (1+ε) × average load) — if a node is overloaded, keys overflow to the next node. Caching hot keys across multiple nodes also helps.
