# ⚡ DSA Quick Revision — Complete (Read Night Before Interview)

> Target: 60–90 minutes. One read-through — no coding needed.

---

## 1. Arrays & Strings
- Contiguous memory, O(1) access, O(n) insert/delete
- **Prefix sum**: precompute for O(1) range queries
- **Kadane's**: max subarray — track curSum, reset when negative
- **Frequency map**: anagram, duplicates — use `int[26]` or HashMap
- **Two sum**: sort → two pointers OR HashMap for O(n)

---

## 2. Sliding Window & Two Pointers
- **Fixed window**: move both ends together, subtract left on exit
- **Variable window**: expand right freely, shrink left on violation
- **Two pointers**: works on sorted arrays — opposite ends for pair sum
- Key: window size = `right - left + 1`

---

## 3. Stack & Queue
- **Stack (LIFO)**: brackets, undo, DFS — use `ArrayDeque`
- **Queue (FIFO)**: BFS, level order — use `LinkedList`
- **Monotonic stack**: next greater → decreasing stack; next smaller → increasing
- **Deque**: sliding window max — remove from front if outside, back if smaller

---

## 4. Binary Search
- Requires sorted or monotonically changing space
- Always: `mid = lo + (hi - lo) / 2`
- Exact match: `lo <= hi`, update both `lo = mid+1` and `hi = mid-1`
- Left boundary: keep `hi = mid-1` when found; Right boundary: keep `lo = mid+1`
- **Binary search on answer**: find min X where `canAchieve(X)` is true

---

## 5. Linked List
- **Reverse**: three pointers (prev, cur, next) — save next before reversing
- **Cycle**: fast (2x) & slow (1x) — meet = cycle; reset one to head for start
- **Middle**: fast & slow — slow stops at middle
- **Dummy node**: prevents edge cases when head changes
- **Nth from end**: fast moves n+1 ahead first

---

## 6. Trees & BST
- **Preorder** (root→L→R): copy/serialize tree
- **Inorder** (L→root→R): sorted order in BST
- **Postorder** (L→R→root): delete tree, compute subtree
- **BFS**: level order — capture `q.size()` before inner loop
- **BST validate**: use `min/max` bounds, not just parent comparison
- **LCA**: if both nodes < root → left; both > root → right; else root

---

