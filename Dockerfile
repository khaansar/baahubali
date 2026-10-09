# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

ARG SERVICE

# Copy Maven wrapper and root/module POMs first.
# This allows Docker to cache dependency resolution separately
# from application source changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY common/pom.xml common/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY iam-service/pom.xml iam-service/pom.xml
COPY test-service/pom.xml test-service/pom.xml
COPY attempt-service/pom.xml attempt-service/pom.xml
COPY analytics-service/pom.xml analytics-service/pom.xml
COPY community-service/pom.xml community-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml

# Make Maven wrapper executable.
RUN chmod +x mvnw

# Copy source code.
COPY common/src common/src
COPY api-gateway/src api-gateway/src
COPY iam-service/src iam-service/src
COPY test-service/src test-service/src
COPY attempt-service/src attempt-service/src
COPY analytics-service/src analytics-service/src
COPY community-service/src community-service/src
COPY notification-service/src notification-service/src
COPY payment-service/src payment-service/src

# Build only the requested service and required internal modules.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl ${SERVICE} package -DskipTests -am -B -q

# Extract Spring Boot layers.
RUN java -Djarmode=layertools \
    -jar ${SERVICE}/target/*.jar \
    extract \
    --destination /workspace/extracted


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

ARG SERVICE

WORKDIR /application

# Run as non-root.
RUN groupadd --system appgroup && \
    useradd --system \
    --gid appgroup \
    --no-create-home \
    appuser

# Copy Spring Boot layers separately for better Docker caching.
COPY --from=build --chown=appuser:appgroup \
    /workspace/extracted/dependencies/ ./

COPY --from=build --chown=appuser:appgroup \
    /workspace/extracted/spring-boot-loader/ ./

COPY --from=build --chown=appuser:appgroup \
    /workspace/extracted/snapshot-dependencies/ ./

COPY --from=build --chown=appuser:appgroup \
    /workspace/extracted/application/ ./

USER appuser

# Container-aware JVM configuration.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]