# CHANGELOG — Quarkus → Spring Boot Migration

Migration of `otel-example-quarkus` (Quarkus 3.36.3) to Spring Boot 3.5.3, performed
following the verification-driven migration workflow prescribed by the SCARFBENCH
research paper (inventory → build migration → dependency mapping → config translation
→ code transforms → behavioral validation). Goal: **behavioral equivalence at the
external boundary** (HTTP routes, response payloads, status codes, persistent state,
telemetry signals) — not syntactic translation.

## Summary of File Changes

### Added
| File | Purpose |
|---|---|
| `src/main/java/.../Application.java` | Spring Boot entrypoint (`@SpringBootApplication`) |
| `src/main/java/.../config/OpenTelemetryConfig.java` | Exposes OTel `Meter` bean (from autoconfigured `OpenTelemetry`) for `UserService` custom metrics |
| `src/main/java/.../controller/UserController.java` | Replaces `resource/UserResource.java` (JAX-RS → Spring MVC) |
| `src/test/java/.../controller/UserControllerTest.java` | Replaces `resource/UserResourceTest.java` (`@QuarkusTest` → `@SpringBootTest(RANDOM_PORT)` + REST Assured) |
| `src/test/java/.../controller/UserControllerUnitTest.java` | Replaces `resource/UserResourceUnitTest.java` (jakarta `Response` → `ResponseEntity` assertions) |
| `src/test/resources/application.properties` | Test profile config (H2, `otel.sdk.disabled=true`) — replaces Quarkus `%test` profile |
| `CHANGELOG.md` | This file |

### Modified
| File | Change |
|---|---|
| `pom.xml` | Quarkus BOM + quarkus-maven-plugin → `spring-boot-starter-parent:3.5.3` + `spring-boot-maven-plugin`; full dependency mapping (below); artifactId `otel-quarkus-crud` → `otel-spring-crud`; removed native profile, failsafe, jboss-logmanager surefire config; kept compiler `-parameters`, JaCoCo rules (LINE 0.80 / BRANCH 0.75), Sonar, Spotless (AOSP) |
| `src/main/java/.../model/User.java` | Dropped `PanacheEntity` base class → explicit `@Id @GeneratedValue(AUTO) public Long id`; all validation/column annotations, public fields, `equals`/`hashCode`/`toString` semantics preserved |
| `src/main/java/.../repository/UserRepository.java` | `@ApplicationScoped implements PanacheRepository<User>` → `interface extends JpaRepository<User, Long>`; Panache queries → derived queries + `@Query` JPQL; same method contracts (`findByEmail`, `searchByName`, `findRecentUsers`, `existsByEmail`, `existsByEmailAndIdNot`, `countUsers`, `deleteUser`) |
| `src/main/java/.../service/UserService.java` | `@ApplicationScoped` → `@Service`; `@Inject` → constructor injection; jakarta → Spring `@Transactional`; JBoss Logger → SLF4J; Panache calls → JpaRepository (`listAll`→`findAll`, `findByIdOptional`→`findById`, `persist`→`save`); all OTel custom metrics (`users.created.total`, `users.errors.total`, `users.total` gauge, `user.search.duration` histogram), `@WithSpan`/`@SpanAttribute` annotations, span attributes, and exception messages preserved verbatim |
| `src/main/resources/application.properties` | `quarkus.*` → Spring equivalents preserving intent: port 8080/0.0.0.0, same H2 URL + credentials, `create-drop` DDL (auto-runs `import.sql`), OTLP grpc `localhost:4317`, same `service.name`/resource attributes, trace-correlated log pattern, Actuator base path `/q` (preserves `/q/health`), springdoc at `/q/openapi` + `/q/swagger-ui`; prod profile (`#---` multi-doc) with MySQL via `DB_*` env vars |
| `src/test/java/.../repository/UserRepositoryTest.java` | `@QuarkusTest` → `@DataJpaTest`; same 14 behavioral assertions |
| `src/test/java/.../service/UserServiceTest.java` | `@QuarkusTest` + `@InjectMock` → `@SpringBootTest` + `@MockitoBean`; same 15/16 tests |
| `src/test/java/.../service/UserServiceUnitTest.java` | Pure Mockito test retained; repo method names remapped; same 23 tests |
| `Dockerfile` | Runtime copies Boot fat jar (`target/otel-spring-crud-*.jar` → `app.jar`); same multi-stage base images, non-root user, `HEALTHCHECK` on `/q/health` |
| `docker-compose.yml` | App env `QUARKUS_*` → `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_*`, `OTEL_EXPORTER_OTLP_*`; container renamed `spring-otel-app`; observability stack unchanged |
| `Makefile` | Comment updated (H2 via Spring test properties) |
| `README.md` | All Quarkus references updated to Spring Boot equivalents |

