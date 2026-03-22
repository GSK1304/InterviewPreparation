# 📚 DSA Deep Dive — Sorting & Searching

---

## 🧠 Core Sorting Algorithms

| Algorithm | Best | Average | Worst | Space | Stable | Use When |
|-----------|------|---------|-------|-------|--------|----------|
| Bubble Sort | O(n) | O(n²) | O(n²) | O(1) | ✅ | Never in prod |
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) | ❌ | Never in prod |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) | ✅ | Small/nearly sorted |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) | ✅ | Default safe choice |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) | ❌ | In-place, fast avg |
| Heap Sort | O(n log n) | O(n log n) | O(n log n) | O(1) | ❌ | Space-constrained |
| Counting Sort | O(n+k) | O(n+k) | O(n+k) | O(k) | ✅ | Small integer range |
| Radix Sort | O(nk) | O(nk) | O(nk) | O(n+k) | ✅ | Fixed-length integers |

**Java internals:**
- `Arrays.sort(primitives)` → Dual-pivot QuickSort
- `Arrays.sort(objects)` / `Collections.sort()` → TimSort (Merge + Insertion)

---

## 🔑 Key Implementations

### Merge Sort
```java
void mergeSort(int[] arr, int lo, int hi) {
    if (lo >= hi) return;
    int mid = lo + (hi - lo) / 2;
    mergeSort(arr, lo, mid);
    mergeSort(arr, mid+1, hi);
    merge(arr, lo, mid, hi);
}

void merge(int[] arr, int lo, int mid, int hi) {
    int[] temp = Arrays.copyOfRange(arr, lo, hi+1);
    int i = 0, j = mid - lo + 1, k = lo;
    while (i <= mid-lo && j <= hi-lo)
        arr[k++] = temp[i] <= temp[j] ? temp[i++] : temp[j++];
    while (i <= mid-lo) arr[k++] = temp[i++];
    while (j <= hi-lo) arr[k++] = temp[j++];
}
```

### Quick Sort
```java
void quickSort(int[] arr, int lo, int hi) {
    if (lo >= hi) return;
    int pivot = partition(arr, lo, hi);
    quickSort(arr, lo, pivot - 1);
    quickSort(arr, pivot + 1, hi);
}

int partition(int[] arr, int lo, int hi) {
    int pivot = arr[hi], i = lo - 1;
    for (int j = lo; j < hi; j++)
        if (arr[j] <= pivot) swap(arr, ++i, j);
    swap(arr, i+1, hi);
    return i + 1;
}
```

### Custom Sort in Java
```java
// Sort by first element, then by second element descending
Arrays.sort(intervals, (a, b) -> a[0] != b[0] ? a[0] - b[0] : b[1] - a[1]);

// Sort strings by length then alphabetically
Arrays.sort(words, (a, b) -> a.length() != b.length() ? a.length() - b.length()
                                                       : a.compareTo(b));

// Sort objects
Collections.sort(list, Comparator.comparing(Person::getAge)
                                 .thenComparing(Person::getName));
```

### Heap (PriorityQueue) for Top-K
```java
// Top K largest elements
PriorityQueue<Integer> minHeap = new PriorityQueue<>(); // min-heap of size k
for (int num : nums) {
    minHeap.offer(num);
    if (minHeap.size() > k) minHeap.poll(); // remove smallest
}
return minHeap.peek(); // kth largest

// Top K smallest — use max-heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
for (int num : nums) {
    maxHeap.offer(num);
    if (maxHeap.size() > k) maxHeap.poll();
}
return maxHeap.peek(); // kth smallest
```

---

## 🌍 Real-World Use Cases

### Use Case 1: FX Order Book Sorting (kACE Trading)
**Problem**: Sort FX options orders by strike price, then by expiry date, then by premium.

```java
// Multi-key sort — classic trading platform requirement
orders.sort(Comparator
    .comparingDouble(Order::getStrikePrice)
    .thenComparing(Order::getExpiryDate)
    .thenComparingDouble(Order::getPremium).reversed());

// Or with lambda for complex logic
orders.sort((a, b) -> {
    if (Double.compare(a.strike, b.strike) != 0)
        return Double.compare(a.strike, b.strike);
    if (!a.expiry.equals(b.expiry))
        return a.expiry.compareTo(b.expiry);
    return Double.compare(b.premium, a.premium); // desc
});
```
**Where it applies**: kACE pricing grid — sorting strategies, legs, expiry ladders.

