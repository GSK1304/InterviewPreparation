# 📚 DSA Deep Dive — HashMaps & Sets

> Priority: 🔴 P0 — Asked in virtually every interview. Most common O(1) optimization.

---

## 🧠 Core Concepts

### How Hashing Works
A **hash function** maps a key to an index in an array (bucket). Collisions are handled via:
- **Chaining**: Each bucket holds a linked list of entries
- **Open Addressing**: Probe for next empty slot (linear, quadratic, double hashing)

Java's `HashMap` uses chaining with a threshold — when chain length ≥ 8, it converts to a **Red-Black Tree** for O(log n) worst case.

### Java Collections Reference
```java
// HashMap — key-value, O(1) avg
Map<String, Integer> map = new HashMap<>();
map.put("key", 1);
map.get("key");                              // 1
map.getOrDefault("missing", 0);             // 0
map.containsKey("key");                     // true
map.putIfAbsent("key", 2);                  // won't overwrite
map.merge("key", 1, Integer::sum);          // key += 1
map.getOrDefault("k", 0) + 1;              // common freq pattern
for (Map.Entry<String, Integer> e : map.entrySet()) { }

// LinkedHashMap — maintains insertion order
Map<String, Integer> linked = new LinkedHashMap<>();

// TreeMap — sorted by key, O(log n)
TreeMap<Integer, String> sorted = new TreeMap<>();
sorted.firstKey(); sorted.lastKey();
sorted.floorKey(5);    // largest key <= 5
sorted.ceilingKey(5);  // smallest key >= 5
sorted.subMap(1, 10);  // keys in [1, 10)

// HashSet — unique elements, O(1) avg
Set<Integer> set = new HashSet<>();
set.add(1); set.contains(1); set.remove(1);

// LinkedHashSet — unique + insertion order
Set<Integer> lset = new LinkedHashSet<>();

// TreeSet — unique + sorted, O(log n)
TreeSet<Integer> tset = new TreeSet<>();
tset.floor(5); tset.ceiling(5);
tset.headSet(5);  // elements < 5
tset.tailSet(5);  // elements >= 5
```

---

## 🔑 Key Patterns

### Frequency Counter
```java
// Count frequency of each element
Map<Character, Integer> freq = new HashMap<>();
for (char c : s.toCharArray())
    freq.merge(c, 1, Integer::sum);
// OR
freq.put(c, freq.getOrDefault(c, 0) + 1);
```

### Two Sum — HashMap Complement
```java
Map<Integer, Integer> seen = new HashMap<>();
for (int i = 0; i < nums.length; i++) {
    int complement = target - nums[i];
    if (seen.containsKey(complement))
        return new int[]{seen.get(complement), i};
    seen.put(nums[i], i);
}
```

### Grouping / Bucketing
```java
// Group anagrams — sort chars as key
Map<String, List<String>> groups = new HashMap<>();
for (String word : words) {
    char[] chars = word.toCharArray();
    Arrays.sort(chars);
    String key = new String(chars);
    groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
}
```

### Sliding Window with HashMap
```java
// Longest substring with at most K distinct characters
Map<Character, Integer> window = new HashMap<>();
int left = 0, maxLen = 0;
for (int right = 0; right < s.length(); right++) {
    window.merge(s.charAt(right), 1, Integer::sum);
    while (window.size() > k) {
        char lc = s.charAt(left++);
        window.merge(lc, -1, Integer::sum);
        if (window.get(lc) == 0) window.remove(lc);
    }
    maxLen = Math.max(maxLen, right - left + 1);
}
```

### Prefix Sum with HashMap (Subarray Sum = K)
```java
// Count subarrays with sum exactly K
Map<Integer, Integer> prefixCount = new HashMap<>();
prefixCount.put(0, 1); // empty prefix
int sum = 0, count = 0;
for (int num : nums) {
    sum += num;
    count += prefixCount.getOrDefault(sum - k, 0);
    prefixCount.merge(sum, 1, Integer::sum);
}
return count;
```

