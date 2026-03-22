#!/usr/bin/env bash
# Run: chmod +x run.sh && ./run.sh
set -e
SRC="src/main/java"
OUT="target/classes"
mkdir -p "$OUT"
echo "Compiling..."
find "$SRC" -name "*.java" | xargs javac --release 21 -d "$OUT"
echo "Running lld.hotel.HotelDemo..."
echo "================================================"
java -cp "$OUT" lld.hotel.HotelDemo
