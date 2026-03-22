# ⚡ DSA Quick Revision — All 22 Topics (Night Before Interview)

> Target: 60–90 min. One read-through. No coding needed.
> Format per topic: What it is → When to use → When NOT to use → Advantage → Tradeoff → Key trick

---

## 1. HashMap & HashSet 🔴 P0

**What**: Hash function maps key → bucket. O(1) avg all ops.
**Use**: Fast lookup, frequency count, two-sum, deduplication, grouping.
**Don't use**: Need sorted order → TreeMap; need O(1) space on sorted data → two pointers.
**Advantage**: Best general-purpose O(1) optimization tool in all of DSA.
**Tradeoff**: O(n) extra space; unordered; O(n) worst case on hash collision.
**Key tricks**:
- `getOrDefault(k, 0) + 1` → frequency count
- `computeIfAbsent(k, fn)` → atomic grouping
- `merge(k, 1, Integer::sum)` → atomic increment
- Prefix sum + HashMap → subarray sum = K in O(n)

---

## 2. Arrays & Prefix Sum 🔴 P0

**What**: Contiguous memory. O(1) access. Prefix sum precomputes cumulative values.
**Use**: Range sum queries, Kadane's max subarray, two-pointer problems on sorted.
**Don't use**: Frequent insert/delete (shifting) → LinkedList.
**Advantage**: Cache-friendly, O(1) random access, O(1) range query after O(n) build.
**Tradeoff**: Fixed size; O(n) insert/delete; prefix sum only works on static arrays.
**Key tricks**:
- `pre[r+1] - pre[l]` → range sum [l..r] in O(1)
- Kadane's: `cur = max(nums[i], cur+nums[i])` → max subarray O(n)
- Dutch National Flag: 3 pointers lo/mid/hi for sort 0s,1s,2s

---

## 3. Two Pointers 🔴 P0

**What**: Two indices traversing from opposite ends or different speeds.
**Use**: Sorted array pair/triplet sum, palindrome, remove duplicates, merge two sorted.
**Don't use**: Unsorted and sorting changes answer; non-contiguous subsequence → DP.
**Advantage**: Reduces O(n²) brute to O(n); O(1) space.
**Tradeoff**: Requires sorted or specific structure; limited problem set.
**Key tricks**:
- Sort first, then squeeze `left++` / `right--` based on sum vs target
- 3Sum: fix one, two-pointer the rest, skip duplicates
- Fast & Slow: cycle detection, middle of list, duplicate detection

---

## 4. Sliding Window 🔴 P0

**What**: Expanding/shrinking window over contiguous elements.
**Use**: Max/min/count in subarray; longest substring with constraint; fixed window average.
**Don't use**: Non-contiguous subsequences → DP; no clear violation/window condition.
**Advantage**: O(n) for what would be O(n²) or worse naively.
**Tradeoff**: Only contiguous; variable window shrink logic is error-prone.
**Key tricks**:
- Fixed window: add right, remove `i-k` when `i >= k`
- Variable window: expand right freely, shrink left while violated
- Window size = `right - left + 1`
- Use HashMap for character frequency windows

---

## 5. Stack & Monotonic Stack 🔴 P0

**What**: LIFO. ArrayDeque preferred over Stack class.
**Use**: Bracket matching; DFS iterative; next greater/smaller element; expression eval.
**Don't use**: Need FIFO → Queue; need random access → Array.
**Advantage**: O(1) push/pop; monotonic stack solves next-greater in O(n) amortized.
**Tradeoff**: O(n) search; stack overflow risk in deep recursion.
**Key tricks**:
- Monotonic decreasing → next GREATER element (pop when `nums[i] > top`)
- Monotonic increasing → next SMALLER element (pop when `nums[i] < top`)
- Each element pushed/popped at most once → O(n) amortized
- For previous greater: iterate right to left

---

## 6. Queue & BFS 🔴 P0

**What**: FIFO. BFS explores level by level.
**Use**: Shortest path unweighted; level order; multi-source spread (rotten oranges).
**Don't use**: Weighted shortest path → Dijkstra; just connectivity → DFS simpler.
**Advantage**: Guarantees shortest path in unweighted graphs.
**Tradeoff**: O(V+E) space; can't handle weighted shortest path.
**Key tricks**:
- Always mark visited **before** adding to queue (not after polling)
- Capture `q.size()` before inner loop for level-order processing
- Multi-source BFS: add ALL sources to queue before starting
- 0-1 BFS: use Deque (addFirst for 0-weight, addLast for 1-weight)

