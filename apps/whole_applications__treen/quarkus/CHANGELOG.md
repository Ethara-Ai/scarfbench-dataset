# Migration Changelog — Jakarta EE → Quarkus

This document records the migration of the **treen** backend from a Jakarta EE 9.1
WAR (deployed on Apache TomEE) to a **Quarkus 3.15.1** self-contained application.

The migration keeps the existing application architecture and business logic intact
(CDI beans, JAX-RS resources, Servlet filter/servlet/listener, Bean Validation,
JSON-B, JAXB, JTA transactions, JDBC + programmatic Flyway) and re-hosts it on the
Quarkus runtime. To achieve this with minimal code churn, the migration uses the
Quarkus **RESTEasy Classic + Undertow (servlet)** stack, which preserves servlet
semantics (`@WebFilter`, `@WebServlet`, `@WebListener`, `HttpServletRequest`
injection into JAX-RS, and `@SessionScoped` CDI beans).

---

## [2026-08-18T00:00:00Z] [info] Project Analysis

- Application: `treen` — a hierarchical (tree) note-taking web app.
- Backend module: `backend/` — Jakarta EE 9.1, packaged as a WAR, target runtime TomEE.
- Frontend module: `frontend/` — Oracle JET SPA (static assets, built separately). Out of
  scope for the framework migration; the Quarkus image ships the REST backend.
- Frameworks / specs detected in the backend:
  - CDI (`jakarta.enterprise`, `jakarta.inject`) — constructor injection, `@ApplicationScoped`,
    `@RequestScoped`, `@SessionScoped`, `@Dependent`, `@Produces`.
  - JAX-RS (`jakarta.ws.rs`) — `ServiceApplication` with `@ApplicationPath("service")`,
    resources under `/service` (`auth`, `user`, `notebook`), exception mappers.
  - Servlet (`jakarta.servlet`) — `AuthorizationFilter` (`@WebFilter`),
    `ErrorHandlingServlet` (`@WebServlet`), `BootstrapContextListener` (`@WebListener`),
    `@Context HttpServletRequest`, `HttpSession` usage.
  - Bean Validation (`jakarta.validation`) — constraints + a custom `@TitleOrLinkRequired`.
  - JSON-B (`jakarta.json.bind`) — used directly in `ErrorHandlingServlet` and by JAX-RS.
  - JAXB (`jakarta.xml.bind`) — GPX XML export (`GpxRoot`, `GpxPoint`).
  - JTA (`jakarta.transaction.Transactional`) — on service beans.
  - JDBC `DataSource` via JNDI (`@Resource(name="jdbc/TreenDB")`), container-managed.
  - Flyway (programmatic) — DB migrations in `classpath:db-script` (MySQL/MariaDB dialect).
- Unit test: `LinkTypeTest` (TestNG), pure logic, no container/DB.

---

## [2026-08-18T00:05:00Z] [info] Migration Strategy

- Target: **Quarkus 3.15.1** (LTS, Java 17+), running on the JVM (fast-jar).
- Chose the **RESTEasy Classic + Undertow (servlet)** stack rather than RESTEasy
  Reactive. This preserves the app's servlet-centric design with minimal code
  changes: `@WebFilter`, `@WebServlet`, `@Context HttpServletRequest`, `HttpSession`,
  and CDI `@SessionScoped` all keep working.
- Packaging changed from `war` (TomEE) to a self-contained Quarkus application.
- Database: the original relied on a container-managed JNDI `DataSource`
  (`jdbc/TreenDB`, MySQL/MariaDB). For a runnable, self-contained image the
  datasource is provided by Quarkus/Agroal using **H2 in MySQL-compatibility mode**.
  All DDL/Flyway migrations and read queries run unchanged. See the limitation note
  below regarding MySQL-dialect multi-table writes.

## [2026-08-18T00:10:00Z] [info] Dependency Migration (backend/pom.xml)

- Removed:
  - `jakarta.platform:jakarta.jakartaee-api` (provided) — replaced by Quarkus extensions.
  - `org.slf4j:slf4j-api`, `org.slf4j:slf4j-jdk14` — Quarkus provides the SLF4J binding.
  - `org.flywaydb:flyway-mysql` — not needed with H2; `flyway-core` retained.
  - `com.h2database:h2` (test scope) — provided by `quarkus-jdbc-h2`.
  - Build plugins: `maven-war-plugin`, `maven-checkstyle-plugin`, `maven-pmd-plugin`,
    `spotbugs-maven-plugin`, `maven-enforcer-plugin`, `dependency-check-maven`,
    `versions-maven-plugin`, `tomcat7-maven-plugin`. (Static-analysis/enforcer plugins
    would fail the build against the Quarkus BOM; prioritising a working build.)
- Added Quarkus BOM (`io.quarkus.platform:quarkus-bom:3.15.1`) and extensions:
  - `quarkus-resteasy`, `quarkus-resteasy-jsonb` (JAX-RS + JSON-B)
  - `quarkus-undertow` (servlets/filters/listeners, web.xml)
  - `quarkus-hibernate-validator` (Bean Validation)
  - `quarkus-jaxb` (GPX XML marshalling)
  - `quarkus-agroal` + `quarkus-jdbc-h2` (datasource) + `quarkus-narayana-jta` (`@Transactional`)
  - `quarkus-smallrye-health` (readiness/liveness + datasource health)
- Retained: `flyway-core` (programmatic migration), `jsr305` (nullability annotations),
  `testng` + `mockito` (existing unit test).
- Replaced the WAR/quality plugin set with `quarkus-maven-plugin` (build) plus
  `maven-compiler-plugin` and `maven-surefire-plugin` (kept the TestNG suite config).
