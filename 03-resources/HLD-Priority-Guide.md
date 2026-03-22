# 🗺️ HLD Topic Priority Guide

> Use this to decide what to study first, what to skim, and what to skip based on your timeline and target role.

---

## Priority Levels

| Level | Meaning | Study depth |
|-------|---------|------------|
| 🔴 **P0** | Asked in virtually every HLD interview | Master completely — must be able to explain and draw from memory |
| 🟠 **P1** | Asked in most senior engineer interviews | Understand deeply, be able to discuss tradeoffs |
| 🟡 **P2** | Asked in Staff/Principal or specialised roles | Conceptual understanding + one good example |
| 🟢 **P3** | Nice to know, adds depth | Read once — enough to mention if relevant |

---

## P0 — Master These First

These come up in every system design round, regardless of company or role.

### Core Concepts
| Topic | Key Points to Know | File |
|-------|-------------------|------|
| **CAP Theorem** | CP vs AP, why P is unavoidable, which DBs are which | 01-Fundamentals |
| **Caching patterns** | Cache-aside, write-through, write-behind, eviction, stampede, penetration, avalanche | 01-Fundamentals |
| **Load balancing** | L4 vs L7, algorithms (RR, least-conn, IP hash, consistent hash), health checks | 08-CDN-DNS |
| **Database scaling** | Read replicas, sharding strategies, when to use each | 10-Database-Deep-Dive |
| **Consistent hashing** | Ring, virtual nodes, K/N keys moved, use cases | 08-CDN-DNS |
| **Message queues** | Kafka vs SQS vs RabbitMQ, when to use async vs sync | 12-Communication-Patterns |
| **SQL vs NoSQL** | Decision framework, tradeoffs, which NoSQL type for which problem | 10-Database-Deep-Dive |
| **Rate limiting** | Sliding window counter, Redis implementation, distributed limits | 06-Rate-Limiter |
| **SSE/WebSocket scaling** | Multi-instance push problem, Redis Pub/Sub solution | 12-Communication-Patterns |

### System Designs (can be asked to any senior candidate)
| System | Why It's P0 | File |
|--------|-------------|------|
| **URL Shortener** | Classic starter, tests all fundamentals | 02-URL-Shortener |
| **Rate Limiter** | Asked as a standalone or component of other designs | 06-Rate-Limiter |
| **Messaging System** | Tests real-time, fan-out, offline delivery | 03-Messaging-System |
| **Notification System** | Multi-channel delivery is in every product | 07-Notification-System |
| **Distributed Cache** | Redis internals tested separately often | 18-Distributed-Cache |

---

## P1 — Essential for Senior Engineer Roles

These appear in the majority of senior (IC3–IC4 equivalent) interviews at top companies.

### Core Concepts
| Topic | Key Points | File |
|-------|-----------|------|
| **CDN** | Push vs pull, cache invalidation, when to use | 08-CDN-DNS |
| **API Gateway vs LB vs Proxy** | What each does, when to use each | 08-CDN-DNS |
| **Circuit Breaker** | States (closed/open/half-open), Resilience4j, bulkhead | 09-Microservices-Patterns |
| **Saga Pattern** | Choreography vs orchestration vs 2PC | 09-Microservices-Patterns |
| **Service Discovery** | Client-side vs server-side, Eureka, Consul, K8s DNS | 09-Microservices-Patterns |
| **Database indexing** | B-Tree, composite index, covering index, when index doesn't help | 10-Database-Deep-Dive |
| **Replication** | Sync vs async vs semi-sync, replication lag problems | 10-Database-Deep-Dive |
| **Observability** | Logs + metrics + traces, golden signals, SLI/SLO/SLA | 11-Observability-Security-DR |
| **Idempotency** | Why needed, idempotency key pattern, at-least-once + idempotent consumer | 11-Observability-Security-DR |
| **JWT/Auth** | Stateless JWT, JWKS, refresh token rotation, RBAC | 04-Auth-System-JWT |

### System Designs
| System | Why P1 | File |
|--------|--------|------|
| **YouTube/Netflix** | Video streaming, CDN, ABR — very commonly asked | 13-YouTube-Netflix |
| **Twitter/Feed** | Fan-out on write vs read, social system design | 14-Twitter-News-Feed |
| **Uber/Rides** | Geo-search (GEORADIUS), matching, real-time location | 15-Uber-Ride-Sharing |
| **Google Drive** | File chunking, delta sync, versioning | 16-Google-Drive |
| **Search Autocomplete** | Trie, data pipeline, freshness — asked standalone | 17-Search-Autocomplete |
| **Auth System** | Asked at every security-conscious company | 04-Auth-System-JWT |
| **Web Crawler** | Classic, tests BFS, dedup, politeness | 19-Web-Crawler |

---

## P2 — Important for Staff+ / Specialised Roles

These appear in Staff/Principal/Architect interviews, or for specific domains.

### Core Concepts
| Topic | Key Points | File |
|-------|-----------|------|
| **CQRS + Event Sourcing** | Separate read/write models, event log as state | 09-Microservices-Patterns |
| **WAL** | Crash recovery, how replication uses WAL | 10-Database-Deep-Dive |
| **Service Mesh** | Sidecar pattern, mTLS, traffic management | 09-Microservices-Patterns |
| **Disaster Recovery** | RTO vs RPO, 4 DR strategies, chaos engineering | 11-Observability-Security-DR |
| **Leader Election** | Raft, ZooKeeper ephemeral nodes, etcd leases, fencing tokens | 11-Observability-Security-DR |
| **Bloom Filter** | False positives, memory savings, use cases | DSA/21-Suffix |
| **Backpressure** | Bounded queue, load shedding, reactive streams | 12-Communication-Patterns |
| **RAG** | Full pipeline, chunking, vector DB, re-ranking | 33-AI-ML-RAG |
| **LLM Fundamentals** | Tokens, context window, hallucination mitigations | 32-AI-ML-How-LLMs-Work |

