# --- Stage 1: сборка ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY . .
RUN mvn -B -pl scrapper -am clean package -DskipTests

# --- Stage 2: рантайм ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /build/scrapper/target/*.jar app.jar
EXPOSE 8081 8091
ENTRYPOINT ["java", "-jar", "app.jar"]