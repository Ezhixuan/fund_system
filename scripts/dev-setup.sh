#!/bin/bash
# 开发环境一键设置脚本

echo "=========================================="
echo "    基金系统 - 开发环境设置"
echo "=========================================="

# 检查命令
command -v docker >/dev/null 2>&1 || { echo "错误: Docker 未安装"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "错误: Maven 未安装"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "错误: Node.js 未安装"; exit 1; }

echo ""
echo "[1/5] 启动基础设施..."
docker run -d --name fund-mysql-dev -p 3307:3306 -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=fund_system -e MYSQL_USER=fund -e MYSQL_PASSWORD=fund123 --restart unless-stopped mysql:8.0 --default-authentication-plugin=mysql_native_password 2>/dev/null || echo "MySQL 容器已存在"
docker run -d --name fund-redis-dev -p 6379:6379 --restart unless-stopped redis:7-alpine 2>/dev/null || echo "Redis 容器已存在"
echo "  ✓ 基础设施启动完成"

echo ""
echo "[2/5] 等待数据库初始化..."
sleep 10

echo ""
echo "[3/5] 安装后端依赖..."
cd fund-service && mvn dependency:resolve -q && cd ..
echo "  ✓ 后端依赖安装完成"

echo ""
echo "[4/5] 安装前端依赖..."
cd fund-view && npm install && cd ..
echo "  ✓ 前端依赖安装完成"

echo ""
echo "[5/5] 安装Python依赖..."
cd collector && python3 -m venv venv 2>/dev/null; source venv/bin/activate && pip install -q -r requirements.txt && cd ..
echo "  ✓ Python依赖安装完成"

echo ""
echo "=========================================="
echo "    开发环境设置完成!"
echo "=========================================="
