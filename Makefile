# 基金系统 - Makefile
# 常用命令快捷方式

.PHONY: help setup dev build test clean deploy stats

# 默认显示帮助
help:
	@echo "基金交易系统 - 常用命令"
	@echo ""
	@echo "  make setup    - 设置开发环境"
	@echo "  make dev      - 启动开发服务"
	@echo "  make build    - 构建项目"
	@echo "  make test     - 运行测试"
	@echo "  make clean    - 清理构建文件"
	@echo "  make deploy   - 部署到生产环境"
	@echo "  make stats    - 显示代码统计"
	@echo "  make backup   - 备份数据"
	@echo ""

# 设置开发环境
setup:
	@echo "Setting up development environment..."
	@./scripts/dev-setup.sh

# 启动开发服务
dev:
	@echo "Starting development services..."
	@docker start fund-mysql-dev fund-redis-dev 2>/dev/null || echo "Infrastructure already running"
	@echo "MySQL: localhost:3307"
	@echo "Redis: localhost:6379"

# 构建项目
build:
	@echo "Building backend..."
	@cd fund-service && mvn clean package -DskipTests -q
	@echo "Building frontend..."
	@cd fund-view && npm run build
	@echo "Build complete!"

# 运行测试
test:
	@echo "Running backend tests..."
	@cd fund-service && mvn test -q

# 清理构建文件
clean:
	@echo "Cleaning build files..."
	@cd fund-service && mvn clean -q
	@rm -rf fund-view/dist
	@docker system prune -f 2>/dev/null || true
	@echo "Clean complete!"

# 部署到生产环境
deploy:
	@echo "Deploying to production..."
	@cd deploy && ./start.sh

# 代码统计
stats:
	@./scripts/code-stats.sh

# 备份数据
backup:
	@cd deploy && ./backup.sh
