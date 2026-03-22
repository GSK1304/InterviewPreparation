# 🎯 DSA Topic Priority Guide — Interview Frequency & Importance

> Based on analysis of FAANG / MANGA interview patterns, roadmap.sh, AlgoMap.io, GeeksForGeeks SDE Sheet, and competitive programming roadmaps.

---

## 🏷️ Priority Levels

| Level | Label | Meaning |
|-------|-------|---------|
| 🔴 P0 | **Must Know** | Asked in almost every interview. Cannot skip. |
| 🟠 P1 | **High Priority** | Frequently tested at senior/lead level. Cover before interviews. |
| 🟡 P2 | **Medium Priority** | Asked at FAANG/top product companies. Good differentiator. |
| 🟢 P3 | **Advanced** | Rare in interviews, common in competitive programming. Know concept. |
| ⚫ P4 | **Expert / CP Only** | Almost never asked in interviews. Only for competitive programming. |

---

## 📊 Complete DSA Topic Priority Table

### 🗂️ Core Data Structures

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| Arrays | 🔴 P0 | Every interview | Foundation of everything |
| Strings | 🔴 P0 | Every interview | Often combined with arrays |
| HashMap / HashSet | 🔴 P0 | Every interview | Most common optimization trick |
| Stack | 🔴 P0 | Very high | Brackets, DFS, monotonic |
| Queue | 🔴 P0 | Very high | BFS, scheduling |
| Linked List | 🔴 P0 | High | Reversal, cycle, merge |
| Binary Tree | 🔴 P0 | Very high | DFS/BFS traversals |
| Binary Search Tree | 🔴 P0 | High | Inorder = sorted, validation |
| Heap / Priority Queue | 🔴 P0 | High | Top-K, Dijkstra, merge K |
| Deque | 🟠 P1 | Medium-High | Sliding window max, monotonic |
| Trie | 🟠 P1 | Medium-High | Prefix search, autocomplete |
| Graph (adjacency list) | 🔴 P0 | Very high | BFS/DFS, shortest path |
| **AVL Tree** | 🟡 P2 | Medium | Concept only — know rotations, not implementation |
| **Red-Black Tree** | 🟡 P2 | Low-Medium | Concept — Java `TreeMap` uses it internally |
| **B-Tree / B+ Tree** | 🟡 P2 | Medium (system design) | DB indexing — asked in HLD/LLD rounds |
| **Skip List** | 🟢 P3 | Low | Know concept, Redis uses it |
| **Suffix Array** | 🟢 P3 | Low-Medium | String heavy companies (Google) |
| **Suffix Tree** | 🟢 P3 | Low | Know concept, rarely implemented |
| Segment Tree | 🟡 P2 | Medium | Range queries + updates |
| Fenwick Tree / BIT | 🟡 P2 | Medium | Prefix sums with updates |
| Disjoint Set / Union-Find | 🟠 P1 | Medium-High | Connected components, Kruskal |
| **Sparse Table** | 🟢 P3 | Low | Range min/max queries O(1) |

---

### 🔍 Searching & Sorting

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| Binary Search (standard) | 🔴 P0 | Very high | Must code in sleep |
| Binary Search on Answer | 🔴 P0 | High | "Minimize max / maximize min" |
| Merge Sort | 🟠 P1 | Medium | Divide & conquer, stable sort |
| Quick Sort / QuickSelect | 🟠 P1 | Medium | In-place, Kth element |
| Heap Sort | 🟡 P2 | Low-Medium | Know concept |
| Counting / Radix Sort | 🟡 P2 | Low-Medium | When constraints allow O(n) |
| Bubble / Insertion / Selection | 🟢 P3 | Low | Theory only |

---

### 🧩 Algorithm Patterns

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| Two Pointers | 🔴 P0 | Very high | Sorted arrays, palindromes |
| Sliding Window | 🔴 P0 | Very high | Subarray/substring problems |
| Fast & Slow Pointers | 🔴 P0 | High | Cycle, middle, duplicate |
| Prefix Sum | 🔴 P0 | Very high | Range sum in O(1) |
| Cyclic Sort | 🟠 P1 | Medium | Missing/duplicate in [1..n] |
| Monotonic Stack | 🟠 P1 | Medium-High | Next greater/smaller |
| Monotonic Deque | 🟠 P1 | Medium | Sliding window max/min |
| Boyer-Moore Voting | 🟡 P2 | Medium | Majority element O(1) space |
| Reservoir Sampling | 🟡 P2 | Medium | Random sample from stream |
| Fisher-Yates Shuffle | 🟡 P2 | Medium | Uniform random shuffle |
| **Randomised Algorithms** | 🟡 P2 | Medium | QuickSelect, randomised hashing |
| **A\* Algorithm** | 🟡 P2 | Medium | Pathfinding, game dev, maps |
| **Line Sweep** | 🟢 P3 | Low | Geometry, interval problems |
| **Meet in the Middle** | 🟢 P3 | Low | Splits search space for large n |

