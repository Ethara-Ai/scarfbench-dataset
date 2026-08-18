#!/usr/bin/env bash
# Rust behavioral test for hello-servlet
set -euo pipefail
PORT="8080"

RESP=$(curl -sL "http://localhost:${PORT}/greeting?name=Duke"); printf '%s' "$RESP" | grep -qi 'Hello.*Duke' || { echo "FAIL greeting: $RESP"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/greeting"); printf '%s' "$RESP" | grep -qi 'Hello.*World' || { echo "FAIL greeting default: $RESP"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/"); printf '%s' "$RESP" | grep -qE 'Hello, World|<html' || { echo "FAIL text /: $RESP"; exit 1; }

echo PASS
