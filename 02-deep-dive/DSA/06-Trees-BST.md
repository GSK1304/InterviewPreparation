# 📚 DSA Deep Dive — Trees & BST

---

## 🧠 Core Concepts

```java
class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int val) { this.val = val; }
}
```

- **Binary Tree**: each node has at most 2 children
- **BST**: left < root < right at every node
- **Height**: O(log n) balanced, O(n) worst case (skewed)
- **DFS**: uses stack (implicit via recursion)
- **BFS**: uses queue (level-order)

---

## 🔑 Key Techniques

### DFS Traversals
```java
// Preorder: root → left → right (copy tree, serialize)
void preorder(TreeNode root) {
    if (root == null) return;
    process(root.val);
    preorder(root.left);
    preorder(root.right);
}

// Inorder: left → root → right (sorted order in BST)
void inorder(TreeNode root) {
    if (root == null) return;
    inorder(root.left);
    process(root.val);
    inorder(root.right);
}

// Postorder: left → right → root (delete tree, compute subtree results)
void postorder(TreeNode root) {
    if (root == null) return;
    postorder(root.left);
    postorder(root.right);
    process(root.val);
}
```

### BFS — Level Order
```java
List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    Queue<TreeNode> q = new LinkedList<>();
    q.offer(root);
    while (!q.isEmpty()) {
        int size = q.size();
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TreeNode node = q.poll();
            level.add(node.val);
            if (node.left != null) q.offer(node.left);
            if (node.right != null) q.offer(node.right);
        }
        result.add(level);
    }
    return result;
}
```

### Tree Height
```java
int height(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(height(root.left), height(root.right));
}
```

### LCA (Lowest Common Ancestor)
```java
TreeNode lca(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left = lca(root.left, p, q);
    TreeNode right = lca(root.right, p, q);
    if (left != null && right != null) return root; // p and q on different sides
    return left != null ? left : right;
}
```

### Validate BST
```java
boolean isValidBST(TreeNode root, long min, long max) {
    if (root == null) return true;
    if (root.val <= min || root.val >= max) return false;
    return isValidBST(root.left, min, root.val) &&
           isValidBST(root.right, root.val, max);
}
// Call: isValidBST(root, Long.MIN_VALUE, Long.MAX_VALUE)
```

### Diameter of Binary Tree
```java
int maxDiameter = 0;
int diameter(TreeNode root) {
    if (root == null) return 0;
    int left = diameter(root.left);
    int right = diameter(root.right);
    maxDiameter = Math.max(maxDiameter, left + right);
    return 1 + Math.max(left, right);
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Org Chart Traversal (Team Management)
**Problem**: Print all team members under a manager level by level (BFS), or find reporting chain (DFS).

```java
// BFS — print team level by level
void printOrgChart(Employee root) {
    Queue<Employee> q = new LinkedList<>();
    q.offer(root);
    while (!q.isEmpty()) {
        int size = q.size();
        for (int i = 0; i < size; i++) {
            Employee emp = q.poll();
            System.out.print(emp.name + " ");
            for (Employee report : emp.directReports) q.offer(report);
        }
        System.out.println(); // next level
    }
}
```
**Where it applies**: kACE team management portal — hierarchy display, leave approval chain.
> 🏭 **Industry Example**: GitHub uses BFS on repository fork trees to show fork networks. LinkedIn's company hierarchy feature uses level-order traversal. Google's organizational directory uses BFS for "reports to" chain display.
> 🏦 **kACE Context**: kACE team management portal — displaying 12-member team hierarchy and leave approval chains.


---

### Use Case 2: File System Navigation (Config / Layout System)
**Problem**: Find all layout config files under a directory tree, compute total size (postorder).

```java
// Postorder — compute subtree sizes bottom-up
long computeSize(FileNode node) {
    if (node.isFile()) return node.size;
    long total = 0;
    for (FileNode child : node.children)
        total += computeSize(child);
    node.totalSize = total;
    return total;
}
```
**Where it applies**: kACE layout config system — `useMergedLayout` hierarchical config resolution.
> 🏭 **Industry Example**: Linux `du -sh` uses postorder DFS to compute directory sizes bottom-up. macOS Finder calculates folder sizes the same way. Git's tree objects (blob → tree → commit) use the same hierarchical structure.
> 🏦 **kACE Context**: kACE layout config system — `useMergedLayout` resolves nested screen config hierarchies bottom-up.


---

### Use Case 3: BST for Order Book (FX Trading)
**Problem**: Maintain a sorted order book — insert bids, find best bid (max), range queries.

```java
// Java TreeMap is a Red-Black BST — O(log n) all ops
TreeMap<Double, Integer> orderBook = new TreeMap<>();
orderBook.put(1.2345, 1000000); // price → volume
orderBook.put(1.2340, 500000);

