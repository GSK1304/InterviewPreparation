# 📋 DSA Cheatsheet — Complete Reference (All 22 Topics)

---

## ⏱️ Time & Space Complexity Master Table

| Structure | Access | Search | Insert | Delete | Space | Notes |
|-----------|--------|--------|--------|--------|-------|-------|
| Array | O(1) | O(n) | O(n) | O(n) | O(n) | Best random access |
| Dynamic Array | O(1) | O(n) | O(1) amort | O(n) | O(n) | ArrayList |
| HashMap | — | O(1) avg | O(1) avg | O(1) avg | O(n) | O(n) worst (collision) |
| HashSet | — | O(1) avg | O(1) avg | O(1) avg | O(n) | |
| TreeMap/TreeSet | O(log n) | O(log n) | O(log n) | O(log n) | O(n) | Red-Black Tree |
| Stack/Queue | — | O(n) | O(1) | O(1) | O(n) | |
| Linked List | O(n) | O(n) | O(1)* | O(1)* | O(n) | *at known node |
| Binary Heap | O(1) top | O(n) | O(log n) | O(log n) | O(n) | |
| BST balanced | O(log n) | O(log n) | O(log n) | O(log n) | O(n) | AVL/RB |
| BST skewed | O(n) | O(n) | O(n) | O(n) | O(n) | Worst case |
| Trie | — | O(L) | O(L) | O(L) | O(26×N×L) | L = word length |
| Segment Tree | — | O(log n) | O(log n) | O(log n) | O(4n) | Range queries |
| Fenwick Tree | — | O(log n) | O(log n) | O(log n) | O(n) | Prefix sums |
| Graph adj list | — | O(V+E) | O(1) | O(E) | O(V+E) | Sparse graphs |
| Graph matrix | O(1) | O(V) | O(1) | O(1) | O(V²) | Dense graphs |

---

## 🔑 Pattern Reference — Trigger / Advantage / Tradeoff / When NOT to Use

### 1. HashMap / HashSet
| | |
|--|--|
| **Trigger** | Fast lookup, counting frequency, finding duplicates, two-sum complement |
| **Advantage** | O(1) avg insert/lookup/delete — best optimization tool in DSA |
| **Tradeoff** | O(n) space; unordered; O(n) worst on hash collision |
| **Use** | Need O(1) lookup; counting occurrences; caching seen values |
| **Don't use** | Need sorted order → TreeMap; need O(1) space on sorted → two pointers |

```java
map.getOrDefault(key, 0) + 1           // frequency count
map.computeIfAbsent(key, k -> fn(k))   // atomic compute
map.merge(key, 1, Integer::sum)        // atomic increment
seen.add(x) == false                   // duplicate detection
```

---

### 2. Arrays & Prefix Sum
| | |
|--|--|
| **Trigger** | Range sum queries, subarray problems, static data |
| **Advantage** | O(1) random access; prefix sum gives O(1) range query after O(n) build |
| **Tradeoff** | Fixed size; O(n) insert/delete; prefix sum only for static arrays |
| **Use** | Multiple range queries on static array; subarray sum problems |
| **Don't use** | Frequent insertions → LinkedList; need O(1) insert anywhere |

```java
// Prefix sum
int[] pre = new int[n+1];
for (int i = 0; i < n; i++) pre[i+1] = pre[i] + nums[i];
int rangeSum = pre[r+1] - pre[l]; // O(1) query [l..r]

// Kadane's — max subarray O(n)
int cur = nums[0], max = nums[0];
for (int i = 1; i < n; i++) { cur = Math.max(nums[i], cur+nums[i]); max = Math.max(max,cur); }
```

---

### 3. Two Pointers
| | |
|--|--|
| **Trigger** | Sorted array, pair sum, palindrome, remove duplicates, merging |
| **Advantage** | Reduces O(n²) brute force to O(n); O(1) extra space |
| **Tradeoff** | Requires sorted input (or specific structure) |
| **Use** | Sorted array pair/triplet sum; palindrome check; merge two sorted |
| **Don't use** | Unsorted and sorting changes answer; need original indices |

```java
int left = 0, right = n-1;
while (left < right) {
    if (check()) { /* found */ left++; right--; }
    else if (needMore()) left++;
    else right--;
}
```

---

