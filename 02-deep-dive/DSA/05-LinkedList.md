# 📚 DSA Deep Dive — Linked List

---

## 🧠 Core Concepts

```java
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}
```

- **No random access** — must traverse from head: O(n)
- **Insert/delete at known node**: O(1)
- **Always draw pointers** before coding to avoid losing references

---

## 🔑 Key Techniques

### Reverse a Linked List
```java
ListNode prev = null, cur = head;
while (cur != null) {
    ListNode next = cur.next; // save next
    cur.next = prev;          // reverse pointer
    prev = cur;               // move prev
    cur = next;               // move cur
}
return prev; // new head
```

### Find Middle (Fast & Slow Pointer)
```java
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
}
return slow; // middle node
```

### Detect Cycle
```java
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
    if (slow == fast) return true; // cycle detected
}
return false;
```

### Find Cycle Start
```java
// After detecting meeting point, reset one pointer to head
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next; fast = fast.next.next;
    if (slow == fast) {
        slow = head; // reset to head
        while (slow != fast) { slow = slow.next; fast = fast.next; }
        return slow; // cycle start
    }
}
return null;
```

### Merge Two Sorted Lists
```java
ListNode dummy = new ListNode(0), cur = dummy;
while (l1 != null && l2 != null) {
    if (l1.val <= l2.val) { cur.next = l1; l1 = l1.next; }
    else { cur.next = l2; l2 = l2.next; }
    cur = cur.next;
}
cur.next = l1 != null ? l1 : l2;
return dummy.next;
```

### Remove Nth Node from End
```java
ListNode dummy = new ListNode(0); dummy.next = head;
ListNode fast = dummy, slow = dummy;
for (int i = 0; i <= n; i++) fast = fast.next; // advance fast by n+1
while (fast != null) { slow = slow.next; fast = fast.next; }
slow.next = slow.next.next; // remove nth from end
return dummy.next;
```

---

## 🌍 Real-World Use Cases

### Use Case 1: LRU Cache (Backend Caching — kACE dropdown cache)
**Problem**: Implement LRU Cache — O(1) get and put.

```java
class LRUCache {
    int capacity;
    Map<Integer, Node> map = new HashMap<>();
    Node head = new Node(0, 0), tail = new Node(0, 0);

    LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail; tail.prev = head;
    }

    int get(int key) {
        if (!map.containsKey(key)) return -1;
        Node node = map.get(key);
        remove(node); addToFront(node);
        return node.val;
    }

    void put(int key, int val) {
        if (map.containsKey(key)) remove(map.get(key));
        Node node = new Node(key, val);
        addToFront(node); map.put(key, node);
        if (map.size() > capacity) {
            Node lru = tail.prev;
            remove(lru); map.remove(lru.key);
        }
    }

    void remove(Node n) { n.prev.next = n.next; n.next.prev = n.prev; }
    void addToFront(Node n) { n.next = head.next; n.prev = head; head.next.prev = n; head.next = n; }
}
```
**Where it applies**: kACE dropdown cache (`dropdownCache.ts`), Spring Boot `@Cacheable` eviction.
> 🏭 **Industry Example**: Redis implements LRU eviction for cache memory management. CPU hardware caches (L1/L2/L3) use LRU policy. CDN edge servers (Cloudflare, Akamai) use LRU to evict cold content. Memcached's eviction is LRU-based.
> 🏦 **kACE Context**: Spring Boot `@Cacheable` with LRU eviction for layout configs; dropdown cache eviction when memory pressure hits.


---

### Use Case 2: Message Queue with Priority Re-ordering
**Problem**: Reorder a linked list of messages — all high-priority first, then low-priority.

