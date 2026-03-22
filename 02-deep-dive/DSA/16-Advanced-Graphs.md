# 📚 DSA Deep Dive — Advanced Graphs

---

## 🧠 Topics Covered
1. Dijkstra's Algorithm (Weighted Shortest Path)
2. Bellman-Ford (Negative Weights)
3. Floyd-Warshall (All-Pairs Shortest Path)
4. Minimum Spanning Tree — Prim's & Kruskal's
5. Strongly Connected Components — Kosaraju's
6. Bridges & Articulation Points — Tarjan's

---

## 1. Dijkstra's Algorithm

**Use**: Shortest path from source to all nodes in **non-negative** weighted graph.
**Time**: O((V + E) log V) with min-heap

```java
int[] dijkstra(int src, int n, List<int[]>[] adj) {
    // adj[u] = list of {v, weight}
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]); // {node, dist}
    pq.offer(new int[]{src, 0});

    while (!pq.isEmpty()) {
        int[] cur = pq.poll();
        int node = cur[0], d = cur[1];
        if (d > dist[node]) continue; // stale entry — skip
        for (int[] edge : adj[node]) {
            int neighbor = edge[0], weight = edge[1];
            if (dist[node] + weight < dist[neighbor]) {
                dist[neighbor] = dist[node] + weight;
                pq.offer(new int[]{neighbor, dist[neighbor]});
            }
        }
    }
    return dist;
}
```

> ⚠️ Dijkstra fails with **negative weights** → use Bellman-Ford

---

## 2. Bellman-Ford

**Use**: Shortest path with **negative weights**; detects **negative cycles**.
**Time**: O(V × E)

```java
int[] bellmanFord(int src, int n, int[][] edges) {
    // edges[i] = {u, v, weight}
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;

    // Relax all edges V-1 times
    for (int i = 0; i < n - 1; i++) {
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            if (dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v])
                dist[v] = dist[u] + w;
        }
    }

    // Check for negative cycle (Vth relaxation still improves)
    for (int[] edge : edges) {
        int u = edge[0], v = edge[1], w = edge[2];
        if (dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v])
            throw new RuntimeException("Negative cycle detected");
    }
    return dist;
}
```

---

## 3. Floyd-Warshall

**Use**: **All-pairs** shortest path. Works with negative weights (not negative cycles).
**Time**: O(V³), **Space**: O(V²)

```java
int[][] floydWarshall(int[][] graph, int n) {
    int[][] dist = new int[n][n];
    for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE / 2);
    for (int i = 0; i < n; i++) dist[i][i] = 0;
    for (int u = 0; u < n; u++)
        for (int v = 0; v < n; v++)
            if (graph[u][v] != 0) dist[u][v] = graph[u][v];

    // Try every node as intermediate
    for (int k = 0; k < n; k++)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (dist[i][k] + dist[k][j] < dist[i][j])
                    dist[i][j] = dist[i][k] + dist[k][j];

    return dist;
}
```

---

## 4. Minimum Spanning Tree

### Kruskal's (Edge-based, Union-Find)
**Time**: O(E log E)
```java
int kruskal(int n, int[][] edges) {
    Arrays.sort(edges, (a, b) -> a[2] - b[2]); // sort by weight
    UnionFind uf = new UnionFind(n);
    int totalWeight = 0, edgesUsed = 0;
    for (int[] edge : edges) {
        if (uf.union(edge[0], edge[1])) { // no cycle
            totalWeight += edge[2];
            if (++edgesUsed == n - 1) break; // MST complete
        }
    }
    return totalWeight;
}
```

### Prim's (Node-based, Min-Heap)
**Time**: O(E log V)
```java
int prims(int n, List<int[]>[] adj) {
    boolean[] inMST = new boolean[n];
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]); // {node, weight}
    pq.offer(new int[]{0, 0});
    int totalWeight = 0;
    while (!pq.isEmpty()) {
        int[] cur = pq.poll();
        int node = cur[0], weight = cur[1];
        if (inMST[node]) continue;
        inMST[node] = true;
        totalWeight += weight;
        for (int[] edge : adj[node])
            if (!inMST[edge[0]]) pq.offer(new int[]{edge[0], edge[1]});
    }
    return totalWeight;
}
```

---

## 5. Strongly Connected Components — Kosaraju's

**Use**: Find groups of nodes where every node can reach every other node.
**Time**: O(V + E)

