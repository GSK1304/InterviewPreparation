# 📚 DSA Deep Dive — Graphs

---

## 🧠 Core Concepts

### Representations
```java
// Adjacency List (preferred — sparse graphs)
List<List<Integer>> adj = new ArrayList<>();
for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
adj.get(u).add(v); // directed edge u → v
adj.get(v).add(u); // undirected: add both

// Adjacency Matrix (dense graphs)
int[][] matrix = new int[n][n];
matrix[u][v] = 1;

// Edge List
int[][] edges = {{0,1}, {1,2}, {2,3}};
```

### Types
- **Directed / Undirected**
- **Weighted / Unweighted**
- **Cyclic / Acyclic (DAG)**
- **Connected / Disconnected**

---

## 🔑 Key Algorithms

### BFS (Shortest Path — Unweighted)
```java
int[] bfs(List<List<Integer>> adj, int start, int n) {
    int[] dist = new int[n];
    Arrays.fill(dist, -1);
    Queue<Integer> q = new LinkedList<>();
    q.offer(start); dist[start] = 0;
    while (!q.isEmpty()) {
        int node = q.poll();
        for (int neighbor : adj.get(node)) {
            if (dist[neighbor] == -1) {
                dist[neighbor] = dist[node] + 1;
                q.offer(neighbor);
            }
        }
    }
    return dist;
}
```

### DFS (Connected Components, Cycle Detection)
```java
void dfs(int node, boolean[] visited, List<List<Integer>> adj) {
    visited[node] = true;
    for (int neighbor : adj.get(node)) {
        if (!visited[neighbor]) dfs(neighbor, visited, adj);
    }
}

// Count connected components
int countComponents(int n, List<List<Integer>> adj) {
    boolean[] visited = new boolean[n];
    int count = 0;
    for (int i = 0; i < n; i++) {
        if (!visited[i]) { dfs(i, visited, adj); count++; }
    }
    return count;
}
```

### Topological Sort — Kahn's Algorithm (BFS)
```java
List<Integer> topSort(int n, List<List<Integer>> adj) {
    int[] indegree = new int[n];
    for (int u = 0; u < n; u++)
        for (int v : adj.get(u)) indegree[v]++;

    Queue<Integer> q = new LinkedList<>();
    for (int i = 0; i < n; i++) if (indegree[i] == 0) q.offer(i);

    List<Integer> order = new ArrayList<>();
    while (!q.isEmpty()) {
        int node = q.poll();
        order.add(node);
        for (int neighbor : adj.get(node))
            if (--indegree[neighbor] == 0) q.offer(neighbor);
    }
    return order.size() == n ? order : new ArrayList<>(); // empty = cycle exists
}
```

### Union-Find (Disjoint Sets)
```java
class UnionFind {
    int[] parent, rank;
    UnionFind(int n) {
        parent = new int[n]; rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }
    int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]); // path compression
        return parent[x];
    }
    boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false; // already connected
        if (rank[px] < rank[py]) parent[px] = py;
        else if (rank[px] > rank[py]) parent[py] = px;
        else { parent[py] = px; rank[px]++; }
        return true;
    }
}
```

