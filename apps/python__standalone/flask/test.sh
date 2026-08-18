#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080/standalone}"
echo "== GET $BASE_URL =="
RESP=$(curl -sL "$BASE_URL")
echo "RESP: $RESP"
if ! echo "$RESP" | grep -q "Greetings!"; then
  echo "FAIL - response missing 'Greetings!'"
  exit 1
fi
STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "$BASE_URL")
if [ "$STATUS" != "200" ]; then
  echo "FAIL - HTTP $STATUS"
  exit 1
fi
echo "PASS"
