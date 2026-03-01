#!/bin/bash
# 验证前端访问脚本

echo "========================================"
echo "  基金系统 - 访问验证测试"
echo "========================================"
echo ""

# 测试地址列表
ADDRESSES=(
    "http://127.0.0.1:8888"
    "http://localhost:8888"
    "http://0.0.0.0:8888"
)

ALL_PASS=true

for url in "${ADDRESSES[@]}"; do
    echo -n "测试 $url: "
    
    # 获取HTTP状态和标题
    result=$(curl -s --max-time 5 "$url" 2>/dev/null)
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null)
    
    if [ "$http_code" == "200" ]; then
        title=$(echo "$result" | grep -o "<title>[^\u003c]*</title>" | sed 's/<[^\u003e]*>//g')
        echo -e "✅ HTTP $http_code (标题: $title)"
    else
        echo -e "❌ HTTP $http_code"
        ALL_PASS=false
    fi
done

echo ""
echo "========================================"

if [ "$ALL_PASS" = true ]; then
    echo "✅ 所有测试通过！服务运行正常"
    echo ""
    echo "请在浏览器中访问:"
    echo "  http://127.0.0.1:8888"
    echo "  http://localhost:8888"
    exit 0
else
    echo "❌ 部分测试失败，请检查服务状态"
    echo "运行: make check"
    exit 1
fi
