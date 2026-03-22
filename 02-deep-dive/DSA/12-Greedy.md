# 📚 DSA Deep Dive — Greedy Algorithms

---

## 🧠 Core Concept

A **Greedy** algorithm makes the **locally optimal choice** at each step, hoping it leads to a **globally optimal** solution.

### When does Greedy work?
✅ **Greedy Choice Property**: A global optimum can be reached by local optimal choices
✅ **Optimal Substructure**: Optimal solution contains optimal solutions to subproblems
❌ Does NOT work when future choices depend on current choice (use DP instead)

### Greedy vs DP
| | Greedy | DP |
|--|--------|-----|
| Choices | One irrevocable choice per step | Considers all choices |
| Speed | Faster O(n log n) typically | Slower O(n²) or more |
| Correctness | Only for specific problems | General |
| When to use | Provably optimal local choice | Overlapping subproblems |

---

## 🔑 Key Patterns & Templates

### Activity Selection / Interval Scheduling
```java
// Maximize number of non-overlapping intervals
// Key insight: always pick interval that ends earliest
int maxActivities(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[1] - b[1]); // sort by END time
    int count = 1, lastEnd = intervals[0][1];
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] >= lastEnd) { // no overlap
            count++;
            lastEnd = intervals[i][1];
        }
    }
    return count;
}
```

### Meeting Rooms II (Min Rooms)
```java
// Minimum rooms = maximum overlapping meetings at any point
int minMeetingRooms(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]); // sort by start
    PriorityQueue<Integer> endTimes = new PriorityQueue<>(); // min-heap of end times
    for (int[] interval : intervals) {
        if (!endTimes.isEmpty() && endTimes.peek() <= interval[0])
            endTimes.poll(); // reuse room
        endTimes.offer(interval[1]);
    }
    return endTimes.size(); // rooms in use
}
```

### Jump Game I (Can Reach End?)
```java
boolean canJump(int[] nums) {
    int maxReach = 0;
    for (int i = 0; i < nums.length; i++) {
        if (i > maxReach) return false; // stuck
        maxReach = Math.max(maxReach, i + nums[i]);
    }
    return true;
}
```

### Jump Game II (Min Jumps)
```java
int jump(int[] nums) {
    int jumps = 0, curEnd = 0, farthest = 0;
    for (int i = 0; i < nums.length - 1; i++) {
        farthest = Math.max(farthest, i + nums[i]);
        if (i == curEnd) { jumps++; curEnd = farthest; } // must jump
    }
    return jumps;
}
```

### Gas Station (Circular Tour)
```java
int canCompleteCircuit(int[] gas, int[] cost) {
    int totalGas = 0, tank = 0, start = 0;
    for (int i = 0; i < gas.length; i++) {
        totalGas += gas[i] - cost[i];
        tank += gas[i] - cost[i];
        if (tank < 0) { start = i + 1; tank = 0; } // reset start
    }
    return totalGas >= 0 ? start : -1;
}
```

### Fractional Knapsack
```java
// Unlike 0/1 knapsack — greedy works because we can take fractions
double fractionalKnapsack(int W, int[][] items) {
    // Sort by value/weight ratio descending
    Arrays.sort(items, (a, b) -> Double.compare((double)b[0]/b[1], (double)a[0]/a[1]));
    double maxValue = 0;
    for (int[] item : items) {
        if (W >= item[1]) { maxValue += item[0]; W -= item[1]; }
        else { maxValue += item[0] * ((double)W / item[1]); break; }
    }
    return maxValue;
}
```

### Assign Cookies (Greedy Match)
```java
// Greedily satisfy smallest appetite child with smallest sufficient cookie
int findContentChildren(int[] g, int[] s) {
    Arrays.sort(g); Arrays.sort(s);
    int child = 0, cookie = 0;
    while (child < g.length && cookie < s.length) {
        if (s[cookie] >= g[child]) child++; // satisfied
        cookie++;
    }
    return child;
}
```

