#!/usr/bin/env bash
# ScarfBench liveness fallback. The real grading is done by smoke.py; this only
# runs if no smoke grader is present.
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
CODE=$(curl -sL -o /dev/null -w "%{http_code}" "${BASE_URL}/" || echo 000)
if [ "$CODE" != "000" ] && [ "$CODE" -lt 500 ]; then
  echo "PASS - got HTTP ${CODE}"; exit 0
else
  echo "FAIL - got HTTP ${CODE}"; exit 1
fi
