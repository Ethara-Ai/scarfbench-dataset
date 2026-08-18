#!/usr/bin/env bash
#
# ScarfBench behavioural oracle — application family: quarkus-htmx-todos.
#
# The bash + curl concretization of oracle/quarkus-htmx-todos.feature: the same 37 scenarios
# as smoke.py, in the same order, asserting the same observations. Shipped verbatim in the
# quarkus and spring variants and required to pass against both (G16/G17).
#
#   BASE_URL       target, default http://localhost:8080      (G13)
#   SMOKE_TIMEOUT  per-request timeout in seconds, default 20  (G14)
#
# Exits 0 when every scenario passes, and non-zero on the first failure (G11). Each line is
# [PASS] or [FAIL] and names the route, the status and, on failure, the body (G12).
#
# `scarf validate` copies smoke.py rather than this file, so smoke.py is what grades a
# candidate; this concretization exists because the specification is supposed to be
# expressible in more than one language, and an oracle that only works in one runtime is a
# weaker claim than one that works in two.
#
# Whitespace between tags is collapsed and the escaped apostrophe decoded before any markup
# comparison — engine artifacts, not behaviour. See FINDINGS.md §2.

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
TIMEOUT="${SMOKE_TIMEOUT:-20}"
PAGE_TITLE='Quarkus/htmx • TodoMVC'
MISSING_ID="00000000-0000-4000-8000-000000000000"

TMP="$(mktemp -d)"
BODY="$TMP/body"
HEAD="$TMP/head"
trap 'rm -rf "$TMP"' EXIT

PASSED=0
SCENARIO=""

scenario() { SCENARIO="$1"; }

pass() {
  PASSED=$((PASSED + 1))
  echo "[PASS] $SCENARIO — $1"
}

fail() {
  echo "[FAIL] $SCENARIO — $1"
  echo "       url    : $LAST_URL"
  echo "       status : $STATUS"
  echo "       body   : $(head -c 800 "$BODY")"
  exit 1
}

# ---------------------------------------------------------------- HTTP

LAST_URL=""
STATUS=""

req() { # method path [form] [hx]
  local method="$1" path="$2" form="${3:-}" hx="${4:-}"
  local -a args=(-s -o "$BODY" -D "$HEAD" -w '%{http_code}'
                 --max-time "$TIMEOUT" -X "$method" -H 'Accept: text/html,*/*')
  [ -n "$hx" ] && args+=(-H 'HX-Request: true')
  if [ "$method" = "POST" ]; then args+=(--data "$form"); fi
  LAST_URL="$BASE_URL$path"
  STATUS="$(curl "${args[@]}" "$LAST_URL")"
  if [ -z "$STATUS" ] || [ "$STATUS" = "000" ]; then
    echo "[FAIL] $SCENARIO — network error contacting $LAST_URL (no HTTP response)"
    exit 2
  fi
}

get()  { req GET  "$1" "" "${2:-}"; }
post() { req POST "$1" "${2:-}" "${3:-}"; }

header_value() { # name -> value, or empty
  tr -d '\r' < "$HEAD" | grep -i "^$1:" | tail -1 | sed "s/^[^:]*: *//"
}

expect_status() { [ "$STATUS" = "$1" ] || fail "expected HTTP $1"; }
expect_status_gte() { [ "$STATUS" -ge "$1" ] || fail "expected HTTP >= $1"; }

expect_header() {
  local actual; actual="$(header_value "$1")"
  [ "$actual" = "$2" ] || fail "expected header $1: $2, got '${actual}'"
}

expect_no_header() {
  local actual; actual="$(header_value "$1")"
  [ -z "$actual" ] || fail "expected no $1 header, got '${actual}'"
}

# ---------------------------------------------------------------- markup

flat() { # normalised single line on stdout
  tr '\n' ' ' < "$BODY" \
    | sed -e "s/&#39;/'/g" -e "s/&#039;/'/g" -e "s/&apos;/'/g" \
    | sed -e 's/> *</></g' -e 's/  */ /g' -e 's/^ //'
}

items() { # one <li ...> block per line, in document order, scoped to the todo list
  flat | sed -e 's|.*<ul class="todo-list" id="todo-list">||' -e 's|</ul>.*||' \
       | sed -e 's/<li /\
<li /g' | grep '^<li ' || true
}

