#!/usr/bin/env bash
# ScarfBench smoke grader for the person-service app (framework-agnostic).
# Assumes the app is already running and serving REST on ${BASE_URL} (:8080).
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

fail() { echo "FAIL - $1"; exit 1; }

echo "1) GET /persons (list, seeded)"
curl -sf "${BASE_URL}/persons" | grep -q "John Smith" || fail "GET /persons did not return seed data"

echo "2) GET /persons/{id}"
curl -sf "${BASE_URL}/persons/1" | grep -q '"id"' || fail "GET /persons/1 did not return a person"

echo "3) GET /persons/name/{name}"
curl -sf "${BASE_URL}/persons/name/Anne%20Brown" | grep -q "Anne Brown" || fail "GET /persons/name/Anne Brown failed"

echo "4) GET /persons/age-greater-than/{age}"
curl -sf "${BASE_URL}/persons/age-greater-than/60" | grep -q "Paul Walker" || fail "age-greater-than/60 missing Paul Walker"

echo "5) POST /persons (create)"
curl -sf -X POST "${BASE_URL}/persons" \
     -H 'Content-Type: application/json' \
     -d '{"name":"Test User","age":33,"gender":"MALE","externalId":99}' \
     | grep -q "Test User" || fail "POST /persons did not echo created person"

echo "PASS - all person-service smoke checks passed"
exit 0