### 4. Sliding Window
| | |
|--|--|
| **Trigger** | Subarray/substring, contiguous elements, window of size k |
| **Advantage** | O(n) for what would be O(n²) naively |
| **Tradeoff** | Only contiguous subarrays; variable window needs careful shrink logic |
| **Use** | Max/min/count in subarray; longest substring with constraint |
| **Don't use** | Non-contiguous subsequences → DP; no clear window constraint |

```java
// Fixed window
int sum = 0;
for (int i = 0; i < n; i++) { sum += nums[i];
    if (i >= k) sum -= nums[i-k];
    if (i >= k-1) ans = Math.max(ans, sum); }

// Variable window
int left = 0;
for (int right = 0; right < n; right++) {
    window.merge(s.charAt(right), 1, Integer::sum);
    while (violated()) { window.merge(s.charAt(left), -1, Integer::sum); left++; }
    ans = Math.max(ans, right - left + 1); }
```

---

### 5. Stack & Monotonic Stack
| | |
|--|--|
| **Trigger** | Matching brackets, undo/redo, next greater/smaller element, DFS iterative |
| **Advantage** | O(1) push/pop; monotonic stack solves next-greater in O(n) amortized |
| **Tradeoff** | O(n) search; monotonic stack destroys elements (need careful design) |
| **Use** | Bracket matching; expression eval; next greater/smaller; histogram |
| **Don't use** | Need FIFO → Queue; need random access → Array |

```java
Deque<Integer> stack = new ArrayDeque<>(); // never use Stack class

// Monotonic decreasing → next greater element
int[] res = new int[n]; Arrays.fill(res, -1);
for (int i = 0; i < n; i++) {
    while (!stack.isEmpty() && nums[i] > nums[stack.peek()])
        res[stack.pop()] = nums[i];
    stack.push(i);
}
// Monotonic increasing → next smaller element (reverse comparison)
```

---

### 6. Queue & BFS
| | |
|--|--|
| **Trigger** | Shortest path unweighted, level order, nearest neighbor, multi-source spread |
| **Advantage** | Guarantees shortest path unweighted; natural level-by-level exploration |
| **Tradeoff** | O(V+E) space; no shortest path for weighted → Dijkstra |
| **Use** | Shortest path unweighted; level order; rotten oranges (multi-source) |
| **Don't use** | Weighted shortest path → Dijkstra; just connectivity → DFS simpler |

```java
Queue<Integer> q = new LinkedList<>();
boolean[] vis = new boolean[n];
q.offer(start); vis[start] = true;
while (!q.isEmpty()) {
    int size = q.size();            // capture for level processing
    for (int i = 0; i < size; i++) {
        int cur = q.poll();
        for (int next : adj.get(cur))
            if (!vis[next]) { vis[next] = true; q.offer(next); }
    }
    level++;
}
```

---

### 7. Binary Search
| | |
|--|--|
| **Trigger** | Sorted input, find position, minimize/maximize, monotonic condition |
| **Advantage** | O(log n) — halves search space every step |
| **Tradeoff** | Requires sorted/monotonic input; off-by-one errors common |
| **Use** | Sorted search; first/last occurrence; binary search on answer |
| **Don't use** | Unsorted (sort first); linked list (no random access) |

```java
// Standard
int lo=0,hi=n-1;
while(lo<=hi){int mid=lo+(hi-lo)/2;
    if(nums[mid]==target)return mid;
    else if(nums[mid]<target)lo=mid+1;else hi=mid-1;}

// Left boundary (first true)
int lo=0,hi=n,ans=n;
while(lo<=hi){int mid=lo+(hi-lo)/2;if(ok(mid)){ans=mid;hi=mid-1;}else lo=mid+1;}

// Binary search on answer (minimize)
int lo=MIN,hi=MAX;
while(lo<hi){int mid=lo+(hi-lo)/2;if(canAchieve(mid))hi=mid;else lo=mid+1;}
return lo;
```

---

### 8. Linked List
| | |
|--|--|
| **Trigger** | O(1) insert/delete at known node, LRU cache, cycle detection |
| **Advantage** | O(1) insert/delete at known position; dynamic size; no shifting |
| **Tradeoff** | O(n) search and access; extra pointer memory; poor cache locality |
| **Use** | Frequent insert/delete at head/tail; LRU; order matters, unknown size |
| **Don't use** | Need random access → Array; need O(1) search → HashMap |

