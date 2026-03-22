#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# start-all.sh — Start Eureka, Gateway, and all 11 LLD Spring Boot services
# ─────────────────────────────────────────────────────────────────────────────
set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$BASE_DIR/logs"
mkdir -p "$LOG_DIR"

PIDS=()

start_service() {
  local name=$1
  local dir="$BASE_DIR/$2"
  local log="$LOG_DIR/$name.log"
  echo "  Starting $name..."
  (cd "$dir" && mvn spring-boot:run -q > "$log" 2>&1) &
  PIDS+=($!)
  echo "    PID: ${PIDS[-1]} | log: logs/$name.log"
}

# Cleanup on Ctrl+C
cleanup() {
  echo ""
  echo "Stopping all services..."
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null && echo "  Stopped PID $pid"
  done
  echo "All services stopped."
  exit 0
}
trap cleanup SIGINT SIGTERM

echo "╔══════════════════════════════════════════════════════╗"
echo "║          LLD Spring Boot — Full Stack Startup        ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# Step 1: Eureka Server
echo "▶ [1/13] Starting Eureka Server..."
start_service "eureka-server" "eureka-server"
echo "  Waiting 15s for Eureka to be ready..."
sleep 15

# Step 2: API Gateway
echo "▶ [2/13] Starting API Gateway..."
start_service "api-gateway" "api-gateway"
sleep 8

# Step 3-13: All LLD services
echo "▶ [3/13] Starting Parking Lot..."
start_service "parking-lot" "01-parking-lot"
sleep 5

echo "▶ [4/13] Starting Splitwise..."
start_service "splitwise" "02-splitwise"
sleep 5

echo "▶ [5/13] Starting BookMyShow..."
start_service "bookmyshow" "03-bookmyshow"
sleep 5

echo "▶ [6/13] Starting Cab Booking..."
start_service "cab-booking" "04-cab-booking"
sleep 5

echo "▶ [7/13] Starting Library Management..."
start_service "library" "05-library-management"
sleep 5

echo "▶ [8/13] Starting ATM Machine..."
start_service "atm" "06-atm-machine"
sleep 5

echo "▶ [9/13] Starting Hotel Reservation..."
start_service "hotel" "07-hotel-reservation"
sleep 5

echo "▶ [10/13] Starting Elevator System..."
start_service "elevator" "08-elevator-system"
sleep 5

echo "▶ [11/13] Starting Snake & Ladder..."
start_service "snake-ladder" "09-snake-ladder"
sleep 5

echo "▶ [12/13] Starting Chess Game..."
start_service "chess" "10-chess-game"
sleep 5

echo "▶ [13/13] Starting Spaceship Game..."
start_service "spaceship" "11-spaceship-game"
sleep 5

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                  All services started!                          ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║  Eureka Dashboard : http://localhost:8761                       ║"
echo "║  API Gateway      : http://localhost:8080                       ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║  Swagger UIs (via Gateway):                                     ║"
echo "║    Parking Lot    : http://localhost:8080/api/parking/swagger-ui.html ║"
echo "║    Splitwise      : http://localhost:8080/api/splitwise/swagger-ui.html ║"
echo "║    BookMyShow     : http://localhost:8080/api/bookmyshow/swagger-ui.html ║"
echo "║    Cab Booking    : http://localhost:8080/api/cab/swagger-ui.html ║"
echo "║    Library        : http://localhost:8080/api/library/swagger-ui.html ║"
echo "║    ATM Machine    : http://localhost:8080/api/atm/swagger-ui.html ║"
echo "║    Hotel          : http://localhost:8080/api/hotel/swagger-ui.html ║"
echo "║    Elevator       : http://localhost:8080/api/elevator/swagger-ui.html ║"
echo "║    Snake & Ladder : http://localhost:8080/api/snake-ladder/swagger-ui.html ║"
echo "║    Chess          : http://localhost:8080/api/chess/swagger-ui.html ║"
echo "║    Spaceship      : http://localhost:8080/api/spaceship/swagger-ui.html ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║  H2 Consoles (via Gateway):                                     ║"
echo "║    Parking Lot    : http://localhost:8080/api/parking/h2-console ║"
echo "║    (others follow same pattern)                                 ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║  Logs: ./logs/<service>.log                                     ║"
echo "║  Stop : Ctrl+C                                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "Waiting... (Press Ctrl+C to stop all services)"
wait