### LRU Cache (HashMap + Doubly Linked List)
```java
class LRUCache {
    int capacity;
    Map<Integer, Node> map = new HashMap<>();
    Node head = new Node(0, 0), tail = new Node(0, 0);

    LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail; tail.prev = head;
    }

    int get(int key) {
        if (!map.containsKey(key)) return -1;
        Node n = map.get(key);
        remove(n); addToFront(n);
        return n.val;
    }

    void put(int key, int val) {
        if (map.containsKey(key)) remove(map.get(key));
        Node n = new Node(key, val);
        addToFront(n); map.put(key, n);
        if (map.size() > capacity) {
            Node lru = tail.prev;
            remove(lru); map.remove(lru.key);
        }
    }

    void remove(Node n) { n.prev.next = n.next; n.next.prev = n.prev; }
    void addToFront(Node n) {
        n.next = head.next; n.prev = head;
        head.next.prev = n; head.next = n;
    }
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Dropdown Cache (kACE — O(1) Lookups)
```java
// Pre-load ~200 dropdowns into HashMap at startup
Map<String, List<DropdownOption>> dropdownCache = new HashMap<>();
// O(1) lookup by field name
List<DropdownOption> options = dropdownCache.get("CURRENCY_PAIR");
```
**Where it applies**: kACE `dropdownCache.ts` — module-level Map for O(1) field option lookups.
> 🏭 **Industry Example**: Netflix uses in-memory HashMaps to cache genre→movie-list mappings per user session, avoiding repeated DB queries during browsing. Spotify caches artist→top-tracks for O(1) playback suggestions.
> 🏦 **kACE Context**: `dropdownCache.ts` — pre-loads ~200 FX option field dropdowns at startup for O(1) UI lookup.


### Use Case 2: WebSocket Session Registry
```java
// Track active WebSocket sessions per subscription topic
Map<String, Set<String>> topicSessions = new HashMap<>();
topicSessions.computeIfAbsent("RFQ_UPDATES", k -> new HashSet<>()).add(sessionId);
// Broadcast to all sessions subscribed to topic
topicSessions.getOrDefault("RFQ_UPDATES", Set.of()).forEach(this::send);
```
**Where it applies**: kACE `SubscriptionRegistry` — WebSocket topic-to-session mapping.
> 🏭 **Industry Example**: Slack uses a ConcurrentHashMap<channelId, Set<userId>> to track which users are viewing each channel in real-time. Discord routes message fan-out using a topic→session registry per gateway server.
> 🏦 **kACE Context**: `SubscriptionRegistry` — maps WebSocket topics to active trader sessions for targeted price updates.


### Use Case 3: Anagram / Duplicate Detection
```java
// Detect duplicate trade IDs in a batch
Set<String> seen = new HashSet<>();
List<String> duplicates = tradeIds.stream()
    .filter(id -> !seen.add(id))
    .collect(Collectors.toList());
```
**Where it applies**: Trade reconciliation, Kafka message deduplication.
> 🏭 **Industry Example**: Google Docs uses duplicate detection to prevent saving identical versions. Amazon deduplicates product listings by normalizing and hashing titles.
> 🏦 **kACE Context**: Trade reconciliation — detecting duplicate trade IDs in a Kafka message batch before processing.


### Use Case 4: Frequency Analysis
```java
// Most common FX pair requested today
Map<String, Integer> pairFreq = new HashMap<>();
rfqStream.forEach(rfq -> pairFreq.merge(rfq.getPair(), 1, Integer::sum));
String mostRequested = Collections.max(pairFreq.entrySet(),
    Map.Entry.comparingByValue()).getKey();
```
**Where it applies**: kACE RFQ analytics, market maker activity monitoring.
> 🏭 **Industry Example**: Twitter uses frequency maps to compute trending hashtags in real-time. YouTube counts view frequencies per video per hour for its trending algorithm.
> 🏦 **kACE Context**: RFQ analytics — counting most-requested FX currency pairs per market maker session.


### Use Case 5: Subarray Sum — P&L Window Analysis
```java
// Count number of time windows where cumulative P&L equals target
int count = subarraySum(dailyPnL, targetPnL); // prefix sum + HashMap
```
**Where it applies**: FX option strategy P&L window analysis.
> 🏭 **Industry Example**: Amazon uses prefix sum + HashMap to compute revenue in arbitrary date ranges for seller dashboards in O(1) per query. Uber Eats uses it for sliding-window order volume metrics.
> 🏦 **kACE Context**: FX option strategy P&L window analysis — count time windows where cumulative P&L equals a target.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Two Sum | HashMap complement | Easy |
| 2 | Valid Anagram | Frequency counter | Easy |
| 3 | Contains Duplicate | HashSet | Easy |
| 4 | Group Anagrams | Sort key + HashMap | Medium |
| 5 | Longest Consecutive Sequence | HashSet O(n) | Medium |
| 6 | Subarray Sum Equals K | Prefix sum + HashMap | Medium |
| 7 | Top K Frequent Elements | HashMap + Heap | Medium |
| 8 | LRU Cache | HashMap + DLL | Medium |
| 9 | Find All Anagrams in String | Sliding window + freq | Medium |
| 10 | Longest Substring with K Distinct | Sliding window + map | Medium |

---

## ⚠️ Common Mistakes

- `map.get(key) + 1` without null check → NPE — always use `getOrDefault`
- Using `==` to compare Integer keys/values — use `.equals()` or unbox to `int`
- Modifying map while iterating — use `entrySet()` iterator or collect to new map
- HashMap not thread-safe — use `ConcurrentHashMap` in multi-threaded contexts
- `TreeMap` operations are O(log n) not O(1) — don't use when O(1) is needed
- Forgetting to remove zero-count entries in sliding window — causes wrong `map.size()`
