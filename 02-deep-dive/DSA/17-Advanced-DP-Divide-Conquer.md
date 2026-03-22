# 📚 DSA Deep Dive — Advanced DP & Divide and Conquer

---

## 🧠 Advanced DP Patterns

### 1. DP on Intervals
### 2. DP on Trees
### 3. DP on Strings
### 4. Bitmask DP
### 5. Digit DP
### 6. Divide and Conquer

---

## 1. DP on Intervals

State: `dp[i][j]` = optimal answer for subarray/subsequence from index `i` to `j`.
Direction: Fill by **increasing length** of interval.

```java
// Matrix Chain Multiplication — min cost to multiply chain of matrices
int matrixChain(int[] dims) {
    int n = dims.length - 1; // number of matrices
    int[][] dp = new int[n][n];
    // dp[i][j] = min multiplications for matrices i..j
    for (int len = 2; len <= n; len++) {       // interval length
        for (int i = 0; i <= n - len; i++) {
            int j = i + len - 1;
            dp[i][j] = Integer.MAX_VALUE;
            for (int k = i; k < j; k++) {       // split point
                int cost = dp[i][k] + dp[k+1][j] + dims[i]*dims[k+1]*dims[j+1];
                dp[i][j] = Math.min(dp[i][j], cost);
            }
        }
    }
    return dp[0][n-1];
}

// Burst Balloons — max coins
int maxCoins(int[] nums) {
    int n = nums.length;
    int[] arr = new int[n + 2];
    arr[0] = arr[n+1] = 1;
    for (int i = 0; i < n; i++) arr[i+1] = nums[i];
    int[][] dp = new int[n+2][n+2];
    // dp[i][j] = max coins for bursting all balloons between i and j (exclusive)
    for (int len = 1; len <= n; len++) {
        for (int i = 1; i <= n - len + 1; i++) {
            int j = i + len - 1;
            for (int k = i; k <= j; k++) { // k = last balloon to burst
                dp[i][j] = Math.max(dp[i][j],
                    dp[i][k-1] + arr[i-1]*arr[k]*arr[j+1] + dp[k+1][j]);
            }
        }
    }
    return dp[1][n];
}

// Palindrome Partitioning II — min cuts
int minCut(String s) {
    int n = s.length();
    boolean[][] isPalin = new boolean[n][n];
    for (int len = 1; len <= n; len++)
        for (int i = 0; i <= n - len; i++) {
            int j = i + len - 1;
            isPalin[i][j] = s.charAt(i) == s.charAt(j) && (len <= 2 || isPalin[i+1][j-1]);
        }
    int[] dp = new int[n]; // dp[i] = min cuts for s[0..i]
    for (int i = 0; i < n; i++) {
        if (isPalin[0][i]) { dp[i] = 0; continue; }
        dp[i] = i; // max cuts = i (cut after each char)
        for (int j = 1; j <= i; j++)
            if (isPalin[j][i]) dp[i] = Math.min(dp[i], dp[j-1] + 1);
    }
    return dp[n-1];
}
```

---

## 2. DP on Trees

State defined on subtrees; typically computed bottom-up via DFS.

```java
// Max path sum in binary tree
int maxPathSum = Integer.MIN_VALUE;
int maxGain(TreeNode node) {
    if (node == null) return 0;
    int leftGain = Math.max(maxGain(node.left), 0);   // ignore negative paths
    int rightGain = Math.max(maxGain(node.right), 0);
    maxPathSum = Math.max(maxPathSum, node.val + leftGain + rightGain);
    return node.val + Math.max(leftGain, rightGain);  // return single path
}

// Diameter of binary tree (DP on tree)
int diameter = 0;
int depth(TreeNode node) {
    if (node == null) return 0;
    int left = depth(node.left), right = depth(node.right);
    diameter = Math.max(diameter, left + right);
    return 1 + Math.max(left, right);
}

// House Robber III — can't rob parent and child together
int[] robTree(TreeNode node) {
    // returns {maxWithoutRob, maxWithRob}
    if (node == null) return new int[]{0, 0};
    int[] left = robTree(node.left);
    int[] right = robTree(node.right);
    int withRob = node.val + left[0] + right[0];     // rob this → skip children
    int withoutRob = Math.max(left[0], left[1]) + Math.max(right[0], right[1]);
    return new int[]{withoutRob, withRob};
}
```

---

## 3. DP on Strings