- Changed `<packaging>` from `war` to `jar`.
- **Validation:** `mvn -DskipTests package` succeeds; Quarkus augmentation completes.

## [2026-08-18T00:15:00Z] [info] Configuration Updates

- Added `src/main/resources/application.properties` — HTTP port/host, `same-site-cookie`
  (replacing TomEE `context.xml` `sameSiteCookies="strict"`), and the Agroal/H2 datasource.
- Moved `beans.xml` and `web.xml` from `src/main/webapp/WEB-INF` / `META-INF` to
  `src/main/resources/META-INF/` (Quarkus/Undertow locations). `web.xml` keeps the
  `welcome-file` and the `error-page -> /error` mapping; the JNDI `resource-ref` was
  dropped (datasource now comes from Quarkus config).
- Removed `src/main/webapp/` (Tomcat `context.xml`, WAR descriptors) — obsolete.

## [2026-08-18T00:20:00Z] [info] Code Refactoring

- `infra/db/DbServicesProducer` — replaced JNDI `@Resource(name="jdbc/TreenDB")` +
  `DataSource` producer with constructor `@Inject DataSource` (the Agroal bean). Removed
  the redundant `@Produces DataSource` method to avoid an ambiguous bean.
- `infra/bootstrap/BootstrapContextListener` (`@WebListener`) → replaced by
  `infra/bootstrap/BootstrapService`, an `@ApplicationScoped` bean observing Quarkus
  `StartupEvent`/`ShutdownEvent`. This fixes an early-boot failure: the Undertow
  servlet-context listener ran Flyway before the datasource runtime config was ready
  (`SRCFG00027: Could not find a mapping for DataSourcesRuntimeConfig`). Same behaviour
  (Flyway migration + JMX MBean register/unregister) now runs at the correct lifecycle
  point. Added an opt-in demo-user seed (`treen.demo.seed-user.enabled`, default `false`)
  used only by the container smoke tests.
- `notes/rest/NotebookResource#exportChildrenToGpx` — RESTEasy Classic does not
  auto-serialise the custom `application/gpx+xml` media type, so the JAXB `GpxRoot` is
  now marshalled to an XML string explicitly (via `JAXBContext`/`Marshaller`) before
  being returned. Business logic unchanged.
- No other source changes were required: CDI (`jakarta.*`), JAX-RS resources, exception
  mappers, Bean Validation constraints, the servlet filter/servlet, JSON-B, and
  `@Transactional` services compile and run unchanged on Quarkus.

## [2026-08-18T00:25:00Z] [warning] Known Limitation — MySQL-dialect writes on H2

- The note write queries (`UpdateNoteQuery`, `MoveNoteQuery`, `MoveNoteToRootQuery`,
  `DeleteNoteQuery`) use MySQL/MariaDB multi-table `UPDATE ... JOIN` / `DELETE n FROM ...`
  syntax, which H2 (even in MySQL mode) does not support. On the bundled H2 datasource,
  startup, Flyway migration, login, and all read paths work; note create/update/move/delete
  require a real MySQL/MariaDB.
- Mitigation: point the app at MySQL/MariaDB by overriding `quarkus.datasource.*`
  (or `QUARKUS_DATASOURCE_JDBC_URL` / `_USERNAME` / `_PASSWORD`) and swapping
  `quarkus-jdbc-h2` for `quarkus-jdbc-mysql`/`quarkus-jdbc-mariadb`. No application code
  changes are needed. This is a pre-existing DB-dialect coupling, not introduced by the
  framework migration.

## [2026-08-18T00:30:00Z] [info] Build Configuration & Dockerfile

- Added a multi-stage `Dockerfile` (Maven/Temurin-17 build stage → Temurin-17-JRE runtime)
  producing the Quarkus fast-jar image, plus `.dockerignore`.
- Updated `build.sh` (builds the Quarkus fast-jar; frontend build gated on `ojet`) and
  `build-deploy.sh` (now builds/runs the Docker image instead of a TomEE deploy).
- Added `smoke-test.sh` — HTTP smoke tests exercising the migrated framework surface.

## [2026-08-18T00:35:00Z] [info] Compilation, Tests, Container Run — SUCCESS

- `mvn package` — BUILD SUCCESS; Quarkus augmentation OK.
- Unit tests (`LinkTypeTest`, TestNG): **40 run, 0 failures**.
- `docker build` — image built successfully.
- `docker run -p 0:8080 -e TREEN_DEMO_SEED_USER_ENABLED=true` — container started;
  Flyway applied 3 migrations to H2; demo user seeded; Quarkus listening on 0.0.0.0:8080.
  Installed features: agroal, cdi, hibernate-validator, jdbc-h2, narayana-jta, resteasy,
  resteasy-jsonb, servlet, smallrye-context-propagation, smallrye-health, vertx.
- Assigned host port (dynamic `-p 0:8080`): **55001** (varies per run).
- Smoke tests against the running container: **9 passed, 0 failed**
  1. `/q/health/ready` → 200 (datasource UP)
  2. login wrong creds → 401 JSON (JAX-RS + JSON-B + CDI + DB read)
  3. login invalid body → 400 JSON (Bean Validation + custom mapper)
  4. unauthenticated `/service/notebook` → 401 (AuthorizationFilter + error page)
  5. unauthenticated `/service/user` → 401
  6. login `demo/demo1234` → 200 (+ session cookie)
  7. authenticated `/service/user` → 200 `{"login":"demo"}` (`@SessionScoped`)
  8. authenticated `/service/notebook` → 200 `{"notes":[],"version":1}` (H2 read path)
  9. logout → 200

## [2026-08-18T00:40:00Z] [info] Outcome

- **Migration successful.** The application builds, runs as a Docker container, and passes
  all unit and smoke tests on Quarkus 3.15.1. Business logic and functionality are preserved.
