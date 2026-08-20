# ---- build stage ----------------------------------------------------------------------
# The JDK, Maven and the whole dependency tree are needed to compile and are not needed to
# run. Keeping them in a stage that is thrown away is what makes the final image a JRE and a
# jar rather than a build environment with an application inside it.
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# pom.xml alone first, so a code change does not re-resolve dependencies. Docker caches per
# layer: as long as this file is unchanged the next line is a cache hit, and a rebuild goes
# from minutes to seconds.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in CI, where Docker is available for Testcontainers. Running them here would mean
# Docker-in-Docker for the integration suite.
RUN mvn -B -q clean package -DskipTests

# ---- runtime stage --------------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# The schema stores wall-clock timestamps that no layer converts, so the container's zone is
# part of what the data means. Left at the default a container writes UTC while the seed data
# is JST, and every timestamp created through the app sits nine hours away from the ones
# beside it — which is exactly what happened the first time this was deployed.
ENV TZ=Asia/Tokyo

# Not root. The process needs to read one jar and open one port; nothing it does requires
# ownership of the filesystem, and a container that runs as root hands an attacker who gets
# in the same authority the image was built with.
RUN groupadd --system loggi && useradd --system --gid loggi --no-create-home loggi

COPY --from=builder --chown=loggi:loggi /build/target/loggi-server-0.0.1-SNAPSHOT.jar app.jar
USER loggi

EXPOSE 8088

# Actuator's own answer, not a TCP probe. A port that accepts a connection says the JVM is up;
# this says the application reached a state where it can serve — which is the question
# `depends_on: condition: service_healthy` is asking. `/actuator/health` is permitted without
# a token on purpose (SecurityConfiguration); the rest of /actuator is not.
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
  CMD curl -fsS http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
