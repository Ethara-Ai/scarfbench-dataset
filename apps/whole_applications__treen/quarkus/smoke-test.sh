#!/bin/sh -
# Smoke tests for the migrated Quarkus "treen" backend.
# Usage: ./smoke-test.sh [BASE_URL]
#   BASE_URL defaults to http://localhost:8080
#
# Exercises the full framework surface migrated from Jakarta EE to Quarkus:
#   - SmallRye Health (Quarkus up + Agroal datasource connectivity)
#   - JAX-RS + JSON-B (login endpoint)
#   - Bean Validation + custom ConstraintViolation exception mapper
#   - Servlet AuthorizationFilter + error-page -> ErrorHandlingServlet
#   - @SessionScoped session state across requests (login -> user -> notebook)
#
# NOTE: the authenticated tests require the app to be started with the demo
#       user seeded (env TREEN_DEMO_SEED_USER_ENABLED=true).

BASE_URL="${1:-http://localhost:8080}"
COOKIES="$(mktemp)"
FAILED=0
PASSED=0

log() { printf '%s\n' "$*"; }

# check_status <description> <expected-status> <actual-status> [body-substring] [body]
check_status() {
    desc="$1"; expected="$2"; actual="$3"; substr="$4"; body="$5"
    ok=1
    [ "$actual" = "$expected" ] || ok=0
    if [ -n "$substr" ]; then
        case "$body" in
            *"$substr"*) : ;;
            *) ok=0 ;;
        esac
    fi
    if [ "$ok" = "1" ]; then
        PASSED=$((PASSED + 1))
        log "PASS  | $desc (status=$actual)"
    else
        FAILED=$((FAILED + 1))
        log "FAIL  | $desc (expected=$expected got=$actual want-substr='$substr' body='$body')"
    fi
}

# request <method> <path> <extra-curl-args...> -> sets GLOBAL_STATUS and GLOBAL_BODY
request() {
    method="$1"; path="$2"; shift 2
    resp="$(curl -s -w '\n%{http_code}' -X "$method" "$@" "$BASE_URL$path")"
    GLOBAL_STATUS="$(printf '%s' "$resp" | tail -n1)"
    GLOBAL_BODY="$(printf '%s' "$resp" | sed '$d')"
}

log "==> Smoke testing $BASE_URL"

# 1. Health / readiness (includes datasource check)
request GET /q/health/ready
check_status "health readiness UP" 200 "$GLOBAL_STATUS" '"status": "UP"' "$GLOBAL_BODY"

# 2. Login with wrong credentials but valid body -> 401 JSON
request POST /service/auth/login \
    -H 'Content-Type: application/json' -H 'Accept: application/json' \
    -d '{"login":"nobody","password":"whatever","rememberMe":false}'
check_status "login wrong creds -> 401" 401 "$GLOBAL_STATUS" 'Login/password pair is wrong' "$GLOBAL_BODY"

# 3. Login with invalid body (login too long, >120 chars) -> 400 (Bean Validation)
LONG="$(awk 'BEGIN{s="";while(i++<130)s=s"x";print s}')"
request POST /service/auth/login \
    -H 'Content-Type: application/json' -H 'Accept: application/json' \
    -d "{\"login\":\"$LONG\",\"password\":\"p\",\"rememberMe\":false}"
check_status "login invalid body -> 400" 400 "$GLOBAL_STATUS" 'Login should be between' "$GLOBAL_BODY"

# 4. Unauthenticated access to secured endpoints -> 401 (AuthorizationFilter)
request GET /service/notebook -H 'Accept: application/json'
check_status "unauthenticated /service/notebook -> 401" 401 "$GLOBAL_STATUS"

request GET /service/user -H 'Accept: application/json'
check_status "unauthenticated /service/user -> 401" 401 "$GLOBAL_STATUS"

# 5. Successful login (seeded demo user) -> 200, capture session cookie
resp="$(curl -s -c "$COOKIES" -w '\n%{http_code}' -X POST "$BASE_URL/service/auth/login" \
    -H 'Content-Type: application/json' -H 'Accept: application/json' \
    -d '{"login":"demo","password":"demo1234","rememberMe":false}')"
LOGIN_STATUS="$(printf '%s' "$resp" | tail -n1)"
check_status "login demo/demo1234 -> 200" 200 "$LOGIN_STATUS"

# 6. Authenticated endpoints using the session cookie
resp="$(curl -s -b "$COOKIES" -w '\n%{http_code}' -H 'Accept: application/json' "$BASE_URL/service/user")"
US_STATUS="$(printf '%s' "$resp" | tail -n1)"; US_BODY="$(printf '%s' "$resp" | sed '$d')"
check_status "authenticated /service/user -> 200 demo" 200 "$US_STATUS" '"login":"demo"' "$US_BODY"

resp="$(curl -s -b "$COOKIES" -w '\n%{http_code}' -H 'Accept: application/json' "$BASE_URL/service/notebook")"
NB_STATUS="$(printf '%s' "$resp" | tail -n1)"; NB_BODY="$(printf '%s' "$resp" | sed '$d')"
check_status "authenticated /service/notebook -> 200" 200 "$NB_STATUS" '"version"' "$NB_BODY"

# 7. Logout -> 200
resp="$(curl -s -b "$COOKIES" -w '\n%{http_code}' -X POST "$BASE_URL/service/auth/logout")"
LO_STATUS="$(printf '%s' "$resp" | tail -n1)"
check_status "logout -> 200" 200 "$LO_STATUS"

rm -f "$COOKIES"

log "-----------------------------------------"
log "Smoke tests: $PASSED passed, $FAILED failed"
[ "$FAILED" = "0" ] || exit 1
