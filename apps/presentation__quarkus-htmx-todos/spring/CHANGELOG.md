# Migration log — quarkus → spring

`quarkus-htmx-todos`, layer `presentation`, tier `focused`.

Every action, every error and every behavioural note, in the order they happened. The
workflow is the paper's Appendix G one: **inspect → migrate one layer → build/run → observe
failure → patch → re-run**.

---

## 1. Inventory (Gate A)

Source: Quarkus 3.30.5, Java 21, fast-jar, single module.

```
src/main/java/todos/
  AppResource.java     @Path("/")      — GET / -> 307 /todos
  TodoResource.java    @Path("/todos") — 8 handlers, @CheckedTemplate inner class
  Todo.java            @Entity extends PanacheEntityBase, 6 static finders, @FormParam on title
src/main/resources/
  application.properties
  db/migration/V1__create_todos_table.sql, V2__todos_table_created_timestamp.sql
  templates/base.html, templates/TodoResource/{list,item}.html
  META-INF/resources/todos.js
```

Runtime assumptions found: no messaging, no scheduling, no WebSocket, no security, no JSON
API. Transactions are declared per write handler. **Three couplings live on the entity** —
it is the persistence model, the query API and the HTTP form-binding target at once — and
that is where most of the transformation work is.

Full detail: `migration-artifacts/step-01-inventory.md`.

## 2. Record the observable contract by probing the running source

Built and started the source container, then probed every route, every rejection and every
URL shape **before writing any target code**. 22 status codes, three `Location` headers, two
`Content-Type` values and the rendered markup of five responses, all measured.

Three source behaviours were surprises worth writing down, because a migration that "cleans
them up" has changed behaviour:

- `POST /todos` with an **empty** title → 302, and an empty todo is created. With the field
  **missing** → 500, because the column is `not null`.
- Deleting an unknown id → **302**, silently. Toggling or renaming an unknown id → **500**.
- `/todos/`, `/todos/active/`, `//todos` and `/todos/toggle-all/` all serve. Nothing in the
  source code says so.

Full table: `migration-artifacts/step-02-contract.md`.

## 3. Dependency-role table, then the build (Gate B)

Wrote the role table first (`step-03-dependency-roles.md`), then edited the pom: eight source
extensions → seven target dependencies. No coordinate was string-replaced.

Two rows carry the risk, and they are the paper's own two: **templating** (Qute → Thymeleaf,
a whole language change) and **persistence** (Panache active-record → Spring Data JPA). B8
applies in the reverse of the paper's direction here: it is the *source* that hides its
queries behind a string-typed abstraction on the entity, and the target that has to name
them.

`webjars-locator-lite` is the row that is easy to miss. Without it,
`/webjars/htmx.org/dist/htmx.min.js` 404s and the templates would have to carry the version —
which would change the rendered markup, i.e. the contract.

`mvn clean install -DskipTests` → **BUILD SUCCESS**, before any deeper transformation.

## 4. Configuration by intent (Gate C)

Ten settings mapped, two dropped with reasons, and — the part a key-for-key conversion loses —
**three target defaults that differ from source behaviour and had to be stated explicitly**:
relative redirects, `ddl-auto=none`, `open-in-view=false`. Details and the reasoning:
`step-04-configuration.md`.

## 5. Preserve / rewrite (Gate D)

Preserved byte-identical: `todos.js`, both Flyway scripts, both webjar versions. Preserved
with three couplings removed: the entity. Rewritten: both resource classes and all three
templates. Added: the entry point, the repository, and one filter (see §7). Not migrated: the
source's test-scope dependencies. Table: `step-05-preserve-rewrite.md`.

## 6. Code transformation (Gate E)

Routing, DI, persistence and transactions: `step-06-code-transformation.md`. All eight route
paths and verbs unchanged.

## 7. Build/run → observe failure → patch → re-run

Four failures, in the order they were hit. Each was observed against a running container, not
predicted.

### 7.1 `th:onclick` is refused outright — a 500 on every page with a row

```
org.thymeleaf.exceptions.TemplateProcessingException: Only variable expressions returning
numbers or booleans are allowed in this context, any other datatypes are not trusted in the
context of this expression... A typical case is HTML attributes for event handlers (e.g.
"onload")   (template: "todos/item" - line 27, col 7)
```

The source renders `onclick="document.getElementById('toggle-{todo.id}').requestSubmit()"`.
The target engine refuses to interpolate a `String` into an event-handler attribute at all —
a deliberate injection guard, and it fires on the *first request that renders a row*, so the
list page and both htmx fragment responses were 500.

Patched to `th:attr="onclick=|…|"`, which is not restricted. Re-ran: fragment 200, list 200.

Note what class of failure this is: the build was green, the application started, and the
empty list page rendered fine. Only a page with at least one row failed.