### Grid DFS (Islands Pattern)
```java
int numIslands(char[][] grid) {
    int count = 0;
    for (int r = 0; r < grid.length; r++)
        for (int c = 0; c < grid[0].length; c++)
            if (grid[r][c] == '1') { dfsGrid(grid, r, c); count++; }
    return count;
}
void dfsGrid(char[][] grid, int r, int c) {
    if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] != '1') return;
    grid[r][c] = '0'; // mark visited
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    for (int[] d : dirs) dfsGrid(grid, r+d[0], c+d[1]);
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Microservice Dependency Graph (kACE Architecture)
**Problem**: Detect circular dependencies between microservices; find startup order.

```java
// Topological sort — startup order for kACE microservices
// If topSort returns empty → circular dependency detected
List<Integer> startupOrder = topSort(services.size(), dependencyGraph);
if (startupOrder.isEmpty()) throw new CircularDependencyException();
```
**Where it applies**: Spring Boot microservice startup ordering, Gradle/Maven build dependency resolution, kace-common-library dependency graph.
> 🏭 **Industry Example**: Netflix uses topological sort on their microservice dependency graph to determine safe deployment order. Kubernetes uses DAG-based dependency resolution for pod startup ordering. npm/yarn use Kahn's algorithm to install packages in dependency order.
> 🏦 **kACE Context**: kACE microservice startup ordering — ensuring auth service starts before pricing service, pricing before RFQ.


---

### Use Case 2: Network Topology — Shortest Path
**Problem**: Find shortest hop count between two services in a microservice mesh.

```java
// BFS on service mesh graph
int shortestPath(Map<String, List<String>> mesh, String src, String dst) {
    Queue<String> q = new LinkedList<>();
    Set<String> visited = new HashSet<>();
    q.offer(src); visited.add(src);
    int hops = 0;
    while (!q.isEmpty()) {
        int size = q.size();
        for (int i = 0; i < size; i++) {
            String node = q.poll();
            if (node.equals(dst)) return hops;
            for (String neighbor : mesh.getOrDefault(node, List.of()))
                if (visited.add(neighbor)) q.offer(neighbor);
        }
        hops++;
    }
    return -1;
}
```
**Where it applies**: API Gateway routing, service mesh latency optimization, Kafka broker topology.
> 🏭 **Industry Example**: Facebook's network infrastructure team uses BFS to find shortest paths between data centers. Cloudflare's Argo Smart Routing uses BFS on their network graph to route traffic through the fastest path. AWS VPC routing uses shortest-path algorithms.
> 🏦 **kACE Context**: API Gateway routing — finding minimum-latency path between kACE microservices.


---

### Use Case 3: WebSocket Session Clustering (Connected Components)
**Problem**: Group WebSocket sessions that share the same subscription topics — find isolated clusters.

```java
// Connected components — each component = cluster of related sessions
int[] sessionCluster = new int[sessions.length];
Arrays.fill(sessionCluster, -1);
int clusterId = 0;
for (int i = 0; i < sessions.length; i++) {
    if (sessionCluster[i] == -1) {
        dfsAssign(i, clusterId++, sessionCluster, sharedTopicsGraph);
    }
}
```
**Where it applies**: kACE WebSocket subscription tracking (`SubscriptionRegistry`), RFQ session grouping.
> 🏭 **Industry Example**: Discord uses connected component detection to identify isolated server clusters during outages. Slack uses Union-Find to group users who share workspace subscriptions for targeted message delivery.
> 🏦 **kACE Context**: `SubscriptionRegistry` — grouping WebSocket sessions by shared RFQ topic subscriptions.


---

### Use Case 4: Union-Find for Network Partition Detection
**Problem**: Given network links, determine if two services can communicate (same partition).

```java
UnionFind uf = new UnionFind(n);
for (int[] link : networkLinks) uf.union(link[0], link[1]);

// Check connectivity
boolean canCommunicate = uf.find(serviceA) == uf.find(serviceB);

// Count isolated partitions
Set<Integer> roots = new HashSet<>();
for (int i = 0; i < n; i++) roots.add(uf.find(i));
int partitions = roots.size();
```
**Where it applies**: Kafka broker partition assignment, network split detection, DR failover grouping.
> 🏭 **Industry Example**: Zookeeper uses Union-Find concepts to detect quorum loss in distributed clusters. Cassandra's gossip protocol detects network partitions using a similar connected-components approach. Kubernetes node controller uses this to detect split-brain scenarios.
> 🏦 **kACE Context**: Kafka broker partition assignment and detecting split-brain between kACE service instances.


---

### Use Case 5: Course Schedule / Feature Dependencies (DAG)
**Problem**: Given feature dependencies, determine if all features can be released (no cycles) and in what order.

```java
// Model features as nodes, dependencies as directed edges
// Kahn's topological sort → release order
// If cycle exists → deadlocked dependencies

List<Integer> releaseOrder = topSort(features.size(), dependencyGraph);
if (releaseOrder.isEmpty()) {
    System.out.println("Circular feature dependency detected — release blocked!");
} else {
    System.out.println("Release order: " + releaseOrder);
}
```
**Where it applies**: Sprint planning — ordering features with dependencies, CI/CD pipeline stage ordering, Jenkins downstream triggers.
> 🏭 **Industry Example**: Jenkins CI/CD pipeline uses topological sort to order build stages. GitHub Actions uses DAG-based workflow execution. Airflow uses topological sort for DAG task scheduling.
> 🏦 **kACE Context**: Sprint planning — ordering kACE feature releases respecting inter-feature dependencies.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Number of Islands | Grid DFS | Medium |
| 2 | Clone Graph | BFS/DFS | Medium |
| 3 | Course Schedule | Topo Sort / Cycle detect | Medium |
| 4 | Course Schedule II | Topo Sort | Medium |
| 5 | Number of Connected Components | Union-Find / DFS | Medium |
| 6 | Rotting Oranges | Multi-source BFS | Medium |
| 7 | Pacific Atlantic Water Flow | Multi-source DFS | Medium |
| 8 | Word Ladder | BFS shortest path | Hard |
| 9 | Redundant Connection | Union-Find | Medium |
| 10 | Alien Dictionary | Topo Sort + BFS | Hard |

---

## ⚠️ Common Mistakes

- Not marking visited **before** adding to BFS queue → same node processed multiple times
- DFS on graph (not tree): always use `visited` array — trees don't have cross edges
- Topological sort: only valid for **directed acyclic graphs** (DAG)
- Union-Find: always use path compression (`find`) + union by rank for O(α(n)) ≈ O(1)
- Grid problems: check bounds before accessing `grid[r][c]`
- Directed cycle detection with DFS: need 3 states (unvisited / in-stack / done), not just boolean