```java
// Reverse
ListNode prev=null,cur=head;
while(cur!=null){ListNode nxt=cur.next;cur.next=prev;prev=cur;cur=nxt;}
return prev;

// Fast & Slow — middle / cycle
ListNode slow=head,fast=head;
while(fast!=null&&fast.next!=null){slow=slow.next;fast=fast.next.next;}
// slow = middle; if slow==fast → cycle

// Always use dummy node to handle head changes
ListNode dummy=new ListNode(0); dummy.next=head; return dummy.next;
```

---

### 9. Trees — DFS Traversals
| | |
|--|--|
| **Trigger** | Path problems, subtree queries, tree DP, BST properties |
| **Advantage** | O(n) traversal; recursive structure mirrors tree; low code complexity |
| **Tradeoff** | Stack overflow on deep trees; O(h) space; no shortest path |
| **Use** | All path problems; subtree sums; LCA; tree DP; BST validation |
| **Don't use** | Need shortest path → BFS; need level processing → BFS |

```java
// Preorder: root→L→R | Inorder: L→root→R (BST sorted) | Postorder: L→R→root
void dfs(TreeNode node) {
    if (node == null) return;
    process(node); // move this line for in/post order
    dfs(node.left); dfs(node.right);
}

// Tree DP — return pair pattern
int[] solve(TreeNode node) { // {withNode, withoutNode}
    if (node == null) return new int[]{0,0};
    int[] L=solve(node.left), R=solve(node.right);
    return new int[]{node.val+L[1]+R[1], Math.max(L[0],L[1])+Math.max(R[0],R[1])};
}

// BST validation — use bounds, not just parent
boolean valid(TreeNode n, long lo, long hi) {
    if (n==null) return true;
    if (n.val<=lo||n.val>=hi) return false;
    return valid(n.left,lo,n.val) && valid(n.right,n.val,hi);
}
```

---

### 10. Heap / Priority Queue
| | |
|--|--|
| **Trigger** | Top-K elements, streaming median, K-way merge, priority scheduling |
| **Advantage** | O(1) peek; O(log n) insert/delete; O(n) heapify from array |
| **Tradeoff** | O(n) search for arbitrary element; not stable; non-top removal is O(n) |
| **Use** | Top-K; dynamic min/max; merge K sorted; Dijkstra; task scheduling |
| **Don't use** | Need sorted output → use sort; static data → sort+index; O(1) search → HashMap |

```java
PriorityQueue<Integer> min = new PriorityQueue<>();             // min-heap (default)
PriorityQueue<Integer> max = new PriorityQueue<>(Collections.reverseOrder()); // max-heap

// Top-K largest → min-heap of size K
for (int n : nums) { min.offer(n); if (min.size()>k) min.poll(); }
return min.peek(); // kth largest

// Two heaps — streaming median
PriorityQueue<Integer> lo=new PriorityQueue<>(Collections.reverseOrder()); // lower half max-heap
PriorityQueue<Integer> hi=new PriorityQueue<>(); // upper half min-heap
// Invariant: lo.size() >= hi.size(); median = lo.peek() or (lo.peek()+hi.peek())/2.0

// K-way merge
PriorityQueue<int[]> pq=new PriorityQueue<>((a,b)->a[0]-b[0]); // {val,listIdx,elemIdx}
```

---

### 11. Recursion & Backtracking
| | |
|--|--|
| **Trigger** | All combinations/permutations/subsets, constraint satisfaction, grid search |
| **Advantage** | Explores all possibilities; pruning dramatically reduces actual work |
| **Tradeoff** | Exponential O(2^n)/O(n!); stack overflow risk; hard to debug |
| **Use** | Need ALL solutions; N-Queens; Sudoku; word search; generate parentheses |
| **Don't use** | Only need ONE optimal value → DP/Greedy; n > 20 without pruning |

```java
void backtrack(int start, List<Integer> curr) {
    if (goalMet()) { result.add(new ArrayList<>(curr)); return; } // MUST copy
    for (int i=start; i<n; i++) {
        if (i>start && nums[i]==nums[i-1]) continue; // skip duplicates (sort first)
        curr.add(nums[i]);           // choose
        backtrack(i+1, curr);        // explore (i for repeats allowed)
        curr.remove(curr.size()-1);  // UNDO — never skip this
    }
}
```

