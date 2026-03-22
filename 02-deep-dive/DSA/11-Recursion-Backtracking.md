# 📚 DSA Deep Dive — Recursion & Backtracking

---

## 🧠 Core Concepts

### Recursion
A function that calls itself with a **smaller subproblem** until it hits a **base case**.

```
f(n) = f(n-1) + ... until base case
```

**Three laws of recursion:**
1. Must have a **base case**
2. Must **change state** and move toward the base case
3. Must **call itself** recursively

### Backtracking
Recursion + **undo** — explore all possibilities by making a choice, recursing, then undoing the choice.

```
backtrack(state):
    if goal reached → record result, return
    for each valid choice:
        make choice
        backtrack(updated state)
        undo choice          ← KEY STEP
```

**Think of it as:** DFS on a decision tree, pruning invalid branches early.

---

## 🔑 Templates

### Subsets (Power Set)
```java
List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(nums, 0, new ArrayList<>(), result);
    return result;
}
void backtrack(int[] nums, int start, List<Integer> current, List<List<Integer>> result) {
    result.add(new ArrayList<>(current)); // add current subset
    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);             // choose
        backtrack(nums, i + 1, current, result); // explore
        current.remove(current.size() - 1); // undo
    }
}
```

### Permutations
```java
List<List<Integer>> permutations(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    boolean[] used = new boolean[nums.length];
    backtrack(nums, used, new ArrayList<>(), result);
    return result;
}
void backtrack(int[] nums, boolean[] used, List<Integer> current, List<List<Integer>> result) {
    if (current.size() == nums.length) { result.add(new ArrayList<>(current)); return; }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        used[i] = true;
        current.add(nums[i]);
        backtrack(nums, used, current, result);
        current.remove(current.size() - 1);
        used[i] = false;
    }
}
```

### Combinations (Choose K from N)
```java
List<List<Integer>> combine(int n, int k) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(1, n, k, new ArrayList<>(), result);
    return result;
}
void backtrack(int start, int n, int k, List<Integer> current, List<List<Integer>> result) {
    if (current.size() == k) { result.add(new ArrayList<>(current)); return; }
    for (int i = start; i <= n; i++) {
        current.add(i);
        backtrack(i + 1, n, k, current, result);
        current.remove(current.size() - 1);
    }
}
```

### N-Queens
```java
List<List<String>> solveNQueens(int n) {
    List<List<String>> result = new ArrayList<>();
    int[] queens = new int[n]; // queens[row] = col
    Arrays.fill(queens, -1);
    backtrack(0, n, queens, new HashSet<>(), new HashSet<>(), new HashSet<>(), result);
    return result;
}
void backtrack(int row, int n, int[] queens,
               Set<Integer> cols, Set<Integer> diag1, Set<Integer> diag2,
               List<List<String>> result) {
    if (row == n) { result.add(buildBoard(queens, n)); return; }
    for (int col = 0; col < n; col++) {
        if (cols.contains(col) || diag1.contains(row-col) || diag2.contains(row+col)) continue;
        queens[row] = col;
        cols.add(col); diag1.add(row-col); diag2.add(row+col);
        backtrack(row+1, n, queens, cols, diag1, diag2, result);
        cols.remove(col); diag1.remove(row-col); diag2.remove(row+col);
    }
}
```

### Word Search on Grid
```java
boolean exist(char[][] board, String word) {
    for (int r = 0; r < board.length; r++)
        for (int c = 0; c < board[0].length; c++)
            if (dfs(board, word, r, c, 0)) return true;
    return false;
}
boolean dfs(char[][] board, String word, int r, int c, int idx) {
    if (idx == word.length()) return true;
    if (r < 0 || r >= board.length || c < 0 || c >= board[0].length) return false;
    if (board[r][c] != word.charAt(idx)) return false;
    char temp = board[r][c];
    board[r][c] = '#'; // mark visited
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    for (int[] d : dirs)
        if (dfs(board, word, r+d[0], c+d[1], idx+1)) { board[r][c] = temp; return true; }
    board[r][c] = temp; // restore
    return false;
}
```