item_ids() { items | sed -n 's/.*id="item-\([0-9a-fA-F-]\{36\}\)".*/\1/p'; }

item_line() { # id -> its <li> block
  items | grep "id=\"item-$1\"" | head -1
}

item_titles() {
  items | sed -n 's|.*<label[^>]*>\([^<]*\)</label>.*|\1|p'
}

id_of_title() { # title -> id
  items | grep -F ">$1</label>" | head -1 \
        | sed -n 's/.*id="item-\([0-9a-fA-F-]\{36\}\)".*/\1/p'
}

opening_tag() { sed 's/>.*/>/'; }

counter() {
  flat | sed -e 's/.*<span class="todo-count">//' -e 's|</span>.*||' \
             -e 's/<[^>]*>/ /g' -e 's/  */ /g' -e 's/^ //' -e 's/ $//'
}

anchor_for() { flat | grep -o "<a[^>]*href=\"$1\"[^>]*>" | head -1; }

toggle_input() { item_line "$1" | grep -o '<input[^>]*class="toggle"[^>]*>' | head -1; }
edit_input()   { item_line "$1" | grep -o '<input[^>]*class="edit"[^>]*>'   | head -1; }

expect_body_contains() {
  flat | grep -qF -- "$1" || fail "expected the response to contain: $1"
}

expect_body_lacks() {
  flat | grep -qiF -- "$1" && fail "expected the response NOT to contain: $1"
  return 0
}

# ---------------------------------------------------------------- operations

listing() { get "${1:-/todos}"; expect_status 200; }

titles_now() { listing "${1:-/todos}"; item_titles | tr '\n' '|'; }

add() { post /todos "title=$(printf '%s' "$1" | sed 's/ /%20/g')" "${2:-}"; }

add_all() {
  local t
  for t in "$@"; do add "$t"; expect_status 302; done
}

find_id() { # title -> id, via the list page
  listing
  local id; id="$(id_of_title "$1")"
  [ -n "$id" ] || fail "no item titled '$1' on the list page"
  printf '%s' "$id"
}

toggle_title() { local id; id="$(find_id "$1")" || exit 1; post "/todos/$id/toggle" "" "${2:-}"; }
rename_title() { local id; id="$(find_id "$1")" || exit 1; post "/todos/$id" "title=$(printf '%s' "$2" | sed 's/ /%20/g')"; }
delete_title() { local id; id="$(find_id "$1")" || exit 1; post "/todos/$id/delete"; }

reset() { # every scenario starts from an empty list (feature file Background)
  local id
  listing
  for id in $(item_ids); do post "/todos/$id/delete"; done
  listing
  [ -z "$(item_ids)" ] || fail "could not empty the todo list before the scenario"
}

completed_flag() { # id -> "yes"/"no"
  if item_line "$1" | opening_tag | grep -q 'class="completed"'; then echo yes; else echo no; fi
}

echo "ScarfBench oracle — quarkus-htmx-todos — target $BASE_URL"
echo

# ================================================================ 1–5 entry points

scenario "01 the application root redirects to the todo list"
reset
get /
expect_status 307
expect_header Location /todos
pass "GET / -> 307 Location: /todos"

scenario "02 the todo list page renders the application shell"
reset
get /todos
expect_status 200
expect_body_contains "<title>$PAGE_TITLE</title>"
expect_body_contains "<h1>todos</h1>"
flat | grep -o '<input[^>]*class="new-todo"[^>]*>' | grep -q 'name="title"' \
  || fail "no new-todo input named title"
flat | grep -o '<input[^>]*class="new-todo"[^>]*>' | grep -q 'placeholder="What needs to be done?"' \
  || fail "the new-todo input has no placeholder"
flat | grep -o '<section class="todoapp"[^>]*>' | grep -q 'hx-boost="true"' \
  || fail "the application section carries no hypermedia boost attribute"
expect_body_contains "Double-click to edit a todo"
expect_body_contains "todomvc.com"
pass "GET /todos -> 200, shell rendered"

