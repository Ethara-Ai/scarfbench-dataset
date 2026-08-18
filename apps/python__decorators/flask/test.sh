#!/usr/bin/env bash
# Behavioral test - asserts payload/shape, not just HTTP 200. App: dependency_injection/decorators
set -euo pipefail
PORT="8080"

# Check 1: shift cipher hello->ifmmp
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/decorators?inputString=hello")
[ "$STATUS" = "200" ] || { echo "FAIL_1_status_$STATUS (shift cipher hello->ifmmp)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/decorators?inputString=hello")
printf '%s' "$RESP" | grep -q 'Coded' && printf '%s' "$RESP" | grep -q 'ifmmp' || { echo "FAIL_1_body (shift cipher hello->ifmmp): $RESP"; exit 1; }

# Check 2: root
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/")
[ "$STATUS" = "200" ] || { echo "FAIL_2_status_$STATUS (root)"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE '^(OK|<html)' || { echo "FAIL_2_body (root): $RESP"; exit 1; }

echo PASS
