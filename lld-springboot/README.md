# 🚀 LLD Spring Boot — Microservices with Eureka + Gateway

11 LLD problems built as independent Spring Boot 3.x microservices — each with REST endpoints, H2 + Flyway, Bean Validation, Swagger UI, and structured logs. All routed through a single API Gateway via Eureka service discovery.

---

## Architecture

```
                     ┌─────────────────────────┐
                     │     Eureka Server        │
                     │     :8761                │
                     │  (Service Registry)      │
                     └────────────┬────────────┘
                                  │ register
          ┌───────────────────────▼──────────────────────┐
          │           API Gateway :8080                   │
          │  /api/parking/**  → lb://PARKING-LOT          │
          │  /api/splitwise/**→ lb://SPLITWISE            │
          │  ...all 11 services                           │
          └──────────┬───────────┬──────────┬────────────┘
                     │           │          │
          ┌──────────▼──┐  ┌─────▼────┐  ┌─▼──────────┐
          │ Parking Lot │  │Splitwise │  │ BookMyShow │  ...
          │  :random    │  │ :random  │  │  :random   │
          │  H2+Flyway  │  │ H2+Flyway│  │  H2+Flyway │
          └─────────────┘  └──────────┘  └────────────┘
```

Services use `server.port=0` — dynamic random ports. Eureka tracks their actual address. Gateway routes by service name. **You only ever need port 8080.**

---

## Quick Start

### Prerequisites
- JDK 21+
- Maven 3.8+

### Start Everything
```bash
cd lld-springboot
chmod +x start-all.sh
./start-all.sh
```

### Stop Everything
```bash
./stop-all.sh
# or Ctrl+C in the start-all.sh terminal
```

---

## Access Points

| URL | What |
|-----|------|
| `http://localhost:8761` | Eureka Dashboard — see all registered services and their ports |
| `http://localhost:8080` | API Gateway — single entry point for all services |
| `http://localhost:8080/api/{service}/swagger-ui.html` | Swagger UI per service |
| `http://localhost:8080/api/{service}/h2-console` | H2 DB console per service |
| `http://localhost:8080/actuator/gateway/routes` | All gateway routes |

---

## Services

| # | Service | Gateway Path | Key Design Pattern | Interesting Endpoint |
|---|---------|-------------|-------------------|---------------------|
| 1 | Parking Lot | `/api/parking/**` | Strategy (NearestFloor / LoadBalanced) | `POST /v1/parking/park` |
| 2 | Splitwise | `/api/splitwise/**` | Strategy (EQUAL/EXACT/PERCENT/SHARE) | `GET /v1/splitwise/settlements/simplified` |
| 3 | BookMyShow | `/api/bookmyshow/**` | State + Seat TTL lock (5 min) | `POST /v1/bookings/lock` |
| 4 | Cab Booking | `/api/cab/**` | State machine + Haversine matching | `POST /v1/cab/rides` |
| 5 | Library Mgmt | `/api/library/**` | Strategy (FineCalculator) + FIFO queue | `POST /v1/library/borrow` |
| 6 | ATM Machine | `/api/atm/**` | Chain of Responsibility (cash dispenser) | `POST /v1/atm/accounts/{id}/withdraw` |
| 7 | Hotel | `/api/hotel/**` | Decorator (amenities) + weekend pricing | `POST /v1/hotel/reservations` |
| 8 | Elevator | `/api/elevator/**` | SCAN algorithm + @Scheduled simulation | `POST /v1/elevator/call` |
| 9 | Snake & Ladder | `/api/snake-ladder/**` | Builder (board) + Strategy (dice) | `POST /v1/snake-ladder/games/{id}/turn` |
| 10 | Chess | `/api/chess/**` | Piece hierarchy + check/checkmate | `POST /v1/chess/games/{id}/moves` |
| 11 | Spaceship | `/api/spaceship/**` | Observer (events) + AABB collision | `POST /v1/spaceship/games/{id}/move` |

---

## Per-Service Standards

Every service follows this exact structure:

```
src/main/java/lld/{service}/
├── {Service}Application.java     @SpringBootApplication @EnableDiscoveryClient
├── enums/                        Domain enums
├── entity/                       @Entity JPA classes
├── dto/                          Request/Response DTOs with Bean Validation
├── repository/                   JpaRepository with custom @Query
├── service/                      Business logic with SLF4J structured logs
├── controller/                   @RestController with @Valid, Swagger @Operation
├── exception/                    Domain exception + @RestControllerAdvice
└── validation/                   Custom @Constraint validators (where applicable)

src/main/resources/
├── application.yml               port:0, Eureka, H2, Flyway, SpringDoc
└── db/migration/
    ├── V1__create_schema.sql     DDL — Flyway managed
    └── V2__seed_data.sql         DML — realistic test data
```

