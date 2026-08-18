#!/usr/bin/env bash
# Rust behavioral test for decorators
set -euo pipefail
PORT="8080"

RESP=$(curl -sL "http://localhost:${PORT}/decorators?inputString=hello"); printf '%s' "$RESP" | grep -q 'Coded: ifmmp' || { echo "FAIL cipher-GET: $RESP"; exit 1; }
RESP=$(curl -sL -X POST -d "inputString=hello" "http://localhost:${PORT}/decorators"); printf '%s' "$RESP" | grep -q 'Coded: ifmmp' || { echo "FAIL cipher-POST: $RESP"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/"); printf '%s' "$RESP" | grep -qE 'OK|<html' || { echo "FAIL text /: $RESP"; exit 1; }

echo PASS
