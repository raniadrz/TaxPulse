# syntax=docker/dockerfile:1.7
# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Resolve dependencies first so they are cached independently of source changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/taxpulse-backend-*.jar extract --layers --launcher --destination target/extracted

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
LABEL org.opencontainers.image.title="taxpulse-backend"
RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /app/storage && chown -R app:app /app
WORKDIR /app
# Layered jar: dependencies change rarely, application classes often -> better layer caching.
COPY --from=build --chown=app:app /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/application/ ./
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    STORAGE_ROOT=/app/storage
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
