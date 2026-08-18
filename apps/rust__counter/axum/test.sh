#!/usr/bin/env bash
# Rust behavioral test for counter
set -euo pipefail
PORT="8080"

RESP=$(curl -sL "http://localhost:${PORT}/counter"); printf '%s' "$RESP" | grep -qE 'accessed [0-9]+ time' || { echo "FAIL: $RESP"; exit 1; }
V1=$(curl -sL "http://localhost:${PORT}/counter" | grep -oE '[0-9]+' | head -1)
V2=$(curl -sL "http://localhost:${PORT}/counter" | grep -oE '[0-9]+' | head -1)
[ "$V2" -gt "$V1" ] || { echo "FAIL counter no_inc"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/"); printf '%s' "$RESP" | grep -qE 'OK|<html' || { echo "FAIL text /: $RESP"; exit 1; }

echo PASS
