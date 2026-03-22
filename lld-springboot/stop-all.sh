#!/bin/bash
echo "Stopping all LLD Spring Boot services..."
pkill -f "spring-boot:run" && echo "All Maven processes stopped." || echo "No processes found."
