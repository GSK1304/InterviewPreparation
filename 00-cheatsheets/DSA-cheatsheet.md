# 📋 DSA Cheatsheet — Complete Quick Reference

---

## ⏱️ Time Complexity at a Glance

| Structure / Algo | Access | Search | Insert | Delete | Notes |
|-----------------|--------|--------|--------|--------|-------|
| Array | O(1) | O(n) | O(n) | O(n) | |
| LinkedList | O(n) | O(n) | O(1) | O(1) | At known node |
| HashMap | — | O(1) | O(1) | O(1) | Avg |
| Stack / Queue | — | O(n) | O(1) | O(1) | |
| Heap (PQ) | O(1) top | O(n) | O(log n) | O(log n) | |
| BST (balanced) | O(log n) | O(log n) | O(log n) | O(log n) | |
| Trie | — | O(L) | O(L) | O(L) | L = word length |
| Segment Tree | — | O(log n) | O(log n) | O(log n) | Range queries |
| Fenwick Tree | — | O(log n) | O(log n) | O(log n) | Prefix sums |
| Binary Search | — | O(log n) | — | — | Sorted input |

---

## 🔑 Pattern → When to Use

| Pattern | Trigger Words | Key Technique |
|---------|--------------|---------------|
| **Sliding Window** | subarray, substring, window of size k | Two pointers on same array |
| **Two Pointers** | sorted array, pair sum, palindrome | Opposite ends or same direction |
| **Fast & Slow** | cycle detection, middle of list | 2x speed difference |
| **Binary Search** | sorted, find position, minimize/maximize | Halve search space each step |
| **BFS** | shortest path, level order, nearest | Queue + visited |
| **DFS** | all paths, components, backtracking | Stack (recursion) + visited |
| **Topological Sort** | ordering, dependency, DAG | Kahn's BFS or DFS with stack |
| **Union-Find** | connected components, cycles | parent[] + find() with compression |
| **Heap / Top-K** | K largest/smallest, streaming median | Min/Max PriorityQueue |
| **Monotonic Stack** | next greater/smaller element | Maintain increasing/decreasing stack |
| **DP** | optimal, count ways, overlapping subproblems | State + recurrence + base case |
| **Backtracking** | all combinations, permutations, constraint | Make choice → recurse → undo |
| **Greedy** | locally optimal = globally optimal | Sort + pick best at each step |
| **Trie** | prefix search, autocomplete | Character-indexed tree nodes |
| **Cyclic Sort** | missing/duplicate in [1..n] | Place at correct index |
| **Boyer-Moore** | majority element O(1) space | Count cancellation |
| **Bit Manipulation** | power of 2, XOR pairs, subsets | Bitwise ops |
| **KMP / Z-Algo** | pattern matching, repeated patterns | Failure function / Z-array |
| **Segment Tree** | range queries + updates | Build O(n), query/update O(log n) |

---

## 🧩 Code Templates

### Sliding Window (Variable)
```java
int left = 0;
Map<Character, Integer> window = new HashMap<>();
for (int right = 0; right < s.length(); right++) {
    window.merge(s.charAt(right), 1, Integer::sum);
    while (/* violation */) window.merge(s.charAt(left++), -1, Integer::sum);
    ans = Math.max(ans, right - left + 1);
}
```

### Binary Search
```java
int lo = 0, hi = n - 1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (check(mid)) hi = mid - 1;
    else lo = mid + 1;
}
```

### BFS
```java
Queue<Integer> q = new LinkedList<>();
boolean[] visited = new boolean[n];
q.offer(start); visited[start] = true;
while (!q.isEmpty()) {
    int cur = q.poll();
    for (int next : adj.get(cur))
        if (!visited[next]) { visited[next] = true; q.offer(next); }
}
```

### Backtracking
```java
void backtrack(int start, List<Integer> current) {
    if (goalReached()) { result.add(new ArrayList<>(current)); return; }
    for (int i = start; i < n; i++) {
        current.add(nums[i]);
        backtrack(i + 1, current);
        current.remove(current.size() - 1);
    }
}
```

