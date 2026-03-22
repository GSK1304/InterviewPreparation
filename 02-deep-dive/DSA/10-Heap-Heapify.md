# 📚 DSA Deep Dive — Heap & Heapify

---

## 🧠 Core Concepts

A **Heap** is a complete binary tree stored as an array satisfying the heap property:
- **Min-Heap**: parent ≤ children → root is the minimum
- **Max-Heap**: parent ≥ children → root is the maximum

### Array Representation
```
Index:    0   1   2   3   4   5   6
Value:    1   3   5   7   9   8   6

Tree:        1
           /   \
          3     5
         / \   / \
        7   9 8   6

Parent of i  → (i - 1) / 2
Left child   → 2*i + 1
Right child  → 2*i + 2
```

---

## 🔑 Key Operations

### Heapify (Build Heap from Array) — O(n)
```java
// Start from last non-leaf node and sift down
void buildHeap(int[] arr) {
    int n = arr.length;
    for (int i = n / 2 - 1; i >= 0; i--)
        siftDown(arr, i, n);
}

void siftDown(int[] arr, int i, int n) {
    int smallest = i;
    int left = 2 * i + 1, right = 2 * i + 2;
    if (left < n && arr[left] < arr[smallest]) smallest = left;
    if (right < n && arr[right] < arr[smallest]) smallest = right;
    if (smallest != i) {
        swap(arr, i, smallest);
        siftDown(arr, smallest, n);
    }
}
```
> ⚠️ **Why O(n) not O(n log n)?** Most nodes are near the leaves and sift down only a few levels. Mathematical sum converges to O(n).

### Sift Up (After Insert) — O(log n)
```java
void siftUp(int[] arr, int i) {
    while (i > 0) {
        int parent = (i - 1) / 2;
        if (arr[parent] > arr[i]) { // min-heap
            swap(arr, parent, i);
            i = parent;
        } else break;
    }
}
```

### Heap Sort — O(n log n), O(1) space
```java
void heapSort(int[] arr) {
    int n = arr.length;
    // Step 1: Build max-heap O(n)
    for (int i = n / 2 - 1; i >= 0; i--)
        siftDownMax(arr, i, n);
    // Step 2: Extract max repeatedly O(n log n)
    for (int i = n - 1; i > 0; i--) {
        swap(arr, 0, i);          // move max to end
        siftDownMax(arr, 0, i);   // restore heap
    }
}
```

### Java PriorityQueue
```java
// Min-heap (default)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// Max-heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

// Custom comparator (by frequency)
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[1] - a[1]);

minHeap.offer(val);   // insert O(log n)
minHeap.poll();       // remove min O(log n)
minHeap.peek();       // view min O(1)
```

---

## 🔑 Key Patterns

### Top-K Elements
```java
// K largest — min-heap of size K
PriorityQueue<Integer> pq = new PriorityQueue<>();
for (int num : nums) {
    pq.offer(num);
    if (pq.size() > k) pq.poll(); // evict smallest
}
return new ArrayList<>(pq); // contains K largest
```

### K-Way Merge
```java
// Merge K sorted arrays/lists
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
// {value, arrayIndex, elementIndex}
for (int i = 0; i < lists.length; i++)
    if (!lists[i].isEmpty()) pq.offer(new int[]{lists[i].get(0), i, 0});

List<Integer> result = new ArrayList<>();
while (!pq.isEmpty()) {
    int[] cur = pq.poll();
    result.add(cur[0]);
    int nextIdx = cur[2] + 1;
    if (nextIdx < lists[cur[1]].size())
        pq.offer(new int[]{lists[cur[1]].get(nextIdx), cur[1], nextIdx});
}
```

### Two Heaps — Sliding Median
```java
PriorityQueue<Integer> lower = new PriorityQueue<>(Collections.reverseOrder()); // max-heap
PriorityQueue<Integer> upper = new PriorityQueue<>(); // min-heap

void addNum(int num) {
    if (lower.isEmpty() || num <= lower.peek()) lower.offer(num);
    else upper.offer(num);
    // Balance: lower can have at most 1 more than upper
    if (lower.size() > upper.size() + 1) upper.offer(lower.poll());
    if (upper.size() > lower.size()) lower.offer(upper.poll());
}

double findMedian() {
    return lower.size() > upper.size() ? lower.peek()
           : (lower.peek() + upper.peek()) / 2.0;
}
```

### Task Scheduler Pattern
```java
// Schedule tasks with cooldown period n
int leastInterval(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char t : tasks) freq[t - 'A']++;
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    for (int f : freq) if (f > 0) maxHeap.offer(f);

    int time = 0;
    Queue<int[]> cooldown = new LinkedList<>(); // {freq, availableAt}
    while (!maxHeap.isEmpty() || !cooldown.isEmpty()) {
        if (!cooldown.isEmpty() && cooldown.peek()[1] == time)
            maxHeap.offer(cooldown.poll()[0]);
        if (!maxHeap.isEmpty()) {
            int f = maxHeap.poll() - 1;
            if (f > 0) cooldown.offer(new int[]{f, time + n + 1});
        }
        time++;
    }
    return time;
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: RFQ Priority Queue (kACE Trading)
**Problem**: Process incoming RFQs — high-value/urgent ones first regardless of arrival order.

```java
PriorityQueue<RFQ> rfqQueue = new PriorityQueue<>((a, b) -> {
    if (a.urgency != b.urgency) return b.urgency - a.urgency; // high urgency first
    return Double.compare(b.notional, a.notional);            // then by notional
});

