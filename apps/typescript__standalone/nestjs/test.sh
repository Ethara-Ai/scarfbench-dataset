#!/usr/bin/env bash
# Behavioral test for standalone — payload/shape assertions
set -euo pipefail
PORT="8080"

# json-/standalone
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/standalone")
[ "$STATUS" = "200" ] || { echo "FAIL_status_$STATUS (/standalone)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/standalone")
printf '%s' "$RESP" | grep -q '"message"' || { echo "FAIL: /standalone missing message: $RESP"; exit 1; }
printf '%s' "$RESP" | grep -q 'Greetings!' || { echo "FAIL: /standalone missing Greetings: $RESP"; exit 1; }
# json-/
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/")
[ "$STATUS" = "200" ] || { echo "FAIL_status_$STATUS (/)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -q '"message"' || { echo "FAIL: / missing message: $RESP"; exit 1; }
printf '%s' "$RESP" | grep -q 'Greetings!' || { echo "FAIL: / missing Greetings: $RESP"; exit 1; }

echo PASS
