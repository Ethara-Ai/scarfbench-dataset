#!/usr/bin/env bash
# Behavioral test - asserts payload/shape, not just HTTP 200. App: persistence/address-book
set -euo pipefail
PORT="8080"

# Check 1: contacts returns JSON array
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/contacts")
[ "$STATUS" = "200" ] || { echo "FAIL_1_status_$STATUS (contacts returns JSON array)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/contacts")
printf '%s' "$RESP" | grep -qE '^\[.*\]$' || { echo "FAIL_1_body (contacts returns JSON array): $RESP"; exit 1; }

# Check 2: root
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/")
[ "$STATUS" = "200" ] || { echo "FAIL_2_status_$STATUS (root)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE '^(OK|<html|\[)' || { echo "FAIL_2_body (root): $RESP"; exit 1; }

echo PASS
