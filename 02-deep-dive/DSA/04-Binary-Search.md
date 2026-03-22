# 📚 DSA Deep Dive — Binary Search

---

## 🧠 Core Concept

Binary search reduces the search space by **half** each iteration → O(log n).
Requires the search space to be **sorted** or **monotonic** (a condition that changes from false→true or true→false).

---

## 🔑 Templates

### Standard Binary Search
```java
int lo = 0, hi = nums.length - 1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2; // avoids overflow
    if (nums[mid] == target) return mid;
    else if (nums[mid] < target) lo = mid + 1;
    else hi = mid - 1;
}
return -1;
```

### Find First True (Left Boundary)
```java
// Find leftmost position where condition is true
int lo = 0, hi = nums.length - 1, ans = -1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (condition(mid)) { ans = mid; hi = mid - 1; } // search left
    else lo = mid + 1;
}
return ans;
```

### Find Last True (Right Boundary)
```java
int lo = 0, hi = nums.length - 1, ans = -1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (condition(mid)) { ans = mid; lo = mid + 1; } // search right
    else hi = mid - 1;
}
return ans;
```

### Binary Search on Answer Space
```java
// When searching for minimum/maximum value satisfying a condition
int lo = minPossible, hi = maxPossible;
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (canAchieve(mid)) hi = mid;   // try smaller
    else lo = mid + 1;
}
return lo;
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Order Book Price Search (FX Trading)
**Problem**: Given sorted bid prices in an FX order book, find the best bid at or below a target rate.

```java
// Find rightmost bid price <= targetRate
int lo = 0, hi = bids.length - 1, bestBid = -1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (bids[mid] <= targetRate) { bestBid = mid; lo = mid + 1; }
    else hi = mid - 1;
}
return bestBid == -1 ? -1 : bids[bestBid];
```
**Where it applies**: FX option pricing — finding best available rate in sorted order book.

---

### Use Case 2: Capacity Planning — Minimum Servers
**Problem**: Given workloads and server capacity, find minimum number of servers needed.

```java
// Binary search on answer: minimum servers
boolean canHandle(int[] workloads, int servers, int maxLoad) {
    int count = 1, current = 0;
    for (int w : workloads) {
        if (current + w > maxLoad) { count++; current = 0; }
        current += w;
    }
    return count <= servers;
}

int lo = max(workloads), hi = sum(workloads);
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (canHandle(workloads, servers, mid)) hi = mid;
    else lo = mid + 1;
}
return lo; // minimum max load per server
```
**Where it applies**: Kafka partition sizing, microservice load distribution.

---

### Use Case 3: Search in Rotated Array (Distributed Systems)
**Problem**: A sorted log array was rotated at an unknown pivot — find a target timestamp.

```java
int lo = 0, hi = nums.length - 1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (nums[mid] == target) return mid;
    if (nums[lo] <= nums[mid]) { // left half is sorted
        if (nums[lo] <= target && target < nums[mid]) hi = mid - 1;
        else lo = mid + 1;
    } else { // right half is sorted
        if (nums[mid] < target && target <= nums[hi]) lo = mid + 1;
        else hi = mid - 1;
    }
}
return -1;
```
**Where it applies**: Searching logs in distributed systems where ring buffers rotate.

---

### Use Case 4: Find Peak Load (Monitoring / Alerting)
**Problem**: In a unimodal array of request counts (increases then decreases), find the peak load minute.

```java
int lo = 0, hi = nums.length - 1;
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (nums[mid] < nums[mid + 1]) lo = mid + 1; // ascending side
    else hi = mid;                                // descending side
}
return lo; // peak index
```
**Where it applies**: Finding peak traffic window in API monitoring dashboards.

---

### Use Case 5: Database Index Lookup (Range Query)
**Problem**: Given sorted user IDs in a DB index, find the range [lo, hi] of IDs between two values.

```java
// Find first occurrence >= left
int firstPos(int[] ids, int val) {
    int lo = 0, hi = ids.length - 1, ans = ids.length;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (ids[mid] >= val) { ans = mid; hi = mid - 1; }
        else lo = mid + 1;
    }
    return ans;
}

// Find last occurrence <= right
int lastPos(int[] ids, int val) {
    int lo = 0, hi = ids.length - 1, ans = -1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (ids[mid] <= val) { ans = mid; lo = mid + 1; }
        else hi = mid - 1;
    }
    return ans;
}
```
**Where it applies**: B-tree index range scans in PostgreSQL, DB2.

---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Binary Search | Standard | Easy |
| 2 | First Bad Version | Left boundary | Easy |
| 3 | Search Insert Position | Left boundary | Easy |
| 4 | Find First and Last Position | Left + Right boundary | Medium |
| 5 | Search in Rotated Sorted Array | Modified BS | Medium |
| 6 | Find Peak Element | Peak finding | Medium |
| 7 | Koko Eating Bananas | Binary search on answer | Medium |
| 8 | Minimum in Rotated Sorted Array | Modified BS | Medium |
| 9 | Split Array Largest Sum | Binary search on answer | Hard |
| 10 | Median of Two Sorted Arrays | Advanced BS | Hard |

---

## ⚠️ Common Mistakes

- Using `(lo + hi) / 2` → causes integer overflow for large indices, use `lo + (hi - lo) / 2`
- Wrong loop condition: use `lo <= hi` for exact match, `lo < hi` for boundary search
- Not updating `lo = mid + 1` / `hi = mid - 1` correctly — causes infinite loop
- Applying binary search on unsorted or non-monotonic space
- Forgetting that "binary search on answer" needs a valid range (`lo`, `hi`) to search within
