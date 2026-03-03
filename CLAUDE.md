# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

基金交易决策辅助系统 - 基于 Java + Vue3 + Python 的多模块基金分析平台。

- **fund-service/**: Java 后端服务 (Spring Boot 3.2 + MyBatis-Plus)
- **fund-view/**: Vue3 前端 (Vite + Element Plus + ECharts)
- **collector/**: Python 数据采集服务 (Flask + akshare)
- **deploy/**: Docker Compose 部署配置

## 常用命令

### 开发环境

```bash
# 启动基础设施 (MySQL + Redis)
docker compose -f deploy/docker-compose.yml up -d mysql redis
# MySQL: localhost:13306 (root/root123, fund/fund123)
# Redis: localhost:16379

# 启动 Python 采集服务
cd collector
source venv/bin/activate
python app.py
# 服务运行在 http://localhost:5005

cd fund-service
# 编译
mvn clean compile
# 运行
mvn spring-boot:run
# 测试
mvn test
# 打包
mvn clean package -DskipTests

cd fund-view
# 安装依赖
npm install
# 开发模式
npm run dev
# 构建
npm run build
# 预览
npm run preview
```

### Makefile 快捷命令

```bash
make dev      # 启动开发基础设施
make build    # 构建前后端
make test     # 运行后端测试
make deploy   # Docker 部署
```

## 架构说明

### 后端架构 (fund-service)

- **Controller**: REST API 层，位于 `src/main/java/com/fund/controller/`
- **Service**: 业务逻辑层，位于 `src/main/java/com/fund/service/`
- **Mapper**: 数据访问层 (MyBatis-Plus)，位于 `src/main/java/com/fund/mapper/`
- **Entity**: 数据实体，位于 `src/main/java/com/fund/entity/`
- **DTO/VO**: 数据传输对象，位于 `src/main/java/com/fund/dto/` 和 `src/main/java/com/fund/vo/`

关键配置:
- 主配置: `src/main/resources/application.yml`
- 开发配置: `application-dev.yml` (端口 3307)
- 生产配置: `application-prod.yml`
- MyBatis Mapper XML: `src/main/resources/mapper/`

API 文档:
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health

### 前端架构 (fund-view)

- **views/**: 页面组件 (FundList.vue, FundDetail.vue, Portfolio.vue 等)
- **components/**: 可复用组件
- **api/**: API 请求封装
- **router/**: Vue Router 配置
- **stores/**: Pinia 状态管理

### Python 采集服务 (collector)

- **app.py**: Flask 应用入口
- **services/**: 业务服务 (fund_data_service.py)
- **core/**: 核心模块 (collector.py, akshare_client.py, data_pipeline.py)
- **scheduler/**: 定时任务调度
- **config/**: 配置文件

采集服务通过 HTTP API 向 Java 后端提供数据，端口 5000。

### 数据库

- 数据库名: `fund_system`
- 主要表: fund_info, fund_nav_history, fund_metrics, fund_signal, portfolio_trade
- 初始化 SQL: `docs/design/schema.sql`

## 服务依赖关系

```
Nginx (8888) → Vue 前端
     ↓
Java Backend (8080) → MySQL (3306) / Redis (6379)
     ↓
Python Collector (5000) → 外部数据源 (akshare)
```

## 开发注意事项

1. **数据库连接**: 开发环境使用 localhost:13306 (Docker 映射)，配置在 `application-dev.yml`
2. **缓存**: 使用 Redis + Redisson，关键数据有缓存策略
3. **采集服务**: Java 通过 `collector.url` 配置调用 Python 服务
4. **跨域**: 开发环境已配置 CORS，生产环境通过 Nginx 反向代理