### Deleted
| File | Reason |
|---|---|
| `src/main/java/.../resource/UserResource.java` | Replaced by `controller/UserController.java` |
| `src/test/java/.../resource/UserResourceTest.java` | Replaced by `controller/UserControllerTest.java` |
| `src/test/java/.../resource/UserResourceUnitTest.java` | Replaced by `controller/UserControllerUnitTest.java` |
| `Dockerfile.native` | GraalVM native build is Quarkus-specific (stale artifact) |
| `docker-compose.native.yml` | Depended on `Dockerfile.native` (stale artifact) |

Unchanged (semantic assets preserved as-is): `src/main/resources/import.sql` (5 seed
users + sequence restart), `src/test/java/.../model/UserTest.java` (framework-free),
`config/` observability stack, `LoadTest.java`, `sonar-project.properties`, `LICENSE`.

## Dependency Mapping

| Quarkus | Spring Boot |
|---|---|
| `quarkus-rest`, `quarkus-rest-jackson` | `spring-boot-starter-web` |
| `quarkus-arc` (CDI) | Spring DI (built-in) |
| `quarkus-hibernate-orm-panache` | `spring-boot-starter-data-jpa` |
| `quarkus-jdbc-h2` | `com.h2database:h2` (runtime) |
| `quarkus-jdbc-mysql` | `com.mysql:mysql-connector-j` (runtime) |
| `quarkus-hibernate-validator` | `spring-boot-starter-validation` |
| `quarkus-smallrye-health` | `spring-boot-starter-actuator` |
| `quarkus-opentelemetry` | `io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter` |
| `quarkus-smallrye-openapi` | `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6` |
| `quarkus-logging-json` | dropped — JSON console format is not part of the external behavioral boundary; trace-correlated pattern preserved via `logging.pattern.console` |
| `quarkus-junit5`, `quarkus-junit5-mockito`, `quarkus-test-h2` | `spring-boot-starter-test` |
| `rest-assured` (Quarkus-managed) | `rest-assured` (Boot-managed) |

Security pin preserved: `opentelemetry-bom:1.62.0` is still imported **before** the
instrumentation BOM in `dependencyManagement` (CVE-2026-45292 override from the
original pom). `opentelemetry-instrumentation-bom:2.28.1` was chosen because it
aligns exactly with API/SDK 1.62.0 (see errors below).

## Chronological Log (actions, errors, resolutions)

1. **Inventory**: read all main/test sources, config, Docker assets; built
   dependency-role table (REST, DI, persistence, validation, health, telemetry,
   OpenAPI, tests) per the paper's Step 1.
2. **Build migration**: rewrote `pom.xml` (Quarkus BOM/plugin → Boot parent/plugin).
3. **Code transforms**: entity, repository, service, controller as summarized above.
4. **Config translation**: `quarkus.*` → Spring/otel starter properties, preserving
   port, datasource, DDL mode, OTLP endpoint/protocol, service name, `/q/*` paths.
5. **Test migration**: 6 test classes → Spring equivalents (115 tests total).
6. **ERROR — Maven Central rate limiting (HTTP 429)** on
   `repo.maven.apache.org` for `spring-boot-starter-parent:3.5.3`.
   **Resolution**: builds run with a settings mirror pointing at
   `maven-central.storage-download.googleapis.com/maven2` (GCS mirror of Central).
   Environment-only workaround; no repo files affected.
