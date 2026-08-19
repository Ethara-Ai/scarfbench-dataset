# Multi-stage Dockerfile for Spring Boot Application
# Stage 1: Build stage
FROM maven:3-eclipse-temurin-26-alpine AS build

# Set working directory
WORKDIR /app

# Copy Maven configuration files first (for better layer caching)
COPY pom.xml .
# Eval-local mirror settings (Maven Central returned HTTP 429 during the run;
# GCS mirror of central used instead - see metadata.json notes)
COPY settings.xml /root/.m2/settings.xml

# Download dependencies (cached layer if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (creates Spring Boot executable jar)
RUN mvn clean package -DskipTests -B

# Verify the build artifacts
RUN ls -la target/

# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre-alpine AS runtime

# Install curl for health checks and create non-root user
RUN apk add --no-cache curl && \
    addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy the Spring Boot executable jar from build stage
COPY --from=build --chown=appuser:appuser /app/target/otel-spring-crud-*.jar ./app.jar

# Create directories for logs
RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check (actuator health exposed at /q/health via management base-path)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/q/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
