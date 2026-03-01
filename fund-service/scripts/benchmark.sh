#!/bin/bash
# 基准测试脚本 - 生成详细报告

BASE_URL="${1:-http://localhost:8080}"
OUTPUT_DIR="/tmp/fund-benchmark-$(date +%Y%m%d-%H%M%S)"
mkdir -p $OUTPUT_DIR

echo "=========================================="
echo "    基金系统基准测试"
echo "=========================================="
echo "输出目录: $OUTPUT_DIR"
echo "=========================================="

# API端点定义
declare -A ENDPOINTS
ENDPOINTS["list"]="GET /api/funds?page=1&size=20"
ENDPOINTS["detail"]="GET /api/funds/011452"
ENDPOINTS["metrics"]="GET /api/funds/011452/metrics"
ENDPOINTS["top"]="GET /api/funds/top?sortBy=sharpe&limit=20"
ENDPOINTS["search"]="GET /api/funds/search?keyword=zhaoshang&limit=10"
ENDPOINTS["signal"]="GET /api/funds/011452/signal"
ENDPOINTS["estimate"]="GET /api/funds/011452/estimate"

# 并发配置
CONCURRENT_LEVELS=(10 50 100)

# 运行测试
for name in "${!ENDPOINTS[@]}"; do
    endpoint="${ENDPOINTS[$name]}"
    method="${endpoint%% *}"
    path="${endpoint#* }"
    url="${BASE_URL}${path}"
    
    echo -e "\n测试接口: $name"
    echo "URL: $url"
    
    for c in "${CONCURRENT_LEVELS[@]}"; do
        n=$((c * 20))  # 每个并发20个请求
        
        echo "  并发 $c, 请求 $n..."
        
        # 执行测试
        RESULT_FILE="$OUTPUT_DIR/${name}_c${c}.txt"
        
        START=$(date +%s.%N)
        for i in $(seq 1 $c); do
            (
                for j in $(seq 1 20); do
                    curl -s -o /dev/null -w "%{http_code},%{time_total}\n" "$url" 2>/dev/null
                done
            ) &
done
        wait
        END=$(date +%s.%N)
        
        # 计算结果
        DURATION=$(echo "$END - $START" | bc)
        QPS=$(echo "scale=2; $n / $DURATION" | bc)
        
        echo "    耗时: ${DURATION}s, QPS: $QPS"
        echo "$name,$c,$n,$DURATION,$QPS" >> "$OUTPUT_DIR/summary.csv"
    done
done

# 生成报告
echo -e "\n=========================================="
echo "    测试摘要"
echo "=========================================="
echo "接口,并发,请求数,耗时,QPS" > "$OUTPUT_DIR/report.csv"
cat "$OUTPUT_DIR/summary.csv" >> "$OUTPUT_DIR/report.csv"
column -t -s ',' "$OUTPUT_DIR/report.csv"

echo -e "\n详细报告已保存到: $OUTPUT_DIR"
