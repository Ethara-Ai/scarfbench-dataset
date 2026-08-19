# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 — build the Quarkus application (fast-jar) with Maven
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Pre-fetch dependencies first (better layer caching)
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -B -f backend/pom.xml -DskipTests -Dquarkus.build.skip=true dependency:go-offline || true

# Copy the sources and build
COPY backend/ ./backend/
RUN mvn -B -f backend/pom.xml -DskipTests clean package

# ---------------------------------------------------------------------------
# Stage 2 — minimal JRE runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Quarkus fast-jar layout: quarkus-run.jar + lib/ + app/ + quarkus/
COPY --from=build /workspace/backend/target/quarkus-app/ /app/

EXPOSE 8080

ENV QUARKUS_HTTP_HOST=0.0.0.0 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]
