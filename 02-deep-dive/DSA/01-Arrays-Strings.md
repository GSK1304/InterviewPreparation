# 📚 DSA Deep Dive — Arrays & Strings

---

## 🧠 Core Concepts

### Arrays
- Contiguous block of memory
- **O(1)** random access via index
- **O(n)** insert/delete (shifting required)
- Fixed size in Java: `int[] arr = new int[n]`
- Dynamic: `ArrayList<Integer> list = new ArrayList<>()`

### Strings
- Immutable in Java — every modification creates a new object
- Use `StringBuilder` for concatenation in loops
- `char[] chars = s.toCharArray()` for in-place mutation

---

## 🔑 Key Techniques

### 1. Prefix Sum
Precompute cumulative sums for O(1) range queries.

```java
int[] prefix = new int[n + 1];
for (int i = 0; i < n; i++) prefix[i+1] = prefix[i] + nums[i];
// Sum from index l to r (inclusive):
int rangeSum = prefix[r+1] - prefix[l];
```

### 2. Frequency Map
Count occurrences for anagram / duplicate problems.

```java
Map<Character, Integer> freq = new HashMap<>();
for (char c : s.toCharArray())
    freq.put(c, freq.getOrDefault(c, 0) + 1);
```

### 3. Kadane's Algorithm — Maximum Subarray Sum
```java
int maxSum = nums[0], curSum = nums[0];
for (int i = 1; i < nums.length; i++) {
    curSum = Math.max(nums[i], curSum + nums[i]);
    maxSum = Math.max(maxSum, curSum);
}
```

### 4. Dutch National Flag — Sort 0s, 1s, 2s
```java
int lo = 0, mid = 0, hi = nums.length - 1;
while (mid <= hi) {
    if (nums[mid] == 0) swap(nums, lo++, mid++);
    else if (nums[mid] == 1) mid++;
    else swap(nums, mid, hi--);
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Stock Price Analysis (Trading Platform — FX Domain)
**Problem**: Given daily FX rates, find the max profit from a single buy/sell.

```java
// Real use case: find best entry/exit in FX option pricing window
int maxProfit = 0;
int minPrice = prices[0];
for (int price : prices) {
    minPrice = Math.min(minPrice, price);
    maxProfit = Math.max(maxProfit, price - minPrice);
}
```
**Where it applies**: In kACE, when analyzing historical rate windows for option pricing.
> 🏭 **Industry Example**: Robinhood and Zerodha use this exact pattern to display "max profit if bought on day X and sold on day Y" on stock charts. LeetCode Best Time to Buy/Sell Stock is directly based on this.
> 🏦 **kACE Context**: Analyzing historical FX rate windows for option pricing entry/exit points.


---

### Use Case 2: Log Aggregation (Backend Systems)
**Problem**: Given a log array with timestamps, find the busiest K-minute window.

```java
// Sliding window of fixed size k
int maxRequests = 0, curRequests = 0;
for (int i = 0; i < logs.length; i++) {
    curRequests++;
    if (i >= k) curRequests--;
    maxRequests = Math.max(maxRequests, curRequests);
}
```
**Where it applies**: Kafka consumer lag monitoring, rate limiter windows.
> 🏭 **Industry Example**: Datadog uses sliding window counters to show requests/minute in their APM dashboards. Cloudflare uses fixed-window counters for DDoS detection per IP.
> 🏦 **kACE Context**: Kafka consumer lag monitoring — finding peak message volume windows.


---

### Use Case 3: Anagram Detection (Search Systems)
**Problem**: Find all anagram start positions of pattern in a string.

```java
// Used in: full-text search, symbol matching in trading terminals
int[] pCount = new int[26], wCount = new int[26];
for (char c : p.toCharArray()) pCount[c - 'a']++;
List<Integer> result = new ArrayList<>();
for (int i = 0; i < s.length(); i++) {
    wCount[s.charAt(i) - 'a']++;
    if (i >= p.length()) wCount[s.charAt(i - p.length()) - 'a']--;
    if (Arrays.equals(pCount, wCount)) result.add(i - p.length() + 1);
}
```

---
> 🏭 **Industry Example**: Elasticsearch uses character frequency arrays for fuzzy search suggestions. Google Search uses anagram detection to suggest "did you mean?" corrections.
> 🏦 **kACE Context**: Symbol matching in FX trading terminals — detecting permutations of currency codes.


### Use Case 4: Prefix Sum for Range Queries (Analytics Dashboard)
**Problem**: Multiple range sum queries on a static array.

```java
// Used in: reporting dashboards, P&L range computations
// Build once O(n), query O(1)
int[] prefix = new int[nums.length + 1];
for (int i = 0; i < nums.length; i++)
    prefix[i + 1] = prefix[i] + nums[i];

// Query [l, r] in O(1)
int rangeSum(int l, int r) { return prefix[r+1] - prefix[l]; }
```

---
> 🏭 **Industry Example**: Google Analytics precomputes prefix sums of page views to serve date-range queries in O(1). Facebook's ad impression reporting uses the same technique for campaign dashboards.
> 🏦 **kACE Context**: FX P&L range computations across strategy legs in the pricing grid.


### Use Case 5: Duplicate Detection (Data Validation)
**Problem**: Find if any duplicate exists in an array.

```java
// Used in: order deduplication, trade ID validation
Set<Integer> seen = new HashSet<>();
for (int num : nums) {
    if (!seen.add(num)) return true; // duplicate found
}
return false;
```

---
> 🏭 **Industry Example**: Stripe uses HashSet-based deduplication on idempotency keys to prevent double-charging. Kafka uses offset tracking (effectively a Set) to ensure at-least-once delivery without duplicate processing.
> 🏦 **kACE Context**: Trade ID validation — preventing duplicate trade records in reconciliation.


## 🏋️ Practice Problems (Beginner → Medium)

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Two Sum | HashMap | Easy |
| 2 | Best Time to Buy/Sell Stock | Greedy scan | Easy |
| 3 | Maximum Subarray | Kadane's | Easy |
| 4 | Contains Duplicate | HashSet | Easy |
| 5 | Product of Array Except Self | Prefix/Suffix | Medium |
| 6 | Find All Anagrams in a String | Sliding Window | Medium |
| 7 | Longest Substring Without Repeating | Sliding Window | Medium |
| 8 | 3Sum | Two Pointers | Medium |
| 9 | Rotate Array | Reversal trick | Medium |
| 10 | Trapping Rain Water | Two Pointers / Stack | Hard |

---

## ⚠️ Common Mistakes

- Forgetting `i - k` condition in fixed sliding window
- Off-by-one in prefix sum (always use `prefix[r+1] - prefix[l]`)
- String comparison with `==` instead of `.equals()` in Java
- Not handling empty array edge case
- Integer overflow in sum — cast to `long` when n > 10⁴