### Partition Labels
```java
// Partition string so each letter appears in at most one part
List<Integer> partitionLabels(String s) {
    int[] last = new int[26];
    for (int i = 0; i < s.length(); i++) last[s.charAt(i) - 'a'] = i;
    List<Integer> result = new ArrayList<>();
    int start = 0, end = 0;
    for (int i = 0; i < s.length(); i++) {
        end = Math.max(end, last[s.charAt(i) - 'a']);
        if (i == end) { result.add(end - start + 1); start = i + 1; }
    }
    return result;
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Trade Window Scheduling (FX Options)
**Problem**: Given FX option trade windows, schedule maximum non-overlapping trades.

```java
// Activity selection — maximize trades without overlap
// Sort by expiry (end time), greedily pick earliest-ending trade
Arrays.sort(trades, (a, b) -> a.expiry.compareTo(b.expiry));
List<Trade> scheduled = new ArrayList<>();
LocalDate lastExpiry = LocalDate.MIN;
for (Trade t : trades) {
    if (!t.startDate.isBefore(lastExpiry)) {
        scheduled.add(t);
        lastExpiry = t.expiry;
    }
}
```
**Where it applies**: FX option trade scheduling, expiry ladder optimization in kACE.

---

### Use Case 2: Sprint Capacity — Meeting Room Allocation
**Problem**: Given team meetings/standups/reviews, find minimum number of conference rooms needed.

```java
// Min meeting rooms = max concurrent meetings
// Same as Meeting Rooms II pattern above
int minRooms = minMeetingRooms(meetings);
System.out.println("Need " + minRooms + " conference rooms for kACE team");
```
**Where it applies**: Team scheduling in kACE management portal, interview slot allocation.

---

### Use Case 3: Kafka Partition Assignment
**Problem**: Assign Kafka topics to brokers to balance load — fractional knapsack style.

```java
// Greedily assign each topic to broker with most remaining capacity
PriorityQueue<Broker> maxCapacity = new PriorityQueue<>(
    (a, b) -> b.remainingCapacity - a.remainingCapacity
);
for (Topic topic : topics) {
    Broker best = maxCapacity.poll();
    best.assign(topic);
    maxCapacity.offer(best);
}
```
**Where it applies**: Kafka broker load balancing, kACE microservice resource allocation.

---

### Use Case 4: Jump Game — Feature Dependency Release
**Problem**: Given features where each feature unlocks N next features, find if all can be released and minimum releases needed.

```java
// Model as jump game: feature[i] = how many subsequent features it unlocks
// Can we reach the final feature?
boolean canReleaseAll = canJump(featureUnlocks);
int minReleases = jump(featureUnlocks);
```
**Where it applies**: Feature flag release planning, CI/CD pipeline stage progression in kACE.

---

### Use Case 5: Log Partition by Service (Partition Labels)
**Problem**: Partition a log stream so each service's logs stay in one contiguous segment.

```java
// Each "character" = service identifier
// Each "partition" = contiguous segment where service logs don't split
List<Integer> partitions = partitionLabels(logServiceIds);
// Each partition can be processed independently
```
**Where it applies**: Log partitioning for parallel processing, Kafka topic partitioning strategy.

---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Assign Cookies | Greedy match | Easy |
| 2 | Jump Game I | Greedy reach | Medium |
| 3 | Jump Game II | Greedy min jumps | Medium |
| 4 | Non-overlapping Intervals | Interval scheduling | Medium |
| 5 | Meeting Rooms II | Min rooms heap | Medium |
| 6 | Gas Station | Circular greedy | Medium |
| 7 | Partition Labels | Last occurrence greedy | Medium |
| 8 | Candy (Rating Array) | Two-pass greedy | Hard |
| 9 | Reorganize String | Greedy + heap | Medium |
| 10 | Minimum Number of Arrows | Interval scheduling | Medium |

---

## ⚠️ Common Mistakes

- Applying greedy when DP is needed (e.g., 0/1 Knapsack — greedy fails, fractional works)
- Wrong sort key — interval scheduling must sort by **end** time, not start
- Gas station: don't restart from scratch each time — track total surplus
- Jump Game II: update `curEnd` only when `i == curEnd`, not every step
- Greedy correctness: always verify with an **exchange argument** or counterexample
- Missing edge cases: all intervals overlap, single element, empty input