```java
// Longest Palindromic Substring
String longestPalindrome(String s) {
    int n = s.length(), start = 0, maxLen = 1;
    boolean[][] dp = new boolean[n][n];
    for (int i = 0; i < n; i++) dp[i][i] = true;
    for (int len = 2; len <= n; len++) {
        for (int i = 0; i <= n - len; i++) {
            int j = i + len - 1;
            dp[i][j] = s.charAt(i) == s.charAt(j) && (len == 2 || dp[i+1][j-1]);
            if (dp[i][j] && len > maxLen) { start = i; maxLen = len; }
        }
    }
    return s.substring(start, start + maxLen);
}

// Distinct Subsequences — count ways s contains t as subsequence
int numDistinct(String s, String t) {
    int m = s.length(), n = t.length();
    long[][] dp = new long[m+1][n+1];
    for (int i = 0; i <= m; i++) dp[i][0] = 1; // empty t matched
    for (int i = 1; i <= m; i++)
        for (int j = 1; j <= n; j++) {
            dp[i][j] = dp[i-1][j]; // skip s[i]
            if (s.charAt(i-1) == t.charAt(j-1)) dp[i][j] += dp[i-1][j-1];
        }
    return (int)dp[m][n];
}

// Regular Expression Matching
boolean isMatch(String s, String p) {
    int m = s.length(), n = p.length();
    boolean[][] dp = new boolean[m+1][n+1];
    dp[0][0] = true;
    for (int j = 2; j <= n; j += 2) // handle patterns like a*b*c*
        if (p.charAt(j-1) == '*') dp[0][j] = dp[0][j-2];
    for (int i = 1; i <= m; i++)
        for (int j = 1; j <= n; j++) {
            char pc = p.charAt(j-1);
            if (pc == '*') {
                dp[i][j] = dp[i][j-2]; // use 0 of preceding element
                if (p.charAt(j-2) == '.' || p.charAt(j-2) == s.charAt(i-1))
                    dp[i][j] |= dp[i-1][j]; // use 1+ of preceding element
            } else {
                dp[i][j] = dp[i-1][j-1] && (pc == '.' || pc == s.charAt(i-1));
            }
        }
    return dp[m][n];
}
```

---

## 4. Bitmask DP

```java
// Travelling Salesman Problem (TSP) — visit all cities exactly once
int tsp(int[][] dist, int n) {
    int[][] dp = new int[1 << n][n];
    for (int[] row : dp) Arrays.fill(row, Integer.MAX_VALUE / 2);
    dp[1][0] = 0; // start at city 0, visited = {0}
    for (int mask = 1; mask < (1 << n); mask++) {
        for (int u = 0; u < n; u++) {
            if ((mask & (1 << u)) == 0) continue; // u not in current path
            for (int v = 0; v < n; v++) {
                if ((mask & (1 << v)) != 0) continue; // v already visited
                int newMask = mask | (1 << v);
                dp[newMask][v] = Math.min(dp[newMask][v], dp[mask][u] + dist[u][v]);
            }
        }
    }
    int fullMask = (1 << n) - 1;
    int minCost = Integer.MAX_VALUE;
    for (int u = 1; u < n; u++)
        minCost = Math.min(minCost, dp[fullMask][u] + dist[u][0]);
    return minCost;
}
```

---

## 5. Digit DP

```java
// Count numbers from 1 to N satisfying a digit property
// Template: dp[pos][tight][...extra states...]
int countNumbers(String num, int digit) {
    int n = num.length();
    Integer[][] memo = new Integer[n][2];
    return solve(num, 0, true, digit, memo);
}
int solve(String num, int pos, boolean tight, int digit, Integer[][] memo) {
    if (pos == num.length()) return 1;
    if (memo[pos][tight ? 1 : 0] != null) return memo[pos][tight ? 1 : 0];
    int limit = tight ? num.charAt(pos) - '0' : 9;
    int count = 0;
    for (int d = 0; d <= limit; d++) {
        if (d == digit) continue; // skip forbidden digit
        count += solve(num, pos+1, tight && d == limit, digit, memo);
    }
    return memo[pos][tight ? 1 : 0] = count;
}
```

---

## 6. Divide and Conquer

