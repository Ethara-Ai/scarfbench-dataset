FROM maven:3.9-eclipse-temurin-21

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY settings.xml /root/.m2/settings.xml
COPY pom.xml ./
COPY src ./src

RUN mvn -B clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "/app/target/spring-mongodb-crud.jar"]
