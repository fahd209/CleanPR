FROM maven:3.8.3-openjdk-17 as builder

WORKDIR /app

COPY . .

RUN mvn clean install -DskipTests

FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY --from=builder ./app/target/*.jar /app/jar-file/cleanpr.jar
COPY /src/main/resources/application.yml /app/config/application.yml
COPY clean-pr.2025-09-15.private-key.pem /app/config/private-key.pem

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/jar-file/cleanpr.jar", "--spring.config.location=/app/config/application.yml"]