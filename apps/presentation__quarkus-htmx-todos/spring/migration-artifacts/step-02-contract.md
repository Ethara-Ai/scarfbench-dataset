# Step 02 — Record the observable contract, by probing the running source

Measured against the source container before any target code existed. Every number below is
a `curl` result, not an expectation.

## Routes

| Request | Status | Notable response detail |
|---|---|---|
| `GET /` | 307 | `Location: /todos` — not 302 |
| `GET /todos` | 200 | full document, `<title>Quarkus/htmx • TodoMVC</title>` |
| `GET /todos/active` | 200 | same shell, `Active` filter marked `selected` |
| `GET /todos/completed` | 200 | same shell, `Completed` filter marked `selected` |
| `GET /todos.js` | 200 | static asset |
| `GET /webjars/htmx.org/dist/htmx.min.js` | 200 | 51,238 bytes, **version-less path** |
| `GET /webjars/todomvc-app-css/index.css` | 200 | 7,277 bytes |
| `POST /todos` (form `title=`) | 302 | `Location: /todos`, **no** `HX-Trigger` |
| `POST /todos` + `HX-Request: true` | 200 | bare `<li>` fragment, `HX-Trigger: clear-add-todo` |
| `POST /todos/{id}` (form `title=`) | 302 | `Location: /todos` |
| `POST /todos/toggle-all` | 302 | `Location: /todos` |
| `POST /todos/{id}/toggle` | 302 | `Location: /todos` |
| `POST /todos/{id}/toggle` + `HX-Request: true` | 200 | bare `<li>` fragment, no `HX-Trigger` |
| `POST /todos/{id}/delete` | 302 | `Location: /todos` |
| `POST /todos/clear-completed` | 302 | `Location: /todos` |
| `GET /q/health` | 200 | framework-specific path |

`Location` is **relative** on every redirect. `Content-Type` on HTML responses is
`text/html`, with no charset parameter.

## Rejections

| Request | Status |
|---|---|
| `GET /no-such-page` | 404 |
| `POST /todos` with no `title` field | 500 (the column is `not null`) |
| `POST /todos` with `title=` (empty) | **302** — an empty todo is created |
| `POST /todos/{unknown-uuid}` | 500 |
| `POST /todos/{unknown-uuid}/toggle` | 500 |
| `POST /todos/{unknown-uuid}/delete` | **302** — deleting a missing row is a no-op |
| `POST /todos/not-a-uuid/toggle` | 404 |
| `GET /todos/toggle-all` (wrong verb) | 405 |
| `PUT /todos` (wrong verb) | 405 |

The three inconsistencies in that table are the source's behaviour, not a misreading:
missing-on-delete is silent, missing-on-toggle is a 500, and an empty title is accepted.

## URI normalisation

| Request | Status |
|---|---|
| `GET /todos/` | 200 |
| `GET /todos/active/` | 200 |
| `GET /todos/completed/` | 200 |
| `GET //todos` | 200 |
| `POST /todos/toggle-all/` | 302 |

## Rendered markup

- Item row: `<li class="{completed?}" id="item-{uuid}">`, containing a toggle form with
  `hx-post`/`hx-target`, a checkbox whose `onclick` submits that form by id, a `<label>`
  with the title, a delete form, and an edit form with the title prefilled.
- Item counter: `<span class="todo-count"> <strong>N</strong> item[s] left </span>` —
  singular at exactly 1.
- Filter links: exactly one carries `class="selected"`; the other two render `class=""`.
- Completed row: `class="completed"` on the `<li>` and a bare `checked` attribute on the
  checkbox.
- Ordering: oldest first, by creation timestamp.
- Initial state: empty list, `0 items left`.

That is the contract. Everything in `oracle/quarkus-htmx-todos.feature` is derived from this
table, and nothing in it was written from reading the target.