```java
List<List<Integer>> kosaraju(int n, List<List<Integer>> adj) {
    // Step 1: DFS on original graph, push to stack in finish order
    boolean[] visited = new boolean[n];
    Deque<Integer> stack = new ArrayDeque<>();
    for (int i = 0; i < n; i++)
        if (!visited[i]) dfs1(i, adj, visited, stack);

    // Step 2: Build reversed graph
    List<List<Integer>> radj = new ArrayList<>();
    for (int i = 0; i < n; i++) radj.add(new ArrayList<>());
    for (int u = 0; u < n; u++)
        for (int v : adj.get(u)) radj.get(v).add(u);

    // Step 3: DFS on reversed graph in stack order
    Arrays.fill(visited, false);
    List<List<Integer>> sccs = new ArrayList<>();
    while (!stack.isEmpty()) {
        int node = stack.pop();
        if (!visited[node]) {
            List<Integer> scc = new ArrayList<>();
            dfs2(node, radj, visited, scc);
            sccs.add(scc);
        }
    }
    return sccs;
}

void dfs1(int node, List<List<Integer>> adj, boolean[] visited, Deque<Integer> stack) {
    visited[node] = true;
    for (int neighbor : adj.get(node))
        if (!visited[neighbor]) dfs1(neighbor, adj, visited, stack);
    stack.push(node);
}
void dfs2(int node, List<List<Integer>> adj, boolean[] visited, List<Integer> scc) {
    visited[node] = true; scc.add(node);
    for (int neighbor : adj.get(node))
        if (!visited[neighbor]) dfs2(neighbor, adj, visited, scc);
}
```

---

## 6. Bridges & Articulation Points — Tarjan's

**Bridges**: Edges whose removal disconnects the graph.
**Articulation Points**: Nodes whose removal disconnects the graph.
**Time**: O(V + E)

```java
int timer = 0;
List<int[]> bridges = new ArrayList<>();
Set<Integer> articulationPoints = new HashSet<>();

void tarjan(int n, List<List<Integer>> adj) {
    int[] disc = new int[n], low = new int[n];
    Arrays.fill(disc, -1);
    for (int i = 0; i < n; i++)
        if (disc[i] == -1) dfs(i, -1, disc, low, adj);
}

void dfs(int u, int parent, int[] disc, int[] low, List<List<Integer>> adj) {
    disc[u] = low[u] = timer++;
    int children = 0;
    for (int v : adj.get(u)) {
        if (disc[v] == -1) {
            children++;
            dfs(v, u, disc, low, adj);
            low[u] = Math.min(low[u], low[v]);
            // Bridge condition
            if (low[v] > disc[u]) bridges.add(new int[]{u, v});
            // Articulation point condition
            if (parent == -1 && children > 1) articulationPoints.add(u);
            if (parent != -1 && low[v] >= disc[u]) articulationPoints.add(u);
        } else if (v != parent) {
            low[u] = Math.min(low[u], disc[v]);
        }
    }
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Shortest Trade Route (Dijkstra — kACE FX)
**Problem**: Find minimum latency path between trading venues in the FX network.

```java
// Nodes = trading venues, edges = network links with latency weights
int[] minLatency = dijkstra(kACEHub, venues.size(), networkGraph);
System.out.println("Min latency to Tokyo: " + minLatency[TOKYO] + "ms");
```
**Where it applies**: FX trade routing optimization, API Gateway latency-based routing.
> 🏭 **Industry Example**: Google Maps uses a variant of Dijkstra (A* with geographic heuristic) for driving directions. Uber and Lyft use Dijkstra on road networks to find optimal driver routes. Network routers use OSPF (Open Shortest Path First), which is Dijkstra on network topology.
> 🏦 **kACE Context**: FX trade routing optimization — finding minimum latency path between trading venues in the kACE network.


---

### Use Case 2: Microservice Cluster Detection (Kosaraju SCC)
**Problem**: Find groups of microservices that are tightly coupled (call each other in cycles).

```java
// Each SCC = tightly coupled service cluster → refactor candidate
List<List<Integer>> sccs = kosaraju(services.size(), callGraph);
for (List<Integer> scc : sccs)
    if (scc.size() > 1) System.out.println("Tight coupling: " + scc);
