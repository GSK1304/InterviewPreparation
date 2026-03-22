# 🗺️ LLD Topic Priority Guide

> Use this to decide what to study first, what to skim, and what to skip based on your timeline.

---

## Priority Levels

| Level | Meaning | Study depth |
|-------|---------|------------|
| 🔴 **P0** | Asked in virtually every LLD interview | Must be able to code from scratch in 45 min |
| 🟠 **P1** | Asked in most senior engineer interviews | Understand deeply, discuss tradeoffs confidently |
| 🟡 **P2** | Asked for Staff+ or specialised roles | Know the concept, can explain the design |
| 🟢 **P3** | Nice to know | Read once — enough to mention if relevant |

---

## P0 — Master These First

### Core Concepts

| Topic | What to Know | File |
|-------|-------------|------|
| **SOLID Principles** | Each principle with a violation + fix example in Java | 01-OOP-SOLID |
| **Composition vs Inheritance** | IS-A test, when each fails, real examples | 01-OOP-SOLID |
| **Strategy Pattern** | @FunctionalInterface, inject + swap at runtime | 02-Design-Patterns |
| **Builder Pattern** | Inner static Builder, validation in build(), immutable result | 02-Design-Patterns |
| **State Pattern** | Each state is a class, transitions validated, context delegates | 02-Design-Patterns |
| **Observer Pattern** | EventBus<T> with CopyOnWriteArrayList, error isolation | 02-Design-Patterns |
| **Thread safety basics** | synchronized, ConcurrentHashMap, AtomicInteger — when to use each | LLD-cheatsheet |
| **Money in paise** | Never use double; record Money(long paise); remainder distribution | LLD-cheatsheet |
| **Custom exceptions** | Domain-specific, context in message, hierarchy | LLD-cheatsheet |
| **Validation in constructor** | requireNonNull, blank check, range check, format check | LLD-cheatsheet |

### Machine Coding Problems

| Problem | Why P0 | File |
|---------|--------|------|
| **Parking Lot** | Classic starter; tests all OOP + Strategy + thread safety | 03-Parking-Lot |
| **Splitwise** | Split strategy + debt simplification algorithm — very common | 09-Splitwise |
| **Snake & Ladder** | Tests Builder, Strategy (dice), sealed types — simple to code fast | 12-Snake-Ladder |

---

## P1 — Essential for Senior Engineer Roles

### Core Concepts

| Topic | What to Know | File |
|-------|-------------|------|
| **Decorator Pattern** | Wraps same interface, stackable, vs subclassing | 02-Design-Patterns |
| **Factory Method** | Abstract creator, subclass decides type | 02-Design-Patterns |
| **Chain of Responsibility** | Handler chain, each handles or passes | 02-Design-Patterns |
| **Command Pattern** | Encapsulate request, undo/redo stacks | 02-Design-Patterns |
| **Abstract Factory** | Family of related objects, whole factory swapped | 02-Design-Patterns |
| **Java 21 Records** | Compact constructors with validation, equals/hashCode | LLD-cheatsheet |
| **Java 21 Sealed classes** | Exhaustive switch, permits clause | LLD-cheatsheet |
| **OOP 4 Pillars** | Violation examples for each, Tell Don't Ask | 01-OOP-SOLID |
| **UML class diagrams** | Draw composition vs aggregation vs dependency | 01-OOP-SOLID |

### Machine Coding Problems

| Problem | Why P1 | File |
|---------|--------|------|
| **Chess Game** | Piece hierarchy, move validation, move history | 04-Chess-Game |
| **ATM Machine** | State pattern, Chain of Responsibility for cash | 05-ATM-Machine |
| **BookMyShow** | Seat locking with TTL, seat selection strategies | 10-BookMyShow |
| **Cab Booking** | Full state machine + fare strategy + matching | 11-Cab-Booking |
| **Library Management** | Strategy (fine), reservation queue, fine blocking | 07-Library-Management |

---

## P2 — Important for Staff+ / Specialised Roles

### Core Concepts

| Topic | What to Know | File |
|-------|-------------|------|
| **Prototype Pattern** | Deep clone, registry, use case (game enemies) | 02-Design-Patterns |
| **Flyweight Pattern** | Intrinsic vs extrinsic state, memory savings | 02-Design-Patterns |
| **Composite Pattern** | Leaf + Composite same interface, recursive operations | 02-Design-Patterns |
| **Proxy Pattern** | Types (virtual, protection, caching), Spring AOP | 02-Design-Patterns |
| **Bridge Pattern** | Two independent hierarchies | 02-Design-Patterns |
| **Template Method** | Final skeleton, abstract steps, hooks | 02-Design-Patterns |
| **Mediator Pattern** | Reduce coupling between many objects | 02-Design-Patterns |
| **Iterator Pattern** | Custom collection traversal | 02-Design-Patterns |

