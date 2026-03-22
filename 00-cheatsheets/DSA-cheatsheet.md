# 📋 DSA Cheatsheet — Quick Reference

---

## ⏱️ Time Complexity at a Glance

| Structure / Algo | Access | Search | Insert | Delete |
|-----------------|--------|--------|--------|--------|
| Array | O(1) | O(n) | O(n) | O(n) |
| LinkedList | O(n) | O(n) | O(1) | O(1) |
| HashMap | — | O(1) | O(1) | O(1) |
| Stack / Queue | — | O(n) | O(1) | O(1) |
| Binary Search Tree | O(log n) | O(log n) | O(log n) | O(log n) |
| Heap (PriorityQueue) | O(1) top | O(n) | O(log n) | O(log n) |
| Binary Search | — | O(log n) | — | — |

---

## 🔑 Pattern → When to Use

| Pattern | Trigger Words | Example |
|---------|--------------|---------|
| **Sliding Window** | subarray, substring, window of size k | Max sum subarray of size k |
| **Two Pointers** | sorted array, pair sum, palindrome | Two sum in sorted array |
| **Fast & Slow Pointers** | cycle detection, middle of list | Detect cycle in linked list |
| **Binary Search** | sorted, find position, minimize/maximize | Search in rotated array |
| **BFS** | shortest path, level order, nearest | Shortest path in grid |
| **DFS** | all paths, connected components, islands | Number of islands |
| **Dynamic Programming** | optimal substructure, overlapping subproblems | Longest common subsequence |
| **Backtracking** | all combinations, permutations, subsets | Generate all permutations |
| **Monotonic Stack** | next greater/smaller element | Stock span problem |
| **Heap** | top K elements, streaming median | K largest elements |

---

## 🧩 Common Patterns — Code Templates

### Sliding Window (Fixed Size)
```java
int max = 0, sum = 0;
for (int i = 0; i < nums.length; i++) {
    sum += nums[i];
    if (i >= k) sum -= nums[i - k];
    if (i >= k - 1) max = Math.max(max, sum);
}
```

### Two Pointers
```java
int left = 0, right = nums.length - 1;
while (left < right) {
    if (nums[left] + nums[right] == target) return true;
    else if (nums[left] + nums[right] < target) left++;
    else right--;
}
```

### Binary Search
```java
int lo = 0, hi = nums.length - 1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (nums[mid] == target) return mid;
    else if (nums[mid] < target) lo = mid + 1;
    else hi = mid - 1;
}
```

### BFS (Graph / Grid)
```java
Queue<int[]> q = new LinkedList<>();
q.add(new int[]{startR, startC});
visited[startR][startC] = true;
int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
while (!q.isEmpty()) {
    int[] cur = q.poll();
    for (int[] d : dirs) {
        int nr = cur[0]+d[0], nc = cur[1]+d[1];
        if (valid(nr,nc) && !visited[nr][nc]) {
            visited[nr][nc] = true;
            q.add(new int[]{nr, nc});
        }
    }
}
```

### DFS (Recursive)
```java
void dfs(int node, boolean[] visited, List<List<Integer>> adj) {
    visited[node] = true;
    for (int neighbor : adj.get(node)) {
        if (!visited[neighbor]) dfs(neighbor, visited, adj);
    }
}
```

### DP (1D)
```java
int[] dp = new int[n + 1];
dp[0] = base;
for (int i = 1; i <= n; i++) {
    dp[i] = Math.max(dp[i-1], /* recurrence */);
}
```

---

## 🗂️ Data Structure Picks

| Need | Use |
|------|-----|
| Fast lookup by key | `HashMap` |
| Unique elements | `HashSet` |
| Sorted order | `TreeMap` / `TreeSet` |
| LIFO | `Stack` / `Deque` |
| FIFO | `Queue` / `LinkedList` |
| Min/Max element fast | `PriorityQueue` |
| Sliding window max | `ArrayDeque` (monotonic) |

---

## ⚠️ Edge Cases to Always Check

- Empty input / null
- Single element
- All same elements
- Negative numbers
- Integer overflow → use `long`
- Sorted vs unsorted assumption
- Cycle in graph / linked list

---

## 📐 Complexity Targets for Interviews

| Input Size | Expected Complexity |
|-----------|-------------------|
| n ≤ 20 | O(2^n) — backtracking ok |
| n ≤ 1000 | O(n²) ok |
| n ≤ 10⁶ | O(n log n) required |
| n > 10⁶ | O(n) or O(log n) required |