```java
// Closest Pair of Points — O(n log n)
double closestPair(Point[] pts, int lo, int hi) {
    if (hi - lo <= 3) return bruteForce(pts, lo, hi);
    int mid = (lo + hi) / 2;
    double d = Math.min(closestPair(pts, lo, mid), closestPair(pts, mid+1, hi));
    // Check strip around dividing line
    List<Point> strip = new ArrayList<>();
    for (int i = lo; i <= hi; i++)
        if (Math.abs(pts[i].x - pts[mid].x) < d) strip.add(pts[i]);
    strip.sort(Comparator.comparingDouble(p -> p.y));
    for (int i = 0; i < strip.size(); i++)
        for (int j = i+1; j < strip.size() && strip.get(j).y - strip.get(i).y < d; j++)
            d = Math.min(d, dist(strip.get(i), strip.get(j)));
    return d;
}

// Quick Select — Kth smallest in O(n) average
int quickSelect(int[] nums, int lo, int hi, int k) {
    int pivot = partition(nums, lo, hi);
    if (pivot == k) return nums[pivot];
    else if (pivot < k) return quickSelect(nums, pivot+1, hi, k);
    else return quickSelect(nums, lo, pivot-1, k);
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Optimal Matrix Operations (Option Greeks Computation)
```java
// kACE pricing: optimal order to multiply sensitivity matrices (Greeks)
// dp on intervals → matrix chain multiplication
int minOps = matrixChain(matrixDimensions);
```
**Where it applies**: FX option Greeks matrix computation order optimization.
> 🏭 **Industry Example**: NumPy's `np.einsum` uses matrix chain optimization to find the most efficient contraction order for tensor operations. TensorFlow and PyTorch optimize neural network computation graphs using similar DP-based operation ordering. BLAS libraries use this for efficient matrix multiply chains.
> 🏦 **kACE Context**: FX option Greeks matrix computation — optimal order for multiplying sensitivity matrices (delta, gamma, vega).


---

### Use Case 2: Tree DP — Team Workload Balancing
```java
// kACE team: max work extractable from team tree where
// manager and direct report can't both be on critical path (House Robber III)
int[] result = robTree(orgChartRoot);
int maxWorkload = Math.max(result[0], result[1]);
```
**Where it applies**: Team resource allocation, sprint planning with manager/IC constraints.
> 🏭 **Industry Example**: Google's Borg cluster manager uses tree DP for hierarchical resource quota allocation. AWS Organizations uses tree DP for hierarchical cost allocation across accounts. Kubernetes uses tree-based DP for hierarchical pod resource limits (Namespace → Deployment → Pod).
> 🏦 **kACE Context**: kACE team resource allocation — maximizing deliverable work across manager/IC constraints.


---

### Use Case 3: TSP — Optimal Multi-Venue Trade Route
```java
// Visit all trading venues exactly once with minimum total latency
int optimalRoute = tsp(latencyMatrix, venues.length);
```
**Where it applies**: FX multi-venue trade execution routing optimization.
> 🏭 **Industry Example**: UPS and FedEx use TSP variants (bitmask DP) for delivery route optimization. Google's OR-Tools solves TSP for last-mile delivery optimization. Salesforce uses TSP for optimal sales representative visit scheduling.
> 🏦 **kACE Context**: FX multi-venue trade execution routing — visiting all required trading venues with minimum total latency.


---

### Use Case 4: Distinct Subsequences — Message Pattern Matching
```java
// How many ways does log message s contain error pattern t?
int ways = numDistinct(logMessage, errorPattern);
```
**Where it applies**: Kafka message pattern analysis, trade ticket validation.
> 🏭 **Industry Example**: Git's diff algorithm uses LCS (related to distinct subsequences) to find minimal edit distance between file versions. Bioinformatics tools use distinct subsequence DP for DNA sequence alignment (Smith-Waterman). Plagiarism detectors count common subsequences between documents.
> 🏦 **kACE Context**: Kafka message pattern analysis — counting how many ways an error pattern appears in a log message stream.


---

### Use Case 5: Quick Select — Real-time P&L Percentile
```java
// Find median or kth percentile P&L without full sort — O(n) average
int kthPnL = quickSelect(dailyPnL, 0, dailyPnL.length-1, k);
```
**Where it applies**: Real-time P&L percentile computation, VaR (Value at Risk) calculation.
> 🏭 **Industry Example**: Numpy's `np.percentile` uses QuickSelect internally. Financial risk systems compute VaR (Value at Risk) using QuickSelect for the 95th/99th percentile of daily returns. Cloudflare uses QuickSelect for p99 latency computation in real-time analytics.
> 🏦 **kACE Context**: Real-time P&L percentile computation — finding median or 95th percentile trade P&L without full sort.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Burst Balloons | DP on Intervals | Hard |
| 2 | Palindrome Partitioning II | DP on Intervals | Hard |
| 3 | House Robber III | DP on Trees | Medium |
| 4 | Binary Tree Max Path Sum | DP on Trees | Hard |
| 5 | Longest Palindromic Substring | DP on Strings | Medium |
| 6 | Distinct Subsequences | DP on Strings | Hard |
| 7 | Regular Expression Matching | DP on Strings | Hard |
| 8 | Travelling Salesman | Bitmask DP | Hard |
| 9 | Count Numbers with Unique Digits | Digit DP | Medium |
| 10 | Kth Largest Element | Quick Select | Medium |

---

## ⚠️ Common Mistakes

- Interval DP: fill by **length** (outer loop), not by `i` or `j`
- Tree DP: return values from subtree — don't use global running state when avoidable
- Bitmask DP: `1 << n` states — only feasible for n ≤ 20
- Digit DP: always track `tight` flag correctly — if not tight, all digits 0-9 are valid
- Quick Select: partitioning modifies the array — only use on a copy if original must be preserved
- Burst Balloons: add sentinel 1s at both ends of the array before DP
