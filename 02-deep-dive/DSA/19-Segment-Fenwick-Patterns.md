# 📚 DSA Deep Dive — Segment Tree, Fenwick Tree & Interview Patterns

---

## 🧠 Topics Covered
1. Segment Tree (Range Queries + Point Updates)
2. Fenwick Tree / Binary Indexed Tree (Prefix Sums with Updates)
3. Cyclic Sort (Find Missing / Duplicate)
4. Boyer-Moore Voting (Majority Element)
5. Reservoir Sampling
6. Fisher-Yates Shuffle
7. Floyd's Cycle Detection (Tortoise & Hare)

---

## 1. Segment Tree

**Use**: Range queries (sum/min/max) + point/range updates — O(log n) each.
**Space**: O(4n) array

```java
class SegmentTree {
    int[] tree;
    int n;

    SegmentTree(int[] arr) {
        n = arr.length;
        tree = new int[4 * n];
        build(arr, 0, 0, n - 1);
    }

    void build(int[] arr, int node, int start, int end) {
        if (start == end) { tree[node] = arr[start]; return; }
        int mid = (start + end) / 2;
        build(arr, 2*node+1, start, mid);
        build(arr, 2*node+2, mid+1, end);
        tree[node] = tree[2*node+1] + tree[2*node+2]; // sum; use min/max as needed
    }

    // Point update: arr[idx] = val
    void update(int node, int start, int end, int idx, int val) {
        if (start == end) { tree[node] = val; return; }
        int mid = (start + end) / 2;
        if (idx <= mid) update(2*node+1, start, mid, idx, val);
        else            update(2*node+2, mid+1, end, idx, val);
        tree[node] = tree[2*node+1] + tree[2*node+2];
    }

    // Range query: sum of arr[l..r]
    int query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) return 0;       // out of range
        if (l <= start && end <= r) return tree[node]; // fully in range
        int mid = (start + end) / 2;
        return query(2*node+1, start, mid, l, r)
             + query(2*node+2, mid+1, end, l, r);
    }

    // Public wrappers
    void update(int idx, int val) { update(0, 0, n-1, idx, val); }
    int query(int l, int r)       { return query(0, 0, n-1, l, r); }
}
```

### Lazy Propagation (Range Updates)
```java
// When you need to update a range of values and query ranges
// Defer updates using a lazy array — only propagate when needed
void updateRange(int node, int start, int end, int l, int r, int val) {
    if (lazy[node] != 0) { // push down pending update
        tree[node] += (end - start + 1) * lazy[node];
        if (start != end) { lazy[2*node+1] += lazy[node]; lazy[2*node+2] += lazy[node]; }
        lazy[node] = 0;
    }
    if (r < start || end < l) return;
    if (l <= start && end <= r) {
        tree[node] += (end - start + 1) * val;
        if (start != end) { lazy[2*node+1] += val; lazy[2*node+2] += val; }
        return;
    }
    int mid = (start + end) / 2;
    updateRange(2*node+1, start, mid, l, r, val);
    updateRange(2*node+2, mid+1, end, l, r, val);
    tree[node] = tree[2*node+1] + tree[2*node+2];
}
```

---

## 2. Fenwick Tree (Binary Indexed Tree)

**Use**: Prefix sum queries + point updates — simpler than Segment Tree, O(log n) each.
**Best for**: When you only need prefix sums / frequency counts.

```java
class FenwickTree {
    int[] bit;
    int n;

    FenwickTree(int n) { this.n = n; bit = new int[n + 1]; }

    // Add val to index i (1-indexed)
    void update(int i, int val) {
        for (; i <= n; i += i & (-i)) bit[i] += val;
    }

    // Prefix sum from 1 to i
    int query(int i) {
        int sum = 0;
        for (; i > 0; i -= i & (-i)) sum += bit[i];
        return sum;
    }

    // Range sum from l to r (1-indexed)
    int rangeQuery(int l, int r) { return query(r) - query(l - 1); }

    // Build from array in O(n log n)
    FenwickTree(int[] arr) {
        this(arr.length);
        for (int i = 0; i < arr.length; i++) update(i + 1, arr[i]);
    }
}
```

### Fenwick vs Segment Tree
| Feature | Fenwick Tree | Segment Tree |
|---------|-------------|--------------|
| Code complexity | Simple | Complex |
| Range updates | Harder | Easy (lazy) |
| Range min/max | ❌ | ✅ |
| Prefix sum + update | ✅ | ✅ |
| Space | O(n) | O(4n) |

---

## 3. Cyclic Sort

**Use**: Find missing/duplicate numbers in range [1..n] — O(n) time, O(1) space.

