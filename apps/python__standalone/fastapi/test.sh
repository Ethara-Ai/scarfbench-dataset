#!/usr/bin/env bash
# Behavioral test - asserts payload/shape, not just HTTP 200. App: business_domain/standalone
set -euo pipefail
PORT="8080"

# Check 1: JSON message
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/standalone")
[ "$STATUS" = "200" ] || { echo "FAIL_1_status_$STATUS (JSON message)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/standalone")
printf '%s' "$RESP" | grep -q '"message"' && printf '%s' "$RESP" | grep -q 'Greetings!' || { echo "FAIL_1_body (JSON message): $RESP"; exit 1; }

# Check 2: root JSON
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/")
[ "$STATUS" = "200" ] || { echo "FAIL_2_status_$STATUS (root JSON)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -q '"message"' && printf '%s' "$RESP" | grep -q 'Greetings!' || { echo "FAIL_2_body (root JSON): $RESP"; exit 1; }

echo PASS
