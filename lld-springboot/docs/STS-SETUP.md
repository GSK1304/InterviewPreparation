# 🟢 Running LLD Spring Boot in Spring Tool Suite (STS)

> STS 4.x — the recommended IDE for Spring Boot projects.
> This guide covers: importing, launch order, Boot Dashboard, and tips.

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| STS | 4.20+ | Download from [spring.io/tools](https://spring.io/tools) |
| JDK | 21+ | Must be registered in STS |
| Maven | Bundled in STS | No separate install needed |

### Repoint JDK to 21 in STS

STS ships with its own embedded JRE and often defaults to an older version. You need to point it to your installed JDK 21 in **three places**.

---

#### 1. Find your JDK 21 home

**Windows** — common locations:
```
C:\Program Files\Java\jdk-21
C:\Program Files\Eclipse Adoptium\jdk-21.x.x.x-hotspot
C:\Users\<you>\.jdks\openjdk-21
```

**Mac**:
```bash
/usr/libexec/java_home -v 21
# prints something like: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
```

**Linux**:
```bash
update-java-alternatives -l
# or: ls /usr/lib/jvm/
```

---

#### 2. Register JDK 21 as an Installed JRE

```
Window → Preferences → Java → Installed JREs
```

You will see STS's bundled JRE listed (e.g. `jre-17` or similar).

```
→ Click Add
→ Select: Standard VM → Next
→ JRE home: Browse → navigate to your JDK 21 folder
  (select the root folder, e.g. C:\Program Files\Java\jdk-21)
→ STS auto-fills the JRE name as "jdk-21"
→ Finish
→ Check the checkbox next to jdk-21 to make it the default
→ Apply and Close
```

> **Important:** Select the JDK root folder (contains `bin/`, `lib/`), not the `bin/` subfolder.

---

#### 3. Set Compiler Compliance Level to 21

```
Window → Preferences → Java → Compiler
  → Compiler compliance level → 21
  → Apply and Close
```

---

#### 4. Repoint the Execution Environment

This ensures Maven uses JDK 21, not a bundled JRE.

```
Window → Preferences → Java → Installed JREs → Execution Environments
  → Select: JavaSE-21
  → On the right, check the box next to your jdk-21 entry
  → Apply and Close
```

---

#### 5. Update Maven projects to pick up the new JDK

After changing the JRE, force Maven to re-resolve:

```
Right-click any project in Package Explorer
  → Maven → Update Project
  → Check: Select All
  → Check: Force Update of Snapshots/Releases
  → OK
```

---

#### Verify it worked

```
Window → Preferences → Java → Installed JREs
  → The checked (default) entry should show jdk-21

Help → About STS → Installation Details → Configuration tab
  → Search for "java.version" → should show 21.x.x
```

Or open a Terminal in STS (`Terminal → Open Terminal`):
```bash
java -version
# should print: openjdk version "21.x.x"
```

---

## Step 1 — Import the Project

STS works best with Maven multi-module projects imported as a single root.

```
File → Import → Maven → Existing Maven Projects
  → Root Directory: browse to lld-springboot/
  → Select All (you should see 13 pom.xml entries):
      lld-springboot/pom.xml                    ← parent
      lld-springboot/eureka-server/pom.xml
      lld-springboot/api-gateway/pom.xml
      lld-springboot/01-parking-lot/pom.xml
      lld-springboot/02-splitwise/pom.xml
      lld-springboot/03-bookmyshow/pom.xml
      lld-springboot/04-cab-booking/pom.xml
      lld-springboot/05-library-management/pom.xml
      lld-springboot/06-atm-machine/pom.xml
      lld-springboot/07-hotel-reservation/pom.xml
      lld-springboot/08-elevator-system/pom.xml
      lld-springboot/09-snake-ladder/pom.xml
      lld-springboot/10-chess-game/pom.xml
      lld-springboot/11-spaceship-game/pom.xml
  → Finish
```

STS will download all dependencies (first time ~3–5 minutes).

---

## Step 2 — Open Boot Dashboard

The Boot Dashboard is the control panel for all your Spring Boot apps.

```
Window → Show View → Other → Spring → Boot Dashboard
```

Or use the toolbar shortcut: the Spring leaf icon in the top bar.

You should see all 13 modules listed under **local**:

```
local
  ├── api-gateway
  ├── atm-machine
  ├── bookmyshow
  ├── cab-booking
  ├── chess-game
  ├── elevator-system
  ├── eureka-server
  ├── hotel-reservation
  ├── library-management
  ├── parking-lot
  ├── snake-ladder
  ├── spaceship-game
  └── splitwise
```

---

## Step 3 — Start Order (CRITICAL)

**Eureka must start before everything else.** Gateway must start before you call any API. Services can start in any order after that.

### Mandatory order:

```
1. eureka-server      ← wait until port 8761 is open
2. api-gateway        ← wait until port 8080 is open
3. Everything else    ← order doesn't matter
```

### How to start a service in Boot Dashboard:

```
Right-click the service → Start
```

Or select it and click the green ▶ play button in the Boot Dashboard toolbar.

---

## Step 4 — Starting Services One by One

### 4.1 Start eureka-server

Right-click `eureka-server` → **Start**

Watch the Console tab. Wait for:
```
Started EurekaServerApplication in X.XXX seconds
Tomcat started on port(s): 8761
```

Verify: open `http://localhost:8761` — you should see the Eureka dashboard with no services registered yet.

---

### 4.2 Start api-gateway

Right-click `api-gateway` → **Start**

Wait for:
```
Started ApiGatewayApplication in X.XXX seconds
Netty started on port 8080
```

> **Note:** Gateway uses Netty (reactive), not Tomcat. This is expected.

---

### 4.3 Start remaining services

Right-click each service → **Start**

You can start all of them at once:
- Hold `Ctrl` and click all 11 services in Boot Dashboard
- Right-click the selection → **Start**

> STS will start them in parallel. Since they use `port: 0`, there are no port conflicts.

Watch the Eureka dashboard at `http://localhost:8761` — services appear as they register:
```
PARKING-LOT     n/a (1) (1)    UP (1) - 192.168.1.x:PARKING-LOT:xxxxx
SPLITWISE       n/a (1) (1)    UP (1) - 192.168.1.x:SPLITWISE:xxxxx
...
```

---

## Step 5 — Verify Everything is Up

Once all services are running, check:

| URL | Expected |
|-----|----------|
| `http://localhost:8761` | Eureka dashboard showing all 11 services as UP |
| `http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| `http://localhost:8080/api/parking/v1/parking/availability` | JSON with floor availability |
| `http://localhost:8080/api/splitwise/v1/splitwise/balances/U1` | Balance for seeded user U1 |

---

## Step 6 — Viewing Logs Per Service

Each running service has its own console in STS.

In Boot Dashboard:
```
Click any running service → the Console view switches to that service's log
```

Or in the Console tab:
```
Click the small dropdown arrow (▼) next to the console icon → pick any service
```

You will see structured logs like:
```
[PARKING-LOT] [ParkingController  ] POST /park | plate=KA01HB1234 type=CAR
[PARKING-LOT] [ParkingService     ] Park request | plate=KA01HB1234 strategy=NEAREST_FLOOR
[PARKING-LOT] [NearestFloorStrategy] Found spot G-C-01 on floor 0
[PARKING-LOT] [ParkingService     ] Ticket issued | ticketId=TKT-3F2A1B4C spot=G-C-01
```

---

## Step 7 — Stopping Services

### Stop one service:
```
Right-click service in Boot Dashboard → Stop
```

### Stop all:
```
Select all in Boot Dashboard (Ctrl+A) → Right-click → Stop
```

### Restart a service (e.g. after code change):
```
Right-click → Restart
```

> Restarting a service does **not** affect others. H2 data resets (fresh in-memory DB), Flyway re-runs migrations, re-registers with Eureka automatically.

---

## Step 8 — Accessing Swagger UI

Every service exposes Swagger UI. Access them all **through the Gateway** at port 8080:

| Service | Swagger UI URL |
|---------|---------------|
| Parking Lot | http://localhost:8080/api/parking/swagger-ui.html |
| Splitwise | http://localhost:8080/api/splitwise/swagger-ui.html |
| BookMyShow | http://localhost:8080/api/bookmyshow/swagger-ui.html |
| Cab Booking | http://localhost:8080/api/cab/swagger-ui.html |
| Library | http://localhost:8080/api/library/swagger-ui.html |
| ATM Machine | http://localhost:8080/api/atm/swagger-ui.html |
| Hotel | http://localhost:8080/api/hotel/swagger-ui.html |
| Elevator | http://localhost:8080/api/elevator/swagger-ui.html |
| Snake & Ladder | http://localhost:8080/api/snake-ladder/swagger-ui.html |
| Chess | http://localhost:8080/api/chess/swagger-ui.html |
| Spaceship | http://localhost:8080/api/spaceship/swagger-ui.html |

> Swagger UI lets you try all endpoints directly in the browser — no curl needed.

---

## Step 9 — H2 Console (inspect DB)

Each service has an H2 in-memory DB. Access via Gateway:

```
http://localhost:8080/api/parking/h2-console
```

Connection settings (same for all):
```
JDBC URL:  jdbc:h2:mem:parkingdb   (replace with service's dbName)
Username:  sa
Password:  (leave blank)
```

DB names per service:
| Service | JDBC URL |
|---------|----------|
| parking-lot | `jdbc:h2:mem:parkingdb` |
| splitwise | `jdbc:h2:mem:spliwisedb` |
| bookmyshow | `jdbc:h2:mem:bmsdb` |
| cab-booking | `jdbc:h2:mem:cabdb` |
| library | `jdbc:h2:mem:librarydb` |
| atm | `jdbc:h2:mem:atmdb` |
| hotel | `jdbc:h2:mem:hoteldb` |
| elevator | `jdbc:h2:mem:elevatordb` |
| snake-ladder | `jdbc:h2:mem:snakedb` |
| chess | `jdbc:h2:mem:chessdb` |
| spaceship | `jdbc:h2:mem:spacedb` |

---

## Step 10 — Running a Single Service (without Eureka/Gateway)

If you only want to test one service independently (e.g. just Parking Lot):

**Option A: Direct port access**

Add a temporary property override in STS:
```
Right-click service → Run As → Run Configurations
  → Spring Boot App → parking-lot
  → Arguments tab → VM Arguments:
      -Dserver.port=8081
      -Deureka.client.enabled=false
  → Run
```

Then hit it directly: `http://localhost:8081/v1/parking/availability`

**Option B: Override in application.yml temporarily**
```yaml
# Temporarily for standalone testing
server:
  port: 8081
eureka:
  client:
    enabled: false
```
> Don't commit this — revert after testing.

---

## Common Issues

### Issue: "Could not resolve placeholder 'random.value'"
**Cause:** Spring Cloud not on classpath.
**Fix:** Right-click project → Maven → Update Project (`Alt+F5`)

---

### Issue: Service shows as DOWN in Eureka
**Cause:** Service started before Eureka was fully ready.
**Fix:** Restart the service — it will re-register.

---

### Issue: Gateway returns 503 Service Unavailable
**Cause:** The target service is not registered in Eureka yet.
**Fix:** Wait 10-15 seconds after service starts for Eureka registration to propagate. Check `http://localhost:8761` to confirm the service shows as UP.

---

### Issue: Port 8761 or 8080 already in use
**Fix:**
```
# Find what's using the port (Windows)
netstat -ano | findstr :8761
taskkill /PID <pid> /F

# Find what's using the port (Mac/Linux)
lsof -ti:8761 | xargs kill -9
```

---

### Issue: H2 console gives "Database not found"
**Cause:** H2 URL doesn't match the one configured in `application.yml`.
**Fix:** Use the exact JDBC URL from the table above. The DB only exists while the service is running.

---

### Issue: Maven build errors on import (red exclamation marks)
**Fix sequence:**
```
1. Right-click parent project → Maven → Update Project → Select All → OK
2. Project → Clean → Clean All Projects
3. If still failing: close STS, delete .m2/repository/lld folder, reopen
```

---

## Quick Reference — Boot Dashboard Actions

| Action | How |
|--------|-----|
| Start one service | Right-click → Start |
| Start all | Ctrl+A → Right-click → Start |
| Stop one | Right-click → Stop |
| Stop all | Ctrl+A → Right-click → Stop |
| Restart | Right-click → Restart |
| View logs | Click service → Console switches |
| Open in browser | Right-click → Open Home Page |
| See actual port | Hover over running service — shows `[port: XXXXX]` |

---

## Recommended STS Workspace Layout

```
┌─────────────────────┬──────────────────────────────┐
│  Package Explorer   │      Editor Area              │
│  (left)             │                               │
│  ┌───────────────┐  │                               │
│  │ lld-springboot│  │                               │
│  │  ├ eureka     │  ├──────────────────────────────┤
│  │  ├ gateway    │  │   Console (bottom)            │
│  │  ├ 01-parking │  │   [shows selected service log]│
│  │  └ ...        │  │                               │
│  └───────────────┘  └──────────────────────────────┘
│  Boot Dashboard     │
│  (below explorer)   │
│  local              │
│    ● eureka-server  │
│    ● api-gateway    │
│    ○ parking-lot    │
│    ...              │
└─────────────────────┘
```

Green dot (●) = running | Grey dot (○) = stopped