```java
// Place each number at its correct index (nums[i] should be at index nums[i]-1)
void cyclicSort(int[] nums) {
    int i = 0;
    while (i < nums.length) {
        int correct = nums[i] - 1; // where nums[i] should go
        if (nums[i] != nums[correct]) swap(nums, i, correct);
        else i++;
    }
}

// Find missing number
int findMissing(int[] nums) {
    cyclicSort(nums);
    for (int i = 0; i < nums.length; i++)
        if (nums[i] != i + 1) return i + 1;
    return nums.length + 1;
}

// Find duplicate
int findDuplicate(int[] nums) {
    cyclicSort(nums);
    for (int i = 0; i < nums.length; i++)
        if (nums[i] != i + 1) return nums[i];
    return -1;
}

// Find all missing numbers
List<Integer> findAllMissing(int[] nums) {
    cyclicSort(nums);
    List<Integer> missing = new ArrayList<>();
    for (int i = 0; i < nums.length; i++)
        if (nums[i] != i + 1) missing.add(i + 1);
    return missing;
}
```

---

## 4. Boyer-Moore Voting — Majority Element

**Use**: Find element appearing > n/2 times — O(n) time, O(1) space.

```java
// Majority element (appears > n/2 times)
int majorityElement(int[] nums) {
    int candidate = nums[0], count = 1;
    for (int i = 1; i < nums.length; i++) {
        if (count == 0) { candidate = nums[i]; count = 1; }
        else if (nums[i] == candidate) count++;
        else count--;
    }
    // Optional: verify candidate is actually majority
    int verify = 0;
    for (int n : nums) if (n == candidate) verify++;
    return verify > nums.length / 2 ? candidate : -1;
}

// Extended: find all elements appearing > n/3 times (at most 2 candidates)
List<Integer> majorityElementN3(int[] nums) {
    int cand1 = 0, cand2 = 1, cnt1 = 0, cnt2 = 0;
    for (int n : nums) {
        if (n == cand1) cnt1++;
        else if (n == cand2) cnt2++;
        else if (cnt1 == 0) { cand1 = n; cnt1 = 1; }
        else if (cnt2 == 0) { cand2 = n; cnt2 = 1; }
        else { cnt1--; cnt2--; }
    }
    // Verify both candidates
    cnt1 = cnt2 = 0;
    for (int n : nums) { if (n == cand1) cnt1++; else if (n == cand2) cnt2++; }
    List<Integer> result = new ArrayList<>();
    if (cnt1 > nums.length / 3) result.add(cand1);
    if (cnt2 > nums.length / 3) result.add(cand2);
    return result;
}
```

---

## 5. Reservoir Sampling

**Use**: Randomly sample K elements from a stream of unknown size — each element has equal probability K/n.

```java
int[] reservoirSample(Iterator<Integer> stream, int k) {
    int[] reservoir = new int[k];
    Random rand = new Random();
    int i = 0;

    // Fill reservoir with first k elements
    while (i < k && stream.hasNext()) reservoir[i++] = stream.next();

    // For each subsequent element, randomly decide to include it
    while (stream.hasNext()) {
        int element = stream.next();
        int j = rand.nextInt(i + 1); // random index in [0, i]
        if (j < k) reservoir[j] = element; // replace with probability k/(i+1)
        i++;
    }
    return reservoir;
}
```

**Why it works**: Element at position i is included with probability k/i (can be proven by induction).

---

## 6. Fisher-Yates Shuffle

**Use**: Uniformly random shuffle of an array — O(n), O(1) space.

```java
void shuffle(int[] arr) {
    Random rand = new Random();
    for (int i = arr.length - 1; i > 0; i--) {
        int j = rand.nextInt(i + 1); // j in [0, i]
        swap(arr, i, j);
    }
}
// Each permutation has probability 1/n! — provably uniform
```

---

## 7. Floyd's Cycle Detection (Tortoise & Hare)

**Use**: Find duplicate in array where values are in [1..n] — treat as linked list.

