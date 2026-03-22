# 🎯 Interview Preparation — Complete Guide

> Built for **Jasti Giri Sai Kumar (Joy)** — Technical Manager, kACE team, BGC Partners (Fenics).
> Covers DSA, HLD, LLD with Java 21 runnable code — structured for IC (Senior/Staff) and EM roles.

---

## ⚡ Quick Start by Role

### Targeting Senior Engineer (IC)
```
Week 1:  01-quick-revision/ → DSA-quick.md + HLD-quick.md + LLD-quick.md
Week 2:  02-deep-dive/DSA/ → P0 topics (Arrays, HashMap, Trees, Graphs, DP)
Week 3:  02-deep-dive/HLD/ → P0 systems (URL Shortener, Rate Limiter, Messaging)
Week 4:  lld-solutions/ → Parking Lot, Splitwise, BookMyShow (run them!)
Night before: 00-cheatsheets/ → all three cheatsheets
```

### Targeting Staff / Principal Engineer
```
Start:   00-cheatsheets/ + 01-quick-revision/ (orientation)
Then:    02-deep-dive/HLD/ → all 35 files + 03-resources/HLD-Master-QnA.md
Then:    02-deep-dive/LLD/ → foundations + all 13 deep-dives + lld-solutions/
Focus:   03-resources/HLD-Priority-Guide.md → P2 systems (Stock Exchange, Kafka, Maps)
```

### Targeting Engineering Manager (EM)
```
Technical:   HLD deep-dives + LLD foundations (show you can still design)
Behavioral:  STAR stories using kACE work (Phoenix modernisation, team of 12, 185 interviews)
Leadership:  03-resources/HLD-Master-QnA.md → Q39-Q50 (real-world judgment questions)
Domain:      02-deep-dive/HLD/05-FX-Trading-Platform.md (your current system)
```

---

## 📁 Repository Structure

```
InterviewPreparation/
│
├── 00-cheatsheets/              ← Read the night before every interview
│   ├── DSA-cheatsheet.md        ← All 22 DSA topics, complexity table, patterns
│   ├── HLD-cheatsheet.md        ← System design components, numbers, SSE/WebSocket
│   └── LLD-cheatsheet.md        ← SOLID, patterns quick-select, Java 21 features
│
├── 01-quick-revision/           ← 20-minute morning-of review
│   ├── DSA-quick.md             ← Pattern recognition, common mistakes
│   ├── HLD-quick.md             ← CANES framework, 14 sections including AI/ML
│   └── LLD-quick.md             ← 5-step approach, patterns, thread safety rules
│
├── 02-deep-dive/
│   ├── DSA/                     ← 23 files — Arrays through Multithreaded DSA
│   │   └── (each: concept + Industry example + kACE context + problems)
│   ├── HLD/                     ← 35 files — Core concepts + Systems + AI/ML
│   │   └── (each: estimations + API + architecture + deep-dives + 5 Q&As)
│   └── LLD/                     ← 13 files — OOP/SOLID + Patterns + Machine coding
│       └── (each: design decisions + complete Java code + extension Q&A)
│
├── 03-resources/                ← Master Q&As, priority guides, platform resources
│   ├── DSA-Platforms-and-Resources.md
│   ├── DSA-Topic-Priority-Guide.md
│   ├── HLD-Master-QnA.md        ← 50 questions with full model answers
│   ├── HLD-Priority-Guide.md    ← P0/P1/P2/P3 + study plans by timeline
│   ├── LLD-Master-QnA.md        ← 30 questions with full model answers
│   └── LLD-Priority-Guide.md    ← P0/P1/P2/P3 + study plans by role
│
└── lld-solutions/               ← Runnable Java 21 code (11 machine coding problems)
    ├── README.md                ← How to compile and run
    ├── 01-parking-lot/          ← chmod +x run.sh && ./run.sh
    ├── 02-chess-game/
    ├── 03-atm-machine/
    ├── 04-elevator-system/
    ├── 05-library-management/
    ├── 06-spaceship-game/
    ├── 07-splitwise/
    ├── 08-bookmyshow/
    ├── 09-cab-booking/
    ├── 10-snake-ladder/
    └── 11-hotel-reservation/
```

---

## 📚 Content Overview

### DSA — 23 Deep-Dives, 115 Use Cases
Every file: concept → complexity → patterns → 🏭 Industry example → 🏦 kACE context → problems.

| Files | Topics |
|-------|--------|
| 00–05 | HashMaps, Arrays/Strings, Linked Lists, Stacks/Queues, Trees, Graphs |
| 06–11 | Heaps, Tries, Segment Trees, Union-Find, Sliding Window, Two Pointers |
| 12–17 | Binary Search, Sorting, Dynamic Programming, Greedy, Backtracking, Bit Manipulation |
| 18–22 | Math, Intervals, Matrix, Monotonic Stack, Multithreaded/Concurrent DSA |

---

### HLD — 35 Deep-Dives, 176 Q&As

