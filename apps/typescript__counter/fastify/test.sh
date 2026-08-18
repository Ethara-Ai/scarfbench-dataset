#!/usr/bin/env bash
# Behavioral test for counter — payload/shape assertions
set -euo pipefail
PORT="8080"

# counter
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/counter")
[ "$STATUS" = "200" ] || { echo "FAIL_status_$STATUS"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/counter")
printf '%s' "$RESP" | grep -qE 'accessed [0-9]+ time' || { echo "FAIL_body: $RESP"; exit 1; }
V1=$(curl -sL "http://localhost:${PORT}/counter" | grep -oE '[0-9]+')
V2=$(curl -sL "http://localhost:${PORT}/counter" | grep -oE '[0-9]+')
[ "$V2" -gt "$V1" ] || { echo "FAIL_counter no_increment $V1->$V2"; exit 1; }
# text-/
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE 'OK|<html' || { echo "FAIL: / missing text: $RESP"; exit 1; }

echo PASS