scenario "03 the page loads the front-end assets it references"
get /todos.js
expect_status 200
expect_body_contains "clear-add-todo"
pass "GET /todos.js -> 200"
get /webjars/htmx.org/dist/htmx.min.js
expect_status 200
[ "$(wc -c < "$BODY")" -gt 1000 ] || fail "the hypermedia library is suspiciously small"
pass "GET /webjars/htmx.org/dist/htmx.min.js -> 200"
get /webjars/todomvc-app-css/index.css
expect_status 200
expect_body_contains "todoapp"
pass "GET /webjars/todomvc-app-css/index.css -> 200"

scenario "04 an unknown path is not served"
get /no-such-page
expect_status 404
pass "GET /no-such-page -> 404"

scenario "05 every route is reachable with a trailing slash"
reset
get /todos/
expect_status 200
expect_body_contains "<h1>todos</h1>"
get /todos/active/
expect_status 200
get /todos/completed/
expect_status 200
post /todos/toggle-all/ ""
expect_status 302
pass "the trailing-slash form of every route is served"

# ================================================================ 6–13 creating

scenario "06 adding a todo from a plain form submission redirects to the list"
reset
add "Buy milk"
expect_status 302
expect_header Location /todos
[ "$(titles_now)" = "Buy milk|" ] || fail "the list should contain exactly 'Buy milk', got '$(titles_now)'"
pass "POST /todos -> 302, item created"

scenario "07 a newly added todo is active"
reset
add "Buy milk"
expect_status 302
listing
ID="$(id_of_title "Buy milk")"
[ "$(completed_flag "$ID")" = "no" ] || fail "a new item should not be marked completed"
toggle_input "$ID" | grep -q "checked" && fail "a new item should not be checked"
pass "a new item is active and unchecked"

scenario "08 adding a todo from a hypermedia request returns only the new item"
reset
add "Buy milk" hx
expect_status 200
flat | grep -q '^<li ' || fail "the response should be a bare item fragment"
expect_body_lacks "<html"
expect_body_lacks "<!doctype"
expect_body_contains ">Buy milk</label>"
flat | grep -q 'id="item-[0-9a-fA-F-]\{36\}"' || fail "the fragment carries no item identifier"
pass "POST /todos (hypermedia) -> 200, single <li> fragment"

scenario "09 the hypermedia add response tells the client to clear the input"
reset
add "Buy milk" hx
expect_status 200
expect_header HX-Trigger clear-add-todo
pass "HX-Trigger: clear-add-todo"

scenario "10 a plain add response carries no client trigger"
reset
add "Buy milk"
expect_status 302
expect_no_header HX-Trigger
pass "no HX-Trigger on the redirect response"

scenario "11 adding a todo without a title is rejected"
reset
post /todos ""
expect_status_gte 400
listing
[ -z "$(item_ids)" ] || fail "a rejected add must not create an item"
pass "POST /todos with no title -> $STATUS, nothing created"

scenario "12 todos are listed oldest first"
reset
add_all one two three
[ "$(titles_now)" = "one|two|three|" ] || fail "expected 'one|two|three|', got '$(titles_now)'"
pass "items are listed oldest first"

scenario "13 every item carries the routes the client needs"
reset
add "Buy milk"
listing
ID="$(id_of_title "Buy milk")"
LINE="$(item_line "$ID")"
for needle in "action=\"/todos/$ID/toggle\"" "action=\"/todos/$ID/delete\"" \
              "action=\"/todos/$ID\"" "hx-post=\"/todos/$ID/toggle\"" \
              "hx-target=\"#item-$ID\""; do
  printf '%s' "$LINE" | grep -qF -- "$needle" || fail "the item markup lacks $needle"
done
toggle_input "$ID" | grep -qF "toggle-$ID" || fail "the checkbox does not reference its toggle form"
toggle_input "$ID" | grep -qF "requestSubmit()" || fail "the checkbox does not submit the toggle form"
pass "the item exposes its toggle, delete and edit routes"

# ================================================================ 14–17 the counter

scenario "14 the counter reports the number of active items"
reset
add_all one two
listing
[ "$(counter)" = "2 items left" ] || fail "expected '2 items left', got '$(counter)'"
pass "counter reads '2 items left'"

scenario "15 the counter is singular for a single active item"
reset
add one
listing
[ "$(counter)" = "1 item left" ] || fail "expected '1 item left', got '$(counter)'"
pass "counter reads '1 item left'"

