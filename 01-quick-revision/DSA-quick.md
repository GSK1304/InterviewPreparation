# ⚡ DSA Quick Revision — Read the Night Before

> Target: 45 minutes. One read-through, no coding needed.

---

## 1. Arrays & Strings

- **Array**: contiguous memory, O(1) access by index
- **Key ops**: traversal O(n), search O(n), insert/delete O(n) due to shifting
- **String tricks**: use `char[]` for mutation in Java, `StringBuilder` for concatenation
- **Must know**: prefix sum, frequency map with HashMap

**Top patterns:**
- Find duplicates → HashSet
- Anagram check → frequency array of size 26
- Reverse / rotate → two pointers

---

## 2. Sliding Window & Two Pointers

- **Sliding Window**: for contiguous subarray/substring problems
  - Fixed size → move both ends together
  - Variable size → expand right, shrink left on violation
- **Two Pointers**: works on **sorted** arrays or when searching pairs

**Key questions to ask yourself:**
- Is it asking for subarray/substring? → Sliding Window
- Is array sorted and asking for pairs? → Two Pointers

---

## 3. Stack & Queue

- **Stack** (LIFO): matching brackets, undo operations, DFS
- **Queue** (FIFO): BFS, level-order traversal, scheduling
- **Monotonic Stack**: next greater element, stock span
  - Decreasing stack → next greater element
  - Increasing stack → next smaller element
- **Deque**: sliding window maximum (use as monotonic queue)

---

## 4. Binary Search

- Works only on **sorted** or **monotonic** space
- Always use `mid = lo + (hi - lo) / 2` to avoid overflow
- Template: `lo <= hi` with `lo = mid+1` / `hi = mid-1`
- **Variants**: first/last occurrence, search in rotated array, find peak

**Think binary search when:** "find minimum X such that condition is true"

---

## 5. Linked List

- No random access — must traverse
- **Cycle detection**: fast (2x) & slow (1x) pointer — meet = cycle
- **Middle node**: fast & slow pointer — slow stops at middle
- **Reverse**: three pointers (prev, cur, next)
- **Merge two sorted**: compare heads, recurse or iterate

---

## 6. Trees & BST

- **DFS on tree**: preorder (root→L→R), inorder (L→root→R), postorder (L→R→root)
- **BFS on tree**: level-order using Queue
- **BST property**: inorder traversal gives sorted order
- **Height**: `1 + max(height(left), height(right))`
- **LCA**: if both nodes are less → go left, if both greater → go right

---

## 7. Graphs

- **Representations**: adjacency list (sparse, preferred) vs matrix (dense)
- **BFS**: shortest path in unweighted graph, level order
- **DFS**: connected components, cycle detection, topological sort
- **Visited array**: always maintain to avoid infinite loops
- **Topological Sort**: Kahn's algorithm (BFS with indegree) or DFS with stack

---

## 8. Recursion & Backtracking

- **Recursion**: base case + recursive case
- **Backtracking** = recursion + undo choice after exploring
- Template:
  ```
  backtrack(state):
    if done: add to result, return
    for each choice:
      make choice
      backtrack(next state)
      undo choice
  ```
- Used for: subsets, permutations, combinations, N-Queens, Sudoku

---

## 9. Dynamic Programming

- **Identify DP**: optimal answer + overlapping subproblems
- **Steps**: define state → write recurrence → base case → iterate
- **Top-down** (memoization): recursion + cache
- **Bottom-up** (tabulation): fill dp array iteratively

**Classic problems:**
| Problem | State | Recurrence |
|---------|-------|------------|
| Fibonacci | dp[i] | dp[i-1] + dp[i-2] |
| 0/1 Knapsack | dp[i][w] | max(dp[i-1][w], dp[i-1][w-wt]+val) |
| LCS | dp[i][j] | if match: dp[i-1][j-1]+1 else max(dp[i-1][j], dp[i][j-1]) |
| Coin Change | dp[i] | min(dp[i], dp[i-coin]+1) |

---

## 10. Sorting & Searching

| Algorithm | Time | Space | Stable? | When to use |
|-----------|------|-------|---------|-------------|
| Merge Sort | O(n log n) | O(n) | Yes | Default safe choice |
| Quick Sort | O(n log n) avg | O(log n) | No | In-place, fast avg |
| Heap Sort | O(n log n) | O(1) | No | When space matters |
| Counting Sort | O(n+k) | O(k) | Yes | Small integer range |

- **Java**: `Arrays.sort()` uses dual-pivot quicksort for primitives, merge sort for objects
- **Custom sort**: `Arrays.sort(arr, (a,b) -> a[0] - b[0])`

---

## 🎯 Interview Strategy

1. **Clarify** inputs, constraints, edge cases (2 min)
2. **Brute force** first — state it even if not coding it
3. **Optimize** — identify bottleneck, apply pattern
4. **Code** — clean, readable, with variable names
5. **Test** — trace through 1-2 examples including edge cases
6. **Complexity** — always state time & space at the end