---

## 7. Binary Search 🔴 P0

**What**: Halve search space each step. Requires sorted/monotonic input.
**Use**: Sorted search; first/last occurrence; binary search on answer space.
**Don't use**: Unsorted (sort first); linked list (no random access).
**Advantage**: O(log n) — eliminates half on every step.
**Tradeoff**: Off-by-one errors very common; wrong loop condition causes infinite loop.
**Key tricks**:
- Always `lo + (hi-lo)/2` — never `(lo+hi)/2` (overflow)
- Exact match: `lo <= hi`, update `lo=mid+1` AND `hi=mid-1`
- Left boundary: `hi=mid-1` when found → finds leftmost
- Right boundary: `lo=mid+1` when found → finds rightmost
- Answer space: "find min X where `canAchieve(X)` is true" → `lo < hi` template

---

## 8. Linked List 🔴 P0

**What**: Chain of nodes with pointers. O(n) access, O(1) insert/delete at known node.
**Use**: Frequent insert/delete at head/tail; LRU cache; when order matters.
**Don't use**: Random access → Array; O(1) search → HashMap.
**Advantage**: Dynamic size; O(1) insert/delete at known position; no shifting.
**Tradeoff**: O(n) search; extra pointer memory; poor cache locality.
**Key tricks**:
- Always use **dummy node** — prevents edge cases when head changes
- Save `next` before reversing: `ListNode nxt = cur.next`
- Fast & Slow: fast 2x speed → meet at cycle / slow at middle
- Cycle start: reset one pointer to head, both advance 1x → meet at start
- Nth from end: advance fast by `n+1`, then both move until fast == null

---

## 9. Trees — DFS 🔴 P0

**What**: Recursive traversal. Pre/In/Post order. O(n) time, O(h) space.
**Use**: Path problems; subtree sums; tree DP; BST validation; LCA; serialize.
**Don't use**: Need shortest path → BFS; need level processing → BFS.
**Advantage**: Code mirrors tree structure; naturally recursive.
**Tradeoff**: Stack overflow on very deep trees; O(h) space (can be O(n) for skewed).
**Key tricks**:
- Inorder of BST = sorted sequence
- BST validation: use `min/max` bounds, NOT just parent comparison
- Tree DP: return array `{with, without}` from recursive call
- LCA: if both nodes < root → left; both > → right; else root = LCA
- Level order BFS: capture `size = q.size()` before inner for loop

---

## 10. Heap / Priority Queue 🔴 P0

**What**: Complete binary tree. O(1) peek, O(log n) insert/delete. Min-heap default in Java.
**Use**: Top-K; streaming median; K-way merge; Dijkstra; task scheduling by priority.
**Don't use**: Need sorted output → sort; static data → sort+index; arbitrary search → HashMap.
**Advantage**: O(1) peek min/max; O(n) heapify from existing array (not O(n log n)).
**Tradeoff**: O(n) search for non-top element; not stable; removal of non-top is O(n).
**Key tricks**:
- Top-K largest: **min**-heap of size K — evict smallest
- Top-K smallest: **max**-heap of size K — evict largest
- Two heaps (median): lower=max-heap, upper=min-heap, balance sizes
- K-way merge: min-heap with `{val, listIdx, elemIdx}`, re-offer next from same list
- Heapify = O(n); inserting n elements one-by-one = O(n log n) → always heapify

---

## 11. Recursion & Backtracking 🟠 P1

**What**: Recursion + undo — explore all possibilities, prune invalid branches.
**Use**: ALL combinations/permutations/subsets; N-Queens; Sudoku; word search; CSP.
**Don't use**: Only need ONE optimal value → DP/Greedy; n > 20 without pruning.
**Advantage**: Systematically explores all possibilities; pruning dramatically cuts actual work.
**Tradeoff**: Exponential O(2^n)/O(n!); stack overflow risk; hard to debug.
**Key tricks**:
- **ALWAYS** copy current list when adding to result: `new ArrayList<>(curr)`
- **ALWAYS** undo choice after recursion — it's the most common bug
- Skip duplicates: sort first, then `if (i > start && nums[i] == nums[i-1]) continue`
- Combinations: start from `i+1` next call (no reuse)
- Permutations: use `boolean[] used` array
- Combinations with repeats: start from `i` next call (allow reuse)

