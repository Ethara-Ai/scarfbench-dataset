# Migration changelog — `mongodb-crud`, Quarkus 3.9.3 → Spring Boot 3.3.4

Record of every action taken to produce `Î_{a,f_t}` from `I_{a,f_s}`, in the
form ScarfBench's single-prompt condition asks for (paper Appendix E) and
following the layer-by-layer, verification-driven workflow of Appendix G:
inspect source → migrate one layer → build/run → observe → patch → re-run.

Source pinned at `c5a6bc010e2f9694a28a261186bbb4a64178dba3`.

## 1. Analysis

Source is a single Maven module, one package `com.mongodb`, three main classes
(159 LOC) and two tests. Framework-specific surface: CDI (`@ApplicationScoped`,
`@Inject`), JAX-RS (`@Path`, `@GET`…, `@PathParam`, `@Consumes`/`@Produces`),
the Quarkus MongoDB client extension (native `MongoClient` + POJO codecs),
SmallRye OpenAPI, and the Quarkus build/packaging plugin.

Semantic assets to preserve: the `{id,name,age}` wire shape, the `/api` base
path and its five routes, the raw unquoted scalar bodies, the greeting literal,
the `test`/`persons` database and collection names, the stored document shape,
and the increment/count semantics of `PUT`/`DELETE`.

## 2. Dependency mapping (`pom.xml`)

