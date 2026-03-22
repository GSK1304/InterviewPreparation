# 📚 DSA Deep Dive — Balanced Trees & Advanced Structures

> AVL Trees (🟡 P2), Red-Black Trees (🟡 P2), B-Trees (🟡 P2), Skip List (🟢 P3)
> Focus: **Understand concepts and trade-offs** — implementation rarely asked in interviews.

---

## 1. AVL Trees — Self-Balancing BST

**Key Property**: For every node, `|height(left) - height(right)| ≤ 1` (balance factor ∈ {-1, 0, 1}).

**Why needed**: Regular BST degrades to O(n) on sorted input. AVL guarantees O(log n).

### Balance Factor & Rotations

```
Balance Factor = height(left subtree) - height(right subtree)

4 Rotation Cases:
┌─────────────┬────────────────────────────────────┐
│ Case        │ Fix                                │
├─────────────┼────────────────────────────────────┤
│ Left-Left   │ Right rotation on unbalanced node  │
│ Right-Right │ Left rotation on unbalanced node   │
│ Left-Right  │ Left rotation on child, then Right │
│ Right-Left  │ Right rotation on child, then Left │
└─────────────┴────────────────────────────────────┘
```

### Right Rotation (Left-Left Case)
```
    z                  y
   / \               /   \
  y   T4   →       x      z
 / \              / \    / \
x   T3           T1 T2  T3  T4
```

```java
TreeNode rightRotate(TreeNode z) {
    TreeNode y = z.left;
    TreeNode T3 = y.right;
    y.right = z;      // rotation
    z.left = T3;
    // Update heights
    z.height = 1 + Math.max(height(z.left), height(z.right));
    y.height = 1 + Math.max(height(y.left), height(y.right));
    return y; // new root
}

TreeNode leftRotate(TreeNode z) {
    TreeNode y = z.right;
    TreeNode T2 = y.left;
    y.left = z;
    z.right = T2;
    z.height = 1 + Math.max(height(z.left), height(z.right));
    y.height = 1 + Math.max(height(y.left), height(y.right));
    return y;
}
```

### Insert with Rebalancing
```java
TreeNode insert(TreeNode node, int key) {
    // Standard BST insert
    if (node == null) return new TreeNode(key);
    if (key < node.val) node.left = insert(node.left, key);
    else if (key > node.val) node.right = insert(node.right, key);
    else return node; // duplicate

    // Update height
    node.height = 1 + Math.max(height(node.left), height(node.right));

    // Get balance factor
    int balance = getBalance(node);

    // Fix imbalance — 4 cases
    if (balance > 1 && key < node.left.val)   return rightRotate(node);        // LL
    if (balance < -1 && key > node.right.val) return leftRotate(node);         // RR
    if (balance > 1 && key > node.left.val) {                                  // LR
        node.left = leftRotate(node.left);
        return rightRotate(node);
    }
    if (balance < -1 && key < node.right.val) {                                // RL
        node.right = rightRotate(node.right);
        return leftRotate(node);
    }
    return node;
}

int height(TreeNode n) { return n == null ? 0 : n.height; }
int getBalance(TreeNode n) { return n == null ? 0 : height(n.left) - height(n.right); }
```

### Complexity
| Operation | Time |
|-----------|------|
| Search | O(log n) |
| Insert | O(log n) |
| Delete | O(log n) |
| Space | O(n) |

### When Asked in Interviews
- **Concept questions**: "What is AVL tree? How does balancing work?"
- **Trade-off questions**: "AVL vs Red-Black Tree — which is faster for reads vs writes?"
- **Real-world**: "What data structure does `TreeMap` use?" → Red-Black Tree

> 💡 **Key insight**: AVL is more strictly balanced → faster reads. Red-Black is more relaxed → faster writes. Java `TreeMap` uses Red-Black Tree.

---

## 2. Red-Black Trees

**Key Properties** (5 invariants):
1. Every node is Red or Black
2. Root is Black
3. Leaves (NIL) are Black
4. Red nodes have only Black children (no two consecutive reds)
5. All paths from any node to its NIL leaves have equal Black height

**Why Java uses it**: More relaxed balancing → fewer rotations on insert/delete → better write performance than AVL.

