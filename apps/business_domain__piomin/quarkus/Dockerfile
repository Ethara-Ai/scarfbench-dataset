FROM maven:3.9.12-ibm-semeru-21-noble

USER root
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache Maven dependencies separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -q || true

# Build application
COPY src ./src
RUN mvn package -DskipTests -q

COPY test.sh .
RUN chmod +x test.sh

CMD ["java", "-jar", "target/quarkus-app/quarkus-run.jar"]
