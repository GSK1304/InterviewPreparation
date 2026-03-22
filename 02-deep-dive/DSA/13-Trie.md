# 📚 DSA Deep Dive — Trie (Prefix Tree)

---

## 🧠 Core Concept

A **Trie** is a tree data structure for storing strings where each node represents a character. Efficient for prefix-based operations.

```
Words: ["cat", "car", "card", "care", "dog"]

        root
       /    \
      c      d
      |      |
      a      o
     / \     |
    t   r    g ✓
    ✓   |
       / \
      d   e
      ✓   ✓
```

### Complexity
| Operation | Time | Space |
|-----------|------|-------|
| Insert | O(L) | O(L) |
| Search | O(L) | O(1) |
| StartsWith | O(L) | O(1) |
| Delete | O(L) | O(1) |

Where L = length of the word. Much faster than HashMap for prefix queries.

---

## 🔑 Implementation

### Basic Trie Node
```java
class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean isEnd = false;
}

class Trie {
    TrieNode root = new TrieNode();

    void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null)
                node.children[idx] = new TrieNode();
            node = node.children[idx];
        }
        node.isEnd = true;
    }

    boolean search(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return false;
            node = node.children[idx];
        }
        return node.isEnd;
    }

    boolean startsWith(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return false;
            node = node.children[idx];
        }
        return true; // prefix exists
    }
}
```

### Trie with HashMap (supports any character)
```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd = false;
    int count = 0; // how many words pass through this node
}
```

### Auto-complete — Get All Words with Prefix
```java
List<String> autocomplete(String prefix) {
    TrieNode node = root;
    for (char c : prefix.toCharArray()) {
        if (!node.children.containsKey(c)) return new ArrayList<>();
        node = node.children.get(c);
    }
    List<String> results = new ArrayList<>();
    dfsCollect(node, new StringBuilder(prefix), results);
    return results;
}

void dfsCollect(TrieNode node, StringBuilder current, List<String> results) {
    if (node.isEnd) results.add(current.toString());
    for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
        current.append(entry.getKey());
        dfsCollect(entry.getValue(), current, results);
        current.deleteCharAt(current.length() - 1); // backtrack
    }
}
```

### Word Search II (Trie + Backtracking)
```java
// Build trie from word list, then DFS grid checking trie paths
List<String> findWords(char[][] board, String[] words) {
    Trie trie = new Trie();
    for (String w : words) trie.insert(w);
    Set<String> result = new HashSet<>();
    for (int r = 0; r < board.length; r++)
        for (int c = 0; c < board[0].length; c++)
            dfs(board, r, c, trie.root, new StringBuilder(), result);
    return new ArrayList<>(result);
}

void dfs(char[][] board, int r, int c, TrieNode node, StringBuilder path, Set<String> result) {
    if (r < 0 || r >= board.length || c < 0 || c >= board[0].length || board[r][c] == '#') return;
    char ch = board[r][c];
    TrieNode next = node.children[ch - 'a'];
    if (next == null) return;
    path.append(ch);
    if (next.isEnd) result.add(path.toString());
    board[r][c] = '#';
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    for (int[] d : dirs) dfs(board, r+d[0], c+d[1], next, path, result);
    board[r][c] = ch;
    path.deleteCharAt(path.length() - 1);
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: FX Symbol Autocomplete (kACE Search)
**Problem**: As a trader types "EUR", suggest all FX pairs starting with "EUR".

```java
Trie symbolTrie = new Trie();
String[] symbols = {"EURUSD","EURGBP","EURJPY","GBPUSD","USDJPY","AUDUSD"};
for (String s : symbols) symbolTrie.insert(s);

List<String> suggestions = symbolTrie.autocomplete("EUR");
// Returns: ["EURUSD", "EURGBP", "EURJPY"]
```
**Where it applies**: kACE trading UI symbol search, dropdown autocomplete for currency pairs.
> 🏭 **Industry Example**: Google Search's autocomplete uses a Trie (with ranking weights) to suggest queries as you type. GitHub's file finder (press 'T') uses Trie-based fuzzy matching. VS Code's IntelliSense uses Trie for method/variable name completion.
> 🏦 **kACE Context**: kACE trading UI symbol search — autocompleting FX currency pairs as traders type (e.g., "EUR" → EURUSD, EURGBP, EURJPY).


---

### Use Case 2: Command Prefix Matching (CLI / Dev Tools)
**Problem**: In the kACE monitoring CLI, match partial commands to valid commands.

```java
Trie commandTrie = new Trie();
String[] commands = {"status","start","stop","restart","stats","stream"};
for (String cmd : commands) commandTrie.insert(cmd);

