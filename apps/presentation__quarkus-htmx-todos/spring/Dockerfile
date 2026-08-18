FROM maven:3.9.12-ibm-semeru-21-noble

USER root

# Smoke-test runtime. The oracle for this family asserts on server-rendered HTML, on
# redirect targets and on htmx fragment responses — all at the HTTP boundary — so
# curl + pytest is sufficient; no browser engine is installed.
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl lsof python3-venv python3-full ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m venv /opt/venv && /opt/venv/bin/pip install --no-cache-dir --quiet pytest
ENV PATH="/opt/venv/bin:$PATH"

WORKDIR /app

# Layer caching: dependency descriptors before sources (F10).
COPY pom.xml ./
COPY settings.xml* ./
RUN mkdir -p /root/.m2 && if [ -f settings.xml ]; then cp settings.xml /root/.m2/settings.xml; fi
RUN mvn -B -q dependency:go-offline -DskipTests || true

# Copy the rest of the application.
COPY . .
RUN mkdir -p /root/.m2 && if [ -f settings.xml ]; then cp settings.xml /root/.m2/settings.xml; fi

# Build at image-build time so startup is not dominated by the build (F3).
RUN mvn clean install -DskipTests

# The oracle is not baked in: .dockerignore excludes test.sh/smoke.py and `make test`
# stages them with `docker cp` at run time, so editing the oracle never invalidates this
# build layer. It is also the path `scarf validate` takes.

# No EXPOSE (F4): the harness runs the oracle inside the container via `docker exec`, not
# through a published port. The application binds 8080 internally (F8).

CMD ["mvn", "spring-boot:run"]
