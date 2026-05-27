#!/usr/bin/env bash
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"

echo "creating a room through the gateway"
room=$(curl -fsS -X POST "$GATEWAY/rooms" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Cedar","capacity":2}')
room_id=$(echo "$room" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "room id: $room_id"

echo "creating a reservation through the gateway"
reservation=$(curl -fsS -X POST "$GATEWAY/reservations" \
  -H 'Content-Type: application/json' \
  -d "{\"roomId\":$room_id,\"guestName\":\"Ada\",\"checkIn\":\"2026-01-01\",\"checkOut\":\"2026-01-03\"}")
reservation_id=$(echo "$reservation" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "reservation id: $reservation_id"

echo "reading the reservation back through the gateway"
fetched=$(curl -fsS "$GATEWAY/reservations/$reservation_id")
echo "$fetched" | grep -q '"guestName":"Ada"'

echo "e2e smoke passed"
