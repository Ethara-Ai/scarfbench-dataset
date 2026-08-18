# Step 01 — Inventory the source (Gate A)

| Role | Framework | Path |
|---|---|---|
| Source | Quarkus 3.30.5, Java 21 | `benchmark/presentation/quarkus-htmx-todos/quarkus` |
| Target | Spring Boot 3.5.7, Java 21 | `benchmark/presentation/quarkus-htmx-todos/spring` |

Layer `presentation` · tier **focused** · single application container. Upstream needs an
external PostgreSQL over Compose; the benchmark variants use H2 in-memory instead, so no
Compose stack is required (A5). That substitution is applied to **both** variants and is
recorded in each pom's header.

## Structure

```
pom.xml                            quarkus-bom 3.30.5 imported, java 21, fast-jar
src/main/java/todos/
  AppResource.java                 @Path("/")      — GET / -> 307 /todos
  TodoResource.java                @Path("/todos") — 8 handlers, @CheckedTemplate inner class
  Todo.java                        @Entity extends PanacheEntityBase, 6 static finders
src/main/resources/
  application.properties           datasource, flyway, packaging, qute content type, logging
  db/migration/V1__create_todos_table.sql
  db/migration/V2__todos_table_created_timestamp.sql
  templates/base.html              layout: {#insert title} / {#insert body}
  templates/TodoResource/list.html the page
  templates/TodoResource/item.html one row, included per item AND returned alone to htmx
  META-INF/resources/todos.js      double-click-to-edit, clears the input on HX-Trigger
```

No test sources. Two webjars carry the front end: `htmx.org` 2.0.10 and
`todomvc-app-css` 2.4.3.

## Framework dependencies

`quarkus-resteasy` (JAX-RS), `quarkus-resteasy-qute` (templating), `quarkus-arc` (CDI),
`quarkus-hibernate-orm-panache` (persistence, active-record), `quarkus-flyway` (schema),
`quarkus-jdbc-h2` (driver), `quarkus-smallrye-health` (`/q/health`),
`quarkus-webjars-locator` (version-less `/webjars/**`). Test scope: `quarkus-junit5`,
`rest-assured` — no tests to run.

Build plugins: `quarkus-maven-plugin` (augmentation + packaging, at the project root),
`maven-compiler-plugin`, `maven-surefire-plugin`. A `native` profile exists and is not
exercised.

## Runtime assumptions

- **The template engine is compile-time-checked.** `@CheckedTemplate` binds
  `Templates.list(...)` / `Templates.item(...)` to `templates/TodoResource/{list,item}.html`
  by class and method name, and augmentation fails the build if a template is missing or a
  parameter does not typecheck. Nothing in the target has an equivalent, so the coupling
  between handler and template becomes a plain string view name — and a broken one is a
  runtime 500 rather than a build error.
- **The entity is also the form-binding target.** `Todo.title` carries
  `@org.jboss.resteasy.annotations.jaxrs.FormParam` and the handler takes
  `@Form Todo todo`. The persistence model doubles as an HTTP request model.
- **The entity is also the query API.** Panache puts six static finders on the entity;
  there is no repository type at all.
- **The runtime normalises request URIs.** `/todos/`, `/todos/active/` and `//todos` all
  serve, and *nothing in the source says so*. This one is invisible on inspection and is
  the single most likely thing to be lost — see step 06.
- **Two routes branch on a request header.** `POST /todos` and `POST /todos/{id}/toggle`
  return an HTML fragment when `HX-Request` is present and a 302 when it is not; the add
  route additionally sets `HX-Trigger: clear-add-todo` on the fragment response only.
- No messaging, no scheduling, no WebSocket, no security, no JSON API. Transactions exist
  and are declared per write method with `jakarta.transaction.Transactional`.
- Startup work is Flyway migration only. There is **no seed data**: a freshly started
  container serves an empty list, which is what makes the oracle repeatable.