**Backtracking vs DP**: Backtracking = ALL solutions or path; DP = ONE optimal value.

---

### 12. Greedy
| | |
|--|--|
| **Trigger** | Optimization where local best = global best; interval problems |
| **Advantage** | O(n) or O(n log n); simple; no subproblem storage |
| **Tradeoff** | Only correct for specific structures; hard to prove; often wrong |
| **Use** | Activity selection; jump game; meeting rooms; Huffman; gas station |
| **Don't use** | 0/1 Knapsack → DP; future choices depend on current → DP |

```java
// Interval scheduling — sort by END time (not start!)
Arrays.sort(intervals, (a,b)->a[1]-b[1]);
int cnt=1, end=intervals[0][1];
for(int i=1;i<n;i++) if(intervals[i][0]>=end){cnt++;end=intervals[i][1];}

// Jump Game
int reach=0;
for(int i=0;i<n;i++){if(i>reach)return false;reach=Math.max(reach,i+nums[i]);}

// Meeting Rooms II
PriorityQueue<Integer> ends=new PriorityQueue<>();
Arrays.sort(intervals,(a,b)->a[0]-b[0]);
for(int[]i:intervals){if(!ends.isEmpty()&&ends.peek()<=i[0])ends.poll();ends.offer(i[1]);}
return ends.size();
```

---

### 13. Dynamic Programming
| | |
|--|--|
| **Trigger** | Optimal value (min/max/count/true-false), overlapping subproblems |
| **Advantage** | Converts exponential to polynomial by caching repeated subproblems |
| **Tradeoff** | O(n²)/O(n·W) space; harder recurrence derivation; not always obvious |
| **Use** | LCS, LIS, knapsack, edit distance, coin change, palindrome partition |
| **Don't use** | No overlapping subproblems → D&C; greedy provably works; need all solutions → backtrack |

```java
// 1D DP
int[] dp=new int[n+1]; dp[0]=base;
for(int i=1;i<=n;i++) dp[i]=recurrence(dp,i);

// 2D DP
int[][]dp=new int[m+1][n+1];
for(int i=1;i<=m;i++) for(int j=1;j<=n;j++) dp[i][j]=recurrence(dp,i,j);
```

| Problem | State | Recurrence |
|---------|-------|------------|
| Climbing stairs | dp[i] | dp[i-1]+dp[i-2] |
| House Robber | dp[i] | max(dp[i-1], dp[i-2]+num) |
| 0/1 Knapsack | dp[i][w] | max(dp[i-1][w], dp[i-1][w-wt]+val) |
| LCS | dp[i][j] | match→dp[i-1][j-1]+1 else max(L,U) |
| Edit Distance | dp[i][j] | match→dp[i-1][j-1] else 1+min(3) |
| Coin Change | dp[i] | min(dp[i-coin]+1) per coin |

---

### 14. Graph Algorithms
| Algorithm | Use | Time | Advantage | Don't use when |
|-----------|-----|------|-----------|----------------|
| BFS | Shortest path unweighted | O(V+E) | Guaranteed shortest | Weighted graph |
| DFS | Connectivity, cycle, topo | O(V+E) | Low space | Need shortest path |
| Dijkstra | Weighted shortest path | O((V+E)log V) | Fast with heap | Negative weights |
| Bellman-Ford | Negative weights | O(VE) | Detects neg cycles | Large dense graphs |
| Floyd-Warshall | All-pairs SP | O(V³) | Simple 3-loop | Large V |
| Kruskal | MST sparse | O(E log E) | Simple sort+UF | Dense graph |
| Prim | MST dense | O(E log V) | Better for dense | Sparse graph |
| Topo Sort | DAG ordering | O(V+E) | Also detects cycle | Cyclic graphs |

