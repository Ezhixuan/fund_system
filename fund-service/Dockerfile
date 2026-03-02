# 使用 Amazon Corretto 镜像
FROM amazoncorretto:17-alpine

WORKDIR /app

# 复制已构建的 jar 文件
COPY target/*.jar app.jar

# 安装 curl 用于健康检查
RUN apk add --no-cache curl tzdata

# JVM 参数
ENV JAVA_OPTS="-server -Xms512m -Xmx512m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
