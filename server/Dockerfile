# 果语服务端镜像。构建上下文是 server/。
# Maven 版本与 .mvn/wrapper/maven-wrapper.properties 里的 3.9.16 相同。
# 运行时使用 Java 17。镜像构建跳过测试：Testcontainers 需要嵌套 Docker。
# 官方标签是多架构清单；本仓库在 linux/amd64 上构建过，ARM NAS 未验收。

FROM maven:3.9.16-eclipse-temurin-17 AS build

WORKDIR /src
COPY pom.xml .
RUN mvn -B -DskipTests dependency:resolve dependency:resolve-plugins
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17.0.20.1_1-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S -g 10001 guoyu \
    && adduser -S -D -H -u 10001 -G guoyu guoyu \
    && mkdir -p /app \
    && chown guoyu:guoyu /app

WORKDIR /app
COPY --from=build /src/target/guoyu-english-server-0.0.1-SNAPSHOT.jar /app/app.jar
USER guoyu

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
    CMD curl -fsS http://127.0.0.1:8080/api/v1/system/health >/dev/null

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
