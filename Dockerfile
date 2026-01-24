# ==== Build stage ====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# cache deps
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# build
COPY src ./src
RUN mvn -q -DskipTests package

# ==== Run stage ====
FROM eclipse-temurin:21-jre
WORKDIR /app

# copy jar
COPY --from=build /app/target/*.jar app.jar

# run with prod profile by default
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]