double bestBid = orderBook.lastKey();           // highest bid O(log n)
double bestAsk = orderBook.firstKey();          // lowest ask O(log n)
NavigableMap<Double, Integer> range =
    orderBook.subMap(1.2300, true, 1.2350, true); // range O(log n)
```
**Where it applies**: FX Options order book management, spread monitoring in kACE.
> 🏭 **Industry Example**: NASDAQ and NYSE matching engines use Red-Black Trees (BST variant) for order books — O(log n) best bid/ask lookup. Java's `TreeMap` (used in many financial systems) is a Red-Black Tree. LMAX Disruptor uses sorted structures for high-frequency trading.
> 🏦 **kACE Context**: FX Options order book management — sorted by strike price for spread monitoring.


---

### Use Case 4: Serialize/Deserialize Config Trees
**Problem**: Save and restore a tree of layout configurations to/from JSON/DB.

```java
// Serialize: preorder traversal with null markers
String serialize(TreeNode root) {
    if (root == null) return "null,";
    return root.val + "," + serialize(root.left) + serialize(root.right);
}

// Deserialize: reconstruct from preorder string
TreeNode deserialize(Queue<String> nodes) {
    String val = nodes.poll();
    if (val.equals("null")) return null;
    TreeNode node = new TreeNode(Integer.parseInt(val));
    node.left = deserialize(nodes);
    node.right = deserialize(nodes);
    return node;
}
```
**Where it applies**: kACE screen layout serialization to DB, config tree persistence.
> 🏭 **Industry Example**: JSON (used by REST APIs everywhere) is a serialized tree. XML DOM (used by SOAP/enterprise systems) is a tree. React's Virtual DOM is a serialized component tree diffed on each render.
> 🏦 **kACE Context**: kACE screen layout serialization to PostgreSQL — config trees persisted as JSON.


---

### Use Case 5: Path Sum — Budget / P&L Analysis
**Problem**: Find if any root-to-leaf path sums to a target (e.g., specific P&L target).

```java
boolean hasPathSum(TreeNode root, int target) {
    if (root == null) return false;
    if (root.left == null && root.right == null) return root.val == target;
    return hasPathSum(root.left, target - root.val) ||
           hasPathSum(root.right, target - root.val);
}

// All paths that sum to target
List<List<Integer>> pathSum(TreeNode root, int target) {
    List<List<Integer>> result = new ArrayList<>();
    dfs(root, target, new ArrayList<>(), result);
    return result;
}
void dfs(TreeNode node, int remain, List<Integer> path, List<List<Integer>> result) {
    if (node == null) return;
    path.add(node.val);
    if (node.left == null && node.right == null && remain == node.val)
        result.add(new ArrayList<>(path));
    dfs(node.left, remain - node.val, path, result);
    dfs(node.right, remain - node.val, path, result);
    path.remove(path.size() - 1); // backtrack
}
```
**Where it applies**: FX option strategy P&L path analysis, decision tree traversal.
> 🏭 **Industry Example**: Game engines (Unity, Unreal) use path-sum on behavior trees to find valid action sequences. AWS Cost Explorer uses tree path sums to aggregate costs from service → region → account.
> 🏦 **kACE Context**: FX option strategy P&L path analysis — finding all pricing paths through a multi-leg tree that hit a target premium.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Maximum Depth of Binary Tree | DFS | Easy |
| 2 | Invert Binary Tree | DFS | Easy |
| 3 | Symmetric Tree | DFS | Easy |
| 4 | Path Sum | DFS | Easy |
| 5 | Level Order Traversal | BFS | Medium |
| 6 | Validate BST | DFS with bounds | Medium |
| 7 | Lowest Common Ancestor | DFS | Medium |
| 8 | Binary Tree Right Side View | BFS | Medium |
| 9 | Diameter of Binary Tree | DFS | Medium |
| 10 | Serialize and Deserialize | Preorder DFS | Hard |

---

## ⚠️ Common Mistakes

- Forgetting base case `if (root == null) return` at start of recursive function
- BST validation: don't just check `root.left.val < root.val` — use min/max bounds
- Level order: capture `q.size()` before the inner loop (queue grows during loop)
- LCA: always check both left and right subtrees before returning
- Diameter: update `maxDiameter` inside the helper, not at the top level call