---

## 12. Greedy 🔴 P0

**What**: Make locally optimal choice at each step — works only for specific problems.
**Use**: Interval scheduling (sort by end); jump game; meeting rooms; gas station.
**Don't use**: 0/1 Knapsack → DP; when future choices depend on current → DP.
**Advantage**: O(n) or O(n log n); simple; no subproblem storage.
**Tradeoff**: Only correct for specific problem structures; hard to prove; often wrong.
**Key tricks**:
- Interval scheduling: sort by **END** time (not start!) — pick earliest ending
- Jump game: track max reachable, return false if `i > reach`
- Meeting rooms II: sort by start, use min-heap of end times for room count
- Gas station: if tank < 0 at station i, new start = i+1; check total sum ≥ 0
- **Greedy vs DP**: Greedy = irrevocable local choice; DP = explores all

---

## 13. Dynamic Programming 🔴 P0

**What**: Cache overlapping subproblems. Top-down (memo) or bottom-up (tabulation).
**Use**: LCS, LIS, knapsack, edit distance, coin change, counting paths, palindrome partition.
**Don't use**: No overlapping subproblems → D&C; greedy works; need all solutions → backtrack.
**Advantage**: Converts exponential to polynomial by caching repeated subproblems.
**Tradeoff**: O(n²) or O(n·W) space can be large; recurrence derivation is hard.
**Key tricks**:
- Define state → write recurrence → base case → iteration order → answer location
- 1D DP: often space-optimize to O(1) using rolling variables
- 2D DP: fill row by row; sometimes process in reverse
- Interval DP: outer loop = length, inner = start, innermost = split point
- Tree DP: postorder DFS, return subtree result up the call stack
- Bitmask DP: feasible only for n ≤ 20 (2^20 ≈ 1M states)

---

## 14. Graphs — Basic 🔴 P0