7. **Build gate**: `mvn clean package` → BUILD SUCCESS, 115/115 tests green.
8. **ERROR — startup crash**: `ClassNotFoundException:
   io.opentelemetry.sdk.autoconfigure.internal.ComponentLoader` —
   `opentelemetry-spring-boot-starter:2.16.0` requires a newer SDK than the pinned
   `opentelemetry-bom:1.62.0`. **Resolution**: bumped instrumentation BOM to
   `2.28.1`, which manages exactly API/SDK 1.62.0, keeping the CVE pin intact.
9. **Behavioral validation run 1** (JAR + curl oracle): 14 of 16 checks passed.
   Two defects found:
   - **DEFECT — negative generated IDs** (`POST` created `id=-43`): Quarkus/Panache
     configures Hibernate's `pooled-lo` sequence optimizer by default, Spring Boot
     leaves Hibernate's default `pooled`, so `import.sql`'s
     `ALTER SEQUENCE users_SEQ RESTART WITH 6` produced `6 − 50 + 1 = −43`.
     **Resolution**: `spring.jpa.properties.hibernate.id.optimizer.pooled.preferred=pooled-lo`.
   - **DEFECT — entire application.properties silently ignored** (`/q/health`,
     `/q/openapi`, `/q/swagger-ui` 404; OTLP fell back to `http/protobuf :4318`;
     log pattern and app name not applied). Root cause isolated by bisection: in
     Spring Boot 3.5 multi-document properties files, a **comment line placed
     immediately after the `#---` document separator invalidates the whole file**
     (no error is logged). **Resolution**: moved
     `spring.config.activate.on-profile=prod` to the first line after `#---`.
10. **Behavioral validation run 2**: all checks pass — seeded data (5 users,
    `john.doe@example.com` …), `POST` → `201` with `id=6`, `PUT` → `200`,
    `DELETE` → `204`, duplicate email → `400 "Email already exists: …"`,
    missing user → `404 "User not found with id: …"`, search/recent/count
    contracts, bean-validation `400`s, `/api/users/health` `{status:UP,
    service:UserService}`, `/q/health` `200 UP` (db + ping), `/q/openapi` `200`,
    `/q/swagger-ui` `200`, OTLP exporting via **grpc** to `localhost:4317`
    (connection refused expected without a collector), trace-correlated log
    pattern active (`traceId=…, spanId=…`).
11. **Docker gate**: image built (multi-stage), container ran with Docker
    `HEALTHCHECK` reaching **healthy**; API served seed data from the container.
    (Build performed with the GCS mirror settings due to error 6; the committed
    `Dockerfile` keeps standard Maven Central.)
12. **Docs**: README migrated; this CHANGELOG written.

## Unresolved Issues / Notes

- **JSON console logging** (`quarkus-logging-json`) was intentionally dropped; add
  a logback JSON encoder if structured console output is required for Loki.
- **`docker-compose.app-only.yml`** referenced by the original README never existed
  in the repo; the stale reference was removed from the README.
- **`/q/health/live` & `/q/health/ready`** (Quarkus sub-probes) are not mapped;
  Actuator's `/q/health` covers the Docker/K8s health checks in this repo. Enable
  `management.endpoint.health.probes.enabled=true` if separate probes are needed.
- **CI workflow** (`.github/workflows/ci.yml`) is framework-agnostic (plain
  `mvn spotless:check` / `test` / `package`, Docker build, and integration curls
  against `/q/health` and `/api/users` — all preserved by this migration) and needs
  no functional changes. Only the Docker Hub image tags
  (`otel-crud-api-quarkus`) still carry the old name (cosmetic).
- Docker builds may hit Maven Central HTTP 429 rate limiting on some networks; use
  a Central mirror in `~/.m2/settings.xml` if so (see log entry 6).