**Core Concepts (files 01–12):** Fundamentals, Auth/JWT, Rate Limiter, Notifications, CDN/DNS, Microservices, Database, Observability, Communication Patterns + **SSE/WebSocket multi-instance scaling**

**System Designs (files 13–31):**

| File | System | Signature Pattern |
|------|--------|-------------------|
| 05 | FX Trading Platform (kACE) | WebSocket STOMP + Redis SubscriptionRegistry |
| 14 | Twitter/News Feed | Fan-out on write vs read |
| 15 | Uber | Redis GEORADIUS + driver matching |
| 20 | Kafka/PubSub | Log-based storage, ISR, consumer groups |
| 21 | Payment Gateway | Idempotency + double-entry bookkeeping |
| 26 | Stock Exchange | TreeMap order book + WAL |
| 27 | Zoom | SFU vs MCU, WebRTC, Simulcast |
| 31 | Google Maps | Contraction Hierarchies routing |

**AI/ML (files 32–35):** How LLMs work, RAG + Vector DBs, LLM Inference Serving, Real-time Billing

**Resources:** 50-question Master Q&A + Priority Guide (P0–P3 + study plans)

---

### LLD — 13 Deep-Dives + 11 Runnable Solutions

**Foundation:** OOP/SOLID (violations → fixes) + all 20 GoF patterns (Java 21 code)

**Machine Coding (each has complete Java + run.sh):**

| Problem | Key Pattern | Signature Concept |
|---------|-------------|-------------------|
| Parking Lot | Strategy, Builder | `synchronized tryOccupy()` |
| Chess Game | Each piece owns its moves | `Deque<Move>` for undo |
| ATM Machine | State + Chain of Responsibility | Rs2000→Rs100 dispenser |
| Elevator | SCAN algorithm | `TreeSet<Integer>` stop queues |
| Library Mgmt | Strategy (FineCalculator) | Reservation FIFO queue |
| Spaceship Game | Observer + Factory | Full game loop + AABB collision |
| Splitwise | Strategy (4 types) | Greedy debt simplification O(N log N) |
| BookMyShow | State + seat locking | `synchronized tryLock()` with TTL |
| Cab Booking | State machine + Haversine | RideStatus transitions validated |
| Snake & Ladder | Builder + Strategy (dice) | Configurable `GameConfig` record |
| Hotel Reservation | Decorator + Pricing strategy | Amenity stacking |

**Resources:** 30-question Master Q&A + Priority Guide (P0–P3 + company guide)

---

## 🗓️ Study Plans

### 1 Week
```
Day 1: All 3 quick-revision files
Day 2: DSA P0 (Arrays, HashMap, Trees, Graphs)
Day 3: HLD P0 (URL Shortener, Rate Limiter, Communication Patterns)
Day 4: LLD OOP/SOLID + run parking-lot + splitwise
Day 5: HLD P1 (YouTube, Twitter, Uber)
Day 6: LLD BookMyShow + Cab Booking (read + run)
Day 7: All 3 Master Q&A files + cheatsheets
```

### 2 Weeks
```
Week 1: All DSA P0/P1 + HLD core concepts (files 01-12)
Week 2: HLD system designs (P0/P1) + all LLD machine coding
```

### 1 Month (recommended)
```
Week 1: DSA complete (23 files + platforms guide)
Week 2: HLD core concepts + P0/P1 system designs
Week 3: HLD P2 + AI/ML section + LLD foundations + patterns
Week 4: All 11 LLD solutions (read + run) + Q&A files + mock interviews
```

---

## 🚀 Running LLD Solutions

Requirements: JDK 21+ only

```bash
cd lld-solutions/01-parking-lot
chmod +x run.sh && ./run.sh

# Or manually:
find src -name "*.java" | xargs javac --release 21 -d target/classes
java -cp target/classes lld.parkinglot.ParkingLotDemo
```

---

## 🔑 Key Numbers

```
Latency:       RAM 100ns | SSD 100μs | Network round-trip 100ms
Throughput:    MySQL ~5K QPS | Redis ~100K ops/sec | Kafka ~1M msg/sec
Availability:  99.9% = 8.7h downtime/yr | 99.99% = 52min | 99.999% = 5min
Token cost:    GPT-4o input $2.50/1M | output $10/1M | ~1K words = 1,300 tokens
DB scale:      Single shard Postgres: ~100K QPS reads with replicas + cache
```

---

## 📊 Stats

| Section | Files | Content |
|---------|-------|---------|
| DSA | 23 deep-dives | 115 use cases (Industry + kACE) |
| HLD | 35 deep-dives | 176 Q&As + 50 master answers |
| LLD | 13 deep-dives | 20 patterns + 11 machine coding |
| Java code | 231 .java files | 11 runnable modules verified on JDK 21 |
| Guides | 6 | Cheatsheets + Quick revision + Priority guides |
| **Total** | **~320 files** | **Complete end-to-end interview prep** |
