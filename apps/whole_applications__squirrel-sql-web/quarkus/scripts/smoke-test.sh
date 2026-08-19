#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Smoke tests for the migrated Quarkus squirrel-sql-web application.
#
# Usage:
#   scripts/smoke-test.sh [BASE_URL]
#
#   BASE_URL defaults to http://localhost:8080 . The REST API is served under
#   the /ws context (see @ApplicationPath("/ws")).
#
# Exercises the same endpoints the original JavaEE/JAX-RS WAR exposed:
#   - AuthFilter rejects unauthenticated access (HTTP 401)
#   - JWT authentication with the default admin/admin credentials
#   - a valid token grants access to a protected endpoint
# ---------------------------------------------------------------------------
set -u

BASE_URL="${1:-http://localhost:8080}"
WS="${BASE_URL}/ws"

# Wrap curl in a function so the "--noproxy '*'" wildcard is passed literally and
# is never subject to shell pathname (glob) expansion. The wildcard disables any
# ambient HTTP proxy so requests reach the container directly.
curlx() { curl -s --noproxy '*' "$@"; }

pass=0
fail=0
check() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    echo "PASS: ${desc} (=${actual})"
    pass=$((pass + 1))
  else
    echo "FAIL: ${desc} (expected '${expected}', got '${actual}')"
    fail=$((fail + 1))
  fi
}

echo "Running smoke tests against ${WS}"

code=$(curlx -o /dev/null -w "%{http_code}" "${WS}/HelloWorld")
check "GET /HelloWorld without token -> 401" "401" "$code"

code=$(curlx -o /dev/null -w "%{http_code}" -X POST -d "username=admin&password=admin" "${WS}/Authenticate")
check "POST /Authenticate (form, valid) -> 200" "200" "$code"

code=$(curlx -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' "${WS}/Authenticate")
check "POST /Authenticate (json, valid) -> 200" "200" "$code"

code=$(curlx -o /dev/null -w "%{http_code}" -X POST -d "username=admin&password=wrong" "${WS}/Authenticate")
check "POST /Authenticate (invalid) -> 401" "401" "$code"

TOKEN=$(curlx -X POST -d "username=admin&password=admin" "${WS}/Authenticate")

body=$(curlx -H "Authorization: Bearer ${TOKEN}" "${WS}/HelloWorld")
check "GET /HelloWorld with token -> body" "Hello World" "$body"

code=$(curlx -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${TOKEN}" "${WS}/CurrentUser")
check "GET /CurrentUser with token -> 200" "200" "$code"

echo "---------------------------------------------"
echo "RESULT: ${pass} passed, ${fail} failed"
[ "$fail" -eq 0 ]
