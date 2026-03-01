#!/bin/bash
# JVM调优参数配置
# 根据环境选择不同配置

ENV="${1:-dev}"

echo "=========================================="
echo "    JVM 调优参数 - $ENV 环境"
echo "=========================================="

# 基础参数
JAVA_OPTS="-server -Dfile.encoding=UTF-8"

# 内存配置
if [ "$ENV" = "prod" ]; then
    # 生产环境：4G堆内存
    JAVA_OPTS="$JAVA_OPTS -Xms4g -Xmx4g"
    JAVA_OPTS="$JAVA_OPTS -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
else
    # 开发/测试环境：1G堆内存
    JAVA_OPTS="$JAVA_OPTS -Xms1g -Xmx1g"
    JAVA_OPTS="$JAVA_OPTS -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
fi

# G1垃圾收集器（Java 17推荐）
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -XX:G1HeapRegionSize=16m"

# GC日志（Java 17使用Xlog）
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=100m"

# OOM时生成堆转储
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=/tmp/heapdump.hprof"

# 性能优化
JAVA_OPTS="$JAVA_OPTS -XX:+UseStringDeduplication"
JAVA_OPTS="$JAVA_OPTS -XX:+AlwaysPreTouch"

# 生产环境额外配置
if [ "$ENV" = "prod" ]; then
    # 关闭显式GC
    JAVA_OPTS="$JAVA_OPTS -XX:+DisableExplicitGC"
    # 优化JIT编译
    JAVA_OPTS="$JAVA_OPTS -XX:ReservedCodeCacheSize=256m"
fi

echo "JVM参数:"
echo "$JAVA_OPTS" | tr ' ' '\n' | grep -v "^$"
echo ""
echo "启动命令:"
echo "java $JAVA_OPTS -jar fund-service.jar"
echo ""
echo "=========================================="