```java
// Partition list: high priority nodes first
ListNode dummy1 = new ListNode(0), dummy2 = new ListNode(0);
ListNode p1 = dummy1, p2 = dummy2, cur = head;
while (cur != null) {
    if (cur.priority == HIGH) { p1.next = cur; p1 = p1.next; }
    else { p2.next = cur; p2 = p2.next; }
    cur = cur.next;
}
p1.next = dummy2.next; p2.next = null;
return dummy1.next;
```
**Where it applies**: Kafka message reordering, WebSocket priority message handling.
> 🏭 **Industry Example**: RabbitMQ uses a priority queue (linked list internally) for message routing. Apache Kafka's consumer group rebalancing uses list partitioning to redistribute partitions. AWS SQS FIFO queues maintain strict insertion-order linked lists.
> 🏦 **kACE Context**: Kafka message priority re-ordering — RFQ events jump ahead of analytics events in the processing queue.


---

### Use Case 3: Detect Circular Dependencies (Build System)
**Problem**: Detect if module dependencies form a cycle — model as linked list traversal.

```java
// Floyd's cycle detection applied to dependency chain
boolean hasCycle(Module head) {
    Module slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next; fast = fast.next.next;
        if (slow == fast) return true;
    }
    return false;
}
```
**Where it applies**: Gradle/Maven dependency cycle detection, Spring bean circular dependency.
> 🏭 **Industry Example**: npm package manager uses cycle detection in the dependency graph to warn about circular dependencies. Spring Framework detects circular bean dependencies at startup. Gradle's build system flags circular task dependencies.
> 🏦 **kACE Context**: Gradle/Maven dependency cycle detection in the kACE microservice build system.


---

### Use Case 4: Merge K Sorted Streams (Real-time Data)
**Problem**: Merge K sorted log streams into one sorted stream.

```java
// Use min-heap to always pick smallest head across K lists
PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);
for (ListNode list : lists) if (list != null) pq.offer(list);
ListNode dummy = new ListNode(0), cur = dummy;
while (!pq.isEmpty()) {
    ListNode node = pq.poll();
    cur.next = node; cur = cur.next;
    if (node.next != null) pq.offer(node.next);
}
return dummy.next;
```
**Where it applies**: Merging sorted Kafka partition streams, multi-source log aggregation.
> 🏭 **Industry Example**: Apache Kafka's log compaction merges multiple sorted segment files. Google's MapReduce merge-sorts output from K reducers. Elasticsearch merges multiple sorted index segments during index optimization.
> 🏦 **kACE Context**: Merging sorted log streams from kACE microservices (gateway, pricing, RFQ, auth) for unified audit trail.


---

### Use Case 5: Palindrome Check (Data Validation)
**Problem**: Check if a singly linked list is a palindrome efficiently.

```java
// Find middle, reverse second half, compare
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; }
ListNode rev = reverse(slow); // reverse second half
ListNode p1 = head, p2 = rev;
while (p2 != null) {
    if (p1.val != p2.val) return false;
    p1 = p1.next; p2 = p2.next;
}
return true;
```
**Where it applies**: Data validation, symmetric trade ticket verification.
> 🏭 **Industry Example**: DNA sequence analysis uses palindrome detection to find restriction enzyme cut sites. Version control systems use palindrome checks for symmetric merge conflict resolution markers.
> 🏦 **kACE Context**: Symmetric trade ticket verification — checking if bid/ask structure is mirror-symmetric.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Reverse Linked List | Three pointers | Easy |
| 2 | Merge Two Sorted Lists | Merge | Easy |
| 3 | Linked List Cycle | Fast & Slow | Easy |
| 4 | Middle of Linked List | Fast & Slow | Easy |
| 5 | Remove Nth Node from End | Two pointers | Medium |
| 6 | LRU Cache | HashMap + DLL | Medium |
| 7 | Reorder List | Find mid + reverse + merge | Medium |
| 8 | Linked List Cycle II (find start) | Floyd's algo | Medium |
| 9 | Sort List | Merge sort on list | Medium |
| 10 | Merge K Sorted Lists | Min-heap | Hard |

---

## ⚠️ Common Mistakes

- Losing reference to `next` before reversing — always save `next` first
- Not using a `dummy` node — head can change, dummy prevents NPE at head
- Fast pointer: check `fast != null && fast.next != null` (both conditions)
- Off-by-one in "remove nth from end" — advance fast by `n+1`, not `n`
- Forgetting to set `tail.next = null` after splitting — causes cycle in merged list