```
**Where it applies**: kACE microservice architecture review, Spring Boot dependency analysis.
> 🏭 **Industry Example**: Twitter's SCC analysis revealed tightly coupled service clusters that became refactoring candidates. Netflix's Chaos Engineering team uses SCC to identify services that will cascade-fail together. LinkedIn uses SCC on their service dependency graph for deployment safety analysis.
> 🏦 **kACE Context**: kACE microservice architecture review — identifying tightly coupled service clusters for modularization.


---

### Use Case 3: Network Single Points of Failure (Bridges/APs)
**Problem**: Find network links/nodes whose failure would split the kACE infrastructure.

```java
tarjan(nodes.size(), networkGraph);
System.out.println("Critical links (bridges): " + bridges);
System.out.println("Critical nodes (APs): " + articulationPoints);
// → Plan redundancy for these
```
**Where it applies**: kACE infrastructure resilience planning, Kafka broker single-point-of-failure detection.
> 🏭 **Industry Example**: AWS's network topology analysis uses bridge detection to identify single-link failure risks. Facebook's data center network team uses articulation point analysis to find network devices whose failure would partition the datacenter. Cloudflare uses bridge analysis to ensure no single PoP disconnect isolates a region.
> 🏦 **kACE Context**: kACE infrastructure resilience planning — finding network links whose failure would split the trading platform.


---

### Use Case 4: Optimal Cable Layout (MST — Kruskal's)
**Problem**: Connect all kACE office locations with minimum total cable length.

```java
// Nodes = office locations, edges = possible connections with costs
int minCost = kruskal(offices.size(), possibleConnections);
System.out.println("Minimum infrastructure cost: $" + minCost);
```
**Where it applies**: Network infrastructure planning, Kafka broker cluster topology optimization.
> 🏭 **Industry Example**: Google uses Kruskal's MST algorithm to design undersea fiber cable networks (minimize total cable length connecting data centers). AWS infrastructure uses MST for VPC peering topology optimization. Telecommunications companies use MST for optimal tower placement networks.
> 🏦 **kACE Context**: Connecting kACE office locations with minimum total network infrastructure cost.


---

### Use Case 5: All-Pairs Service Latency Matrix (Floyd-Warshall)
**Problem**: Precompute minimum latency between all pairs of kACE microservices.

```java
int[][] latencyMatrix = floydWarshall(directLatencies, services.size());
// Now O(1) query: what's the min latency from Auth to Pricing?
int minLatency = latencyMatrix[AUTH][PRICING];
```
**Where it applies**: kACE service mesh latency optimization, API Gateway routing table precomputation.
> 🏭 **Industry Example**: Cloudflare Argo precomputes all-pairs latency between PoPs using Floyd-Warshall for routing tables. AWS Global Accelerator uses precomputed latency matrices. Akamai CDN routing uses all-pairs precomputation for optimal edge selection.
> 🏦 **kACE Context**: kACE service mesh — precomputing minimum latency between all microservice pairs for API Gateway routing.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Network Delay Time | Dijkstra | Medium |
| 2 | Cheapest Flights Within K Stops | Modified Dijkstra/BF | Medium |
| 3 | Path with Minimum Effort | Dijkstra on grid | Medium |
| 4 | Min Cost to Connect All Points | Prim's / Kruskal's | Medium |
| 5 | Redundant Connection II | Union-Find | Hard |
| 6 | Critical Connections in Network | Bridges Tarjan | Hard |
| 7 | Number of Provinces (SCC) | DFS / Union-Find | Medium |
| 8 | Find the City (Floyd-Warshall) | All-pairs SP | Medium |
| 9 | Reconstruct Itinerary | Eulerian path DFS | Hard |
| 10 | Swim in Rising Water | Dijkstra / Binary Search | Hard |

---

## ⚠️ Common Mistakes

- Dijkstra: **skip stale entries** — check `if (d > dist[node]) continue`
- Dijkstra with negative weights → wrong answer; use Bellman-Ford
- Bellman-Ford: relax exactly **V-1** times (not V), then check once more for negative cycles
- Floyd-Warshall: use `Integer.MAX_VALUE / 2` not `MAX_VALUE` to avoid overflow in `dist[i][k] + dist[k][j]`
- Kruskal: sort edges by weight **ascending** before processing
- Tarjan bridges: use `low[v] > disc[u]` for bridge, `low[v] >= disc[u]` for articulation point
- SCC: Kosaraju needs **two separate DFS passes** on original and reversed graph
