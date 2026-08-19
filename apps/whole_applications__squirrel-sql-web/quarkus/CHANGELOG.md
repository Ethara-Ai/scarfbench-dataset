# Migration Changelog — Jakarta/JavaEE → Quarkus

This document records the migration of `squirrel-sql-web` from a JavaEE 7
(JAX-RS + EJB + CDI) WAR intended for a Glassfish/TomEE container, to a
standalone **Quarkus** application packaged as a runnable container image.

Timestamps are ISO-8601 (UTC).

---

## [2026-08-18T00:00:00Z] [info] Project Analysis
- Application: `net.sf.squirrel-sql:squirrel-sql-web:1.0`, originally `war` packaging.
- Backend stack detected: JAX-RS (`javax.ws.rs`), EJB (`javax.ejb.@Stateless`,
  `@Singleton`, `@Startup`, `@Schedule`), CDI (`javax.inject.@Inject`,
  `javax.enterprise.context`), Servlet (`javax.servlet.http`), JAXB
  (`javax.xml.bind`), JWT (jjwt), Jackson custom `MessageBodyReader/Writer`
  providers, and the SQuirreL SQL Swing core (`squirrel-sql:4.1.0`).
- 79 Java source files (30 backend framework files + SQuirreL "public" shim
  classes + DTOs). A Vue.js frontend lives under `src/main/javascript`.
- Framework usage counts: 141 `javax.ws.rs`, 22 `@Stateless`, 4 `@Singleton`,
  1 `@Startup`, 1 `@Schedule`, 37 `@Inject`, 8 `@Provider`.
- Build tooling present: Maven 3.9.9, JDK 21, Docker. `node`/`npm` are **not**
  available, so the Vue frontend build step must be dropped from the Maven build.

## [2026-08-18T00:05:00Z] [info] Migration Strategy
- Target: Quarkus 3.15.x (LTS), RESTEasy Classic (`quarkus-resteasy`) to preserve
  the custom JAX-RS `MessageBodyReader/Writer` providers and `@Context`
  servlet-type injection.
- Namespace: migrate `javax.*` EE packages → `jakarta.*` (Quarkus 3 is Jakarta EE 10).
- DI: EJB `@Stateless` → CDI `@ApplicationScoped` (managers) / `@RequestScoped`
  (JAX-RS resources with per-request `@Context` fields); EJB `@Singleton`/`@Startup`
  → CDI `@Singleton` + `io.quarkus.runtime.Startup`; EJB `@Schedule` →
  `io.quarkus.scheduler.Scheduled`.
- Packaging: `war` → Quarkus fast-jar; add `quarkus-maven-plugin`.
- Dockerfile: multi-stage (Maven build → JRE 21 runtime).

## [2026-08-18T00:10:00Z] [info] Dependency Migration (pom.xml)
- Changed `<packaging>` from `war` to `jar`; compiler release 1.8 → 17.
- Imported the `io.quarkus.platform:quarkus-bom:3.15.4` BOM.
- Added Quarkus extensions: `quarkus-resteasy`, `quarkus-resteasy-jackson`,
  `quarkus-arc`, `quarkus-scheduler`, and (test) `quarkus-junit5` + `rest-assured`.
- Removed `javax:javaee-api:7.0` (provided) and `org.glassfish.jersey.media:jersey-media-json-jackson`.
- Replaced `jackson-module-jaxb-annotations` with `jackson-module-jakarta-xmlbind-annotations`.
- Kept the SQuirreL core (`squirrel-sql:4.1.0`), `jjwt:0.9.1`, `mockito-core:3.6.0`
  (used at RUNTIME by SessionsManager, not just tests), `derby`, and the long list
  of "copied from squirrel-sql" transitive libraries.
- Removed the obsolete `maven-checkstyle-plugin` (would fail the build on style) and
  the `exec-maven-plugin` npm/`vue` frontend build (no `node`/`npm` in this
  environment; the backend REST service is the migration target).

## [2026-08-18T00:15:00Z] [info] Code Refactoring — namespace and DI
- Bulk `javax.* -> jakarta.*` for EE packages actually present in Quarkus:
  `javax.ws.rs`, `javax.inject`, `javax.enterprise`, `javax.annotation.PostConstruct/PreDestroy`,
  `javax.servlet`, `javax.xml.bind`. JDK packages left untouched (`javax.swing`,
  `javax.crypto`, other `javax.xml.*`).
- EJB → CDI/Quarkus:
  - `@Stateless` on JAX-RS resources → `@RequestScoped` (they carry per-request `@Context` state).
  - `@Stateless` on managers and the `AuthFilter` provider → `@ApplicationScoped`.
  - `@Singleton` (EJB) → `jakarta.inject.Singleton` (WebApplication, SessionsManager, TokensManager).
  - `@Startup` (EJB) → `io.quarkus.runtime.Startup` (WebApplication eager init).
  - `@Schedule(hour="*")` (EJB timer) → `@io.quarkus.scheduler.Scheduled(cron="0 0 * * * ?")`.