**What**: Nodes + edges. Adjacency list for sparse, matrix for dense.
**Key patterns**:
- BFS: shortest path unweighted, mark visited **before** enqueue
- DFS: connectivity, cycle detection, topological sort
- Topo Sort (Kahn's): indegree array → empty result means cycle
- Union-Find: path compression + union by rank = O(α(n)) ≈ O(1)
- Grid DFS: mark `grid[r][c] = '#'` to visit, restore on backtrack

**Critical mistakes**:
- Not marking visited before enqueue → duplicate processing
- Directed cycle detection needs 3 states (unvisited/in-stack/done)
- Topo sort: only valid for DAGs

---

## 15. Advanced Graphs 🟠 P1

| Algorithm | When to use | Key rule |
|-----------|------------|---------|
| Dijkstra | Weighted shortest, no negatives | Skip stale: `if (d > dist[node]) continue` |
| Bellman-Ford | Negative weights | Relax V-1 times; Vth relaxation = negative cycle |
| Floyd-Warshall | All-pairs SP, small V | `dist[i][k]+dist[k][j]`; use `INF/2` not `MAX_VALUE` |
| Kruskal | MST | Sort edges → Union-Find to avoid cycles |
| Prim | MST | Min-heap on nodes, expand greedily |
| Kosaraju | SCC | DFS → finish order → reverse graph → DFS |
| Tarjan | Bridges/APs | `low[v] > disc[u]` = bridge; `>= disc[u]` = AP |

**Advantage of Dijkstra over BFS**: handles weighted edges.
**Advantage of Bellman-Ford over Dijkstra**: handles negative edges, detects negative cycles.
**Advantage of Floyd-Warshall**: simple 3-loop, gives all-pairs in one shot.

---

## 16. Trie 🟠 P1

**What**: Tree where each node = one character. Shared prefixes share nodes.
**Use**: Prefix search, autocomplete, word dictionary with wildcard, IP routing.
**Don't use**: Only exact lookup → HashMap (O(1) vs O(L)); very small dictionary.
**Advantage**: O(L) ops regardless of dictionary size; efficient prefix operations.
**Tradeoff**: High memory (26 children/node); more code than HashMap.
**Key tricks**:
- Use `HashMap<Character, TrieNode>` for non-lowercase or varied charset
- Autocomplete: DFS from prefix node, collect all `isEnd` nodes
- Word Search II: build Trie from words, then DFS grid checking Trie paths
- After finding a word in Word Search II, mark `isEnd=false` to avoid duplicates

---

## 17. Bit Manipulation 🟠 P1

**What**: Operate on bits directly. O(1) operations.
**Use**: Flags/permissions; find single number; enumerate 2^n subsets; bitmask DP.
**Don't use**: n > 64 for bitmask; when readability matters more than micro-optimization.
**Advantage**: O(1) operations; extreme space efficiency for flag/subset problems.
**Tradeoff**: Hard to debug; Java sign issues (use `>>>` not `>>`); bitmask DP capped at n=20.
**Key tricks**:
- `n & (n-1)` → clear lowest set bit; `n>0 && (n&(n-1))==0` → isPowerOf2
- `a ^ a = 0`, `a ^ 0 = a` → XOR pairs cancel → find single number
- `n & (-n)` → isolate lowest set bit
- `(n >> i) & 1` → check bit i (use `>>>` for unsigned)
- Enumerate subsets: `for(int m=0; m<(1<<n); m++)` → all 2^n combinations

---

## 18. Math & Number Theory 🟠 P1

**What**: Mathematical foundations for algorithm problems.
**Key patterns**:
- GCD: `gcd(a,b) = gcd(b, a%b)` — O(log min(a,b))
- LCM: `a/gcd(a,b)*b` — divide FIRST to avoid overflow
- isPrime: check up to `√n` only — O(√n)
- Sieve: mark composites from `i*i` (not `2*i`) — O(n log log n)
- Fast power: `base^exp % mod` via squaring — O(log exp)
- Mod subtraction: always `(a - b + MOD) % MOD` to avoid negative

**Advantage**: Reduces complex problems to simple formulas.
**Tradeoff**: Problem-specific; hard to recognize pattern without practice.

---

## 19. String Algorithms 🟠 P1

| Algorithm | Use | Advantage | Tradeoff |
|-----------|-----|-----------|---------|
| KMP | Exact pattern matching | O(n+m), no backtrack | LPS build tricky |
| Rabin-Karp | Rolling hash, multi-pattern | O(n+m) avg, multiple patterns | Collision → always verify |
| Z-Algorithm | Pattern matching | Simpler than KMP | Less known |
| Manacher's | Longest palindrome O(n) | Optimal, handles even/odd | # transform complex |
| String Hashing | O(1) substring compare | Fast range compare | Collision possible |

**Key tricks**:
- KMP LPS: when mismatch with `len > 0`, set `len = lps[len-1]` (don't reset to 0)
- Rabin-Karp: divide first char's contribution using `power[m-1]`
- Manacher's: transform `"abc" → "#a#b#c#"` to handle even palindromes
- Z-algo pattern search: concatenate `pattern + "$" + text`, Z[i]==m → match

---

## 20. Segment Tree & Fenwick Tree 🟡 P2

**Segment Tree**: Range queries (sum/min/max) + point/range updates. O(log n) each. O(4n) space.
**Fenwick Tree**: Prefix sums + point updates. Simpler code. O(log n) each. O(n) space. **1-indexed always.**

**Use Segment Tree when**: Range updates needed; need range min/max; complex queries.
**Use Fenwick Tree when**: Only prefix sums + point updates; need simpler code.
**Advantage**: Both O(log n) — much better than O(n) naive.
**Tradeoff**: Segment Tree: 4n space, complex lazy propagation. Fenwick: no range min/max.

**Key tricks**:
- Fenwick: `i += i&(-i)` to update; `i -= i&(-i)` to query
- Segment Tree: `4*n` array size (never `2*n`)
- Lazy propagation: defer range updates, propagate only when queried

---

## 21. Cyclic Sort / Boyer-Moore / Reservoir / Floyd 🟡 P2

**Cyclic Sort**: Values in [1..n] → place at index `val-1` → O(n) O(1) find missing/duplicate.
**Boyer-Moore**: Majority element (>n/2) in O(n) O(1) space. **Always verify the candidate.**
**Reservoir Sampling**: K uniform random samples from stream of unknown size. Random index in [0,i].
**Fisher-Yates**: Uniform shuffle — iterate backwards, swap with random [0..i].
**Floyd's Cycle (Duplicate)**: Treat array as linked list → two-phase (fast/slow then reset one).

**Key tricks**:
- Boyer-Moore: counter hits 0 → new candidate, not necessarily majority — VERIFY
- Reservoir: element i included with probability k/i — proven uniform
- Floyd's: Phase 1 finds intersection; Phase 2 reset one to start, both 1x → cycle entrance

---

## 22. Balanced Trees, Suffix Arrays, A*, Randomised, Flow 🟡 P2

**AVL vs Red-Black**: AVL = stricter balance → faster reads. RB = fewer rotations → faster writes. Java uses RB.
**B-Tree/B+ Tree**: Disk-optimised. B+ links leaves → efficient range scans. Used in MySQL/PostgreSQL.
**Skip List**: Probabilistic O(log n). Redis sorted sets. Easier concurrent implementation than BST.
**2-3 Tree**: Conceptual basis for Red-Black trees. 2-node=1 key, 3-node=2 keys. All leaves same level.
**Suffix Array**: Sort all suffixes. LCP array for distinct substring count. O(n log n) build, O(log n) search.
**A\***: `f = g + h`. Heuristic must be admissible (never overestimate). Faster than Dijkstra single-target.
**Bloom Filter**: False positives possible, never false negatives. O(1) check. Redis, Cassandra, Chrome.
**Randomised QS**: Randomise pivot → avoids O(n²) worst case. Expected O(n log n).
**Network Flow**: Max-flow = Min-cut. Ford-Fulkerson with BFS = Edmonds-Karp O(VE²). Bipartite matching.
**Multi-threaded**: ConcurrentHashMap > Collections.synchronizedMap. `compute()` > `get()+put()`. `volatile` for double-checked locking.

---

## 🎯 Pattern Selection Decision Tree

```
Is input sorted or can be sorted?
├── Yes → Two Pointers or Binary Search
└── No
    ├── Contiguous subarray/substring?
    │   ├── Yes → Sliding Window
    │   └── No → HashMap for lookups
    │
    ├── Need shortest path?
    │   ├── Unweighted → BFS
    │   ├── Weighted (no neg) → Dijkstra
    │   └── Negative weights → Bellman-Ford
    │
    ├── Need ALL solutions?
    │   └── Backtracking
    │
    ├── Need OPTIMAL single value?
    │   ├── Locally optimal = globally optimal → Greedy
    │   └── Overlapping subproblems → DP
    │
    ├── Tree problem?
    │   ├── Path/subtree → DFS
    │   └── Level/shortest → BFS
    │
    └── Priority / K-th element?
        └── Heap
```

---

## 🎯 Interview Strategy — 6 Steps

1. **Clarify** — input type, size, constraints, edge cases (2 min, don't skip)
2. **Brute force** — state it even if not coding: "naive is O(n²) because..."
3. **Optimize** — identify bottleneck, name the pattern: "sliding window reduces to O(n)"
4. **Code** — clean variable names, no magic numbers, talk through logic
5. **Test** — trace through 1–2 examples + edge cases (empty, single, all same)
6. **Complexity** — always state time AND space with justification

---

## ⚠️ Top 10 Interview Bugs

1. `(lo + hi) / 2` → overflow → use `lo + (hi-lo)/2`
2. Missing `if (d > dist[node]) continue` in Dijkstra → processes stale entries
3. `result.add(curr)` in backtracking → adds reference, not copy → use `new ArrayList<>(curr)`
4. Forgetting to undo in backtracking → corrupts state
5. Not marking visited before enqueue in BFS → duplicate processing
6. `int` overflow in multiplication → cast to `long`
7. `==` instead of `.equals()` for String/Integer comparison
8. BST validation using only parent value → use `min/max` bounds
9. Modular arithmetic without `+MOD`: `(a-b) % MOD` can be negative → `(a-b+MOD) % MOD`
10. Fenwick Tree using 0-index → always 1-indexed, shift all inputs by +1
