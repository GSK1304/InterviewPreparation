# 📚 DSA Deep Dive — Dynamic Programming

---

## 🧠 Core Concepts

DP solves problems by breaking them into **overlapping subproblems** and storing results to avoid recomputation.

### When to use DP?
✅ Problem asks for **optimal** (min/max/count/boolean)
✅ Problem has **overlapping subproblems**
✅ Problem has **optimal substructure** (optimal solution built from optimal sub-solutions)

### Two Approaches
| Approach | Style | Direction |
|----------|-------|-----------|
| **Top-down** (Memoization) | Recursive + cache | Problem → base case |
| **Bottom-up** (Tabulation) | Iterative + dp array | Base case → problem |

---

## 🔑 Step-by-Step Framework

1. **Define state**: What does `dp[i]` or `dp[i][j]` represent?
2. **Write recurrence**: How does `dp[i]` relate to previous states?
3. **Base case**: Smallest valid value
4. **Iteration order**: Which direction to fill the table
5. **Answer**: Where in the dp table is the final answer?

---

## 🔑 Classic Patterns & Templates

### 1D DP — Fibonacci / Climbing Stairs
```java
// dp[i] = number of ways to reach step i
int[] dp = new int[n + 1];
dp[0] = 1; dp[1] = 1;
for (int i = 2; i <= n; i++) dp[i] = dp[i-1] + dp[i-2];
return dp[n];
```

### 1D DP — House Robber (Non-adjacent)
```java
// dp[i] = max money from first i houses
int prev2 = 0, prev1 = 0;
for (int num : nums) {
    int cur = Math.max(prev1, prev2 + num);
    prev2 = prev1; prev1 = cur;
}
return prev1;
```

### 2D DP — 0/1 Knapsack
```java
// dp[i][w] = max value using first i items with capacity w
int[][] dp = new int[n+1][W+1];
for (int i = 1; i <= n; i++) {
    for (int w = 0; w <= W; w++) {
        dp[i][w] = dp[i-1][w]; // don't take item i
        if (weights[i-1] <= w)
            dp[i][w] = Math.max(dp[i][w], dp[i-1][w-weights[i-1]] + values[i-1]);
    }
}
return dp[n][W];
```

### 2D DP — Longest Common Subsequence (LCS)
```java
// dp[i][j] = LCS of text1[0..i-1] and text2[0..j-1]
int[][] dp = new int[m+1][n+1];
for (int i = 1; i <= m; i++) {
    for (int j = 1; j <= n; j++) {
        if (text1.charAt(i-1) == text2.charAt(j-1)) dp[i][j] = dp[i-1][j-1] + 1;
        else dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
    }
}
return dp[m][n];
```

### 1D DP — Coin Change (Min Coins)
```java
// dp[i] = min coins to make amount i
int[] dp = new int[amount + 1];
Arrays.fill(dp, amount + 1); // init to "infinity"
dp[0] = 0;
for (int i = 1; i <= amount; i++)
    for (int coin : coins)
        if (coin <= i) dp[i] = Math.min(dp[i], dp[i - coin] + 1);
return dp[amount] > amount ? -1 : dp[amount];
```

### 2D DP — Unique Paths (Grid)
```java
// dp[r][c] = number of ways to reach cell (r, c)
int[][] dp = new int[m][n];
for (int r = 0; r < m; r++) dp[r][0] = 1;
for (int c = 0; c < n; c++) dp[0][c] = 1;
for (int r = 1; r < m; r++)
    for (int c = 1; c < n; c++)
        dp[r][c] = dp[r-1][c] + dp[r][c-1];
return dp[m-1][n-1];
```

### Longest Increasing Subsequence (LIS)
```java
// dp[i] = length of LIS ending at index i
int[] dp = new int[nums.length];
Arrays.fill(dp, 1);
int maxLen = 1;
for (int i = 1; i < nums.length; i++) {
    for (int j = 0; j < i; j++)
        if (nums[j] < nums[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
    maxLen = Math.max(maxLen, dp[i]);
}
return maxLen;
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Option Pricing — Binomial Model (FX Domain)
**Problem**: Compute FX option price using a binomial tree (DP on tree states).

```java
// dp[j] = option value at node j in current time step
// Work backwards from expiry to today
double[] dp = new double[steps + 1];
for (int j = 0; j <= steps; j++)
    dp[j] = Math.max(0, spotPrice * Math.pow(up, j) * Math.pow(down, steps-j) - strike);
// Backward induction
for (int i = steps - 1; i >= 0; i--)
    for (int j = 0; j <= i; j++)
        dp[j] = discount * (p * dp[j+1] + (1-p) * dp[j]);
