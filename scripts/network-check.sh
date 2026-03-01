#!/bin/bash
# 基金系统网络诊断脚本
# 用于排查 Docker 部署后的网络访问问题

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务配置
NGINX_PORT=8888
API_PORT=18080
MYSQL_PORT=13306
REDIS_PORT=16379

echo "=========================================="
echo "    基金系统 - 网络诊断工具"
echo "=========================================="
echo ""

# 检查 Docker
echo -e "${BLUE}[1/6] 检查 Docker 环境...${NC}"
if ! command -v docker >/dev/null 2>&1; then
    echo -e "${RED}  ✗ Docker 未安装${NC}"
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}  ✗ Docker 守护进程未运行${NC}"
    exit 1
fi
echo -e "${GREEN}  ✓ Docker 运行正常${NC}"

# 检查容器状态
echo ""
echo -e "${BLUE}[2/6] 检查容器状态...${NC}"
cd "$(dirname "$0")/../deploy" 2>/dev/null || cd "./deploy"

CONTAINERS=("fund-mysql" "fund-redis" "fund-api" "fund-nginx")
ALL_HEALTHY=true

for container in "${CONTAINERS[@]}"; do
    status=$(docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null || echo "not_found")
    health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")
    
    if [ "$status" == "running" ]; then
        if [ "$health" == "healthy" ] || [ "$health" == "none" ]; then
            echo -e "${GREEN}  ✓ $container: $status ($health)${NC}"
        else
            echo -e "${YELLOW}  ⚠ $container: $status (health: $health)${NC}"
            ALL_HEALTHY=false
        fi
    else
        echo -e "${RED}  ✗ $container: $status${NC}"
        ALL_HEALTHY=false
    fi
done

# 检查端口监听
echo ""
echo -e "${BLUE}[3/6] 检查端口监听...${NC}"
PORTS=($NGINX_PORT $API_PORT $MYSQL_PORT $REDIS_PORT)
PORT_NAMES=("Nginx" "API" "MySQL" "Redis")

for i in "${!PORTS[@]}"; do
    port="${PORTS[$i]}"
    name="${PORT_NAMES[$i]}"
    
    # 使用 nc 或 curl 检查端口
    if curl -s -o /dev/null --connect-timeout 2 "http://127.0.0.1:$port" 2>/dev/null || \
       (command -v nc >/dev/null 2>&1 && nc -z 127.0.0.1 "$port" 2>/dev/null); then
        echo -e "${GREEN}  ✓ $name (端口 $port): 监听中${NC}"
    else
        echo -e "${RED}  ✗ $name (端口 $port): 未监听${NC}"
    fi
done

# 测试 HTTP 访问
echo ""
echo -e "${BLUE}[4/6] 测试 HTTP 访问...${NC}"

echo -n "  测试前端 (127.0.0.1:$NGINX_PORT): "
frontend_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "http://127.0.0.1:$NGINX_PORT/" 2>/dev/null || echo "000")
if [ "$frontend_code" == "200" ]; then
    echo -e "${GREEN}HTTP $frontend_code ✓${NC}"
else
    echo -e "${RED}HTTP $frontend_code ✗${NC}"
fi

echo -n "  测试 API (127.0.0.1:$API_PORT): "
api_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "http://127.0.0.1:$API_PORT/actuator/health" 2>/dev/null || echo "000")
if [ "$api_code" == "200" ]; then
    echo -e "${GREEN}HTTP $api_code ✓${NC}"
else
    echo -e "${RED}HTTP $api_code ✗${NC}"
fi

# 检查防火墙 (macOS)
echo ""
echo -e "${BLUE}[5/6] 检查系统防火墙...${NC}"
if [ "$(uname)" == "Darwin" ]; then
    # macOS
    if sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate 2>/dev/null | grep -q "enabled"; then
        echo -e "${YELLOW}  ⚠ macOS 防火墙已启用${NC}"
        echo -e "    建议: 系统偏好设置 -> 安全性与隐私 -> 防火墙"
        echo -e "    或运行: sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off"
    else
        echo -e "${GREEN}  ✓ macOS 防火墙未启用${NC}"
    fi
else
    echo -e "    跳过 (非 macOS 系统)"
fi

# 诊断总结
echo ""
echo -e "${BLUE}[6/6] 诊断总结...${NC}"

if [ "$ALL_HEALTHY" = true ] && [ "$frontend_code" == "200" ] && [ "$api_code" == "200" ]; then
    echo -e "${GREEN}  ✓ 所有检查通过，服务运行正常！${NC}"
    echo ""
    echo "  访问地址:"
    echo "    前端界面: http://127.0.0.1:$NGINX_PORT"
    echo "    API接口:  http://127.0.0.1:$API_PORT/api"
    echo "    健康检查: http://127.0.0.1:$API_PORT/actuator/health"
else
    echo -e "${YELLOW}  ⚠ 检测到问题，建议按以下步骤修复:${NC}"
    echo ""
    
    if [ "$ALL_HEALTHY" != true ]; then
        echo "  1. 重启所有服务:"
        echo "     cd deploy && docker-compose restart"
    fi
    
    if [ "$frontend_code" != "200" ]; then
        echo "  2. 检查 Nginx 日志:"
        echo "     docker logs fund-nginx"
    fi
    
    if [ "$api_code" != "200" ]; then
        echo "  3. 检查后端日志:"
        echo "     docker logs fund-api"
    fi
    
    echo ""
    echo "  4. 如果仍无法访问，尝试使用前端开发模式:"
    echo "     cd fund-view && npm run dev"
    echo "     然后访问 http://localhost:5174"
fi

echo ""
echo "=========================================="
