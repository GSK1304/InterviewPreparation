# 📚 DSA Deep Dive — Suffix Arrays, A*, Randomised Algorithms & Network Flow

> Suffix Arrays (🟡 P2), A* Algorithm (🟡 P2), Randomised Algorithms (🟡 P2), Network Flow (🟢 P3)

---

## 1. Suffix Array

**What it is**: Array of all suffixes of a string, sorted lexicographically.
**Use**: Powerful string data structure — enables O(log n) substring search after O(n log n) build.

### Build Suffix Array (O(n log n))
```java
// For string s = "banana"
// Suffixes:
// 0: banana
// 1: anana
// 2: nana
// 3: ana
// 4: na
// 5: a
//
// Sorted: a(5), ana(3), anana(1), banana(0), na(4), nana(2)
// Suffix Array: [5, 3, 1, 0, 4, 2]

int[] buildSuffixArray(String s) {
    int n = s.length();
    Integer[] sa = new Integer[n];
    for (int i = 0; i < n; i++) sa[i] = i;
    // Sort by suffix string
    Arrays.sort(sa, (a, b) -> s.substring(a).compareTo(s.substring(b)));
    return Arrays.stream(sa).mapToInt(Integer::intValue).toArray();
}
// Note: Above is O(n² log n) — production uses O(n log n) DC3/SA-IS algorithm
```

### LCP Array (Longest Common Prefix)
```java
// LCP[i] = length of longest common prefix between sa[i] and sa[i-1]
// For "banana": SA = [5,3,1,0,4,2], LCP = [0,1,3,0,0,2]
int[] buildLCP(String s, int[] sa) {
    int n = s.length();
    int[] rank = new int[n], lcp = new int[n];
    for (int i = 0; i < n; i++) rank[sa[i]] = i;
    int h = 0;
    for (int i = 0; i < n; i++) {
        if (rank[i] > 0) {
            int j = sa[rank[i] - 1];
            while (i + h < n && j + h < n && s.charAt(i+h) == s.charAt(j+h)) h++;
            lcp[rank[i]] = h;
            if (h > 0) h--;
        }
    }
    return lcp;
}
```

### Key Applications
```java
// 1. Count distinct substrings
// Total substrings = n*(n+1)/2
// Subtract sum of LCP array (duplicates)
long countDistinct(String s) {
    int[] sa = buildSuffixArray(s);
    int[] lcp = buildLCP(s, sa);
    long n = s.length();
    long total = n * (n + 1) / 2;
    for (int l : lcp) total -= l;
    return total;
}

// 2. Longest repeated substring
// = max value in LCP array
int longestRepeated(int[] lcp) {
    return Arrays.stream(lcp).max().orElse(0);
}

// 3. Pattern search in O(log n) using binary search on SA
boolean contains(String s, String pattern, int[] sa) {
    int lo = 0, hi = sa.length - 1;
    while (lo <= hi) {
        int mid = (lo + hi) / 2;
        String suffix = s.substring(sa[mid]);
        int cmp = suffix.startsWith(pattern) ? 0
                : suffix.compareTo(pattern) < 0 ? -1 : 1;
        if (cmp == 0) return true;
        else if (cmp < 0) lo = mid + 1;
        else hi = mid - 1;
    }
    return false;
}
```

---

## 2. A* Algorithm

**What it is**: Informed search algorithm — like Dijkstra but uses a **heuristic** to guide search towards the goal.

**Formula**: `f(n) = g(n) + h(n)`
- `g(n)` = actual cost from start to node n
- `h(n)` = heuristic estimate from n to goal (must be admissible — never overestimates)

