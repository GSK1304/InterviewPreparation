# 📚 DSA Deep Dive — Stack & Queue

---

## 🧠 Core Concepts

### Stack (LIFO — Last In First Out)
```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);       // push to top
stack.pop();         // remove top
stack.peek();        // view top without removing
stack.isEmpty();
```

### Queue (FIFO — First In First Out)
```java
Queue<Integer> queue = new LinkedList<>();
queue.offer(1);      // enqueue
queue.poll();        // dequeue
queue.peek();        // view front
queue.isEmpty();
```

### Deque (Double-ended — use as both Stack and Queue)
```java
Deque<Integer> deque = new ArrayDeque<>();
deque.addFirst(1);   deque.addLast(2);
deque.removeFirst(); deque.removeLast();
deque.peekFirst();   deque.peekLast();
```

---

## 🔑 Key Techniques

### Monotonic Stack (Next Greater Element)
```java
// For each element, find the next greater element to its right
int[] result = new int[nums.length];
Arrays.fill(result, -1);
Deque<Integer> stack = new ArrayDeque<>(); // stores indices
for (int i = 0; i < nums.length; i++) {
    while (!stack.isEmpty() && nums[i] > nums[stack.peek()])
        result[stack.pop()] = nums[i];
    stack.push(i);
}
```

### Monotonic Deque (Sliding Window Maximum)
```java
Deque<Integer> deque = new ArrayDeque<>(); // stores indices
int[] result = new int[nums.length - k + 1];
for (int i = 0; i < nums.length; i++) {
    // remove elements outside window
    while (!deque.isEmpty() && deque.peekFirst() < i - k + 1)
        deque.pollFirst();
    // maintain decreasing order
    while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i])
        deque.pollLast();
    deque.addLast(i);
    if (i >= k - 1) result[i - k + 1] = nums[deque.peekFirst()];
}
```

### Valid Parentheses
```java
Deque<Character> stack = new ArrayDeque<>();
Map<Character, Character> map = Map.of(')', '(', ']', '[', '}', '{');
for (char c : s.toCharArray()) {
    if (!map.containsKey(c)) stack.push(c);
    else if (stack.isEmpty() || stack.pop() != map.get(c)) return false;
}
return stack.isEmpty();
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Undo/Redo in Trading UI (kACE)
**Problem**: Implement undo/redo for a pricing form with field changes.

```java
Deque<String> undoStack = new ArrayDeque<>();
Deque<String> redoStack = new ArrayDeque<>();

void applyChange(String state) {
    undoStack.push(state);
    redoStack.clear(); // new change clears redo history
}
void undo() {
    if (!undoStack.isEmpty()) redoStack.push(undoStack.pop());
}
void redo() {
    if (!redoStack.isEmpty()) undoStack.push(redoStack.pop());
}
```
**Where it applies**: Pricing form field clearing/restoring in kACE Phoenix.

---

### Use Case 2: WebSocket Message Queue (Kafka / STOMP)
**Problem**: Process incoming WebSocket messages in order while allowing priority messages to jump queue.

```java
// Priority queue for ordered WebSocket message processing
PriorityQueue<Message> pq = new PriorityQueue<>(
    (a, b) -> a.priority != b.priority ? b.priority - a.priority
                                        : a.timestamp - b.timestamp
);
pq.offer(new Message("RFQ_UPDATE", 2, timestamp));
pq.offer(new Message("HEARTBEAT", 1, timestamp));
Message next = pq.poll(); // processes RFQ_UPDATE first
```
**Where it applies**: kACE WebSocket/STOMP message handling, Kafka consumer ordering.

---

### Use Case 3: Expression Evaluator (Formula Engine)
**Problem**: Evaluate arithmetic expressions like `"3 + 2 * 4"` respecting precedence.

```java
Deque<Integer> nums = new ArrayDeque<>();
Deque<Character> ops = new ArrayDeque<>();
for (int i = 0; i < s.length(); i++) {
    char c = s.charAt(i);
    if (Character.isDigit(c)) {
        int num = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i)))
            num = num * 10 + (s.charAt(i++) - '0');
        i--;
        nums.push(num);
    } else if (c == '+' || c == '-' || c == '*' || c == '/') {
        while (!ops.isEmpty() && hasPrecedence(c, ops.peek()))
            nums.push(applyOp(ops.pop(), nums.pop(), nums.pop()));
        ops.push(c);
    }
}
while (!ops.isEmpty()) nums.push(applyOp(ops.pop(), nums.pop(), nums.pop()));
return nums.pop();
```
**Where it applies**: Pricing formula engines, options payoff calculators.

---

### Use Case 4: BFS Level Order — Org Chart / Team Tree
**Problem**: Print all members of a team hierarchy level by level.

```java
Queue<TreeNode> queue = new LinkedList<>();
queue.offer(root);
List<List<Integer>> result = new ArrayList<>();
while (!queue.isEmpty()) {
    int size = queue.size();
    List<Integer> level = new ArrayList<>();
    for (int i = 0; i < size; i++) {
        TreeNode node = queue.poll();
        level.add(node.val);
        if (node.left != null) queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
    result.add(level);
}
```
**Where it applies**: Team hierarchy display, org charts, dependency resolution.

---

### Use Case 5: Stock Span Problem (Trading Analytics)
**Problem**: For each day, find how many consecutive days before it had price ≤ today's price.

```java
// Monotonic stack — classic trading analytics problem
int[] span = new int[prices.length];
Deque<Integer> stack = new ArrayDeque<>(); // stores indices
for (int i = 0; i < prices.length; i++) {
    while (!stack.isEmpty() && prices[stack.peek()] <= prices[i])
        stack.pop();
    span[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
    stack.push(i);
}
```
**Where it applies**: FX rate trend analysis, option pricing momentum signals.

---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Valid Parentheses | Stack | Easy |
| 2 | Min Stack | Stack with aux | Easy |
| 3 | Implement Queue using Stacks | Two Stacks | Easy |
| 4 | Daily Temperatures | Monotonic Stack | Medium |
| 5 | Stock Span Problem | Monotonic Stack | Medium |
| 6 | Sliding Window Maximum | Monotonic Deque | Hard |
| 7 | Largest Rectangle in Histogram | Monotonic Stack | Hard |
| 8 | Decode String | Stack | Medium |
| 9 | Number of Islands (BFS) | BFS + Queue | Medium |
| 10 | Rotten Oranges | Multi-source BFS | Medium |

---

## ⚠️ Common Mistakes

- Using `Stack` class in Java — prefer `ArrayDeque` (faster, no legacy overhead)
- Forgetting `isEmpty()` check before `peek()` / `pop()`
- Monotonic stack: confusing when to use index vs value in stack
- BFS: not marking nodes visited **before** adding to queue (causes duplicates)