scenario "16 the counter counts only active items"
reset
add_all one two
toggle_title one
listing
[ "$(counter)" = "1 item left" ] || fail "expected '1 item left', got '$(counter)'"
pass "completed items are not counted"

scenario "17 the counter reads zero for an empty list"
reset
listing
[ "$(counter)" = "0 items left" ] || fail "expected '0 items left', got '$(counter)'"
pass "counter reads '0 items left'"

# ================================================================ 18–25 toggling

scenario "18 completing a todo from a plain form submission redirects to the list"
reset
add "Buy milk"
toggle_title "Buy milk"
expect_status 302
expect_header Location /todos
listing
ID="$(id_of_title "Buy milk")"
[ "$(completed_flag "$ID")" = "yes" ] || fail "the item should be marked completed"
toggle_input "$ID" | grep -q "checked" || fail "the checkbox should be checked"
pass "POST /todos/{id}/toggle -> 302, item completed"

scenario "19 completing a todo from a hypermedia request returns only that item"
reset
add "Buy milk"
toggle_title "Buy milk" hx
expect_status 200
expect_body_lacks "<html"
flat | grep -q '^<li ' || fail "the response should be a bare item fragment"
flat | opening_tag | grep -q 'class="completed"' || fail "the fragment should be marked completed"
flat | grep -o '<input[^>]*class="toggle"[^>]*>' | grep -q checked || fail "the fragment checkbox should be checked"
expect_body_contains ">Buy milk</label>"
pass "POST /todos/{id}/toggle (hypermedia) -> 200, completed fragment"

scenario "20 toggling a completed todo makes it active again"
reset
add "Buy milk"
toggle_title "Buy milk"
toggle_title "Buy milk"
expect_status 302
listing
ID="$(id_of_title "Buy milk")"
[ "$(completed_flag "$ID")" = "no" ] || fail "the item should be active again"
[ "$(counter)" = "1 item left" ] || fail "expected '1 item left', got '$(counter)'"
pass "toggling twice returns the item to active"

scenario "21 toggling an unknown identifier is rejected"
reset
post "/todos/$MISSING_ID/toggle" ""
expect_status_gte 400
pass "POST /todos/{unknown}/toggle -> $STATUS"

scenario "22 toggling all items completes every one of them"
reset
add_all one two
post /todos/toggle-all ""
expect_status 302
expect_header Location /todos
listing
for id in $(item_ids); do
  [ "$(completed_flag "$id")" = "yes" ] || fail "item $id should be completed"
done
[ "$(counter)" = "0 items left" ] || fail "expected '0 items left', got '$(counter)'"
pass "POST /todos/toggle-all -> 302, every item completed"

scenario "23 toggling all items again reactivates every one of them"
reset
add_all one two
post /todos/toggle-all ""
post /todos/toggle-all ""
expect_status 302
listing
for id in $(item_ids); do
  [ "$(completed_flag "$id")" = "no" ] || fail "item $id should be active"
done
[ "$(counter)" = "2 items left" ] || fail "expected '2 items left', got '$(counter)'"
pass "toggle-all is a toggle, not a set"

scenario "24 toggling all items when some are completed completes the rest"
reset
add_all one two
toggle_title one
post /todos/toggle-all ""
expect_status 302
listing
for id in $(item_ids); do
  [ "$(completed_flag "$id")" = "yes" ] || fail "item $id should be completed"
done
pass "a partially completed list completes fully"

scenario "25 toggling all items on an empty list is accepted and changes nothing"
reset
post /todos/toggle-all ""
expect_status 302
listing
[ -z "$(item_ids)" ] || fail "the list should still be empty"
pass "toggle-all on an empty list -> 302"

# ================================================================ 26–29 filters

scenario "26 the active filter lists only active todos"
reset
add_all one two
toggle_title one
listing /todos/active
[ "$(item_titles | tr '\n' '|')" = "two|" ] || fail "expected 'two|', got '$(item_titles | tr '\n' '|')'"
pass "GET /todos/active -> only active items"

scenario "27 the completed filter lists only completed todos"
reset
add_all one two
toggle_title one
listing /todos/completed
[ "$(item_titles | tr '\n' '|')" = "one|" ] || fail "expected 'one|', got '$(item_titles | tr '\n' '|')'"
pass "GET /todos/completed -> only completed items"

