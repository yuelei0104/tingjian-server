FROM maven:3.9.11-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /workspace
COPY microservices ./microservices
RUN mvn -q -f microservices/pom.xml -pl "${SERVICE}" -am -DskipTests package \
    && cp "microservices/${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar" /tmp/service.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /tmp/service.jar /app/service.jar
USER 10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/service.jar"]
