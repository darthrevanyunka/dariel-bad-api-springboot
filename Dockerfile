# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies separately from source so code edits don't re-download the internet
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Only pom.xml and src/ are ever copied in - solution/ never enters the build context
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r badapi && useradd -r -g badapi badapi
COPY --from=build /app/target/bad-api-1.0.0.jar app.jar
RUN chown badapi:badapi app.jar
USER badapi

EXPOSE 8080

# Explicit heap ceiling rather than the container-percentage default - this app's
# working set is a few thousand in-memory records, so 512m is already generous
# headroom for ~20 concurrent participants.
ENV JAVA_OPTS="-Xms128m -Xmx512m"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