```java
// Dijkstra — skip stale entries!
PriorityQueue<int[]> pq=new PriorityQueue<>((a,b)->a[1]-b[1]);
pq.offer(new int[]{src,0}); Arrays.fill(dist,INF); dist[src]=0;
while(!pq.isEmpty()){int[]c=pq.poll();if(c[1]>dist[c[0]])continue; // STALE CHECK
    for(int[]e:adj.get(c[0]))if(dist[c[0]]+e[1]<dist[e[0]])
        pq.offer(new int[]{e[0],dist[e[0]]=dist[c[0]]+e[1]});}

// Topological Sort (Kahn's) — empty result = cycle
int[]indeg=new int[n]; for(int u=0;u<n;u++) for(int v:adj.get(u)) indeg[v]++;
Queue<Integer>q=new LinkedList<>();
for(int i=0;i<n;i++) if(indeg[i]==0) q.offer(i);
while(!q.isEmpty()){int u=q.poll();order.add(u);
    for(int v:adj.get(u)) if(--indeg[v]==0) q.offer(v);}

// Union-Find with compression + rank
int find(int x){return parent[x]==x?x:(parent[x]=find(parent[x]));}
boolean union(int x,int y){int px=find(x),py=find(y);if(px==py)return false;
    if(rank[px]<rank[py])parent[px]=py;
    else if(rank[px]>rank[py])parent[py]=px;
    else{parent[py]=px;rank[px]++;}return true;}
```

---

### 15. Trie
| | |
|--|--|
| **Trigger** | Prefix search, autocomplete, word dictionary, IP routing |
| **Advantage** | O(L) ops; shared prefixes save space vs storing all strings separately |
| **Tradeoff** | High memory (26 children/node); slower exact match vs HashMap O(1) |
| **Use** | Prefix queries; autocomplete; word search with wildcard |
| **Don't use** | Only exact lookups → HashMap; very small dictionary |

```java
class TrieNode{TrieNode[]ch=new TrieNode[26];boolean end;}
void insert(String w){TrieNode n=root;for(char c:w.toCharArray())
    {int i=c-'a';if(n.ch[i]==null)n.ch[i]=new TrieNode();n=n.ch[i];}n.end=true;}
boolean search(String w){TrieNode n=root;for(char c:w.toCharArray())
    {if(n.ch[c-'a']==null)return false;n=n.ch[c-'a'];}return n.end;}
```

---

### 16. Bit Manipulation
| | |
|--|--|
| **Trigger** | Power of 2, XOR pairs, subset enumeration, permission flags |
| **Advantage** | O(1) operations; space efficient; bitmask DP compresses state |
| **Tradeoff** | Hard to read; bitmask DP limited to n≤20; Java sign issues with >> |
| **Use** | Flags/permissions; find single number; enumerate subsets; bitmask DP |
| **Don't use** | n>64 for bitmask; when readability matters more |

```java
n & (n-1)        // clear lowest set bit; isPow2: n>0 && (n&(n-1))==0
n & (-n)         // isolate lowest set bit
a ^ b ^ b = a    // XOR self-inverse → single number
(n>>i)&1         // check bit i (use >>> for unsigned)
n|(1<<i)         // set bit i
n&~(1<<i)        // clear bit i
for(int m=0;m<(1<<n);m++) // enumerate all 2^n subsets
```

---

### 17. String Algorithms
| Algorithm | Use | Time | Advantage | Tradeoff |
|-----------|-----|------|-----------|---------|
| KMP | Exact pattern match | O(n+m) | No backtracking in text | LPS build tricky |
| Rabin-Karp | Multi-pattern/rolling | O(n+m) avg | Multiple patterns | Collision → verify |
| Z-Algorithm | Pattern matching | O(n+m) | Simpler than KMP | Less well-known |
| Manacher's | Longest palindrome | O(n) | Optimal | Complex with # transform |
| String Hash | Substring compare | O(1) query | Fast range compare | Collision possible |

```java
// KMP — LPS array
int[]lps=new int[m];int len=0,i=1;
while(i<m){if(p.charAt(i)==p.charAt(len))lps[i++]=++len;
    else if(len!=0)len=lps[len-1];else lps[i++]=0;}

// String Hashing — O(1) substring compare
long[]hash=new long[n+1],pw=new long[n+1]; pw[0]=1;
for(int i=0;i<n;i++){hash[i+1]=(hash[i]*B+s.charAt(i))%M;pw[i+1]=pw[i]*B%M;}
long getHash(int l,int r){return(hash[r+1]-hash[l]*pw[r-l+1]%M+M)%M;}
```

---

