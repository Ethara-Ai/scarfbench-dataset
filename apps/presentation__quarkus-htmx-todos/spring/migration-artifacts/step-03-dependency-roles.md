# Step 03 — Dependency-role table (Gate B1/B2)

Written before any dependency coordinate was edited. Each row is an **architectural
concern** first; the target coordinate is chosen to fill that role, never by string
similarity.

| Concern | Quarkus source | Spring target | Note |
|---|---|---|---|
| Build management | `quarkus-bom` 3.30.5 (imported) | `spring-boot-starter-parent` 3.5.7 (parent) | B3. Import → inherit: the target's dependency management arrives through the parent, not a BOM import |
| Build plugin | `quarkus-maven-plugin` at the project root | `spring-boot-maven-plugin` at the project root | B4. Root placement matters: the launch contract is `mvn spring-boot:run`, and the prefix has to resolve where the harness runs it |
| Packaging | fast-jar, `target/quarkus-app/` | Boot executable jar, `target/todos-1.0.0-SNAPSHOT.jar` | B5 |
| Java level | 21 | 21 | B6 |
| HTTP / routing | `quarkus-resteasy` (JAX-RS on Vert.x) | `spring-boot-starter-web` (Spring MVC on Tomcat) | |
| Dependency injection | `quarkus-arc` (CDI) | Spring container, inside the web starter | no separate coordinate exists on the target side |
| **Server-side templating** | `quarkus-resteasy-qute` | `spring-boot-starter-thymeleaf` | **The high-risk row for this family.** Not an annotation rename: the template language, the layout mechanism and the fragment-selection mechanism all change, and the rendered markup is the contract |
| **Persistence** | `quarkus-hibernate-orm-panache` | `spring-boot-starter-data-jpa` | **The second high-risk row.** Same ORM underneath; the active-record statics on the entity have to become an explicit repository (step 06) |
| Transactions | `quarkus-narayana-jta`, pulled in transitively; `jakarta.transaction.Transactional` | `spring-tx`, inside the data-jpa starter; `org.springframework.transaction.annotation.Transactional` | E12. Same boundaries, different annotation package |
| Schema migration | `quarkus-flyway` | `org.flywaydb:flyway-core` | same scripts, same `db/migration` location, same run-at-start semantics |
| JDBC driver | `quarkus-jdbc-h2` | `com.h2database:h2` (runtime) | |
| Health | `quarkus-smallrye-health` → `/q/health` | `spring-boot-starter-actuator` → `/actuator/health` | capability maps, **path does not**. Deliberately outside the asserted contract: asserting either path would fingerprint one framework |
| Version-less webjar paths | `quarkus-webjars-locator` | `org.webjars:webjars-locator-lite` | Easy to miss, and it changes rendered markup: without it `/webjars/htmx.org/dist/htmx.min.js` 404s and the templates would have to carry the version |
| Front-end assets | `org.webjars.npm:htmx.org` 2.0.10, `todomvc-app-css` 2.4.3 | unchanged, same versions | framework-neutral |
| Tests | `quarkus-junit5`, `rest-assured` (no tests present) | **nothing** | G7 — the oracle is framework-neutral and lives outside the application |

## Roles absent from this application

Not given a dependency on either side, and named so the absence is a decision rather than an
oversight: **messaging** (B7), **scheduling**, **WebSocket**, **validation** (no bean-validated
input; the one constraint is a `not null` column), **security**, **JSON/REST API** (every
response is HTML or a redirect).

B7 and B8 are the paper's two designated high-risk rows. B7 (messaging) is **N/A** here.
B8 (persistence, "source-side derived-query abstractions must be re-expressed as explicit
code") applies **in the opposite direction from the paper's worked example**: it is the
*source* that hides its queries behind an abstraction, and the target that has to name them.
Panache's `Todo.list("completed=false", Sort.ascending(…))` is a string-typed query on the
entity; the target expresses the same query as a derived repository method.

## Deliberately not carried over

`quarkus-kubernetes` and `quarkus-container-image-jib` were removed from the benchmark
source variant itself, not by this migration — see the source pom's header. Manifest
generation and image building produce no observable behaviour, the benchmark's launch
contract is the Dockerfile, and `quarkus-kubernetes` alone pulls in several hundred fabric8
model artifacts that no gate exercises.
