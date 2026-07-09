ARG DOCKER_REGISTRY=docker.io

FROM ${DOCKER_REGISTRY}/maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests

COPY src ./src
RUN mvn clean package -DskipTests

FROM ${DOCKER_REGISTRY}/openjdk:17

WORKDIR /app

COPY --from=build /app/target/dormitory-washbasin-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
