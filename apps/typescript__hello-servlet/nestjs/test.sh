#!/usr/bin/env bash
# Behavioral test for hello-servlet — payload/shape assertions
set -euo pipefail
PORT="8080"

# greeting-named
RESP=$(curl -sL "http://localhost:${PORT}/greeting?name=Duke")
printf '%s' "$RESP" | grep -qi 'Hello.*Duke' || { echo "FAIL greeting: $RESP"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/greeting")
printf '%s' "$RESP" | grep -qi 'Hello.*World' || { echo "FAIL greeting default: $RESP"; exit 1; }
# text-/
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE 'Hello, World|<html' || { echo "FAIL: / missing text: $RESP"; exit 1; }

echo PASS
