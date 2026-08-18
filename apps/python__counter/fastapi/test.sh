#!/usr/bin/env bash
# Behavioral test - asserts payload/shape, not just HTTP 200. App: business_domain/counter
set -euo pipefail
PORT="8080"

# Check 1: counter hit
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/counter")
[ "$STATUS" = "200" ] || { echo "FAIL_1_status_$STATUS (counter hit)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/counter")
printf '%s' "$RESP" | grep -qE 'accessed [0-9]+ time' || { echo "FAIL_1_body (counter hit): $RESP"; exit 1; }

# Check 2: counter re-hit
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/counter")
[ "$STATUS" = "200" ] || { echo "FAIL_2_status_$STATUS (counter re-hit)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/counter")
printf '%s' "$RESP" | grep -qE 'accessed [0-9]+ time' || { echo "FAIL_2_body (counter re-hit): $RESP"; exit 1; }

# Check 3: root
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/")
[ "$STATUS" = "200" ] || { echo "FAIL_3_status_$STATUS (root)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE '^(OK|<html)' || { echo "FAIL_3_body (root): $RESP"; exit 1; }

V1=$(curl -sL "http://localhost:${PORT}/counter" | grep -oE 'accessed [0-9]+' | grep -oE '[0-9]+')
V2=$(curl -sL "http://localhost:${PORT}/counter" | grep -oE 'accessed [0-9]+' | grep -oE '[0-9]+')
[ "$V2" -gt "$V1" ] || { echo "FAIL_counter_no_increment $V1 -> $V2"; exit 1; }

echo PASS