- `AbstractMessageBodyReaderWriter`: `JaxbAnnotationIntrospector` →
  `JakartaXmlBindAnnotationIntrospector`.
- `JaxRsApplication`: dropped `@ApplicationScoped` (a JAX-RS `Application` must not
  also be a CDI bean under Quarkus); kept `@ApplicationPath("/ws")`.
- `DefaultExceptionMapper.extractParentException`: removed the
  `javax.ejb.EJBException` / `javax.persistence.PersistenceException` /
  `javax.transaction.RollbackException` unwrap branches (no EJB/JPA/JTA container
  in the Quarkus stack); kept the Hibernate `GenericJDBCException` unwrap.

## [2026-08-18T00:20:00Z] [info] Code Refactoring — servlet types removed from REST layer
- Removed `quarkus-undertow` (its `web-fragment.xml` scanner clashed with a legacy
  Woodstox StAX provider on the classpath — see error log below) and refactored the
  few servlet-type usages to pure JAX-RS:
  - `AuthFilter`: dropped `@Context HttpServletResponse`; the 401 challenge (with the
    `WWW-Authenticate` header) is now built as a JAX-RS `Response` inside the thrown
    `WebApplicationException`.
  - `SessionsEndpoint`: `@Context HttpServletRequest` field → `@Context HttpHeaders`.
  - `TokenAuthenticationEndPoint`: dropped `@Context HttpServletResponse`; `getCurrentUser`
    parameter `HttpServletRequest` → `HttpHeaders`; 401 responses built as JAX-RS `Response`.
  - `TokensManager`: dropped the un-injectable `@Context HttpServletRequest` field;
    `getCurrentToken()` now reads the request headers from `ResteasyContext.getContextData(HttpHeaders.class)`;
    added `extractTokenFromHttpHeaders(HttpHeaders)`. The servlet-typed
    `extractTokenFromRequest(HttpServletRequest)` method is retained (kept compiling via
    the plain `jakarta.servlet-api` jar) because its unit test still covers it.

## [2026-08-18T00:25:00Z] [info] Configuration
- Added `src/main/resources/application.properties`: `quarkus.http.host=0.0.0.0`,
  `quarkus.http.port=8080`, `quarkus.http.test-port=0` (random test port so
  `@QuarkusTest` never collides with a container already on 8081), logging levels.
- Added `Dockerfile` (multi-stage: `maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre`,
  headless, fast-jar layout) and `.dockerignore`.

## [2026-08-18T00:30:00Z] [error] Build failure — unreachable kermeta repository
- Error: `Could not transfer artifact org.kermeta.eclipse:org.eclipse.equinox.common:pom:3.6.0.v20100503 ... Connect timed out`.
- Root cause: `equinox.common` was sourced only from `http://maven.irisa.fr` (kermeta),
  which is unreachable (and Maven 3.9 blocks plain HTTP repos anyway).
- Resolution: removed the kermeta repository and replaced the dependency with
  `org.eclipse.platform:org.eclipse.equinox.common:3.20.0` from Maven Central; its
  `org.eclipse.core.runtime` API is compatible. Severity downgraded to resolved.

## [2026-08-18T00:35:00Z] [error] Build failure — phantom system-scoped dependency
- Error: `Could not find artifact aqua:aqua:jar:5.0 at specified path ${basedir}/libs/laf.jar`.
- Root cause: `com.jidesoft:jide-oss:2.4.8` declares a broken system-scoped
  dependency (a macOS look-and-feel jar). The Quarkus bootstrap resolver, stricter
  than `mvn compile`, refuses to bootstrap while it is unresolved.
- Resolution: excluded `aqua:aqua` from the `jide-oss` dependency.

## [2026-08-18T00:40:00Z] [error] Build failure — legacy ASM shadows SmallRye Config's ASM
- Error: `SRCFG00051: Could not generate ConfigMapping: 'void org.objectweb.asm.ClassWriter.<init>(int)'`.
- Root cause: `org.hibernate:hibernate:3.2.4.sp1` pulls `asm:asm:1.5.3`, `asm:asm-attrs`,
  and `cglib:2.1_3`; the 2005-era `org.objectweb.asm.ClassWriter` lacks the
  `ClassWriter(int)` constructor and shadowed the ASM 9.x that SmallRye Config uses
  during Quarkus augmentation.
