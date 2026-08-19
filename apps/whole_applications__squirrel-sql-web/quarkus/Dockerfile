# =============================================================================
# Multi-stage Dockerfile for the Quarkus-migrated squirrel-sql-web application.
#
# The original app was a JavaEE/JAX-RS WAR meant to be dropped into a Glassfish
# server. After the migration to Quarkus it is a self-contained runnable
# application (Quarkus "fast-jar"), so the image no longer needs an app server.
# =============================================================================

# ---- Stage 1: build -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the Maven project (including the bundled local-maven-repo that hosts the
# squirrel-sql:4.1.0 artifact) and build the Quarkus fast-jar.
COPY pom.xml ./
COPY local-maven-repo ./local-maven-repo
COPY src ./src

# Build the Quarkus application. Tests are executed during the local Maven build
# and again as smoke tests against the running container, so we skip them here to
# keep image builds deterministic and independent of test-time port allocation.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests clean package

# ---- Stage 2: runtime ---------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# The SQuirreL SQL core embedded in the app is a Swing/AWT application; run fully
# headless. It also persists settings under $HOME/.squirrel-sql and writes a JWT
# signing key (server.key) in the working directory, so both must be writable.
ENV JAVA_OPTS="-Djava.awt.headless=true"
ENV HOME=/app

# Copy the Quarkus fast-jar layout (order matters for layer caching: libraries
# change least often, the application code most often).
COPY --from=build /build/target/quarkus-app/lib/ ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080

# Bind to all interfaces so the container port is reachable via the mapped host
# port (docker run -p 0:8080 ...).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dquarkus.http.host=0.0.0.0 -jar /app/quarkus-run.jar"]
