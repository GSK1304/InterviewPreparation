# LLD Solutions — Runnable Java 21

> 11 complete machine coding solutions. Each compiles and runs with a single command.

## Quick Start

**Requirements:** JDK 21+

```bash
cd lld-solutions/01-parking-lot
chmod +x run.sh && ./run.sh
```

## Modules

| # | Module | Key Patterns | Signature Concept |
|---|--------|--------------|-------------------|
| 01 | `01-parking-lot` | Strategy, Builder | `synchronized tryOccupy()` — thread-safe spot assignment |
| 02 | `02-chess-game` | Command (move history) | Each piece validates its own moves (OCP) |
| 03 | `03-atm-machine` | State, Chain of Responsibility | Rs2000→Rs500→Rs200→Rs100 dispenser chain |
| 04 | `04-elevator-system` | Strategy (SCAN/Nearest), Builder | `TreeSet<Integer>` sorted stop queues for SCAN algorithm |
| 05 | `05-library-management` | Strategy (FineCalculator) | Reservation FIFO queue with notification |
| 06 | `06-spaceship-game` | Observer (EventBus), Strategy (Movement), Factory | Game loop with AABB collision detection |
| 07 | `07-splitwise` | Strategy (4 split types) | Greedy O(N log N) debt simplification |
| 08 | `08-bookmyshow` | Strategy (SeatSelection), State (Booking) | `synchronized tryLock()` with TTL expiry |
| 09 | `09-cab-booking` | State (Ride), Strategy (Fare+Match) | Haversine distance, state machine validation |
| 10 | `10-snake-ladder` | Strategy (Dice), Builder (Board) | Configurable rules via GameConfig record |
| 11 | `11-hotel-reservation` | Decorator (Amenities), Strategy (Pricing) | Stacked amenity decorators (Breakfast+Parking+Spa) |

## How to Run

### Option 1: Shell script (recommended — zero dependencies)
```bash
cd 01-parking-lot
chmod +x run.sh
./run.sh
```

### Option 2: Maven (when internet/cache available)
```bash
# Single module
mvn compile exec:java -pl 01-parking-lot

# Verify all compile
mvn compile
```

### Option 3: Manual javac
```bash
cd 01-parking-lot
mkdir -p target/classes
find src -name "*.java" | xargs javac --release 21 -d target/classes
java -cp target/classes lld.parkinglot.ParkingLotDemo
```

## Java 21 Features Used

| Feature | Where |
|---------|-------|
| Records | Money, Vehicle, Position, Location, DateRange, GameConfig, Rating |
| Switch expressions | MemberType borrow limits, applyPowerUp(), VehicleType.fitsIn() |
| Pattern matching instanceof | SpaceShooterGame collision streams |
| Optional | findAvailableCopy(), shoot(), strategy returns |
| ConcurrentHashMap | ParkingLot, CabBookingService, BookingService |
| CopyOnWriteArrayList | GameEventBus listener list |
| AtomicInteger | Thread-safe ID counters |

## All Design Patterns

```
Strategy:      SpotSelection, FareCalc, DriverMatch, SplitType (4),
               SeatSelection, FineCalc, Dice, Movement, Pricing, Dispatch
Builder:       ParkingLot, Building, Board, SnakeLadderGame
Decorator:     BreakfastIncluded, ParkingIncluded, SpaAccess (Hotel)
               SurgeFareStrategy wraps StandardFareStrategy (Cab)
State:         ATMMachine, Ride, Booking, Reservation
Observer:      GameEventBus (Spaceship), CabBookingService listeners
Chain of Resp: CashDispenser Rs2000->Rs500->Rs200->Rs100 (ATM)
Command:       Move record with piece + capturedPiece (Chess history)
Factory:       EnemyFactory (level-scaled health/speed/drops)
```
