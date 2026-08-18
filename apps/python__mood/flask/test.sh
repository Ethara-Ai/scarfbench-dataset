#!/usr/bin/env bash
# Behavioral test - asserts payload/shape, not just HTTP 200. App: presentation/mood
set -euo pipefail
PORT="8080"

# Check 1: mood report
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/report")
[ "$STATUS" = "200" ] || { echo "FAIL_1_status_$STATUS (mood report)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/report")
printf '%s' "$RESP" | grep -qi 'mood' && printf '%s' "$RESP" | grep -qi 'awake' || { echo "FAIL_1_body (mood report): $RESP"; exit 1; }

# Check 2: root
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/")
[ "$STATUS" = "200" ] || { echo "FAIL_2_status_$STATUS (root)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE '^(OK|<html)' || { echo "FAIL_2_body (root): $RESP"; exit 1; }

echo PASS