### 18. Segment Tree & Fenwick Tree
| | Segment Tree | Fenwick Tree |
|--|-------------|--------------|
| **Use** | Range queries + range updates | Prefix sums + point updates |
| **Advantage** | Range min/max/sum + lazy prop | Simpler code, less space |
| **Tradeoff** | 4n space, complex lazy | No range min/max, harder range update |
| **Use when** | Range updates needed; range min/max | Just prefix sums with point updates |

```java
// Fenwick Tree (1-indexed always)
void update(int i,int v){for(;i<=n;i+=i&-i)bit[i]+=v;}
int query(int i){int s=0;for(;i>0;i-=i&-i)s+=bit[i];return s;}
int range(int l,int r){return query(r)-query(l-1);}

// Segment Tree — point update, range sum
void update(int node,int s,int e,int idx,int v){
    if(s==e){tree[node]=v;return;}int m=(s+e)/2;
    if(idx<=m)update(2*node+1,s,m,idx,v);else update(2*node+2,m+1,e,idx,v);
    tree[node]=tree[2*node+1]+tree[2*node+2];}
int query(int node,int s,int e,int l,int r){
    if(r<s||e<l)return 0;if(l<=s&&e<=r)return tree[node];int m=(s+e)/2;
    return query(2*node+1,s,m,l,r)+query(2*node+2,m+1,e,l,r);}
```

---

### 19. Interview Patterns (Cyclic Sort / Boyer-Moore / Reservoir / Floyd)
| Pattern | Trigger | Time | Space | Advantage |
|---------|---------|------|-------|-----------|
| Cyclic Sort | Missing/duplicate in [1..n] | O(n) | O(1) | No extra space |
| Boyer-Moore | Majority element >n/2 | O(n) | O(1) | No extra space, always verify |
| Reservoir Sampling | Random K from unknown-size stream | O(n) | O(k) | Uniform probability |
| Fisher-Yates | Uniform random shuffle | O(n) | O(1) | Provably uniform |
| Floyd's Cycle | Duplicate in [1..n] no modification | O(n) | O(1) | Read-only array |

```java
// Cyclic Sort — place nums[i] at index nums[i]-1
int i=0; while(i<n){int j=nums[i]-1;if(nums[i]!=nums[j])swap(i,j);else i++;}
int findMissing(){for(int i=0;i<n;i++)if(nums[i]!=i+1)return i+1;return n+1;}

// Boyer-Moore — ALWAYS verify after
int c=nums[0],cnt=1;
for(int i=1;i<n;i++){if(cnt==0){c=nums[i];cnt=1;}else if(nums[i]==c)cnt++;else cnt--;}

// Reservoir — k samples from stream
int[]res=Arrays.copyOf(stream,k);
for(int i=k;i<n;i++){int j=rand.nextInt(i+1);if(j<k)res[j]=stream[i];}
```

---

### 20. Balanced Trees & Advanced Structures
| Structure | Advantage | Tradeoff | Use When |
|-----------|-----------|---------|---------|
| AVL Tree | Strict balance → fastest reads | More rotations on write | Read-heavy in-memory |
| Red-Black | Fewer rotations → faster writes | Slightly taller than AVL | General (Java TreeMap) |
| B+ Tree | Flat + linked leaves → range queries | Complex, disk-oriented | DB indexes (MySQL/PG) |
| Skip List | Simple concurrent implementation | Probabilistic, more space | Redis sorted sets |

**AVL rotations**: LL→Right rotate | RR→Left rotate | LR→Left+Right | RL→Right+Left

```java
// Java gives you Red-Black Tree via:
TreeMap<K,V> map = new TreeMap<>();    // sorted map O(log n) all ops
TreeSet<E> set = new TreeSet<>();      // sorted set
map.floorKey(k);  map.ceilingKey(k);  map.firstKey(); map.lastKey();
map.subMap(k1, k2); // range [k1, k2)
```

---

### 21. Multi-threaded DSA
| Need | Collection | Advantage | Tradeoff |
|------|------------|-----------|---------|
| Thread-safe map | `ConcurrentHashMap` | Segment locking, fast | Slightly slower than HashMap |
| Read-heavy list | `CopyOnWriteArrayList` | Lock-free reads | O(n) per write |
| Producer-consumer | `LinkedBlockingQueue` | Backpressure built-in | Bounded capacity |
| Lock-free counter | `AtomicInteger` | CAS — no lock | Single value only |
| Many readers | `ReentrantReadWriteLock` | Concurrent reads | Write blocks all readers |