### Union-Find
```java
int find(int x) { return parent[x] == x ? x : (parent[x] = find(parent[x])); }
boolean union(int x, int y) {
    int px = find(x), py = find(y);
    if (px == py) return false;
    if (rank[px] < rank[py]) parent[px] = py;
    else if (rank[px] > rank[py]) parent[py] = px;
    else { parent[py] = px; rank[px]++; }
    return true;
}
```

### Top-K (Min Heap)
```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
for (int num : nums) { pq.offer(num); if (pq.size() > k) pq.poll(); }
return pq.peek(); // kth largest
```

### DP (1D)
```java
int[] dp = new int[n + 1];
dp[0] = base;
for (int i = 1; i <= n; i++) dp[i] = Math.max(dp[i-1], /* recurrence */);
```

### Fenwick Tree
```java
void update(int i, int val) { for (; i <= n; i += i&-i) bit[i] += val; }
int query(int i) { int s = 0; for (; i > 0; i -= i&-i) s += bit[i]; return s; }
```

### KMP
```java
int[] buildLPS(String p) {
    int[] lps = new int[p.length()]; int len = 0, i = 1;
    while (i < p.length()) {
        if (p.charAt(i) == p.charAt(len)) lps[i++] = ++len;
        else if (len != 0) len = lps[len-1];
        else lps[i++] = 0;
    }
    return lps;
}
```

---

## 🗂️ Data Structure Picks

| Need | Use |
|------|-----|
| Fast lookup by key | `HashMap` |
| Sorted order + range | `TreeMap` / `TreeSet` |
| LIFO | `ArrayDeque` as stack |
| FIFO | `LinkedList` / `ArrayDeque` |
| Min/Max element fast | `PriorityQueue` |
| Sliding window max | `ArrayDeque` (monotonic) |
| Prefix search | `Trie` |
| Range queries + updates | `SegmentTree` or `FenwickTree` |
| Find missing in [1..n] | Cyclic Sort |
| Connected components | Union-Find |

---

## 📐 Sorting Reference

| Algorithm | Time | Space | Stable | Use When |
|-----------|------|-------|--------|----------|
| Merge Sort | O(n log n) | O(n) | ✅ | Default, linked list sort |
| Quick Sort | O(n log n) avg | O(log n) | ❌ | In-place, fast avg |
| Heap Sort | O(n log n) | O(1) | ❌ | Space-constrained |
| Counting Sort | O(n+k) | O(k) | ✅ | Small integer range |
| Radix Sort | O(nk) | O(n+k) | ✅ | Fixed-length integers |

---

## 📐 Graph Algorithm Picks

| Problem | Algorithm | Time |
|---------|-----------|------|
| Shortest path (unweighted) | BFS | O(V+E) |
| Shortest path (weighted, no neg) | Dijkstra | O((V+E) log V) |
| Shortest path (negative weights) | Bellman-Ford | O(VE) |
| All-pairs shortest path | Floyd-Warshall | O(V³) |
| Minimum spanning tree | Kruskal / Prim | O(E log E) |
| Topological order | Kahn's BFS | O(V+E) |
| Strongly connected components | Kosaraju | O(V+E) |
| Bridges / Articulation Points | Tarjan | O(V+E) |

---

## ⚠️ Edge Cases Always Check

- Empty input / null / single element
- Negative numbers / integer overflow → use `long`
- Duplicates in backtracking → sort + skip `if (i > start && nums[i] == nums[i-1])`
- Cycle in graph → visited array; Linked List → fast & slow
- Binary search overflow → `lo + (hi - lo) / 2`

---

## 📐 Complexity Targets

| Input Size | Max Acceptable Complexity |
|-----------|--------------------------|
| n ≤ 20 | O(2ⁿ) backtracking/bitmask |
| n ≤ 1000 | O(n²) |
| n ≤ 10⁵ | O(n log n) |
| n ≤ 10⁶ | O(n) or O(n log n) |
| n > 10⁶ | O(n) or O(log n) |