---

## Sample Flows via Gateway

### Parking Lot
```bash
# Park a car
curl -X POST http://localhost:8080/api/parking/v1/parking/park \
  -H "Content-Type: application/json" \
  -d '{"licensePlate":"KA01HB1234","vehicleType":"CAR","strategy":"NEAREST_FLOOR"}'

# Check availability
curl http://localhost:8080/api/parking/v1/parking/availability

# Unpark
curl -X POST http://localhost:8080/api/parking/v1/parking/unpark \
  -H "Content-Type: application/json" \
  -d '{"ticketId":"TKT-XXXXXXXX"}'
```

### Splitwise
```bash
# Add EQUAL expense among seeded users U1-U4
curl -X POST http://localhost:8080/api/splitwise/v1/splitwise/expenses \
  -H "Content-Type: application/json" \
  -d '{"description":"Dinner","amountRupees":800,"paidByUserId":"U1","participantIds":["U1","U2","U3","U4"],"splitType":"EQUAL","category":"FOOD"}'

# Get balances
curl http://localhost:8080/api/splitwise/v1/splitwise/balances/U2

# Simplified settlements (minimum transactions)
curl http://localhost:8080/api/splitwise/v1/splitwise/settlements/simplified
```

### BookMyShow
```bash
# Check availability for Show 1 (Kalki 2898 AD)
curl http://localhost:8080/api/bookmyshow/v1/bookings/shows/1/availability

# Lock seats (5-min TTL)
curl -X POST http://localhost:8080/api/bookmyshow/v1/bookings/lock \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","showId":1,"seatCount":2,"strategy":"BEST_AVAILABLE"}'

# Confirm booking (must call within 5 minutes)
curl -X POST http://localhost:8080/api/bookmyshow/v1/bookings/BKG-XXXXXXXX/confirm
```

### ATM Machine
```bash
# Step 1: Insert card (ACC001 has Rs.50,000 balance, PIN=1234)
curl -X POST http://localhost:8080/api/atm/v1/atm/card/validate \
  -d '{"cardNumber":"1234567890123456"}' -H "Content-Type: application/json"

# Step 2: Verify PIN
curl -X POST http://localhost:8080/api/atm/v1/atm/accounts/ACC001/pin/verify \
  -d '{"pin":"1234"}' -H "Content-Type: application/json"

# Step 3: Withdraw (Rs.2000 → Rs.500 → Rs.200 → Rs.100 chain)
curl -X POST http://localhost:8080/api/atm/v1/atm/accounts/ACC001/withdraw \
  -d '{"amountRupees":2800}' -H "Content-Type: application/json"
```

### Chess
```bash
# Create game
curl -X POST http://localhost:8080/api/chess/v1/chess/games \
  -d '{"whitePlayer":"Alice","blackPlayer":"Bob"}' -H "Content-Type: application/json"

# Move pawn: e2→e4 (col=4,row=1 → col=4,row=3)
curl -X POST http://localhost:8080/api/chess/v1/chess/games/1/moves \
  -d '{"playerName":"Alice","fromCol":4,"fromRow":1,"toCol":4,"toRow":3}' \
  -H "Content-Type: application/json"
```

---

## What You'll See in Logs

```
[GATEWAY]    --> POST /api/parking/v1/parking/park routed to service
[ParkingController] POST /park | plate=KA01HB1234 type=CAR
[ParkingService] Park request | plate=KA01HB1234 type=CAR strategy=NEAREST_FLOOR
[NearestFloor] Looking for CAR spot among sizes: [COMPACT, LARGE]
[NearestFloor] Found spot G-C-01 on floor 0
[ParkingService] Ticket issued | ticketId=TKT-3F2A1B4C plate=KA01HB1234 spot=G-C-01 floor=0
[GATEWAY]    <-- POST /api/parking/v1/parking/park | status=201 | 48ms
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Service Registry | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway (reactive) |
| Framework | Spring Boot 3.2, Java 21 |
| Database | H2 in-memory per service |
| Migrations | Flyway V1 + V2 per service |
| Validation | Jakarta Bean Validation 3.0 |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Logging | SLF4J + Logback structured |
| Monitoring | Spring Boot Actuator |
