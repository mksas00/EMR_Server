# Multi-stage build dla aplikacji EMR
# Etap build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
# Pobranie zależności (layer caching)
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package && cp target/*-SNAPSHOT.jar app.jar

# Etap runtime (lekki obraz)
FROM eclipse-temurin:21-jre-alpine
ENV APP_HOME=/app \
    JAVA_OPTS=""
WORKDIR ${APP_HOME}
# Użytkownik nie-root
RUN addgroup -S emr && adduser -S emr -G emr
COPY --from=build /workspace/app.jar app.jar
COPY scripts/entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh && chown -R emr:emr ${APP_HOME}
EXPOSE 8080
USER emr
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["/app/entrypoint.sh"]