```java
// You won't implement this in interviews — Java gives it to you:
TreeMap<Integer, String> map = new TreeMap<>();  // Red-Black Tree internally
TreeSet<Integer> set = new TreeSet<>();           // Red-Black Tree internally

// All operations O(log n):
map.put(key, val);
map.get(key);
map.floorKey(key);    // largest key <= given
map.ceilingKey(key);  // smallest key >= given
map.firstKey();
map.lastKey();
```

### AVL vs Red-Black Tree

| | AVL Tree | Red-Black Tree |
|--|----------|---------------|
| Balance | Strict (BF ≤ 1) | Relaxed (BH balanced) |
| Search | Faster (shorter height) | Slightly slower |
| Insert/Delete | More rotations | Fewer rotations |
| Use case | Read-heavy | Write-heavy, general |
| Java usage | Not in stdlib | `TreeMap`, `TreeSet` |

---

## 3. B-Tree & B+ Tree

**Use case**: Database indexes, file systems — optimised for **disk access** (read large blocks).

### B-Tree Properties (Order m)
- Every node has at most **m children**
- Every non-root node has at least **⌈m/2⌉ children**
- All leaves are at the **same level**
- A node with k children has k-1 keys

```
B-Tree of order 3 (2-3 Tree):
        [10 | 20]
       /    |    \
   [5|7]  [15]  [25|30]
```

### B+ Tree (Used in Databases)
- All data stored in **leaf nodes** only
- Internal nodes store only **keys for routing**
- Leaf nodes are **linked** → range queries are O(log n + k)

```
B+ Tree:
     [10 | 20]           ← internal (routing only)
    /     |     \
[5,7] → [10,15] → [20,25,30]   ← leaves (all data + linked)
```

### Why Databases Use B+ Trees
```
- MySQL InnoDB:    B+ Tree for all indexes
- PostgreSQL:      B-Tree (similar to B+)
- MongoDB:         B-Tree for WiredTiger storage
- File systems:    ext4, NTFS use B-Tree variants
```

### Interview Talking Points
- "B-Tree is good for disk — a single node = one disk page read"
- "B+ Tree is better for range queries — leaf linking gives O(k) after first find"
- "Height of B-Tree of order m with n keys = O(log_m(n)) — very flat"
- "Why not use BST in DB? → BST height = O(n) worst case, O(log n) avg; B-Tree height = O(log_m(n)) always and much flatter"

---

## 4. Skip List

**What it is**: Probabilistic data structure — layered linked lists for O(log n) avg search.

```
Level 3: 1 ─────────────────────────── 50
Level 2: 1 ────────── 20 ──────────── 50
Level 1: 1 ──── 10 ── 20 ── 30 ────── 50
Level 0: 1 ─ 5 ─ 10 ─ 20 ─ 30 ─ 40 ─ 50  (full list)
```

**Operations**: All O(log n) average — Search, Insert, Delete

```java
// Concept — not usually implemented in interviews
// Redis uses Skip List for Sorted Sets (ZSET)
// Key advantage over balanced BST: simpler concurrent implementation
```

### Interview Talking Points
- "Redis uses skip lists for sorted sets (`ZADD`, `ZRANGE`) — O(log n) ops"
- "Skip list vs balanced BST: similar avg complexity but skip list is easier to implement concurrently"
- "Space: O(n) expected due to probabilistic level generation"

---

## 🌍 Real-World Use Cases

### Use Case 1: kACE Layout Config — TreeMap for Sorted Priority
```java
// Store layout configs sorted by priority (lower number = higher priority)
TreeMap<Integer, LayoutConfig> priorityMap = new TreeMap<>();
priorityMap.put(1, defaultConfig);
priorityMap.put(2, classConfig);
priorityMap.put(3, screenConfig);
// Get highest priority (lowest key):
LayoutConfig active = priorityMap.firstEntry().getValue();
```
**Where it applies**: kACE SQL priority-based layout fallback — `applyClassSpecificLayout`.

### Use Case 2: Database Indexing (B+ Tree)
```java
// PostgreSQL B-Tree index on trade timestamp
// CREATE INDEX idx_trade_ts ON trades(timestamp);
// Enables: range scan O(log n + k), exact lookup O(log n)
// SELECT * FROM trades WHERE timestamp BETWEEN t1 AND t2;
// → B+ Tree: find t1 in O(log n), scan leaves for k results in O(k)
```
**Where it applies**: kACE PostgreSQL/DB2 index design for trade query performance.