rfqQueue.offer(new RFQ("EUR/USD", 1_000_000, URGENT));
rfqQueue.offer(new RFQ("GBP/USD", 50_000_000, NORMAL));
RFQ next = rfqQueue.poll(); // processes URGENT first
```
**Where it applies**: kACE RFQ system — priority-based processing of incoming option requests.

---

### Use Case 2: Top-K Active Kafka Partitions (Monitoring)
**Problem**: From all Kafka partitions, find top-K with highest consumer lag for alerting.

```java
// O(n log k) — far better than sorting all O(n log n)
PriorityQueue<Partition> minHeap = new PriorityQueue<>(
    (a, b) -> a.lag - b.lag
);
for (Partition p : allPartitions) {
    minHeap.offer(p);
    if (minHeap.size() > k) minHeap.poll();
}
// minHeap = top K lagging partitions
```
**Where it applies**: Kafka consumer lag monitoring dashboard in kACE.

---

### Use Case 3: Merge K Sorted Log Streams
**Problem**: Merge sorted log streams from K microservices into a single chronological stream.

```java
// K-way merge with min-heap on timestamp
PriorityQueue<LogEntry> pq = new PriorityQueue<>(
    Comparator.comparing(e -> e.timestamp)
);
// Seed with first entry from each service
for (LogStream stream : streams)
    if (stream.hasNext()) pq.offer(stream.next());

List<LogEntry> merged = new ArrayList<>();
while (!pq.isEmpty()) {
    LogEntry entry = pq.poll();
    merged.add(entry);
    if (entry.source.hasNext()) pq.offer(entry.source.next());
}
```
**Where it applies**: Merging logs from kACE microservices (gateway, pricing, RFQ, auth), Jenkins build logs.

---

### Use Case 4: Sliding Window Median — FX Rate Volatility
**Problem**: For each window of K FX rate ticks, compute the median rate (volatility indicator).

```java
// Two heaps for O(log n) per insertion, O(1) median
// Use the two-heap pattern above
// For each new rate:
//   1. Add to appropriate heap
//   2. Rebalance
//   3. Read median from lower.peek()
double[] medians = new double[rates.length - k + 1];
for (int i = 0; i < rates.length; i++) {
    addNum(rates[i]);
    if (i >= k) removeNum(rates[i - k]); // remove outgoing element
    if (i >= k - 1) medians[i - k + 1] = findMedian();
}
```
**Where it applies**: FX option volatility computation, real-time spread monitoring in kACE.

---

### Use Case 5: Build Huffman Encoding (Data Compression)
**Problem**: Build optimal prefix-free code for compressing trading messages.

```java
// Greedy + min-heap: always merge two smallest frequency nodes
PriorityQueue<HuffNode> pq = new PriorityQueue<>(
    Comparator.comparingInt(n -> n.freq)
);
for (Map.Entry<Character, Integer> e : freqMap.entrySet())
    pq.offer(new HuffNode(e.getKey(), e.getValue()));

while (pq.size() > 1) {
    HuffNode left = pq.poll();
    HuffNode right = pq.poll();
    HuffNode merged = new HuffNode('\0', left.freq + right.freq);
    merged.left = left; merged.right = right;
    pq.offer(merged);
}
HuffNode root = pq.poll(); // Huffman tree root
```
**Where it applies**: Compressing Kafka message payloads, WebSocket frame compression.

---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Kth Largest Element in Array | Min-heap size K | Medium |
| 2 | Top K Frequent Elements | Max-heap / bucket | Medium |
| 3 | Find Median from Data Stream | Two heaps | Hard |
| 4 | Merge K Sorted Lists | K-way merge heap | Hard |
| 5 | Task Scheduler | Max-heap + cooldown | Medium |
| 6 | K Closest Points to Origin | Min-heap | Medium |
| 7 | Sliding Window Median | Two heaps + removal | Hard |
| 8 | Ugly Number II | Multi-heap merge | Medium |
| 9 | Reorganize String | Max-heap greedy | Medium |
| 10 | IPO (Maximize Capital) | Two heaps | Hard |

---

## ⚠️ Common Mistakes

- Confusing `heapify` O(n) vs inserting n elements one-by-one O(n log n)
- Java `PriorityQueue` is **min-heap** by default — always double-check
- Heap sort produces ascending order using a **max-heap** (counterintuitive)
- Two-heap median: balance invariant — `lower.size() >= upper.size()` always
- K-way merge: must re-offer the **next element from the same list**, not just any
- Removing arbitrary element from Java `PriorityQueue` is O(n) — use `LazyDeletion` for sliding median
