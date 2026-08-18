# Step 08 — Oracle construction (Gate G)

## Step 1 — the framework-independent specification

`oracle/quarkus-htmx-todos.feature`: **37 Gherkin scenarios**, written from the contract
measured in step 02, before any target code existed. Read it and you cannot tell which
framework serves it — no template engine, no bean scope, no persistence API, no package or
class name appears (G3/G6).

Each scenario is atomic (G2): initial state (`Background: the todo list is empty`) →
one protocol-level operation → an expected observable outcome.

Coverage, by group:

| Scenarios | Group | What is asserted |
|---|---|---|
| 1–5 | entry points | root redirect and its status, page shell, front-end assets, 404, trailing-slash tolerance |
| 6–13 | creating | redirect vs fragment, the `HX-Trigger` header and its absence, rejection, ordering, the routes each row exposes |
| 14–17 | the counter | plural, singular, completed items excluded, zero |
| 18–25 | toggling | plain and hypermedia, toggling back, unknown id, toggle-all in all four states |
| 26–29 | filters | active, completed, which filter is marked selected, shell consistency |
| 30–32 | renaming | title replaced, edit field prefilled, unknown id |
| 33–36 | deleting | one row only, unknown id is a no-op, clear-completed, clear-completed with nothing completed |
| 37 | persistence | add + rename + complete all survive, **on the same row id** |

## Step 2 — two concretizations

| File | Runtime | What it is |
|---|---|---|
| `smoke.py` | pytest, stdlib `urllib` | what `scarf validate` stages and grades |
| `test.sh` | bash + curl | the same 37 scenarios, in the same order |

Both are shipped verbatim in **both** variants. Both take `BASE_URL` (G13) and a timeout
(G14), handle transport failures distinctly from HTTP errors, exit 0 on success and non-zero
on the first failure (G11), and print `[PASS]`/`[FAIL]` with the URL, the status and the body
(G12).

Every scenario resets to an empty list first, using only routes the specification already
covers, so the suite is order-independent.

## Step 3 — proven against the expert variants (the admission rule)

| Variant | `smoke.py` | `test.sh` |
|---|---|---|
| quarkus (source, expert) | **37 / 37** | **37 / 37** |
| spring (target, expert) | **37 / 37** | **37 / 37** |

Run through `make test`, which builds the image, starts the container, waits for the
framework's readiness pattern, and executes the oracle inside the container via `docker exec`.

**G15 is not met: this application family has no Jakarta EE variant.** The three-variant
invariant is satisfied for two of three. That the same file passes two idiomatically distinct
implementations rules out fingerprinting either, but the family is not admissible until a
Jakarta EE variant exists and passes.

## Tolerances, and why each one exists (G9)

| Case | quarkus | spring | Asserted as |
|---|---|---|---|
| add with no `title` field | 500 | 400 | rejected, `>= 400`, and nothing created |
| rename an unknown id | 500 | 500 | rejected, `>= 400` |
| toggle an unknown id | 500 | 500 | rejected, `>= 400` |
| `Content-Type` on HTML | `text/html` | `text/html;charset=UTF-8` | not asserted |
| health endpoint | `/q/health` | `/actuator/health` | not asserted |
| a non-UUID path segment | 404 | 400 | not asserted |
| `checked` attribute form | bare `checked` | `checked="checked"` | presence of `checked` inside the toggle input |
| escaped `'` in an attribute | literal `'` | `&#39;` | decoded before comparison |
| inter-tag whitespace | — | — | collapsed before comparison |

The last three are engine serialisation, not behaviour: the DOM a browser builds is identical.
None of the normalisations can hide a wrong route, a wrong status, a wrong header, a wrong
title or a wrong flag — the checkbox assertion still fails if `checked` is absent when it
should be present, and the item-count assertion still fails on the wrong number or the wrong
plural.

## Root-path trap audit (G18)

`GET /` returns **307 with `Location: /todos`** from *both* variants, and the oracle asserts
exactly that. Neither variant is a servlet/JSF web application serving a bare context root
that the other lacks — this application's root is a real redirect handler in both. The
`roster` failure mode (an oracle probing a context root that only the Jakarta variant serves)
does not apply.

## Does the oracle reward adding behaviour? (K8)

No. Every asserted route exists in the source, and every assertion was derived from a
response measured against the running source before the target existed. Scenario 05
(trailing slashes) is the case to check hardest, because the target needed new code to pass
it — and the source answers 200/302 for all four of those requests, so the scenario asserts
behaviour the reference *has*, not behaviour the migration invented.
