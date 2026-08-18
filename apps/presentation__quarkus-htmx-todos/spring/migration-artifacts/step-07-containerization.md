# Step 07 — Containerize (Gate F)

The `CMD` is the deploy gate: it is the command the harness uses to start the application, so
it defines what "deploys" means operationally.

| | Source variant | Target variant |
|---|---|---|
| Base image (F1) | `maven:3.9.12-ibm-semeru-21-noble` | same |
| Smoke-test runtime (F2) | `curl`, `lsof`, `pytest` in `/opt/venv` | same |
| Build at image-build time (F3) | `RUN mvn clean install -DskipTests` | same |
| `EXPOSE` (F4) | none | none |
| Host port mapping (F5) | none — `docker run` without `-p` | none |
| `CMD` (F6) | `["mvn", "quarkus:run"]` | `["mvn", "spring-boot:run"]` |
| Readiness pattern (F7) | `started in.*Listening on:` | `Tomcat started on port` \| `Started .* in .* seconds` |
| Internal port (F8) | 8080 | 8080 |
| Maven wrapper (F9) | none; the image provides Maven | none |
| Layer caching (F10) | `pom.xml` + `settings.xml` before sources, `dependency:go-offline` in its own layer | same |

No browser engine is installed. The oracle asserts on server-rendered HTML, response headers
and redirect targets at the HTTP boundary; a Chromium download would add ~400 MB to both
images and grade nothing extra. The one place a browser would matter — double-click-to-edit —
is client-side JavaScript in `todos.js`, which is byte-identical on both sides and therefore
not what a migration can break.

The oracle is **not baked into the image**: `.dockerignore` excludes `test.sh`, `smoke.py` and
`smoke/`, and `make test` stages the current copy into the running container with `docker cp`.
Editing the oracle therefore never invalidates the Maven build layer — and it is also exactly
the path `scarf validate` takes, which copies the benchmark's `smoke.py` into the candidate's
`output/` before running `make test`.

Both readiness patterns were observed in the actual container logs, not taken from the
contract table:

```
quarkus  todos 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.30.5) started in 2.232s.
         Listening on: http://0.0.0.0:8080
spring   Tomcat started on port 8080 (http) with context path '/'
         Started TodosApplication in 3.895 seconds (process running for 4.109)
```

`make up` polls for the readiness pattern **and**, in the same loop, watches for
`BUILD FAILURE` and for the container exiting (F/H16) — so a broken build reports in seconds
instead of timing out after five minutes.