return dp[0]; // option price today
```
**Where it applies**: FX European option pricing engine in kACE Phoenix — binomial/trinomial models.
> 🏭 **Industry Example**: Black-Scholes option pricing (used by Goldman Sachs, JP Morgan) is a continuous version of binomial DP. The binomial tree model is taught in every quantitative finance program and used in production by trading desks worldwide. Bloomberg Terminal's OVME function uses binomial trees for exotic option pricing.
> 🏦 **kACE Context**: FX European option pricing engine — binomial/trinomial backward induction models.


---

### Use Case 2: Resource Allocation — Sprint Planning
**Problem**: Given team capacity (W) and feature story points + values, maximize value delivered in sprint (0/1 Knapsack).

```java
// features[i] = {storyPoints, businessValue}
// capacity = total team capacity in story points
int[][] dp = new int[features.length+1][capacity+1];
for (int i = 1; i <= features.length; i++) {
    int sp = features[i-1][0], val = features[i-1][1];
    for (int w = 0; w <= capacity; w++) {
        dp[i][w] = dp[i-1][w];
        if (sp <= w) dp[i][w] = Math.max(dp[i][w], dp[i-1][w-sp] + val);
    }
}
return dp[features.length][capacity];
```
**Where it applies**: Sprint capacity planning, feature prioritization for kACE roadmap.
> 🏭 **Industry Example**: Google uses 0/1 knapsack variants for server bin-packing (maximizing workload per machine). Uber uses it for driver-trip matching optimization. Airlines use it for crew scheduling to maximize coverage within hour constraints.
> 🏦 **kACE Context**: Sprint capacity planning — maximizing business value delivered within the team's story point budget.


---

### Use Case 3: Edit Distance — Config Diff / Code Review
**Problem**: Minimum edits (insert/delete/replace) to transform one config string to another.

```java
// dp[i][j] = min edits to convert word1[0..i-1] to word2[0..j-1]
int[][] dp = new int[m+1][n+1];
for (int i = 0; i <= m; i++) dp[i][0] = i;
for (int j = 0; j <= n; j++) dp[0][j] = j;
for (int i = 1; i <= m; i++) {
    for (int j = 1; j <= n; j++) {
        if (word1.charAt(i-1) == word2.charAt(j-1)) dp[i][j] = dp[i-1][j-1];
        else dp[i][j] = 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
    }
}
return dp[m][n];
```
**Where it applies**: Config diff tools, JIRA ticket description similarity, layout config migration.
> 🏭 **Industry Example**: Git's `diff` algorithm uses edit distance (Myers diff algorithm, a variant). GitHub's PR review shows minimum edit distance between file versions. VS Code's merge conflict resolution uses edit distance to suggest resolutions.
> 🏦 **kACE Context**: kACE layout config versioning — detecting minimal changes between screen config versions.


---

### Use Case 4: Maximum Profit — Trade Window Selection
**Problem**: With at most K transactions, find maximum FX profit (DP + state machine).

```java
// dp[k][i] = max profit with at most k transactions up to day i
int[][] dp = new int[K+1][prices.length];
for (int k = 1; k <= K; k++) {
    int maxSoFar = -prices[0];
    for (int i = 1; i < prices.length; i++) {
        dp[k][i] = Math.max(dp[k][i-1], prices[i] + maxSoFar);
        maxSoFar = Math.max(maxSoFar, dp[k-1][i] - prices[i]);
    }
}
return dp[K][prices.length - 1];
```
**Where it applies**: FX trade window optimization, P&L maximization across multiple option legs.
> 🏭 **Industry Example**: Hedge funds use DP-based models to find optimal trade entry/exit windows. Renaissance Technologies' Medallion Fund reportedly uses DP variants for multi-transaction profit maximization. Algorithmic trading systems at Two Sigma use similar patterns.
> 🏦 **kACE Context**: FX trade window optimization — maximizing P&L across multiple option strategy legs.


---

### Use Case 5: Word Break — Symbol/Ticker Validation
**Problem**: Check if a trading symbol string can be segmented into valid sub-symbols from a dictionary.

```java
// dp[i] = true if s[0..i-1] can be segmented
boolean[] dp = new boolean[s.length() + 1];
dp[0] = true;
Set<String> dict = new HashSet<>(wordDict);
for (int i = 1; i <= s.length(); i++) {
    for (int j = 0; j < i; j++) {
        if (dp[j] && dict.contains(s.substring(j, i))) {
            dp[i] = true; break;
        }
    }
}
return dp[s.length()];
```
**Where it applies**: FX symbol parsing (EURUSD → EUR + USD), trade ticket validation.
> 🏭 **Industry Example**: Google's spell checker uses word break DP to segment unknown strings. NLP systems segment Chinese/Japanese text (no word spaces) using word break DP. DNS resolver uses it to parse domain component boundaries.
> 🏦 **kACE Context**: FX symbol parsing — segmenting 'EURUSDGBP' into valid currency pair combinations.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Climbing Stairs | 1D DP | Easy |
| 2 | House Robber | 1D DP | Easy |
| 3 | Maximum Subarray | Kadane's (DP) | Easy |
| 4 | Coin Change | 1D DP | Medium |
| 5 | Longest Increasing Subsequence | 1D DP | Medium |
| 6 | Unique Paths | 2D DP | Medium |
| 7 | Longest Common Subsequence | 2D DP | Medium |
| 8 | Word Break | 1D DP | Medium |
| 9 | 0/1 Knapsack | 2D DP | Medium |
| 10 | Edit Distance | 2D DP | Hard |
| 11 | Best Time to Buy/Sell Stock IV | DP + State | Hard |
| 12 | Regular Expression Matching | 2D DP | Hard |

---

## ⚠️ Common Mistakes

- Not defining state clearly before writing recurrence
- Wrong base case — off-by-one in dp array size (use `n+1` to include 0)
- Top-down: forgetting to return cached value (`return memo[i]` after check)
- 0/1 Knapsack: iterating weight **backwards** in space-optimized 1D version
- Confusing LCS (subsequence — can skip) with LCSubstring (must be contiguous)
- Edit distance: initializing dp[i][0] = i and dp[0][j] = j (deletions to reach empty string)