### Implementation
```java
int aStar(int[][] grid, int[] start, int[] goal) {
    int rows = grid.length, cols = grid[0].length;
    // Priority queue ordered by f = g + h
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[2] - b[2]);
    // {row, col, f_score}
    pq.offer(new int[]{start[0], start[1], heuristic(start, goal)});

    int[][] gScore = new int[rows][cols];
    for (int[] row : gScore) Arrays.fill(row, Integer.MAX_VALUE);
    gScore[start[0]][start[1]] = 0;

    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    while (!pq.isEmpty()) {
        int[] cur = pq.poll();
        int r = cur[0], c = cur[1];

        if (r == goal[0] && c == goal[1]) return gScore[r][c];

        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] == 1)
                continue;
            int newG = gScore[r][c] + 1;
            if (newG < gScore[nr][nc]) {
                gScore[nr][nc] = newG;
                int f = newG + heuristic(new int[]{nr, nc}, goal);
                pq.offer(new int[]{nr, nc, f});
            }
        }
    }
    return -1; // no path
}

// Manhattan distance heuristic (admissible for grid)
int heuristic(int[] a, int[] goal) {
    return Math.abs(a[0] - goal[0]) + Math.abs(a[1] - goal[1]);
}
```

### A* vs Dijkstra
| | Dijkstra | A* |
|--|----------|----|
| Strategy | Explores all directions equally | Guided towards goal |
| Speed | Slower (explores more) | Faster with good heuristic |
| Guarantee | Optimal always | Optimal if heuristic is admissible |
| Use case | All-pairs shortest path | Single source → single target |

---

## 3. Randomised Algorithms

### 3a. Randomised QuickSort / QuickSelect
```java
// Randomise pivot to avoid O(n²) worst case on sorted input
int partition(int[] nums, int lo, int hi) {
    // Random pivot — swap with hi before partitioning
    int pivotIdx = lo + new Random().nextInt(hi - lo + 1);
    swap(nums, pivotIdx, hi);
    int pivot = nums[hi], i = lo - 1;
    for (int j = lo; j < hi; j++)
        if (nums[j] <= pivot) swap(nums, ++i, j);
    swap(nums, i + 1, hi);
    return i + 1;
}
// Expected O(n log n) regardless of input distribution
```

### 3b. Randomised Hashing (Monte Carlo)
```java
// Use random base for string hashing to reduce collision probability
long randomBase = (long)(Math.random() * (MOD - 2)) + 2;
// String hashing with random base: collision prob = O(1/MOD)
```

### 3c. Bloom Filter (Probabilistic Set)
```java
// Space-efficient probabilistic set — may have false positives, never false negatives
// Used in: Cassandra, Redis, Chrome safe browsing
class BloomFilter {
    BitSet bits;
    int[] seeds; // multiple hash functions

    boolean mightContain(String key) {
        for (int seed : seeds)
            if (!bits.get(hash(key, seed) % bits.size())) return false;
        return true; // might be in set (false positive possible)
    }

    void add(String key) {
        for (int seed : seeds) bits.set(hash(key, seed) % bits.size());
    }
}
```

### 3d. Skip List (Randomised Balancing)
```java
// Already covered in Balanced Trees — skip list uses random level generation
// Each new node is promoted to level i with probability 1/2
int randomLevel() {
    int level = 1;
    while (Math.random() < 0.5 && level < MAX_LEVEL) level++;
    return level;
}
```

### 3e. Las Vegas vs Monte Carlo
| Type | Guarantee | Randomness |
|------|-----------|------------|
| Las Vegas | Always correct | Random runtime |
| Monte Carlo | Correct with probability | Deterministic runtime |

Examples:
- **Las Vegas**: Randomised QuickSort (always correct, expected O(n log n))
- **Monte Carlo**: Miller-Rabin primality test (probably prime), Bloom Filter

---

## 4. Network Flow

**Problem**: Given a directed graph with capacities, find maximum flow from source to sink.

