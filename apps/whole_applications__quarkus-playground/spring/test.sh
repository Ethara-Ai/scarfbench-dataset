#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
echo "Health check: ${BASE_URL}/"
CODE=$(curl -sL -o /dev/null -w "%{http_code}" --max-time 20 "${BASE_URL}/" || echo 000)
if [ "$CODE" != "000" ] && [ "$CODE" -lt 500 ]; then echo "PASS - got HTTP ${CODE}"; exit 0; else echo "FAIL - got HTTP ${CODE}"; exit 1; fi