### 7.2 Redirects came back absolute

```
POST /todos  ->  302  Location: http://localhost:8080/todos     (target, first run)
POST /todos  ->  302  Location: /todos                          (source)
```

The servlet container resolves a relative `sendRedirect` to an absolute URL by default.
`Location` is part of the contract and the oracle asserts it verbatim, so
`server.tomcat.use-relative-redirects=true`. Re-ran: `Location: /todos`.

### 7.3 Template comments were rendered into the page

The rewritten templates carry explanatory headers. Written as HTML comments they were
**emitted into the response**, ahead of the doctype. Switched to the engine's parser-level
comment form (`<!--/* … */-->`), which is removed at parse time. Re-ran: byte-for-byte clean.

### 7.4 Four URL shapes that worked on the source returned 404

```
GET /todos/           source 200   target 404
GET /todos/active/    source 200   target 404
GET //todos           source 200   target 404
POST /todos/toggle-all/  source 302   target 404
```

**This is the failure this task is built to expose.** The source's HTTP runtime normalises
the request URI before matching; the target matches it as given. Trailing-slash matching was
on by default through Spring Framework 5, deprecated in 6.0, and is gone. Nothing in the
source *code* asks for the behaviour, so there is nothing to port — the migration looks
complete, every canonical route works, and four URL shapes have silently stopped working.

Patched with `UriNormalizationFilter`: collapse repeated slashes, drop trailing ones,
**forward** rather than redirect (a redirect would turn a 200 into a 3xx and add a round trip
the source does not have). Expressed once as a filter, not as a second path on each of eight
mappings, because the source's behaviour is uniform runtime normalisation — writing it per
route would drift the first time a route is added.

Added as oracle scenario 05, so it is now graded rather than incidental.

## 8. Oracle (Gate G)

37 framework-neutral Gherkin scenarios, two concretizations, **37/37 against both expert
variants** in both concretizations. Construction, coverage and every tolerance:
`step-08-oracle.md`.

## 9. Layered validation (Gate H)

| Level | Result |
|---|---|
| Production build | `mvn clean install -DskipTests` → BUILD SUCCESS |
| Container build | image builds |
| Container run | `Tomcat started on port 8080`, `Started TodosApplication in 3.895 seconds` |
| Schema initialisation | `Successfully applied 2 migrations to schema "PUBLIC"`, before readiness |
| REST endpoints | expected status **and** content on all 22 probed requests |
| Behavioural oracle | 37/37 (`smoke.py`), 37/37 (`test.sh`) |
| Repeatable initial state | empty list, `0 items left`, on every fresh container |

`make local` was **not** run: this build host has no Maven, so every build and run happens
inside the container. Stated rather than ticked.

## 10. Stronger than the oracle asks for: page diffs

37 passing assertions prove the assertions were satisfied. They do not prove that rewriting
three templates into a different language preserved the presentation layer. So five responses
were captured from **both containers running simultaneously**, with the same three todos added
in the same order and the same one completed, then compared with UUIDs normalised positionally
and inter-tag whitespace collapsed:

| Response | Result |
|---|---|
| `GET /todos` (3 rows, 1 completed) | identical except the `checked` attribute form — 3,277 chars vs 3,277 |
| `GET /todos/active` | **byte-identical**, 2,715 chars |
| `GET /todos/completed` | identical except the `checked` attribute form — 2,201 vs 2,201 |
| `POST /todos` htmx fragment | **byte-identical**, 537 chars |
| `POST /todos/{id}/toggle` htmx fragment | identical except the `checked` attribute form — 563 vs 563 |

The single remaining difference is one attribute's serialisation: the source emits a bare
`checked`, the target emits `checked="checked"` and places it last in the tag. The engine
offers no way to emit the bare form, and the DOM a browser builds is identical. Recorded in
`FINDINGS.md` §2 rather than normalised away silently.

One earlier difference *was* fixable and was fixed: the item counter's text node rendered as
`<strong>2</strong>\nitems left` because the removed `<th:block>` tag left newlines around
the noun. Keeping the count and the noun on one template line restores
`<strong>2</strong> items left` exactly.

## 11. Failure categories (Gate L5)

| Failure | Phase | Category |
|---|---|---|
| `th:onclick` refused (7.1) | runtime | template/view rendering — surfaced only on a page containing a row |
| absolute redirect `Location` (7.2) | runtime | configuration: a target default differing from source behaviour |
| comments rendered into the page (7.3) | runtime | template syntax |
| trailing-slash routes 404 (7.4) | runtime | **behaviour provided by the source runtime with no code to port** |

None of the four is a build failure. All four passed the compile gate and three of the four
would have passed a deploy gate too — which is the paper's Finding 2 in miniature: build- or
deploy-only oracles substantially overstate migration quality.