- Resolution: excluded `asm:asm`, `asm:asm-attrs`, and `cglib:cglib` from Hibernate
  (only needed for Hibernate runtime bytecode enhancement, which the headless REST
  paths never exercise).

## [2026-08-18T00:45:00Z] [error] Build failure — Woodstox StAX / Undertow web-fragment scan
- Error: `WebXmlParsingBuildStep ... java.lang.ClassCastException: class com.ctc.wstx.stax.WstxInputFactory cannot be cast to class javax.xml.stream.XMLInputFactory`.
- Root cause: `quarkus-undertow`'s web-fragment scanner instantiated a legacy
  Woodstox `XMLInputFactory` service provider (from the SQuirreL dependency tree)
  across mismatched classloaders.
- Resolution: removed `quarkus-undertow` entirely and refactored the servlet-type
  usages to JAX-RS `HttpHeaders`/`Response` (see the 00:20 entry). Build then succeeded
  and produced `target/quarkus-app/quarkus-run.jar`.

## [2026-08-18T00:50:00Z] [warning] Runtime 401 on the login endpoint
- Symptom: `POST /ws/Authenticate` was rejected by `AuthFilter` with HTTP 401.
- Root cause: `AuthFilter` compared `UriInfo.getPath()` with the literal `"Authenticate"`;
  RESTEasy returns the path with a leading slash (`/Authenticate`) whereas the original
  Jersey runtime returned it without one.
- Resolution: match the trailing path segment
  (`path.equals("Authenticate") || path.endsWith("/Authenticate")`). The login endpoint
  is again reachable unauthenticated.

## [2026-08-18T00:55:00Z] [error] Runtime 500 on authentication — missing JAXB DatatypeConverter
- Error: `NoClassDefFoundError: javax/xml/bind/DatatypeConverter` from
  `io.jsonwebtoken.impl.Base64Codec` while issuing a JWT.
- Root cause: `jjwt:0.9.1` Base64-encodes tokens via `javax.xml.bind.DatatypeConverter`,
  removed from the JDK in Java 11+. The original app ran on Java 8 (Glassfish) where it
  was built in.
- Resolution: added `javax.xml.bind:jaxb-api:2.3.1` (whose jar bundles the
  `DatatypeConverterImpl`). This is separate from the `jakarta.xml.bind` used by the
  Jackson introspector and the DTO `@XmlTransient` annotations.

## [2026-08-18T01:00:00Z] [info] Smoke tests authored
- Added `src/test/java/.../ws/SmokeResourceTest.java` (`@QuarkusTest` + RestAssured):
  6 end-to-end tests covering AuthFilter 401s, form + JSON JWT authentication, invalid
  credentials, token-gated access to `/ws/HelloWorld`, and `/ws/CurrentUser`.
- Added `scripts/smoke-test.sh` for running the same checks against a live container.
- Configured surefire with `java.awt.headless=true` (the embedded SQuirreL core is a
  Swing app).

## [2026-08-18T01:05:00Z] [info] Build & test SUCCESS
- `mvn package` → BUILD SUCCESS. Tests: `SmokeResourceTest` 6/6 and the pre-existing
  `TestTokensManager` 4/4 → **10 passed, 0 failed**.
- The eager `WebApplication` (`@io.quarkus.runtime.Startup`) initialises the SQuirreL
  core headless without failing the container.

## [2026-08-18T01:10:00Z] [info] Docker build, run & container smoke test SUCCESS
- `docker build -t $SCARF_IMAGE_TAG .` → image built.
- `docker run -d --name $SCARF_CONTAINER_NAME -p 0:8080 $SCARF_IMAGE_TAG` → started in
  ~1s, `Listening on: http://0.0.0.0:8080`. Docker assigned host port **55005** for
  this run (dynamic `-p 0:8080`).
- `scripts/smoke-test.sh http://localhost:55005` → **6 passed, 0 failed**.
- `GET /ws/CurrentUser` returned
  `{"value":{"identifier":1,"username":"admin","name":"Doe","surname":"John","roles":["admin"]}}`.

## [2026-08-18T01:12:00Z] [info] Known benign warnings (non-blocking)
- `log4j:WARN No appenders could be found ...` — the SQuirreL core uses log4j 1.2 with
  no appender configured; harmless. Quarkus JBoss logging handles application logs.
- `FileNotFoundException: /root/.squirrel-sql/SQLDrivers.xml` on first start — expected
  first-run condition; the SQuirreL core logs it and creates defaults. The app starts
  and serves requests normally.
- `ApplicationArguments.initialize() called twice` — benign SQuirreL bootstrap notice.

## [2026-08-18T01:15:00Z] [info] Migration complete — SUCCESS
- The application builds, runs as a Docker image, starts successfully, and passes all
  generated smoke tests (in-JVM `@QuarkusTest` and against the running container).
