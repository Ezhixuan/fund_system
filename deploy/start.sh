#!/bin/bash
# 基金系统启动脚本

set -e

echo "=========================================="
echo "    基金交易系统 - 启动脚本"
echo "=========================================="

# 检查 docker 和 docker-compose
if ! command -v docker >/dev/null 2>&1; then
    echo "错误：Docker 未安装"
    exit 1
fi

if ! command -v docker-compose >/dev/null 2>&1; then
    echo "错误：docker-compose 未安装"
    exit 1
fi

# 创建必要目录
mkdir -p init-scripts
mkdir -p backup

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "警告：.env 文件不存在，使用默认配置"
    echo "建议：复制 .env.example 为 .env 并修改密码"
    cp .env.example .env
fi

# 构建并启动
echo ""
echo "[1/3] 构建并启动服务..."
docker-compose up -d --build

echo ""
echo "[2/3] 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "[3/3] 检查服务状态..."
docker-compose ps

echo ""
echo "=========================================="
echo "    基金系统启动完成"
echo "=========================================="
echo ""
echo "访问地址："
echo "  前端界面: http://localhost"
echo "  API接口: http://localhost:8080/api"
echo "  监控面板: http://localhost:8080/actuator"
echo ""
echo "常用命令："
echo "  查看日志: docker-compose logs -f"
echo "  停止服务: docker-compose stop"
echo "  重启服务: docker-compose restart"
echo "=========================================="
