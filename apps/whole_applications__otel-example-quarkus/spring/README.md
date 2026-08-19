# OpenTelemetry Spring Boot Example

[![CI](https://img.shields.io/github/actions/workflow/status/devops-thiago/otel-example-quarkus/ci.yml?branch=main&label=CI)](https://github.com/devops-thiago/otel-example-quarkus/actions)
[![Java Version](https://img.shields.io/badge/java-21-007396?logo=openjdk)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/github/license/devops-thiago/otel-example-quarkus)](LICENSE)
[![Codecov](https://img.shields.io/codecov/c/github/devops-thiago/otel-example-quarkus?label=coverage)](https://app.codecov.io/gh/devops-thiago/otel-example-quarkus)
[![Sonar Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=devops-thiago_otel-example-quarkus&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=devops-thiago_otel-example-quarkus)
[![Sonar Coverage](https://sonarcloud.io/api/project_badges/measure?project=devops-thiago_otel-example-quarkus&metric=coverage)](https://sonarcloud.io/summary/new_code?id=devops-thiago_otel-example-quarkus)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-enabled-blue?logo=opentelemetry)](https://opentelemetry.io)
[![Docker](https://img.shields.io/badge/Docker-ready-blue?logo=docker)](https://www.docker.com)

A production-ready Spring Boot REST API with comprehensive OpenTelemetry instrumentation, featuring distributed tracing, metrics collection, and structured logging. Built with clean architecture principles and designed for cloud-native deployments.

> **Note**: This project was migrated from Quarkus to Spring Boot while preserving the external behavioral contract (HTTP routes, response payloads, persistent state, and telemetry). See [CHANGELOG.md](CHANGELOG.md) for the full migration log.

## 📋 Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Deployment Options](#deployment-options)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Observability](#observability)
- [Development](#development)
- [Testing](#testing)
- [Contributing](#contributing)

## ✨ Features

- **🍃 Spring Boot Framework** - Industry-standard Java framework with a rich ecosystem
- **📊 Full Observability** - Distributed tracing, metrics, and structured logging
- **🔌 OpenTelemetry Starter** - OTLP exporter support for traces, metrics, and logs via `opentelemetry-spring-boot-starter`
- **🏗️ Clean Architecture** - Repository pattern with Spring Data JPA for simplified data access
- **🐳 Docker Ready** - Multi-stage Dockerfile with security best practices
- **🔒 Security First** - Non-root user, minimal attack surface, vulnerability scanning
- **🧪 Well Tested** - Comprehensive test coverage with JUnit 5 and REST-assured
- **📝 API Documentation** - OpenAPI/Swagger UI automatically generated via springdoc
- **💾 MySQL Integration** - JDBC with full OpenTelemetry instrumentation

## 📚 Prerequisites

- Java 21+ (for local development)
- Maven 3.9+
- Docker & Docker Compose
- MySQL 8.0+ (or use the provided docker-compose)
- OpenTelemetry Collector (optional - included in full setup)

## 🚀 Quick Start

### Option 1: Full Stack (App + Database + Observability)

```bash
# Clone the repository
git clone https://github.com/devops-thiago/otel-example-quarkus.git
cd otel-example-quarkus

# Start everything with docker-compose
docker-compose up -d

# Check if services are running
docker-compose ps
```

**Access points:**
- API: http://localhost:8080
- API Docs (Swagger): http://localhost:8080/q/swagger-ui
- Health: http://localhost:8080/q/health
- Grafana: http://localhost:3000 (admin/admin)
- Alloy UI: http://localhost:12345

> Metrics are pushed via OTLP to Alloy and stored in Mimir.
> Query them in Grafana with PromQL instead of scraping an endpoint on the app.

### Option 2: Run Locally

```bash
# Install dependencies and run in dev mode
mvn spring-boot:run

# Or build and run
mvn clean package
java -jar target/otel-spring-crud-1.0.0.jar
```

## 🚢 Deployment Options

### Using Your Own OpenTelemetry Collector

If you already have an OpenTelemetry infrastructure:

**Required environment variables:**
```bash
# OpenTelemetry Configuration
OTEL_EXPORTER_OTLP_ENDPOINT=your-collector:4320
OTEL_SERVICE_NAME=otel-quarkus-crud

# Database Configuration
DB_HOST=your-mysql-host
DB_PORT=3306
DB_USER=your-db-user
DB_PASSWORD=your-db-password
DB_NAME=your-db-name
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: otel-spring-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: otel-spring-api
  template:
    metadata:
      labels:
        app: otel-spring-api
    spec:
      containers:
      - name: api
        image: otel-spring-crud:latest
        ports:
        - containerPort: 8080
        env:
        - name: OTEL_EXPORTER_OTLP_ENDPOINT
          value: "http://otel-collector:4320"
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          value: "mysql-service"
        livenessProbe:
          httpGet:
            path: /q/health
            port: 8080
          initialDelaySeconds: 30
        readinessProbe:
          httpGet:
            path: /q/health
            port: 8080
          initialDelaySeconds: 10
```

### Building Docker Image

```bash
# Build the image locally
docker build -t otel-spring-crud:latest .

# Build multi-platform image
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t otel-spring-crud:latest .
```

## 📖 API Documentation

### Health Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/q/health` | Overall health check (Spring Boot Actuator) |

### User API

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| GET | `/api/users` | List all users | - |
| GET | `/api/users/{id}` | Get user by ID | - |
| GET | `/api/users/email/{email}` | Get user by email | - |
| GET | `/api/users/search?name={name}` | Search users by name | - |
| GET | `/api/users/recent?days={days}` | Get recent users | - |
| GET | `/api/users/count` | Get user count | - |
| POST | `/api/users` | Create new user | `{"name": "John", "email": "john@example.com", "bio": "Developer"}` |
| PUT | `/api/users/{id}` | Update user | `{"name": "John Updated"}` |
| DELETE | `/api/users/{id}` | Delete user | - |

### Example Requests

```bash
# Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "bio": "Software Engineer"}'

# Get all users
curl http://localhost:8080/api/users

# Get user by ID
curl http://localhost:8080/api/users/1

# Search users
curl http://localhost:8080/api/users/search?name=John

# Get recent users (last 7 days)
curl http://localhost:8080/api/users/recent?days=7

# Update user
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "bio": "Senior Engineer"}'

# Delete user
curl -X DELETE http://localhost:8080/api/users/1
```

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| **OpenTelemetry** | | |
| `OTEL_SDK_DISABLED` | Disable OpenTelemetry | `false` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP collector endpoint | `http://localhost:4317` |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | OTLP protocol | `grpc` |
| **Database (prod profile)** | | |
| `DB_HOST` | MySQL host | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_USERNAME` | MySQL user | `user` |
| `DB_PASSWORD` | MySQL password | `password` |
| `DB_NAME` | MySQL database name | `userdb` |
| **Server** | | |
| `SERVER_PORT` | API server port | `8080` |
| `SERVER_ADDRESS` | API server host | `0.0.0.0` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | (default: H2 in-memory) |

## 🔭 Observability

This project includes a complete observability stack using the LGTM (Loki, Grafana, Tempo, Mimir) stack:

### Distributed Tracing (Tempo)
- Trace all HTTP requests and database queries
- Correlate logs with traces using trace IDs
- View spans in Grafana with trace context

### Metrics Collection (Mimir)

Metrics are exported via OTLP (no Prometheus scrape endpoint). The `opentelemetry-spring-boot-starter` provides:

**Automatic metrics:**
- HTTP server metrics (request duration, status codes, route)
- JVM metrics (heap, GC, threads, class loading)

**Custom business metrics (via the OpenTelemetry Meter API in `UserService`):**
- `users.created.total` — counter, incremented on successful user creation
- `users.errors.total` — counter with `error.type` attribute (`duplicate_email`, `not_found`)
- `users.total` — observable gauge, current number of users
- `user.search.duration` — histogram of search operation latency in ms

### Log Aggregation (Loki)
- Trace-correlated logs (trace_id / span_id in every log line)
- Log levels and filtering
- Full-text search capabilities

### Visualization (Grafana)
Pre-configured dashboards for:
- Application overview
- HTTP request metrics
- JVM performance
- Trace exploration

**Access Grafana**: http://localhost:3000 (admin/admin)

## 🏗️ Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/br/com/arquivolivre/otelquarkus/
│   │   │   ├── Application.java    # Spring Boot entrypoint
│   │   │   ├── config/             # Spring configuration beans
│   │   │   ├── model/              # JPA entities
│   │   │   ├── repository/         # Data access layer (Spring Data JPA)
│   │   │   ├── controller/         # REST endpoints
│   │   │   └── service/            # Business logic
│   │   └── resources/
│   │       ├── application.properties  # Configuration
│   │       └── import.sql          # Initial data
│   └── test/
│       ├── java/                   # Unit and integration tests
│       └── resources/              # Test configuration
├── config/                         # Observability stack configs
│   ├── alloy.alloy                # Grafana Alloy configuration
│   ├── tempo.yaml                 # Tempo tracing backend
│   ├── mimir.yaml                 # Mimir metrics backend
│   ├── loki.yaml                  # Loki logging backend
│   └── grafana/                   # Grafana provisioning
├── docker-compose.yml             # Full stack deployment
├── Dockerfile                     # Multi-stage Docker build
├── pom.xml                        # Maven dependencies
└── README.md                      # This file
```

## 🛠️ Development

### Running in Dev Mode

```bash
mvn spring-boot:run
```

This starts the app on http://localhost:8080 with an H2 in-memory database and seed data.

### Code Quality

```bash
# Format code
mvn spotless:apply

# Check code style
mvn spotless:check

# Run static analysis
mvn verify
```

### Database Migrations

```bash
# The app automatically creates/updates schema on startup
# Initial data is loaded from src/main/resources/import.sql
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn verify

# Run specific test class
mvn test -Dtest=UserControllerTest
```

### Test Coverage

- **Unit Tests**: Repository, Service, and Controller layers
- **Integration Tests**: Full API endpoint testing
- **Coverage Target**: >80% line coverage, >75% branch coverage

View coverage report: `target/site/jacoco/index.html`

## 🐳 Docker

### Build and Run

```bash
# Build the Docker image
docker build -t otel-spring-crud .

# Run the container
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=mysql \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://alloy:4320 \
  otel-spring-crud
```

### Docker Compose

```bash
# Start full stack
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up -d --build app
```

## 📊 Monitoring

### Endpoints

- Health check: http://localhost:8080/q/health
- OpenAPI spec: http://localhost:8080/q/openapi

Metrics are not exposed on the app. They are pushed via OTLP to Alloy and queried in Grafana.

### Grafana Dashboards

1. **Application Overview**: Real-time metrics and request rates
2. **JVM Metrics**: Memory, GC, and thread monitoring
3. **Trace Analysis**: Distributed tracing visualization

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards

- Follow Java code conventions
- Add tests for new features
- Update documentation as needed
- Run `mvn spotless:apply` before committing

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