### Sudoku Solver
```java
boolean solveSudoku(char[][] board) {
    for (int r = 0; r < 9; r++) {
        for (int c = 0; c < 9; c++) {
            if (board[r][c] != '.') continue;
            for (char ch = '1'; ch <= '9'; ch++) {
                if (isValid(board, r, c, ch)) {
                    board[r][c] = ch;
                    if (solveSudoku(board)) return true;
                    board[r][c] = '.'; // undo
                }
            }
            return false; // no valid digit → backtrack
        }
    }
    return true; // all filled
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Generate All Option Strategy Combinations (kACE)
**Problem**: Given a set of FX option legs, generate all valid strategy combinations of size K.

```java
// Choose K legs from available legs to form strategies
void generateStrategies(List<OptionLeg> legs, int k) {
    backtrack(legs, 0, k, new ArrayList<>(), results);
}
void backtrack(List<OptionLeg> legs, int start, int k,
               List<OptionLeg> current, List<List<OptionLeg>> results) {
    if (current.size() == k) {
        if (isValidStrategy(current)) results.add(new ArrayList<>(current));
        return;
    }
    for (int i = start; i < legs.size(); i++) {
        current.add(legs.get(i));
        backtrack(legs, i + 1, k, current, results);
        current.remove(current.size() - 1);
    }
}
```
**Where it applies**: kACE pricing — generating valid multi-leg option strategy combinations (straddle, strangle, butterfly, condor).

---

### Use Case 2: Permission Role Assignment (Auth System)
**Problem**: Generate all valid permission sets for a role (subsets of available permissions).

```java
// All subsets of permissions that satisfy constraints
void generateRoles(List<String> permissions, List<String> mandatory) {
    backtrack(permissions, 0, new ArrayList<>(mandatory), validRoles);
}
// Each valid subset = a possible role configuration
```
**Where it applies**: kACE JWT privileges system — role/permission combination generation for new user types.

---

### Use Case 3: Query Plan Generation (Database Optimizer)
**Problem**: Generate all possible join orders for a multi-table query to find the optimal plan.

```java
// Permutations of table join order
void generateJoinOrders(List<String> tables) {
    boolean[] used = new boolean[tables.size()];
    backtrack(tables, used, new ArrayList<>(), allOrders);
}
// Then evaluate cost of each order, pick minimum
```
**Where it applies**: Database query optimization in PostgreSQL/DB2 used by kACE backend.

---

### Use Case 4: Configuration Validation (Constraint Satisfaction)
**Problem**: Assign microservices to servers such that no two conflicting services share a server (graph coloring variant).

```java
// Backtracking CSP — assign service to server
boolean assignServices(int[] assignment, int serviceIdx,
                        boolean[][] conflicts, int servers) {
    if (serviceIdx == assignment.length) return true;
    for (int s = 0; s < servers; s++) {
        if (canAssign(assignment, serviceIdx, s, conflicts)) {
            assignment[serviceIdx] = s;
            if (assignServices(assignment, serviceIdx+1, conflicts, servers)) return true;
            assignment[serviceIdx] = -1; // backtrack
        }
    }
    return false;
}
```
**Where it applies**: Kubernetes pod anti-affinity assignment, microservice placement in kACE infra.

---

### Use Case 5: Palindrome Partitioning (String Processing)
**Problem**: Partition a trade symbol/message string into all possible palindrome substrings.

```java
List<List<String>> partition(String s) {
    List<List<String>> result = new ArrayList<>();
    backtrack(s, 0, new ArrayList<>(), result);
    return result;
}
void backtrack(String s, int start, List<String> current, List<List<String>> result) {
    if (start == s.length()) { result.add(new ArrayList<>(current)); return; }
    for (int end = start + 1; end <= s.length(); end++) {
        String sub = s.substring(start, end);
        if (isPalindrome(sub)) {
            current.add(sub);
            backtrack(s, end, current, result);
            current.remove(current.size() - 1);
        }
    }
}
```
**Where it applies**: String validation in FX symbol parsing, message format decomposition.

---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Subsets | Backtracking | Medium |
| 2 | Subsets II (with duplicates) | Backtracking + sort | Medium |
| 3 | Permutations | Backtracking | Medium |
| 4 | Permutations II (with duplicates) | Backtracking + sort | Medium |
| 5 | Combinations | Backtracking | Medium |
| 6 | Combination Sum | Backtracking | Medium |
| 7 | Word Search | Grid DFS + Backtrack | Medium |
| 8 | N-Queens | Backtracking + sets | Hard |
| 9 | Sudoku Solver | Backtracking CSP | Hard |
| 10 | Palindrome Partitioning | Backtracking + DP | Medium |
| 11 | Letter Combinations of Phone | Backtracking | Medium |
| 12 | Generate Parentheses | Backtracking | Medium |

---

## ⚠️ Common Mistakes

- Forgetting to **undo** the choice after recursion (most common backtracking bug)
- Not copying the list when adding to results: `result.add(new ArrayList<>(current))` not `result.add(current)`
- Missing duplicate handling — sort first, then skip `if (i > start && nums[i] == nums[i-1]) continue`
- Marking visited inside DFS but forgetting to **unmark** on backtrack
- Stack overflow on deep recursion — check if iterative DFS is needed
- Not pruning early — always add constraint checks **before** recursing, not after