---

### Use Case 2: Top-K Active RFQ Sessions (Monitoring)
**Problem**: From millions of RFQ sessions, find the K most active ones by message count.

```java
// Min-heap of size K — O(n log k) vs O(n log n) for full sort
PriorityQueue<Session> minHeap = new PriorityQueue<>(
    (a, b) -> a.messageCount - b.messageCount
);
for (Session s : allSessions) {
    minHeap.offer(s);
    if (minHeap.size() > k) minHeap.poll();
}
// minHeap now contains top-K sessions
List<Session> topK = new ArrayList<>(minHeap);
topK.sort((a, b) -> b.messageCount - a.messageCount);
```
**Where it applies**: kACE RFQ monitoring dashboard, Kafka consumer lag top-K reporting.

---

### Use Case 3: Merge Intervals — Trade Window Consolidation
**Problem**: Given overlapping trade validity windows, merge them into non-overlapping intervals.

```java
// Sort by start time, then merge overlapping
Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
List<int[]> merged = new ArrayList<>();
for (int[] interval : intervals) {
    if (merged.isEmpty() || merged.get(merged.size()-1)[1] < interval[0])
        merged.add(interval);
    else
        merged.get(merged.size()-1)[1] = Math.max(merged.get(merged.size()-1)[1], interval[1]);
}
```
**Where it applies**: FX option expiry window merging, maintenance window consolidation.

---

### Use Case 4: Counting Sort — Log Level Aggregation
**Problem**: Sort thousands of log entries by severity level (0=DEBUG, 1=INFO, 2=WARN, 3=ERROR).

```java
// Counting sort O(n) — perfect for small integer range
int[] count = new int[4]; // 4 levels
for (LogEntry log : logs) count[log.level]++;
// Reconstruct sorted order
int idx = 0;
for (int level = 0; level < 4; level++)
    for (int i = 0; i < count[level]; i++)
        sortedLogs[idx++] = getLogsOfLevel(level);
```
**Where it applies**: Log aggregation in Jenkins CI, GitLab pipeline log sorting.

---

### Use Case 5: Find Median from Data Stream (Real-time Analytics)
**Problem**: Continuously find median of incoming trade prices in O(log n) per update.

```java
// Two heaps: maxHeap for lower half, minHeap for upper half
PriorityQueue<Integer> lower = new PriorityQueue<>(Collections.reverseOrder()); // max-heap
PriorityQueue<Integer> upper = new PriorityQueue<>(); // min-heap

void addNum(int num) {
    lower.offer(num);
    upper.offer(lower.poll()); // balance
    if (lower.size() < upper.size()) lower.offer(upper.poll());
}

double findMedian() {
    return lower.size() > upper.size() ? lower.peek()
                                       : (lower.peek() + upper.peek()) / 2.0;
}
```
**Where it applies**: Real-time FX rate median computation, streaming trade price analytics.

---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Sort Colors (Dutch Flag) | 3-way partition | Medium |
| 2 | Merge Intervals | Sort + merge | Medium |
| 3 | Kth Largest Element | QuickSelect / Heap | Medium |
| 4 | Top K Frequent Elements | Heap / Bucket sort | Medium |
| 5 | Find Median from Data Stream | Two heaps | Hard |
| 6 | Meeting Rooms II | Sort + heap | Medium |
| 7 | Largest Number | Custom sort | Medium |
| 8 | Sort List | Merge sort on list | Medium |
| 9 | K Closest Points to Origin | Heap | Medium |
| 10 | Wiggle Sort | In-place partition | Medium |

---

## ⚠️ Common Mistakes

- Using `a - b` comparator with large integers → overflow; use `Integer.compare(a, b)`
- Confusing stable sort requirement — use Merge/TimSort when stability matters
- QuickSort worst case O(n²) on already-sorted input → randomize pivot in interviews
- Heap: Java `PriorityQueue` is a **min-heap** by default — use `Collections.reverseOrder()` for max
- Merge intervals: sort by **start** time first, then check overlap with `merged.last.end`
- Counting sort: only works for **non-negative integers** with small range (k)