scenario "28 each filter page marks its own filter as the selected one"
reset
listing /todos
anchor_for "/todos"           | grep -q selected || fail "the All filter should be selected on /todos"
anchor_for "/todos/active"    | grep -q selected && fail "only one filter should be selected"
listing /todos/active
anchor_for "/todos/active"    | grep -q selected || fail "the Active filter should be selected on /todos/active"
anchor_for "/todos"           | grep -q selected && fail "only one filter should be selected"
listing /todos/completed
anchor_for "/todos/completed" | grep -q selected || fail "the Completed filter should be selected on /todos/completed"
anchor_for "/todos"           | grep -q selected && fail "only one filter should be selected"
pass "each filter page selects its own filter"

scenario "29 every filter page renders the same shell"
reset
for path in /todos/active /todos/completed; do
  listing "$path"
  expect_body_contains "<title>$PAGE_TITLE</title>"
  expect_body_contains "<h1>todos</h1>"
  expect_body_contains 'class="new-todo"'
done
pass "the filter pages render the same shell"

# ================================================================ 30–32 renaming

scenario "30 renaming a todo replaces its title"
reset
add "Buy milk"
rename_title "Buy milk" "Buy oat milk"
expect_status 302
expect_header Location /todos
[ "$(titles_now)" = "Buy oat milk|" ] || fail "expected 'Buy oat milk|', got '$(titles_now)'"
pass "POST /todos/{id} -> 302, title replaced"

scenario "31 the edit form is prefilled with the current title"
reset
add "Buy milk"
listing
ID="$(id_of_title "Buy milk")"
edit_input "$ID" | grep -q 'value="Buy milk"' || fail "the edit field is not prefilled"
pass "the edit field holds the current title"

scenario "32 renaming an unknown identifier is rejected"
reset
post "/todos/$MISSING_ID" "title=nope"
expect_status_gte 400
pass "POST /todos/{unknown} -> $STATUS"

# ================================================================ 33–36 deleting

scenario "33 deleting a todo removes it and leaves the others"
reset
add_all one two
delete_title one
expect_status 302
expect_header Location /todos
[ "$(titles_now)" = "two|" ] || fail "expected 'two|', got '$(titles_now)'"
pass "POST /todos/{id}/delete -> 302, only that item removed"

scenario "34 deleting an unknown identifier is accepted and changes nothing"
reset
add one
post "/todos/$MISSING_ID/delete" ""
expect_status 302
[ "$(titles_now)" = "one|" ] || fail "expected 'one|', got '$(titles_now)'"
pass "POST /todos/{unknown}/delete -> 302, list unchanged"

scenario "35 clearing completed todos removes only the completed ones"
reset
add_all one two three
toggle_title one
toggle_title three
post /todos/clear-completed ""
expect_status 302
expect_header Location /todos
[ "$(titles_now)" = "two|" ] || fail "expected 'two|', got '$(titles_now)'"
pass "POST /todos/clear-completed -> 302, only completed removed"

scenario "36 clearing completed todos with none completed changes nothing"
reset
add_all one two
post /todos/clear-completed ""
expect_status 302
[ "$(titles_now)" = "one|two|" ] || fail "expected 'one|two|', got '$(titles_now)'"
pass "clear-completed with nothing completed is a no-op"

# ================================================================ 37 persistence

scenario "37 every change survives into later requests"
reset
add "Buy milk"
CREATED_ID="$(find_id "Buy milk")"
rename_title "Buy milk" "Buy oat milk"
toggle_title "Buy oat milk"
listing
ID="$(id_of_title "Buy oat milk")"
[ "$(titles_now)" = "Buy oat milk|" ] || fail "expected 'Buy oat milk|', got '$(titles_now)'"
listing
[ "$(completed_flag "$ID")" = "yes" ] || fail "the item should still be completed"
[ "$(counter)" = "0 items left" ] || fail "expected '0 items left', got '$(counter)'"
[ "$ID" = "$CREATED_ID" ] || fail "the row was replaced rather than updated ($CREATED_ID -> $ID)"
pass "add, rename and complete all persist, on the same row"

echo
echo "[PASS] all scenarios passed ($PASSED assertions across 37 scenarios)"
exit 0