### Use Case 3: Redis Sorted Set (Skip List)
```java
// Redis ZADD uses skip list internally
// zadd leaderboard 100 "player1"
// zadd leaderboard 200 "player2"
// zrange leaderboard 0 -1 WITHSCORES  → O(log n + k)
// zrangebyscore leaderboard 100 200    → O(log n + k)
```
**Where it applies**: Real-time leaderboards, rate limiting with sliding window scores.

### Use Case 4: AVL Tree for In-Memory Dictionary
```java
// AVL tree for read-heavy in-memory FX symbol dictionary
// Faster lookups than Red-Black for read-dominant workload
// Java: use TreeMap (Red-Black) — implement AVL only if asked
TreeMap<String, FXPair> symbolMap = new TreeMap<>();
symbolMap.put("EURUSD", new FXPair("EUR", "USD"));
FXPair pair = symbolMap.get("EURUSD"); // O(log n)
```

### Use Case 5: File System (B-Tree)
```java
// Concept: OS file systems use B-Trees for directory entries
// ext4 uses HTree (B-Tree variant) for large directories
// NTFS uses B-Tree for MFT (Master File Table)
// Your CI/CD pipelines reading/writing build artifacts go through this
```

---

## 🏋️ Practice Problems

| # | Problem | Focus | Difficulty |
|---|---------|-------|------------|
| 1 | Implement BST Insert/Delete/Search | BST foundation | Medium |
| 2 | Balance a BST | Convert sorted array → BST | Medium |
| 3 | AVL Tree Insert (implement) | Rotations | Hard |
| 4 | Design a data structure with O(log n) insert and O(1) min | TreeMap + track min | Medium |
| 5 | Count of Smaller Numbers After Self | BST / BIT / Merge Sort | Hard |
| 6 | Kth Smallest in BST | Inorder traversal | Medium |
| 7 | Range Sum of BST | DFS with pruning | Easy |
| 8 | My Calendar I / II | TreeMap floorKey/ceilingKey | Medium |

---

## ⚠️ Key Interview Points to Remember

- **AVL**: Strictly balanced, 4 rotation cases (LL/RR/LR/RL), O(log n) all ops
- **Red-Black**: Relaxed, 5 invariants, fewer rotations → Java `TreeMap`/`TreeSet`
- **AVL vs RB**: AVL = faster reads; RB = faster writes; Java chose RB
- **B-Tree**: Disk-optimised, all leaves same level, m-way branching
- **B+ Tree**: Leaf-linked B-Tree → efficient range scans → used in MySQL, PostgreSQL
- **Skip List**: Probabilistic O(log n), used in Redis sorted sets, easier concurrent impl

---

## 5. 2-3 Trees

**What it is**: A self-balancing search tree where every internal node has **2 or 3 children**, and all leaves are at the same level. It is a special case of B-Tree of order 3.

### Properties
- **2-node**: has 1 key, 2 children (left < key < right)
- **3-node**: has 2 keys, 3 children (left < key1 < middle < key2 < right)
- All leaves at **same depth** — perfectly balanced
- Height = O(log n)

```
2-3 Tree storing: 1, 2, 3, 4, 5, 6, 7

         [3 | 5]
        /   |   \
     [1|2] [4]  [6|7]

Inserting 8 → 3-node [6|7] becomes full → split:
         [3 | 5 | 7]
        /   |   |   \
     [1|2] [4] [6]  [8]

[3|5|7] is now a 4-node → split and push 5 up:
              [5]
            /     \
         [3]       [7]
        /   \     /   \
     [1|2]  [4] [6]   [8]
```

### Operations
| Operation | Time |
|-----------|------|
| Search | O(log n) |
| Insert | O(log n) — split upward if 4-node formed |
| Delete | O(log n) — merge/redistribute |

### Why it Matters
- 2-3 trees are the **conceptual foundation** of Red-Black Trees
- A Red-Black Tree is a 2-3 tree encoded as a binary tree:
  - 2-node → Black node
  - 3-node → Black node with a Red left child
