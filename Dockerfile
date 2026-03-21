# Stage 1: Build (Maven compiling inside Docker)
FROM maven:3.8.6-openjdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
# Scarica le dipendenze in cache
RUN mvn dependency:go-offline -B
# Copia il sorgente e compila
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run (Leggera immagine JRE per l'esecuzione)
FROM openjdk:11-jre-slim
WORKDIR /app
# Copia solo il JAR compilato dal primo stage
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Avvia forzando il profilo "stag"
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=stag", "app.jar"]
