#!/bin/bash
# 基金系统压力测试脚本
# 使用: ./loadtest.sh [基准URL] [并发数] [总请求数]

BASE_URL="${1:-http://localhost:8080}"
CONCURRENT="${2:-50}"
TOTAL="${3:-1000}"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "    基金系统压力测试"
echo "=========================================="
echo "基准URL: $BASE_URL"
echo "并发数: $CONCURRENT"
echo "总请求数: $TOTAL"
echo "=========================================="

# 检查服务是否可用
echo -e "\n${YELLOW}[1/5] 检查服务状态...${NC}"
if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}服务不可用，请确保服务已启动${NC}"
    exit 1
fi
echo -e "${GREEN}服务运行正常${NC}"

# 测试1: 基金列表接口
echo -e "\n${YELLOW}[2/5] 测试基金列表接口...${NC}"
API1="$BASE_URL/api/funds?page=1&size=20"
echo "URL: $API1"

START_TIME=$(date +%s)
for i in $(seq 1 $CONCURRENT); do
    (
        for j in $(seq 1 $((TOTAL / CONCURRENT))); do
            curl -s -o /dev/null -w "%{http_code},%{time_total}\n" "$API1" 2>/dev/null
        done
    ) &
done
wait
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
QPS=$((TOTAL / DURATION))
echo -e "${GREEN}完成: ${DURATION}s, QPS: ${QPS}${NC}"

# 测试2: 基金详情接口
echo -e "\n${YELLOW}[3/5] 测试基金详情接口...${NC}"
API2="$BASE_URL/api/funds/011452"
echo "URL: $API2"

START_TIME=$(date +%s)
for i in $(seq 1 $CONCURRENT); do
    (
        for j in $(seq 1 $((TOTAL / CONCURRENT))); do
            curl -s -o /dev/null -w "%{http_code},%{time_total}\n" "$API2" 2>/dev/null
        done
    ) &
done
wait
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
QPS=$((TOTAL / DURATION))
echo -e "${GREEN}完成: ${DURATION}s, QPS: ${QPS}${NC}"

# 测试3: 基金指标接口
echo -e "\n${YELLOW}[4/5] 测试基金指标接口...${NC}"
API3="$BASE_URL/api/funds/011452/metrics"
echo "URL: $API3"

START_TIME=$(date +%s)
for i in $(seq 1 $CONCURRENT); do
    (
        for j in $(seq 1 $((TOTAL / CONCURRENT))); do
            curl -s -o /dev/null -w "%{http_code},%{time_total}\n" "$API3" 2>/dev/null
        done
    ) &
done
wait
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
QPS=$((TOTAL / DURATION))
echo -e "${GREEN}完成: ${DURATION}s, QPS: ${QPS}${NC}"

# 测试4: TOP排名接口
echo -e "\n${YELLOW}[5/5] 测试TOP排名接口...${NC}"
API4="$BASE_URL/api/funds/top?sortBy=sharpe&limit=20"
echo "URL: $API4"

START_TIME=$(date +%s)
for i in $(seq 1 $CONCURRENT); do
    (
        for j in $(seq 1 $((TOTAL / CONCURRENT))); do
            curl -s -o /dev/null -w "%{http_code},%{time_total}\n" "$API4" 2>/dev/null
        done
    ) &
done
wait
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
QPS=$((TOTAL / DURATION))
echo -e "${GREEN}完成: ${DURATION}s, QPS: ${QPS}${NC}"

echo -e "\n=========================================="
echo "    压力测试完成"
echo "=========================================="
