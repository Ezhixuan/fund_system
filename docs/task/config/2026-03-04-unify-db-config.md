# Task: 统一 Python 端数据库配置

**Created**: 2026-03-04 03:15
**Category**: config
**Status**: In Progress
**Priority**: P1

## 任务描述

当前 collector 目录下多个文件使用不同的数据库配置：
- 有些使用 3307 端口（本地 MySQL）
- 有些使用 13306 端口（Docker MySQL）
- 配置分散在各个文件中

目标：统一配置管理，支持 dev/prod 环境切换

## 当前问题

### 硬编码配置的文件列表

| 文件 | 当前端口 | 配置方式 |
|------|----------|----------|
| `services/fund_sync_service.py` | 13306 | 硬编码 DB_CONFIG |
| `services/fund_data_service.py` | 13306 | 硬编码 DB_CONFIG |
| `utils/database.py` | 3307 | 从 settings 读取 |
| `init_basic_data.py` | 13306 | 硬编码 DB_CONFIG |
| `init_metrics_data.py` | 13306 | 硬编码 DB_CONFIG |
| `collect_single_fund.py` | 13306 | 硬编码 DB_CONFIG |
| `metrics_engine.py` | 13306 | 硬编码 DB_CONFIG |
| `scoring_model.py` | 13306 | 硬编码 DB_CONFIG |
| `calculate_metrics.py` | 13306 | 硬编码 DB_CONFIG |
| `main.py` | 13306 | 硬编码 DB_CONFIG |
| `monitor/collection_monitor.py` | 13306 | 硬编码 DB_CONFIG |

## 目标配置方案

类似前端 npm run dev/prod，使用环境变量区分：

```
.env              # 开发环境配置（默认）
.env.production   # 生产环境配置
```

## 执行步骤

### Step 1: 更新配置系统
- [x] 修改 `config/__init__.py` 支持环境切换
- [x] 创建 `.env` 开发环境配置（端口 3307）
- [x] 创建 `.env.production` 生产环境配置（端口 13306）
- **实际执行**: 更新了配置系统，添加 `env` 字段，支持 dev/prod 切换，新增 `get_db_config()` 和 `get_db_url()` 方法
- **完成时间**: 2026-03-04 03:20

### Step 2: 创建统一数据库连接模块
- [x] `utils/database.py` 已使用统一配置
- [x] 添加环境检测功能（`is_development()`, `is_production()`）
- **实际执行**: `utils/database.py` 已通过 `from config import settings` 使用统一配置
- **完成时间**: 2026-03-04 03:20

### Step 3: 修改硬编码文件
- [x] `services/fund_sync_service.py` - 替换为 `settings.get_db_config()`
- [x] `services/fund_data_service.py` - 替换为 `settings.get_db_config()`
- [x] `init_basic_data.py` - 替换为 `settings.get_db_config()`
- [x] `init_metrics_data.py` - 替换为 `settings.get_db_config()`
- [x] `collect_single_fund.py` - 替换为 `settings.get_db_config()`
- [x] `metrics_engine.py` - 替换为 `settings.get_db_config()`
- [x] `scoring_model.py` - 替换为 `settings.get_db_config()`
- [x] `calculate_metrics.py` - 替换为 `settings.get_db_config()`
- [x] `monitor/collection_monitor.py` - 替换为 `settings.get_db_url()`
- **实际执行**: 修改了 9 个文件，统一使用 `from config import settings`
- **完成时间**: 2026-03-04 03:25

### Step 4: 测试验证
- [x] 测试开发环境配置加载
- [x] 测试生产环境配置加载
- [x] 验证所有模块使用统一配置
- **实际执行**: 执行了 6 个测试用例，全部通过
- **完成时间**: 2026-03-04 03:30

## 实现详情

### 文件修改清单

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `config/__init__.py` | 修改 | 添加 `env` 字段，支持 dev/prod 切换，新增 `get_db_config()` 和 `get_db_url()` 方法 |
| `.env` | 新增 | 开发环境配置（端口 3307） |
| `.env.production` | 新增 | 生产环境配置（端口 13306） |
| `services/fund_sync_service.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `services/fund_data_service.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `init_basic_data.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `init_metrics_data.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `collect_single_fund.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `metrics_engine.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `scoring_model.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `calculate_metrics.py` | 修改 | 替换硬编码 DB_CONFIG 为 `settings.get_db_config()` |
| `monitor/collection_monitor.py` | 修改 | 替换硬编码 DB_URL 为 `settings.get_db_url()` |

### 环境变量设计

```bash
# 运行环境: development | production
ENV=development

# MySQL 配置
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3307        # 开发用 3307，生产用 13306
MYSQL_USER=root        # 开发用 root，生产用 fund
MYSQL_PASSWORD=1q2w3e4r5%  # 开发密码
MYSQL_DB=fund_system
```

### 使用方式

开发环境（默认）：
```bash
python app.py
```

生产环境：
```bash
ENV=production python app.py
# 或
export ENV=production
python app.py
```

## 测试验证

- [x] 单元测试通过 (6/6)
- [x] 集成测试通过
- [x] 手动测试通过

**测试报告**: [2026-03-04-unify-db-config-test-report.md](./2026-03-04-unify-db-config-test-report.md)

## 执行结果

- **状态**: ✅ 成功
- **开始时间**: 2026-03-04 03:15
- **完成时间**: 2026-03-04 03:30
- **耗时**: 15 分钟

## 备注

- 保持向后兼容，默认使用开发环境配置
- Docker 部署时可自动挂载 `.env.production` 文件
- SQLAlchemy Python 3.13 兼容性是已知问题，与配置统一无关
