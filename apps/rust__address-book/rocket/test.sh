#!/usr/bin/env bash
# Rust behavioral test for address-book
set -euo pipefail
PORT="8080"

RESP=$(curl -sL "http://localhost:${PORT}/contacts"); printf '%s' "$RESP" | grep -qE '^\[' || { echo "FAIL empty: $RESP"; exit 1; }
RESP=$(curl -sL "http://localhost:${PORT}/"); printf '%s' "$RESP" | grep -qE 'OK|<html' || { echo "FAIL text /: $RESP"; exit 1; }

echo PASS
