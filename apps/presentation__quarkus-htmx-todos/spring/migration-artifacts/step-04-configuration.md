# Step 04 — Migrate configuration by intent (Gate C)

Not a key-for-key translation. Each source setting was read for what it *does*, and the
target expresses the same intent in its own vocabulary — including the settings that have no
counterpart and the settings the source never had to write down.

| Intent | Source key | Target key |
|---|---|---|
| HTTP binding (C2) | `quarkus.http.port=8080` | `server.port=8080` |
| Datasource URL (C3) | `quarkus.datasource.jdbc.url=jdbc:h2:mem:todos;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | `spring.datasource.url=` same URL |
| Datasource credentials | `quarkus.datasource.username/password` | `spring.datasource.username/password` |
| Driver selection | `quarkus.datasource.db-kind=h2` | inferred from the URL; the driver is the `h2` dependency |
| Connection-pool ceiling | `quarkus.datasource.jdbc.max-size=16` (Agroal) | `spring.datasource.hikari.maximum-pool-size=16` (HikariCP) — different pool, same ceiling |
| Schema ownership (C4/C5) | `quarkus.flyway.migrate-at-start=true` | `spring.flyway.enabled=true`, `spring.flyway.locations=classpath:db/migration`, **plus** `spring.jpa.hibernate.ddl-auto=none` |
| Logging (C7) | `quarkus.log.category."todos".level=DEBUG` | `logging.level.todos=DEBUG` |
| Static resources (C9) | `src/main/resources/META-INF/resources/` | `src/main/resources/static/` |
| Templates (C9) | `src/main/resources/templates/` | `src/main/resources/templates/` — same root, different subdirectory names (step 05) |

## Settings with no target counterpart, dropped deliberately

- `quarkus.qute.content-types.ts-html=text/vnd.turbo-stream.html` — an upstream leftover
  from the sibling Hotwire application. No template in this application produces a Turbo
  Stream response, and the target engine has no per-extension content-type registry.
  Nothing observable is lost. Recorded rather than silently dropped.
- `quarkus.package.jar.type=fast-jar` — a source packaging mode. The target's packaging is
  the Boot executable jar, which is a build concern in `pom.xml`, not a runtime property.

## Settings the source never wrote down, and the target must

This is where a mechanical conversion loses behaviour. Three defaults differ, and in each
case the *source's* behaviour is the contract:

| Behaviour | Source | Target default | Stated explicitly in the target |
|---|---|---|---|
| Redirect `Location` is relative | relative (`/todos`) | absolute (`http://host:8080/todos`), because the servlet container resolves `sendRedirect` | `server.tomcat.use-relative-redirects=true` |
| Hibernate never touches the schema | `none`, Flyway owns it | `create-drop` for an embedded datasource — Flyway's presence usually suppresses that, but relying on an inference where the source made a statement is not the same statement | `spring.jpa.hibernate.ddl-auto=none` |
| No request-scoped persistence session | rendering happens outside the transaction | `open-in-view=true`, allowing queries during view rendering | `spring.jpa.open-in-view=false` |

The first was found by measurement, not by reading: the target's first run answered
`Location: http://localhost:8080/todos`, and the oracle asserts the header verbatim.

## Done when (C10)

Verified against the running target container: the application starts, Flyway creates the
schema (`Successfully applied 2 migrations to schema "PUBLIC"`), the list page and both
filter pages serve, and both webjar assets and `/todos.js` resolve.