```java
// Find duplicate number in array [1..n] with one duplicate (no modification allowed)
int findDuplicateFloyd(int[] nums) {
    // Phase 1: Find intersection point
    int slow = nums[0], fast = nums[0];
    do {
        slow = nums[slow];
        fast = nums[nums[fast]];
    } while (slow != fast);

    // Phase 2: Find cycle entrance (= duplicate)
    slow = nums[0];
    while (slow != fast) {
        slow = nums[slow];
        fast = nums[fast];
    }
    return slow;
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: FX Rate Range Queries with Updates (Segment Tree — kACE)
**Problem**: Given continuously updating FX rates, answer range sum/min/max queries in O(log n).

```java
SegmentTree st = new SegmentTree(historicalRates);
// New rate comes in — update O(log n)
st.update(todayIdx, newRate);
// Query: min rate over last 30 days — O(log n)
int minRate = st.query(todayIdx - 30, todayIdx);
```
**Where it applies**: kACE FX rate range analytics, option pricing historical volatility windows.
> 🏭 **Industry Example**: Codeforces and competitive programming judges use Segment Trees for range query problems. TradingView's chart engine uses Segment Trees for efficient OHLC (Open-High-Low-Close) range queries over time windows. Bloomberg Terminal's analytics use range query structures for yield curve analysis.
> 🏦 **kACE Context**: kACE FX rate range analytics — O(log n) range min/max queries over historical rate windows with live updates.


---

### Use Case 2: Order Book Frequency Tracking (Fenwick Tree)
**Problem**: Track cumulative order counts by price level, supporting O(log n) updates and prefix queries.

```java
FenwickTree bit = new FenwickTree(MAX_PRICE);
// New order at price p — O(log n)
bit.update(price, 1);
// How many orders at price ≤ target? — O(log n)
int count = bit.query(targetPrice);
// Orders in price range [lo, hi] — O(log n)
int rangeCount = bit.rangeQuery(lo, hi);
```
**Where it applies**: FX option order book analytics, cumulative volume at price levels.
> 🏭 **Industry Example**: NASDAQ's order book uses BIT-like prefix sum structures for cumulative volume at price levels. Binance's order book depth chart uses prefix sums to show cumulative volume. Interactive Brokers' TWS uses similar structures for order flow analysis.
> 🏦 **kACE Context**: FX option order book analytics — cumulative order counts by price level with O(log n) updates.


---

### Use Case 3: Missing Trade IDs (Cyclic Sort — Data Integrity)
**Problem**: Given trade IDs 1..n with some missing, find all gaps in O(n) time, O(1) space.

```java
int[] tradeIds = fetchTradeIds(); // should be [1..n]
List<Integer> missingIds = findAllMissing(tradeIds);
System.out.println("Missing trade IDs: " + missingIds);
// Alert operations team for reconciliation
```
**Where it applies**: Trade reconciliation, Kafka message sequence gap detection in kACE.
> 🏭 **Industry Example**: Stock exchange reconciliation systems detect missing trade IDs in daily settlement batches. Visa/Mastercard transaction reconciliation identifies gaps in sequential transaction numbers. Bank end-of-day processing validates that no transaction IDs are missing in a sequence.
> 🏦 **kACE Context**: Trade reconciliation — detecting gaps in sequential kACE trade ID sequences for audit compliance.


---

### Use Case 4: Most Active Trader Detection (Boyer-Moore)
**Problem**: Find the trader responsible for > 50% of RFQs in a session — O(n) time, O(1) space.

```java
// Stream of trader IDs — find majority without storing all
int dominantTrader = majorityElement(rfqTraderIds);
if (dominantTrader != -1)
    System.out.println("Dominant trader: " + dominantTrader);
```
**Where it applies**: kACE RFQ analysis, market maker activity monitoring, suspicious trading detection.
> 🏭 **Industry Example**: Twitter uses Boyer-Moore voting to find trending topics that appear in more than 50% of recent tweets. Facebook uses it to detect dominant reactions on viral posts. Reddit uses a variant to find the majority-upvoted comment in a thread.
> 🏦 **kACE Context**: kACE RFQ analysis — finding the market maker responsible for the majority of RFQ activity in a session.


---

### Use Case 5: Random Sampling for Load Testing (Reservoir Sampling)
**Problem**: Randomly sample 1000 representative trades from a stream of millions for load testing.

```java
// Stream of all trades — sample 1000 uniformly at random
int[] sampleTrades = reservoirSample(tradeStream.iterator(), 1000);
// Each trade has equal probability of being selected regardless of stream size
runLoadTest(sampleTrades);
```
**Where it applies**: kACE performance testing, A/B testing trade sample selection, audit sampling.
> 🏭 **Industry Example**: Netflix uses reservoir sampling to sample 1000 representative user sessions from millions for A/B test analysis. Google Analytics uses it for sampled reporting at scale. Twitter's streaming API uses reservoir sampling to provide a statistically representative 1% sample of all tweets.
> 🏦 **kACE Context**: kACE performance testing — randomly sampling representative trades from millions for load test scenarios.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Range Sum Query — Mutable | Segment Tree / BIT | Medium |
| 2 | Count of Smaller Numbers After Self | Segment Tree / BIT | Hard |
| 3 | Find Missing Number | Cyclic Sort / XOR | Easy |
| 4 | Find All Duplicates in Array | Cyclic Sort | Medium |
| 5 | First Missing Positive | Cyclic Sort | Hard |
| 6 | Majority Element | Boyer-Moore | Easy |
| 7 | Majority Element II (n/3) | Boyer-Moore extended | Medium |
| 8 | Shuffle an Array | Fisher-Yates | Medium |
| 9 | Linked List Cycle II | Floyd's | Medium |
| 10 | Find the Duplicate Number | Floyd's / Cyclic | Medium |

---

## ⚠️ Common Mistakes

- Segment Tree: use `4*n` size for tree array — `2*n` is not always sufficient
- Fenwick Tree: **1-indexed** — always shift by +1 when building from 0-indexed array
- Cyclic Sort: only works when values are in range `[1..n]` or `[0..n-1]`
- Boyer-Moore: always **verify** the candidate — it may not be a majority if no majority exists
- Reservoir Sampling: the random index `j` must be in `[0, i]` not `[0, k]`
- Fisher-Yates: iterate **backwards** from `n-1` to `1`, not forwards
- Floyd's cycle: Phase 2 must reset **one** pointer to start, keep other at intersection point