```java
ConcurrentHashMap<K,V> map = new ConcurrentHashMap<>();
map.compute(key,(k,v)->v==null?1:v+1); // atomic read-modify-write

BlockingQueue<Task> q = new LinkedBlockingQueue<>(100);
q.put(task); // blocks if full    q.take(); // blocks if empty

AtomicInteger cnt = new AtomicInteger();
cnt.incrementAndGet(); cnt.compareAndSet(expected, newVal);
```

---

### 22. Suffix Arrays, A*, Randomised, Network Flow
| Topic | Advantage | Tradeoff | Use When |
|-------|-----------|---------|---------|
| Suffix Array | O(log n) string search after O(n log n) build | Complex build | String-heavy interviews |
| A* | Faster than Dijkstra with good heuristic | Heuristic must be admissible | Single src→dst pathfinding |
| Bloom Filter | O(1) lookup, tiny space | False positives possible | Dedup at massive scale |
| Randomised QS | Avoids O(n²) worst case | Expected not guaranteed | Sorting with adversarial input |
| Network Flow | Solves bipartite matching, assignment | O(VE²) slow for large graphs | Matching/assignment problems |

---

## 🗂️ Quick Data Structure Picks

| Need | Use | Why |
|------|-----|-----|
| O(1) lookup by key | `HashMap` | Hash-based |
| Sorted + range | `TreeMap` | Red-Black Tree |
| Unique + fast check | `HashSet` | Hash-based |
| Unique + sorted | `TreeSet` | Red-Black Tree |
| LIFO | `ArrayDeque` as stack | Faster than `Stack` |
| FIFO | `LinkedList`/`ArrayDeque` | |
| Dynamic min/max | `PriorityQueue` | Min-heap default |
| Sliding window max | `ArrayDeque` monotonic | O(1) amortized |
| Prefix search | `Trie` | Shared prefix nodes |
| Range + updates | Segment Tree | O(log n) both |
| Prefix sum + updates | Fenwick Tree | Simpler |
| Missing in [1..n] | Cyclic Sort | O(n) O(1) space |
| Connected components | Union-Find | O(α(n)) ≈ O(1) |
| Thread-safe map | `ConcurrentHashMap` | Segment locking |

---

## 📐 Graph Algorithm Quick Pick

| Problem | Algorithm | Time |
|---------|-----------|------|
| Shortest path unweighted | BFS | O(V+E) |
| Shortest path weighted (no neg) | Dijkstra | O((V+E)log V) |
| Shortest path with negative weights | Bellman-Ford | O(VE) |
| All-pairs shortest path | Floyd-Warshall | O(V³) |
| Minimum Spanning Tree | Kruskal/Prim | O(E log E) |
| Topological order | Kahn's BFS | O(V+E) |
| Strongly Connected Components | Kosaraju | O(V+E) |
| Bridges & Articulation Points | Tarjan | O(V+E) |
| Connected components | DFS/Union-Find | O(V+E) |
| Bipartite matching | Max Flow | O(VE²) |

---

## ⚠️ Universal Edge Cases

- Empty / null / single element
- All same elements / all duplicates  
- Already sorted / reverse sorted input
- Integer overflow → use `long`; `lo + (hi-lo)/2` not `(lo+hi)/2`
- Negative numbers in sum/product problems
- Cycle in graph → visited array; in linked list → fast & slow
- Duplicates in backtracking → sort first + skip `if(i>start&&nums[i]==nums[i-1])`
- HashMap: `.equals()` not `==` for object comparison
- Backtracking: `new ArrayList<>(curr)` not `curr` when adding to result

---

## 📐 Complexity Targets

| n | Max Complexity | Approach |
|---|---------------|---------|
| ≤ 10 | O(n!) | Permutations |
| ≤ 20 | O(2^n) | Bitmask DP, backtracking |
| ≤ 500 | O(n³) | Floyd-Warshall, interval DP |
| ≤ 5000 | O(n²) | Basic DP, brute nested loop |
| ≤ 10^5 | O(n log n) | Merge sort, heap, binary search |
| ≤ 10^6 | O(n) | Sliding window, prefix sum, counting sort |
| > 10^6 | O(log n)/O(1) | Binary search, math formula |