- Understanding 2-3 trees makes Red-Black trees much easier to reason about

```java
// You never implement 2-3 trees directly in Java interviews
// But the concept maps directly to:
TreeMap<K,V>    // Red-Black Tree (2-3 tree encoded as binary)
TreeSet<E>      // Red-Black Tree
```

### Interview Talking Point
> "A 2-3 tree guarantees perfect balance because splits propagate upward and all leaves stay at the same level. Red-Black trees achieve the same guarantee through color invariants — they're essentially 2-3 trees encoded as binary trees where Red edges represent 3-nodes."

---

## 6. Indexing — Linear & Tree-Based

Indexing is the mechanism that makes database queries fast. Understanding this is critical for **System Design / HLD rounds** and shows senior-level depth.

### What is an Index?
An index is a separate data structure that maps **search keys → record locations**, avoiding full table scans.

```
Without index: SELECT * FROM trades WHERE symbol = 'EURUSD'
→ Full table scan: O(n) — reads every row

With index on symbol:
→ Index lookup: O(log n) — jump directly to matching rows
```

### Linear Indexing (Dense & Sparse)

**Dense Index**: One index entry per record
```
Record:  [1,Alice] [2,Bob] [3,Carol] [4,Dave]
Index:   1→page1  2→page1  3→page2  4→page2

Pros: Can find any record directly
Cons: Index itself can be large
```

**Sparse Index**: One index entry per block/page
```
Record pages: Page1[1,2,3,4]  Page2[5,6,7,8]  Page3[9,10]
Index:        1→Page1         5→Page2          9→Page3

Pros: Smaller index
Cons: Must scan within page after finding block
Use: When records are sorted (clustered index)
```

### Tree-Based Indexing

**B+ Tree Index** (most common in RDBMS):
```
                [30 | 60]              ← Root (routing)
              /     |     \
          [10|20] [40|50] [70|80]     ← Internal (routing)
         /   |      |      |   \
       [...]  [...] [...]  [...] [...]  ← Leaves (actual data pointers, linked)

Leaf nodes are linked → efficient range scans
```

```java
// MySQL InnoDB: every table has a clustered B+ Tree index on primary key
// Secondary indexes store (secondary_key → primary_key) pairs

// PostgreSQL: default index type is B-Tree
// CREATE INDEX idx_symbol ON trades(symbol);  → B+ Tree
// CREATE INDEX idx_ts ON trades(timestamp);   → B+ Tree, range-friendly

// Hash Index (for exact lookups only):
// CREATE INDEX idx_id ON trades USING HASH (trade_id);
// O(1) lookup, but NO range queries supported
```

**Composite Index & Selectivity**:
```sql
-- Index on (symbol, timestamp) supports:
-- WHERE symbol = 'EURUSD'                    ✅ (leftmost prefix)
-- WHERE symbol = 'EURUSD' AND timestamp > t  ✅ (both columns)
-- WHERE timestamp > t                        ❌ (not leftmost prefix)

-- High selectivity = good index candidate
-- Low selectivity (e.g., boolean) = index rarely helps
```

### Covering Index
```sql
-- A covering index contains ALL columns needed by the query
-- → No need to access the actual table rows ("index-only scan")
CREATE INDEX idx_cover ON trades(symbol, timestamp, price);
SELECT timestamp, price FROM trades WHERE symbol = 'EURUSD';
-- ↑ Answered entirely from index — no table access needed
```

### Real-World Application (kACE)
```java
// kACE screen layout DB schema — index design
// Table: screen_layouts(screen_id, class_id, user_id, priority, config)

// Query: find layout for screen X with best priority
// SELECT * FROM screen_layouts 
//   WHERE screen_id = ? ORDER BY priority ASC LIMIT 1

// Optimal index: CREATE INDEX idx_layout ON screen_layouts(screen_id, priority)
// → B+ Tree: O(log n) to find screen_id, scan leaves for min priority
```

### Index Trade-offs
| | B+ Tree Index | Hash Index |
|--|--------------|------------|
| Exact lookup | O(log n) | O(1) |
| Range query | O(log n + k) | ❌ Not supported |
| Order by | ✅ (already sorted) | ❌ |
| Space | Moderate | Low |
| Write overhead | Moderate | Low |