### System Designs
| System | Why P2 | File |
|--------|--------|------|
| **Kafka / Pub-Sub** | Often asked as infrastructure design | 20-PubSub-Kafka |
| **Payment Gateway** | Domain-critical for fintech/e-commerce roles | 21-Payment-Gateway |
| **Recommendation System** | ML-heavy, asked at Netflix/Spotify/Amazon | 23-Recommendation-System |
| **Google Maps** | Complex routing (Contraction Hierarchies) — asked at mapping companies | 31-Google-Maps |
| **Stock Exchange** | Trading domain, very relevant for Joy — asked at fintech | 26-Stock-Exchange |
| **Distributed Lock** | Often asked as a deep-dive component | 29-Distributed-Locking |
| **Google Docs** | OT/CRDTs — asked at collaboration tool companies | 25-Google-Docs |
| **CI/CD** | Asked at DevOps-focused companies | 28-CICD |

---

## P3 — Nice to Know

These add depth but are rarely the primary design question.

### Core Concepts
| Topic | Notes | File |
|-------|-------|------|
| **Speculative decoding** | LLM inference optimisation | 34-AI-ML-Inference |
| **PagedAttention** | vLLM internals | 34-AI-ML-Inference |
| **Contraction Hierarchies** | Routing algorithm detail | 31-Google-Maps |
| **Redlock** | Multi-node distributed lock | 29-Distributed-Locking |
| **Wilson score** | Comment ranking math | 30-Reddit-Quora |
| **Contextual retrieval** | Advanced RAG technique | 33-AI-ML-RAG |
| **DNS internals** | Record types, TTL strategy | 08-CDN-DNS |
| **mTLS / SPIFFE** | Service identity in zero-trust | 11-Observability-Security-DR |

### System Designs
| System | Notes | File |
|--------|-------|------|
| **Zoom** | SFU/MCU difference, WebRTC | 27-Zoom |
| **Reddit/Quora** | Hot algorithm, vote system | 30-Reddit-Quora |
| **Food Delivery** | Uber Eats-style, similar to Uber rides | 24-Food-Delivery |
| **Proximity/Yelp** | Covered by Uber if GEORADIUS known | 22-Proximity-Yelp |
| **LLM Billing** | Relevant if interviewing for AI product roles | 35-AI-ML-Billing |
| **ChatGPT-style service** | Within 34-AI-ML-Inference | 34-AI-ML-Inference |

---

## Study Plans by Timeline

### 1 Week (Sprint)
Focus only on P0. In this order:
1. Read `HLD-quick.md` fully (2 hours)
2. CAP + Caching + Consistent Hashing (from `01-Fundamentals.md`)
3. `06-Rate-Limiter.md` (always asked)
4. `02-URL-Shortener.md` (easiest full design to practise)
5. `12-Communication-Patterns.md` Section 5 (SSE/WebSocket — always asked as follow-up)
6. `18-Distributed-Cache.md` (Redis internals)
7. One mock design per day from: messaging, notification, Twitter, Uber

### 2 Weeks (Standard Prep)
Complete P0 in week 1, then P1 in week 2:
- Week 1: all P0 topics + URL Shortener, Rate Limiter, Cache, Messaging
- Week 2: YouTube, Twitter, Uber, Google Drive, Auth System + P1 concepts

### 1 Month (Deep Prep — recommended)
Week 1: P0 concepts + 5 P0 system designs
Week 2: P1 concepts + 7 P1 system designs
Week 3: P2 concepts + 4 P2 system designs most relevant to your target companies
Week 4: Mock interviews, HLD Master Q&A (50 questions), revise weak areas

### For Joy's Target (IC + EM roles at senior level)
**Must master**: P0 all + P1 all + kACE-specific designs (FX Trading, Auth JWT)
**Focus on**: Stock Exchange (fintech domain expertise), Kafka (deep usage), Recommendation (ML for EM)
**AI/ML**: At minimum P2 level — LLM Fundamentals + RAG — increasingly tested at senior roles
**EM angle**: Questions Q39–Q50 in HLD-Master-QnA.md are judgment questions EMs get asked

---

## Companies and Their Favourite Topics

| Company Type | Commonly Asked |
|--------------|----------------|
| FAANG/Big Tech | YouTube, Twitter, Uber, Google Drive, consistency models |
| Fintech (Goldman, JP Morgan, trading firms) | Stock Exchange, Payment Gateway, Rate Limiter, Auth |
| Startup (Series B+) | Whatever their core product is + distributed systems fundamentals |
| FAANG India (Flipkart, Swiggy, Meesho) | Food Delivery, Payment Gateway, Recommendation |
| AI companies (OpenAI, Anthropic, Cohere) | LLM Serving, RAG, Vector DBs, billing |
| Infra companies (Databricks, Confluent) | Kafka deep-dive, distributed systems, storage engines |
| Collaboration (Notion, Figma) | Google Docs (OT/CRDTs), real-time collab |