---

### 🌳 Tree Algorithms

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| DFS (Pre/In/Post order) | 🔴 P0 | Every interview | Must know all 3 |
| BFS / Level Order | 🔴 P0 | Very high | Level order, shortest path |
| LCA (Lowest Common Ancestor) | 🔴 P0 | High | Classic tree problem |
| Tree DP | 🟠 P1 | Medium-High | House Robber III, max path sum |
| Serialize / Deserialize Tree | 🟠 P1 | Medium | Design round classic |
| **AVL Rotations** | 🟡 P2 | Medium | Concept — LL/RR/LR/RL rotations |
| **Heavy-Light Decomposition** | ⚫ P4 | Very rare | Competitive programming |

---

### 📈 Graph Algorithms

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| BFS (shortest path unweighted) | 🔴 P0 | Very high | Grid + graph problems |
| DFS (components, cycle) | 🔴 P0 | Very high | Islands, connected components |
| Topological Sort (Kahn's) | 🔴 P0 | High | Course schedule, dependencies |
| Union-Find | 🟠 P1 | Medium-High | Kruskal, connected components |
| Dijkstra's | 🟠 P1 | Medium-High | Weighted shortest path |
| Bellman-Ford | 🟡 P2 | Medium | Negative weights |
| Floyd-Warshall | 🟡 P2 | Medium | All-pairs shortest path |
| Prim's / Kruskal's MST | 🟡 P2 | Medium | Minimum spanning tree |
| Kosaraju's SCC | 🟡 P2 | Medium | Strongly connected components |
| Tarjan's (Bridges/APs) | 🟡 P2 | Medium | Critical connections |
| **A\* Search** | 🟡 P2 | Medium | Heuristic pathfinding |
| **Network Flow (Ford-Fulkerson)** | 🟢 P3 | Low | FAANG hard level |
| **Eulerian Path / Circuit** | 🟢 P3 | Low | Reconstruct itinerary |
| **Bipartite Matching** | 🟢 P3 | Low | Assignment problems |

---

### 🧠 Dynamic Programming

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| 1D DP (Fibonacci, Stairs, Robber) | 🔴 P0 | Very high | Starting point |
| 2D DP (Grid, Knapsack, LCS) | 🔴 P0 | Very high | Most common DP |
| DP on Strings (Edit dist, Palindrome) | 🟠 P1 | High | Hard but common |
| DP on Trees | 🟠 P1 | Medium-High | Bottom-up DFS |
| DP on Intervals | 🟠 P1 | Medium | Burst balloons, matrix chain |
| Bitmask DP | 🟡 P2 | Medium | TSP, team assignment |
| Digit DP | 🟡 P2 | Medium | Count numbers with property |
| **Probability DP** | 🟢 P3 | Low | Game theory, expected value |
| **Divide & Conquer DP** | 🟢 P3 | Low | Optimization for interval DP |
| **Knuth's DP Optimization** | ⚫ P4 | Very rare | CP only |

---

### 🔤 String Algorithms

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| String Hashing | 🟠 P1 | Medium-High | O(1) substring compare |
| KMP | 🟠 P1 | Medium-High | Pattern matching O(n+m) |
| Z-Algorithm | 🟠 P1 | Medium | Pattern matching alternative |
| Rabin-Karp | 🟠 P1 | Medium | Rolling hash, multi-pattern |
| Manacher's | 🟡 P2 | Medium | Longest palindrome O(n) |
| **Suffix Array** | 🟡 P2 | Medium | String-heavy company interviews |
| **Aho-Corasick** | 🟢 P3 | Low | Multi-pattern matching |
| **Suffix Automaton** | ⚫ P4 | Very rare | CP only |

---

### 🔢 Math & Bit Manipulation

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| Bit Manipulation (basic) | 🔴 P0 | High | XOR, AND, OR tricks |
| Bitmask Enumeration | 🟠 P1 | Medium-High | Subsets via bits |
| GCD / LCM | 🔴 P0 | High | Euclidean algorithm |
| Sieve of Eratosthenes | 🟠 P1 | Medium-High | All primes up to N |
| Fast Power | 🟠 P1 | Medium-High | Modular exponentiation |
| Modular Arithmetic | 🟠 P1 | Medium-High | (a+b)%m patterns |
| Combinatorics (nCr) | 🟡 P2 | Medium | Pascal's triangle |
| **Randomised Hashing** | 🟡 P2 | Medium | Monte Carlo approach |
| **Number Theory (advanced)** | 🟢 P3 | Low | CRT, Euler's totient |
| **Game Theory (Sprague-Grundy)** | ⚫ P4 | Very rare | CP only |
| **Geometric Algorithms** | 🟢 P3 | Low | Convex hull, line intersection |

---

### 🔁 Recursion & Backtracking

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| Basic Recursion | 🔴 P0 | Very high | Foundation |
| Subsets / Combinations | 🔴 P0 | High | Classic backtracking |
| Permutations | 🔴 P0 | High | With/without duplicates |
| N-Queens / Sudoku | 🟠 P1 | Medium-High | Classic hard backtracking |
| Word Search on Grid | 🟠 P1 | Medium-High | DFS + backtracking |

---

### 🟡 Greedy Algorithms

| Topic | Priority | Interview Frequency | Notes |
|-------|----------|-------------------|-------|
| Interval Scheduling | 🔴 P0 | High | Sort by end time |
| Jump Game | 🔴 P0 | High | Classic greedy |
| Meeting Rooms | 🟠 P1 | Medium-High | Min rooms = max overlap |
| Gas Station | 🟠 P1 | Medium | Circular tour |
| Huffman Encoding | 🟡 P2 | Medium | Concept for compression |
| **Fractional Knapsack** | 🟡 P2 | Medium | vs 0/1 Knapsack comparison |

---

## 🗓️ Suggested Study Order by Priority

### Week 1–2 — P0 (Must Know)
Arrays, Strings, HashMap/Set, Stack, Queue, Linked List, Binary Search, Two Pointers, Sliding Window, Prefix Sum, Basic Recursion, Subsets/Permutations, Binary Tree (all traversals), BST, Heap, BFS, DFS, Topological Sort, 1D/2D DP, Bit Manipulation basics, GCD/LCM, Interval Scheduling, Jump Game

### Week 3–4 — P1 (High Priority)
Trie, Union-Find, Monotonic Stack/Deque, Cyclic Sort, Fast & Slow Pointers, Tree DP, Serialize/Deserialize, Dijkstra's, KMP, Z-Algorithm, Rabin-Karp, String Hashing, Sieve, Fast Power, Modular Arithmetic, Bitmask Enumeration, DP on Strings/Trees/Intervals, Deque patterns, Meeting Rooms, Gas Station, Merge Sort, QuickSelect

### Week 5–6 — P2 (Good Differentiator)
AVL Trees (concept), Red-Black Trees (concept), B-Trees (for HLD), Suffix Arrays, Manacher's, Bellman-Ford, Floyd-Warshall, MST (Prim/Kruskal), Kosaraju SCC, Tarjan Bridges, A\* Algorithm, Boyer-Moore, Reservoir Sampling, Bitmask DP, Digit DP, Segment Tree, Fenwick Tree, Combinatorics (nCr), Randomised Algorithms

### Week 7+ — P3/P4 (Competitive / Differentiating at FAANG)
Skip List, Suffix Tree, Aho-Corasick, Network Flow, Heavy-Light Decomposition, Geometry, Game Theory, Line Sweep, Divide & Conquer DP

---

## 📌 Priority by Interview Type

| Interview Type | Focus Areas |
|---------------|-------------|
| **Startup / Product company** | P0 only — Arrays, Trees, Graphs, DP, Sorting |
| **Senior / Lead Engineer (non-FAANG)** | P0 + P1 — All of above + Tries, Dijkstra, DP patterns |
| **FAANG / MANGA** | P0 + P1 + P2 — Full coverage including Segment Trees, SCC, Suffix Arrays |
| **Competitive Programming** | P0 + P1 + P2 + P3 + P4 — Everything |
| **Technical Manager / EM** | P0 + P1 — Focus on patterns & system thinking over implementation depth |

> 💡 **For Joy (Technical Manager targeting both IC and EM roles):**
> Master **P0 fully + P1 thoroughly + P2 conceptually**. For EM rounds, focus on articulating trade-offs and system thinking rather than perfect implementation.