boolean isValidPrefix = commandTrie.startsWith("sta"); // true → "start", "stats"
List<String> matches = commandTrie.autocomplete("sta"); // ["start", "stats"]
```
**Where it applies**: Node.js monitoring proxy CLI, Jenkins build command completion.
> 🏭 **Industry Example**: Linux shell tab-completion uses Trie for command suggestions. Redis CLI uses Trie-based command completion. AWS CLI uses prefix matching for command and subcommand suggestions.
> 🏦 **kACE Context**: Node.js monitoring proxy CLI — matching partial commands like "sta" to "start", "stats", "status".


---

### Use Case 3: IP Routing Table (Longest Prefix Match)
**Problem**: Find the most specific route for an IP address using longest prefix match.

```java
// Store IP prefixes in trie (bit-by-bit)
// For each incoming packet, traverse trie and return longest matching route
class IPTrie {
    IPTrieNode root = new IPTrieNode();
    void insert(String ipPrefix, String route) {
        IPTrieNode node = root;
        for (char bit : toBinary(ipPrefix).toCharArray()) {
            int idx = bit - '0';
            if (node.children[idx] == null) node.children[idx] = new IPTrieNode();
            node = node.children[idx];
        }
        node.route = route;
    }
}
```
**Where it applies**: Network routing in kACE infrastructure, API Gateway route matching.
> 🏭 **Industry Example**: Every internet router uses a radix/Patricia trie for IP routing — longest prefix match is fundamental to BGP routing. Cloudflare's network uses binary tries for firewall rule matching. AWS VPC routing tables use prefix matching for traffic routing.
> 🏦 **kACE Context**: API Gateway route matching — routing `/api/rfq/*` to RFQ service using longest-prefix trie traversal.


---

### Use Case 4: Log Pattern Detection
**Problem**: Given thousands of log messages, find all logs starting with specific error prefixes.

```java
Trie logTrie = new Trie(); // HashMap-based for arbitrary chars
for (String log : logs) logTrie.insert(log);

// Find all logs starting with "ERROR: Connection"
List<String> errorLogs = logTrie.autocomplete("ERROR: Connection");
```
**Where it applies**: kACE log monitoring, Kafka consumer error pattern detection.
> 🏭 **Industry Example**: Splunk uses Trie-based pattern matching to group similar log messages (log clustering). Elasticsearch's tokenizer uses prefix tries for fast term lookup. Datadog's log anomaly detection uses Trie structures to identify new log patterns.
> 🏦 **kACE Context**: kACE log monitoring — fast detection of error prefixes like "ERROR: Connection" across Kafka consumer logs.


---

### Use Case 5: Maximum XOR (Bit Trie)
**Problem**: Find two numbers in array with maximum XOR — used in encryption/hashing.

```java
// Store numbers bit by bit (MSB first) in binary trie
// For each number, greedily pick opposite bit to maximize XOR
int findMaximumXOR(int[] nums) {
    int max = 0, mask = 0;
    for (int i = 31; i >= 0; i--) {
        mask |= (1 << i);
        Set<Integer> prefixes = new HashSet<>();
        for (int num : nums) prefixes.add(num & mask);
        int candidate = max | (1 << i);
        for (int prefix : prefixes)
            if (prefixes.contains(candidate ^ prefix)) { max = candidate; break; }
    }
    return max;
}
```
**Where it applies**: JWT token XOR validation patterns, hash collision detection.
> 🏭 **Industry Example**: Cryptographic systems use XOR-based Trie structures for fast key lookups. Networking protocols use XOR tries (as in Kademlia DHT — used by BitTorrent) for peer discovery. Consistent hashing implementations use XOR distance metrics.
> 🏦 **kACE Context**: JWT token XOR validation patterns and hash collision detection in kACE's auth layer.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Implement Trie | Basic Trie | Medium |
| 2 | Search Suggestions System | Trie + autocomplete | Medium |
| 3 | Word Search II | Trie + backtracking | Hard |
| 4 | Replace Words | Trie prefix | Medium |
| 5 | Map Sum Pairs | Trie with values | Medium |
| 6 | Maximum XOR of Two Numbers | Binary Trie | Medium |
| 7 | Design Add and Search Words | Trie + wildcard | Medium |
| 8 | Longest Word in Dictionary | Trie BFS | Medium |
| 9 | Palindrome Pairs | Trie + reverse | Hard |
| 10 | Stream of Characters | Trie + suffix | Hard |

---

## ⚠️ Common Mistakes

- Using `TrieNode[26]` but input has uppercase or special chars — use `HashMap` for safety
- Forgetting to set `isEnd = true` after inserting last character
- Autocomplete DFS: must **backtrack** the `StringBuilder` after each branch
- Word Search II: pruning — remove word from trie after found to avoid duplicates
- Trie vs HashMap tradeoff: Trie wins for **prefix queries**; HashMap wins for **exact lookups**
