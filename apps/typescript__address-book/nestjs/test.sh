#!/usr/bin/env bash
# Behavioral test for address-book — payload/shape assertions
set -euo pipefail
PORT="8080"

# empty-array
RESP=$(curl -sL "http://localhost:${PORT}/contacts")
printf '%s' "$RESP" | grep -qE '^\[' || { echo "FAIL empty-array: $RESP"; exit 1; }
# text-/
RESP=$(curl -sL "http://localhost:${PORT}/")
printf '%s' "$RESP" | grep -qE 'OK|<html' || { echo "FAIL: / missing text: $RESP"; exit 1; }

echo PASS
