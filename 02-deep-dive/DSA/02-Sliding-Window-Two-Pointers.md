# 📚 DSA Deep Dive — Sliding Window & Two Pointers

---

## 🧠 Core Concepts

### Sliding Window
A technique to reduce nested loops (O(n²)) to a single pass (O(n)) for **contiguous subarray/substring** problems.

- **Fixed Window**: window size `k` stays constant
- **Variable Window**: window expands/shrinks based on a condition

### Two Pointers
Two indices traversing from opposite ends or at different speeds.

- Works best on **sorted arrays**
- Also used for palindrome checks, pair sums, merging

---

## 🔑 Templates

### Fixed Sliding Window
```java
int sum = 0, maxSum = 0;
for (int i = 0; i < nums.length; i++) {
    sum += nums[i];
    if (i >= k) sum -= nums[i - k]; // remove leftmost
    if (i >= k - 1) maxSum = Math.max(maxSum, sum);
}
```

### Variable Sliding Window
```java
int left = 0, maxLen = 0;
Map<Character, Integer> window = new HashMap<>();
for (int right = 0; right < s.length(); right++) {
    char c = s.charAt(right);
    window.put(c, window.getOrDefault(c, 0) + 1);

    while (/* violation condition */) {
        char leftChar = s.charAt(left++);
        window.put(leftChar, window.get(leftChar) - 1);
        if (window.get(leftChar) == 0) window.remove(leftChar);
    }
    maxLen = Math.max(maxLen, right - left + 1);
}
```

### Two Pointers (Pair Sum)
```java
int left = 0, right = nums.length - 1;
while (left < right) {
    int sum = nums[left] + nums[right];
    if (sum == target) return new int[]{left, right};
    else if (sum < target) left++;
    else right--;
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Rate Limiter (API Gateway)
**Problem**: Given a stream of API requests with timestamps, check if any user made more than K requests in a sliding window of T seconds.

```java
// Sliding window over timestamp array
Deque<Integer> window = new ArrayDeque<>();
for (int time : timestamps) {
    while (!window.isEmpty() && time - window.peekFirst() >= T)
        window.pollFirst();
    window.addLast(time);
    if (window.size() > K) return false; // rate limit exceeded
}
return true;
```
**Where it applies**: kACE API Gateway rate limiting, Kafka consumer throttling.

---

### Use Case 2: Longest Valid Trading Window (FX Domain)
**Problem**: Find the longest contiguous period where FX rate stayed within a valid spread (max - min ≤ threshold).

```java
// Variable window: shrink when spread exceeds threshold
TreeMap<Integer, Integer> map = new TreeMap<>();
int left = 0, maxLen = 0;
for (int right = 0; right < prices.length; right++) {
    map.put(prices[right], map.getOrDefault(prices[right], 0) + 1);
    while (map.lastKey() - map.firstKey() > threshold) {
        int leftVal = prices[left++];
        map.put(leftVal, map.get(leftVal) - 1);
        if (map.get(leftVal) == 0) map.remove(leftVal);
    }
    maxLen = Math.max(maxLen, right - left + 1);
}
```
**Where it applies**: Monitoring FX option pricing validity windows.

---

### Use Case 3: Minimum Window Substring (Search / Compliance)
**Problem**: Find smallest window in a document containing all required keywords.

```java
Map<Character, Integer> need = new HashMap<>(), have = new HashMap<>();
for (char c : t.toCharArray()) need.put(c, need.getOrDefault(c, 0) + 1);
int formed = 0, required = need.size(), left = 0;
int[] ans = {-1, 0, 0};
for (int right = 0; right < s.length(); right++) {
    char c = s.charAt(right);
    have.put(c, have.getOrDefault(c, 0) + 1);
    if (need.containsKey(c) && have.get(c).equals(need.get(c))) formed++;
    while (formed == required) {
        if (ans[0] == -1 || right - left + 1 < ans[0])
            ans = new int[]{right - left + 1, left, right};
        char lc = s.charAt(left++);
        have.put(lc, have.get(lc) - 1);
        if (need.containsKey(lc) && have.get(lc) < need.get(lc)) formed--;
    }
}
```
**Where it applies**: Compliance keyword scanning in trade messages/logs.

---

### Use Case 4: Container With Most Water (Capacity Planning)
**Problem**: Given heights of walls, find two walls forming the largest water container.

```java
// Two pointers from both ends
int left = 0, right = height.length - 1, maxWater = 0;
while (left < right) {
    maxWater = Math.max(maxWater, Math.min(height[left], height[right]) * (right - left));
    if (height[left] < height[right]) left++;
    else right--;
}
```
**Where it applies**: Infrastructure capacity planning, buffer size optimization.

---

### Use Case 5: 3Sum (Multi-factor Analysis)
**Problem**: Find all unique triplets that sum to zero — useful in delta-neutral portfolio analysis.

```java
Arrays.sort(nums);
List<List<Integer>> result = new ArrayList<>();
for (int i = 0; i < nums.length - 2; i++) {
    if (i > 0 && nums[i] == nums[i-1]) continue; // skip duplicates
    int left = i + 1, right = nums.length - 1;
    while (left < right) {
        int sum = nums[i] + nums[left] + nums[right];
        if (sum == 0) {
            result.add(Arrays.asList(nums[i], nums[left++], nums[right--]));
            while (left < right && nums[left] == nums[left-1]) left++;
        } else if (sum < 0) left++;
        else right--;
    }
}
```
**Where it applies**: Delta-neutral option strategies, multi-leg trade analysis.

---

## 🏋️ Practice Problems

| # | Problem | Type | Difficulty |
|---|---------|------|------------|
| 1 | Max Sum Subarray of Size K | Fixed Window | Easy |
| 2 | Longest Substring Without Repeating | Variable Window | Medium |
| 3 | Minimum Size Subarray Sum | Variable Window | Medium |
| 4 | Two Sum II (Sorted) | Two Pointers | Easy |
| 5 | Container With Most Water | Two Pointers | Medium |
| 6 | 3Sum | Two Pointers + Sort | Medium |
| 7 | Minimum Window Substring | Variable Window | Hard |
| 8 | Sliding Window Maximum | Monotonic Deque | Hard |
| 9 | Longest Repeating Character Replacement | Variable Window | Medium |
| 10 | Fruit Into Baskets | Variable Window | Medium |

---

## ⚠️ Common Mistakes

- Using fixed window template for variable window problem
- Forgetting to shrink the window (left pointer never moves)
- Not handling duplicate avoidance in 3Sum
- Off-by-one: window size is `right - left + 1`
- Not clearing `window` map entries when count hits 0 (causes wrong `size()`)
