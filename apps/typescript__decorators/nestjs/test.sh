#!/usr/bin/env bash
# Behavioral test for decorators — payload/shape assertions
set -euo pipefail
PORT="8080"

# cipher-GET-/decorators
RESP=$(curl -sL "http://localhost:${PORT}/decorators?inputString=hello")
printf '%s' "$RESP" | grep -q 'Coded: ifmmp' || { echo "FAIL cipher-GET: $RESP"; exit 1; }
# cipher-POST-/decorators
RESP=$(curl -sL -X POST -d "inputString=hello" "http://localhost:${PORT}/decorators")
printf '%s' "$RESP" | grep -q 'Coded: ifmmp' || { echo "FAIL cipher-POST: $RESP"; exit 1; }
# text-/
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE 'OK|<html' || { echo "FAIL: / missing text: $RESP"; exit 1; }

echo PASS