| Quarkus | Spring Boot |
|---|---|
| `io.quarkus.platform:quarkus-bom:3.9.3` (import scope) | parent `spring-boot-starter-parent:3.3.4` |
| `quarkus-rest`, `quarkus-rest-jackson` | `spring-boot-starter-web` |
| `quarkus-mongodb-client` | `spring-boot-starter-data-mongodb` |
| `quarkus-arc` | (implicit — Spring's own container) |
| `quarkus-smallrye-openapi` | `springdoc-openapi-starter-webmvc-ui:2.6.0` |
| `quarkus-junit5`, `rest-assured` | `spring-boot-starter-test` (JUnit 5 + MockMvc) |
| `quarkus-maven-plugin` (`build`, `generate-code`) | `spring-boot-maven-plugin` (`repackage`) |
| `maven.compiler.release=21` | unchanged: `java.version`/`maven.compiler.release=21` |
| `native` profile | dropped — no native-image equivalent is in scope |

`<finalName>spring-mongodb-crud</finalName>` is set so the Dockerfile `CMD` has
a stable jar path (Quarkus produced `target/quarkus-app/quarkus-run.jar`;
Spring produces a single fat jar).

`settings.xml` pins a Google-hosted Maven Central mirror. Not part of the
migration proper: repo.maven.apache.org returns HTTP 429 to container traffic
from this network, which is the paper's `dependency resolution` build-failure
category. It is copied into the build image at `/root/.m2/settings.xml`.

## 3. Configuration mapping

`src/main/resources/application.properties`:

| Quarkus | Spring Boot | Intent |
|---|---|---|
| `quarkus.mongodb.connection-string=mongodb://localhost:27017` | `spring.data.mongodb.uri=mongodb://localhost:27017` | connection |
| hard-coded `getDatabase("test")` in the repository | `spring.data.mongodb.database=test` | database selection, externalized |
| (implicit 8080) | `server.port=8080` | HTTP binding — stated explicitly to avoid the paper's dominant port-mismatch deploy bug |
| (n/a) | `spring.data.mongodb.auto-index-creation=false` | Spring Data would otherwise create indexes the source never had |
| SmallRye default `/q/openapi` | `springdoc.api-docs.path=/v3/api-docs` | generated API document location (framework convention) |

The compose file overrides `SPRING_DATA_MONGODB_URI=mongodb://mongo:27017` and
`SPRING_DATA_MONGODB_DATABASE=test` for the containerized run; relaxed binding
maps those env vars onto the properties above.

## 4. Code mapping

### `PersonEntity`

Plain POJO → `@Document(collection = "persons")` with `@Id` on the `ObjectId
id` field. Public fields, both constructors, getters/setters, `hashCode` and
`equals` are carried over unchanged. `@JsonSerialize(using =
ToStringSerializer.class)` is **retained** — it is what renders `id` as a
24-char hex string in responses, and dropping it would emit Jackson's
structural view of `ObjectId` instead.

### `PersonRepository`

`@ApplicationScoped` + injected `MongoClient` + `MongoCollection<PersonEntity>`
→ `@Repository` + constructor-injected `MongoTemplate`. Method signatures are
unchanged.

| Source | Target |
|---|---|
| `coll.insertOne(p).getInsertedId().asObjectId().getValue().toHexString()` | `mongoTemplate.insert(p).getId().toHexString()` |
| `coll.find().into(new ArrayList<>())` | `mongoTemplate.findAll(PersonEntity.class)` |
| `coll.updateOne(eq("_id", new ObjectId(id)), inc("age",1)).getModifiedCount()` | `mongoTemplate.updateFirst(query(where("_id").is(new ObjectId(id))), new Update().inc("age",1), PersonEntity.class).getModifiedCount()` |
| `coll.deleteOne(eq("_id", new ObjectId(id))).getDeletedCount()` | `mongoTemplate.remove(query(where("_id").is(new ObjectId(id))), PersonEntity.class).getDeletedCount()` |

`new ObjectId(id)` still throws `IllegalArgumentException` on a malformed id,
which Spring surfaces as HTTP 500 — matching the source's 500.

### `PersonResource` → `PersonController`

`@Path("/api")` → `@RestController @RequestMapping(path = "/api", produces =
APPLICATION_JSON_VALUE)`; `@GET @Path("/hello")` → `@GetMapping("/hello")` and
so on; `@PathParam` → `@PathVariable`. Return types stay `String`,
`List<PersonEntity>` and `long`.

### `MongoConfiguration` (new)

`@Configuration` overriding the `mappingMongoConverter` bean with
`converter.setTypeMapper(new DefaultMongoTypeMapper(null))`. No source
counterpart; it exists purely to suppress a Spring Data default (see §5.2).

### `MongodbCrudApplication` (new)

`@SpringBootApplication` entry point. Quarkus needs no such class.

### Tests

`PersonResourceTest` (`@QuarkusTest` + REST Assured, asserts `GET /api/hello`
== 200 with body `Hello from Quarkus REST`) → `PersonControllerTest`
(`@WebMvcTest(PersonController.class)` + `MockMvc` + `@MockBean
PersonRepository`, same two assertions). The web slice needs no MongoDB, so the
unit test stays runnable in the build image without a database.

`PersonResourceIT` (`@QuarkusIntegrationTest`) has no Spring equivalent that
would add signal here — the packaged-artifact check it performs is covered by
Gate D, and the behavioral check by Gate T.

## 5. Behavior-preservation decisions

Three points where the idiomatic Spring default would have changed observable
behavior. Each was resolved before the first build, from the recorded source
contract.

1. **`consumes` is declared on the POST handler only.** JAX-RS `@Consumes` at
   class level is advisory for requests without a body; Spring's class-level
   `consumes` is enforced, so `PUT /api/person/{id}` — which the source accepts
   with no body and no `Content-Type` — would have returned **415** instead of
   **200**. This is the paper's `HTTP 4xx / assertion mismatch` test-phase
   failure.
2. **`_class` discriminator suppressed.** Spring Data MongoDB writes a `_class`
   type key into every document; the native driver's POJO codec does not.
   Without `MongoConfiguration`, persisted state would differ from the source's
   even though every HTTP response still matched.
3. **Raw scalar bodies.** `hello` and `createPerson` return `String`, the
   update/delete handlers return `long`. With `produces =
   application/json`, `StringHttpMessageConverter` writes the string bytes
   unquoted and Jackson writes the long bare — reproducing
   `Hello from Quarkus REST`, the bare hex id, and `1`/`0` exactly. Wrapping
   any of these in a DTO or `ResponseEntity<Map<…>>` would be more idiomatic
   Spring and a behavioral regression.

The greeting literal is *not* renamed: it is response payload, not framework
syntax.

## 6. Packaging

`Dockerfile`: `maven:3.9-eclipse-temurin-21`, installs `curl` (needed by the
compose healthcheck), copies `settings.xml` to `/root/.m2/`, copies `pom.xml`
and `src`, runs `mvn -B clean package -DskipTests`, exposes 8080, and runs
`java -jar /app/target/spring-mongodb-crud.jar`.

`docker-compose.yml`: `mongo` service (`mongo:7`, `mongosh` ping healthcheck)
and `app` service (`depends_on: mongo: condition: service_healthy`, curl
healthcheck against `/api/hello`, host port `${SCARF_HOST_PORT:-8080}:8080`).
The host port is parameterized so the stack can be brought up on a machine
where 8080 is already taken; the container port is always 8080.

## 7. Validation performed

| Step | Command | Result |
|---|---|---|
| Source contract capture | Quarkus jar on :8081 against `mongo:7`, curl every route incl. malformed id, unknown route, unsupported method, `/q/openapi` | contract table recorded in `TASK.md` |
| Oracle validity | oracle image vs containerized Quarkus variant | 17 passed |
| Compile (host, tests on) | `mvn -B -s settings.xml clean package` | BUILD SUCCESS |
| Compile + deploy (container) | `SCARF_HOST_PORT=18080 docker compose -p scarfmongo up -d --build` | both services healthy |
| Behavioral tests | oracle image on `scarfmongo_default`, `BASE_URL=http://app:8080/api` | 17 passed |
| Persisted-state parity | `mongosh --quiet test --eval 'printjson(db.persons.findOne())'` | `{_id, name, age}`, no `_class` |

## 8. Errors encountered and resolutions

None. Gates C, D and T passed on the first attempt; no build, deploy or test
failure was observed at any point during the migration.