### Machine Coding Problems

| Problem | Why P2 | File |
|---------|--------|------|
| **Elevator System** | SCAN algorithm (TreeSet), dispatch strategy | 06-Elevator-System |
| **Hotel Reservation** | Decorator + date range overlap + pricing strategy | 13-Hotel-Reservation |
| **Spaceship Game** | Game loop, sealed events, Observer, Factory | 08-Spaceship-Game |

---

## P3 — Nice to Know

| Topic | Notes |
|-------|-------|
| Visitor pattern | AST traversal, rarely asked in interviews |
| Memento pattern | Undo systems, know the concept |
| Interpreter pattern | Rarely asked unless you're interviewing at a compiler company |
| Abstract Factory deep dive | Often confused with Factory Method — just know the difference |

---

## Study Plans by Timeline

### 1 Week (Sprint Prep)
Day 1: `LLD-quick.md` fully + `01-OOP-SOLID-Foundations.md`
Day 2: Design Patterns — Strategy, Builder, State, Observer (P0 patterns only from `02-Design-Patterns-Reference.md`)
Day 3: Code Parking Lot from scratch (no peeking), then check against `03-Parking-Lot.md`
Day 4: Code Splitwise from scratch, check against `09-Splitwise.md`
Day 5: Code Snake & Ladder from scratch, check against `12-Snake-Ladder.md`
Day 6: Read ATM + BookMyShow deep-dives. Study LLD-Master-QnA Q1–Q15.
Day 7: Mock coding — pick one problem, 45 min timer. Read Q16–Q30.

### 2 Weeks (Standard Prep)
Week 1: P0 concepts + P0 machine coding (Parking Lot, Splitwise, Snake & Ladder) + P1 patterns
Week 2: P1 machine coding (Chess, ATM, BookMyShow, Cab Booking, Library) + all Q&As

### 1 Month (Deep Prep — recommended)
Week 1: Foundations (OOP, SOLID, all patterns) — read + code examples
Week 2: P0 + P1 machine coding — code each from scratch, no reference
Week 3: P2 machine coding + mock interviews with timer
Week 4: Q&A review, weakness areas, speed drills on patterns

### For Joy's Target (IC + EM roles at Senior/Staff level)
**Must master**: All P0 + P1 machine coding + SOLID deeply
**Focus extra on**: BookMyShow (seat locking — classic concurrency question), Chess (OOP purity), Splitwise (algorithm — greedy debt simplification)
**EM angle**: Q&A questions 7, 9, 10, 29, 30 — these are design judgment questions EMs get
**Java 21 specifically**: Records, sealed classes, pattern matching switch — use them naturally
**kACE relevance**: State machines (Ride state ↔ RFQ state), Strategy (FareCalc ↔ PricingStrategy), Observer (EventBus ↔ Kafka events)

---

## What Interviewers at Different Companies Typically Ask

| Company Type | Typically Asked |
|--------------|----------------|
| Product companies (Flipkart, Swiggy, Meesho) | Parking Lot, Splitwise, BookMyShow, Cab Booking |
| Fintech (Zerodha, Groww, PhonePe, Razorpay) | ATM, Splitwise, Payment system, Trading system |
| Gaming (mobile, online) | Chess, Snake & Ladder, Spaceship/any game |
| Infra/Platform (Atlassian, Confluent) | Logger system, Rate limiter, Event system |
| FAANG India (Amazon, Google, Microsoft) | Any of the above + expect OOP + SOLID discussion |
| Startups (Series A-C) | Usually one problem + design discussion — focus on code quality |

---

## The 10 Signs Your LLD Is Interview-Ready

```
✅ All fields are private with validation in constructors
✅ Custom domain exceptions with context in messages
✅ Money stored as long (paise/cents), not double
✅ Design patterns are named and justified
✅ Composition used instead of inheritance where IS-A test fails
✅ Thread safety applied where multiple threads share mutable state
✅ State transitions validated with precondition checks
✅ Java 21 features used naturally (records, sealed, switch expressions)
✅ A working main() demonstrates end-to-end flow
✅ When asked "how do you add X?", you show OCP — new class, no changes to existing
```