### Ford-Fulkerson (BFS augmenting paths = Edmonds-Karp)
```java
int maxFlow(int[][] capacity, int source, int sink, int n) {
    int[][] residual = new int[n][n];
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            residual[i][j] = capacity[i][j];

    int maxFlow = 0;
    int[] parent = new int[n];

    while (bfs(residual, source, sink, n, parent)) {
        // Find min capacity along augmenting path
        int pathFlow = Integer.MAX_VALUE;
        for (int v = sink; v != source; v = parent[v])
            pathFlow = Math.min(pathFlow, residual[parent[v]][v]);

        // Update residual capacities
        for (int v = sink; v != source; v = parent[v]) {
            residual[parent[v]][v] -= pathFlow;
            residual[v][parent[v]] += pathFlow; // back edge
        }
        maxFlow += pathFlow;
    }
    return maxFlow;
}

boolean bfs(int[][] res, int src, int sink, int n, int[] parent) {
    boolean[] visited = new boolean[n];
    Queue<Integer> q = new LinkedList<>();
    q.offer(src); visited[src] = true; parent[src] = -1;
    while (!q.isEmpty()) {
        int u = q.poll();
        for (int v = 0; v < n; v++) {
            if (!visited[v] && res[u][v] > 0) {
                parent[v] = u; visited[v] = true;
                if (v == sink) return true;
                q.offer(v);
            }
        }
    }
    return false;
}
// Edmonds-Karp: O(VE²)
```

### Key Network Flow Applications
```
Max Flow = Min Cut (Ford-Fulkerson theorem)

Applications:
- Bipartite matching → max matching
- Assignment problem → min cost flow
- Circulation with demands
- Baseball elimination
- Image segmentation
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Suffix Array — FX Symbol Search
```java
// Find all FX symbols containing substring "USD" efficiently
int[] sa = buildSuffixArray(allSymbolsConcatenated);
// Binary search for "USD" in O(log n) — much faster than linear scan
```
**Where it applies**: Symbol search engines, trade log pattern analysis.

### Use Case 2: A* for Trade Route Optimisation
```java
// Find fastest path between trading venues considering network topology
// Heuristic: geographic distance between venues
int optimalRoute = aStar(networkGrid, kaceHubCoords, targetVenueCoords);
```
**Where it applies**: FX trade routing with latency-based heuristic.

### Use Case 3: Bloom Filter — Kafka Deduplication
```java
// Check if message ID was already processed (no false negatives)
BloomFilter processed = new BloomFilter(10_000_000, 0.01);
if (!processed.mightContain(messageId)) {
    process(message);
    processed.add(messageId);
}
// Saves DB lookup for 99%+ of duplicate checks
```
**Where it applies**: Kafka consumer deduplication, API rate limiting at scale.

### Use Case 4: Network Flow — Team Task Assignment
```java
// Bipartite matching: assign engineers to tasks optimally
// Source → engineers → tasks → sink
// Max flow = max number of tasks that can be assigned
int maxAssignments = maxFlow(assignmentCapacity, source, sink, n);
```
**Where it applies**: kACE team management — sprint task allocation.

### Use Case 5: Randomised Hashing — Config Deduplication
```java
// Use random-base polynomial hashing to detect duplicate layout configs
// Collision probability < 1/10^9 with 64-bit hash
long hash = randomHashConfig(layoutConfig);
if (configHashes.contains(hash)) return cachedConfig(hash);
```
**Where it applies**: kACE layout config deduplication, screen config caching.

---

## 🏋️ Practice Problems

| # | Problem | Topic | Difficulty |
|---|---------|-------|------------|
| 1 | Longest Duplicate Substring | Suffix Array + BS | Hard |
| 2 | Shortest Path in Binary Matrix | A* / BFS | Medium |
| 3 | Path with Minimum Effort | A* / Dijkstra | Medium |
| 4 | Randomised QuickSort | Randomised algo | Medium |
| 5 | Shuffle an Array | Fisher-Yates | Medium |
| 6 | Max Flow (Network Flow) | Ford-Fulkerson | Hard |
| 7 | Bipartite Matching | Max flow | Hard |
| 8 | Count Distinct Substrings | Suffix Array + LCP | Hard |
| 9 | Minimum Window Substring | Sliding window / SA | Hard |
| 10 | Critical Connections | Tarjan + flow concept | Hard |

---

## ⚠️ Key Interview Points

- **Suffix Array**: O(n log n) build, O(log n) search — good for string-heavy interviews (Google)
- **A***: Best for single source → single target pathfinding with heuristic knowledge
- **Bloom Filter**: Space-efficient, false positives possible, no false negatives — great for dedup at scale
- **Network Flow**: Max-flow min-cut theorem — bipartite matching reduces to max flow
- **Randomised algos**: Las Vegas = always correct; Monte Carlo = probably correct