## 7. Graphs
- **BFS**: shortest path unweighted, level order — mark visited before enqueue
- **DFS**: connected components, cycle, topological sort
- **Topo sort (Kahn's)**: indegree array + queue; empty result = cycle exists
- **Union-Find**: path compression + union by rank = O(α(n)) ≈ O(1)
- **Grid DFS**: mark cell visited by setting to '#' or 0; restore on backtrack

---

## 8. Advanced Graphs
- **Dijkstra**: min-heap, skip stale entries — fails with negative weights
- **Bellman-Ford**: relax V-1 times; Vth relaxation = negative cycle
- **Floyd-Warshall**: `dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])`
- **Kruskal MST**: sort edges → Union-Find to avoid cycles
- **Prim MST**: min-heap on nodes, expand greedily
- **Kosaraju SCC**: DFS → finish order → reverse graph → DFS again
- **Tarjan Bridges**: `low[v] > disc[u]` = bridge; `low[v] >= disc[u]` = articulation point

---

## 9. Heap & Priority Queue
- **Min-heap default** in Java `PriorityQueue` — use `Collections.reverseOrder()` for max
- **Heapify**: O(n) — build from array starting at `n/2-1` sifting down
- **Top-K**: min-heap of size K — evict smallest to keep K largest
- **K-way merge**: min-heap with `{value, listIdx, elemIdx}` — re-offer next from same list
- **Two heaps (median)**: lower max-heap, upper min-heap — balance sizes, read from lower.peek()
- **Task scheduler**: max-heap + cooldown queue

---

## 10. Recursion & Backtracking
- **Base case first** — always define it before recursive case
- **Backtrack template**: make choice → recurse → undo choice
- **Copy on add**: `result.add(new ArrayList<>(current))` — not reference
- **Duplicates**: sort first, skip `if (i > start && nums[i] == nums[i-1])`
- **Prune early**: add constraint checks BEFORE recursing

---

## 11. Greedy
- Only works when local optimal = global optimal (prove with exchange argument)
- **Interval scheduling**: sort by END time, pick earliest ending
- **Jump game**: track `maxReach`, return false if `i > maxReach`
- **Meeting rooms**: sort by start, use min-heap of end times for room count
- **Gas station**: if tank < 0, reset start; if total ≥ 0, solution exists

---

## 12. Dynamic Programming
- **Identify**: optimal + overlapping subproblems
- **Steps**: define state → recurrence → base case → iterate → answer location
- **1D DP**: climbing stairs, house robber, coin change, LIS
- **2D DP**: knapsack, LCS, edit distance, unique paths
- **Interval DP**: iterate by length (outer), then start, then split point
- **Tree DP**: bottom-up via DFS, return pair `{withRob, withoutRob}`
- **Bitmask DP**: feasible for n ≤ 20 (2^n states)

---

## 13. Trie
- Each node: `TrieNode[26] children` + `boolean isEnd`
- Use `HashMap<Character, TrieNode>` for arbitrary characters
- **Insert/Search/StartsWith**: all O(L) where L = word length
- **Autocomplete**: DFS from prefix node, collect all words
- **Word Search II**: build trie from word list, then backtrack on grid

---

## 14. Bit Manipulation
- `n & (n-1)` clears lowest set bit → count bits, check power of 2
- `n & (-n)` isolates lowest set bit
- `a ^ a = 0`, `a ^ 0 = a` → XOR pairs cancel → find single number
- `1 << i` checks/sets/toggles bit i
- Enumerate all subsets: `for (mask = 0; mask < (1<<n); mask++)`
- Java: use `>>>` for unsigned right shift, `long` for bit 31+

---

## 15. Math & Number Theory
- **GCD**: `gcd(a,b) = gcd(b, a%b)` — O(log min(a,b))
- **LCM**: `a / gcd(a,b) * b` — divide first to avoid overflow
- **Sieve**: mark composites from `i*i`, not `2*i`
- **Is prime**: check up to `√n` only
- **Fast power**: `base^exp % mod` in O(log exp) via squaring
- **Mod subtraction**: always `(a - b + MOD) % MOD`

---

## 16. String Algorithms
- **KMP**: build LPS array, use it to skip re-checks → O(n+m)
- **Rabin-Karp**: rolling hash → O(n+m) avg, verify on hash match
- **Manacher's**: transform with `#`, use mirror property → O(n) palindrome
- **Z-algorithm**: Z[i] = longest prefix match at i → concatenate `pattern$text`
- **String hashing**: O(1) substring comparison after O(n) build

---

## 17. Segment Tree & Fenwick Tree
- **Segment Tree**: range queries (sum/min/max) + updates — O(log n) each, O(4n) space
- **Fenwick (BIT)**: prefix sums + point updates — simpler, O(log n), O(n) space
- **Fenwick is 1-indexed** always
- **Lazy propagation**: defer range updates — propagate only when queried

---

## 18. Interview Patterns
- **Cyclic Sort**: values in [1..n] → place at index `val-1` → scan for mismatch
- **Boyer-Moore**: majority (>n/2) in O(1) space — always verify candidate
- **Reservoir Sampling**: K samples from stream of unknown size — uniform probability
- **Fisher-Yates**: uniform shuffle — iterate backwards, swap with random [0,i]
- **Floyd's Cycle**: find duplicate = cycle entrance — two-phase (fast+slow, then reset one)

---

## 🎯 Interview Strategy (6 Steps)
1. **Clarify** — input type, size, constraints, edge cases (2 min)
2. **Brute force** — state it even if not coding it
3. **Optimize** — identify bottleneck, name the pattern
4. **Code** — clean, meaningful variable names
5. **Test** — trace through 1-2 examples + edge cases
6. **Complexity** — always state time & space at end
