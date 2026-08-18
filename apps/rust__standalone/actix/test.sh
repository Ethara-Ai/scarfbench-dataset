#!/usr/bin/env bash
# Rust behavioral test for standalone
set -euo pipefail
PORT="8080"

RESP=$(curl -sL "http://localhost:${PORT}/standalone"); printf '%s' "$RESP" | grep -q '"message"' || { echo "FAIL json msg: $RESP"; exit 1; }; printf '%s' "$RESP" | grep -q 'Greetings!' || { echo "FAIL json Greetings: $RESP"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/"); printf '%s' "$RESP" | grep -q '"message"' || { echo "FAIL json msg: $RESP"; exit 1; }; printf '%s' "$RESP" | grep -q 'Greetings!' || { echo "FAIL json Greetings: $RESP"; exit 1; }

echo PASS
