# ---- 构建阶段 ----
# 本 Slice 不升级，builder 用 JDK 11（与 pom.xml java.version 一致）；SB3 升级 Slice 时改 temurin-17
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline   # 先缓存依赖，利用 Docker layer cache
COPY src ./src
# 构建镜像时跳过测试（测试在 CI 单独跑；集成测试需要 Docker-in-Docker 才能用 Testcontainers）
RUN mvn -B -q clean package -DskipTests

# ---- 运行阶段 ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/loggi-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
