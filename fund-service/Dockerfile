# 多阶段构建：构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 先复制 pom.xml 下载依赖（利用缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 安装必要的工具
RUN apk add --no-cache curl

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /app/target/*.jar app.jar

# JVM 参数（可通过环境变量覆盖）
ENV JAVA_OPTS="-server -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
