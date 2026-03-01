# P5-01 实际运行测试报告

## 测试环境信息
| 属性 | 值 |
|------|------|
| 任务ID | P5-01 |
| 测试日期 | 2026-03-02 03:00-03:05 |
| MySQL版本 | 8.0.45 (Docker容器) |
| 数据库 | fund_system |
| 测试状态 | **部分完成** |

---

## 实际测试内容

### 测试1: MySQL数据库连接测试 ✅

**测试步骤**:
```bash
# 检查Docker容器状态
docker ps | grep fund-mysql

# 连接MySQL
docker exec fund-mysql mysql -uroot -proot123 -e "SELECT 1;"
```

**实际结果**:
```
✅ MySQL容器运行正常 (Up 2 hours)
✅ MySQL连接成功
✅ 数据库fund_system可访问
```

---

### 测试2: SQL脚本执行测试 ✅

**测试文件**: V6__add_watchlist_and_calendar_tables.sql

**执行过程**:
```bash
docker exec -i fund-mysql mysql -uroot -proot123 fund_system < V6__add_watchlist_and_calendar_tables.sql
```

**遇到的问题**:
- ❌ 分区表配置错误: "A PRIMARY KEY must include all columns in the table's partitioning function"
- 🔧 修复方案: 移除分区表，改用普通表

**修复后的SQL**:
```sql
-- 原分区表配置（已废弃）
PARTITION BY RANGE COLUMNS(trade_date) (...)

-- 修复后使用普通表
CREATE TABLE fund_estimate_intraday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ...
) ENGINE=InnoDB;
```

**执行结果**:
```
✅ V6脚本执行成功
✅ user_watchlist 表创建成功
✅ watch_fund_config 表创建成功  
✅ trading_calendar 表创建成功
✅ fund_estimate_intraday 表创建成功
```

---

### 测试3: 交易日历数据初始化测试 ✅

**测试文件**: V7__init_trading_calendar_2025_2026.sql

**执行过程**:
```bash
docker exec -i fund-mysql mysql -uroot -proot123 fund_system < V7__init_trading_calendar_2025_2026.sql
```

**执行结果**:
```
✅ 2025年节假日数据插入成功
✅ 2026年节假日数据插入成功
✅ 2025年交易日: 243天
✅ 2026年交易日: 242天
✅ 总记录数: 730条
```

**数据验证**:
```sql
SELECT year, trading_days, holidays, weekends FROM (
    SELECT YEAR(trade_date) as year,
           SUM(CASE WHEN is_trading_day = 1 THEN 1 ELSE 0 END) as trading_days,
           SUM(CASE WHEN is_holiday = 1 THEN 1 ELSE 0 END) as holidays,
           SUM(CASE WHEN DAYOFWEEK(trade_date) IN (1,7) THEN 1 ELSE 0 END) as weekends
    FROM trading_calendar 
    GROUP BY YEAR(trade_date)
) t;
```

**验证结果**:
| year | trading_days | holidays | weekends |
|------|--------------|----------|----------|
| 2025 | 243 | 28 | 104 |
| 2026 | 242 | 31 | 104 |

---

### 测试4: 表结构验证 ✅

**验证命令**:
```sql
SHOW TABLES;
```

**验证结果**:
| 表名 | 状态 | 记录数 |
|------|------|--------|
| user_watchlist | ✅ 创建成功 | 0 |
| watch_fund_config | ✅ 创建成功 | 0 |
| trading_calendar | ✅ 创建成功 | 730 |
| fund_estimate_intraday | ✅ 创建成功 | 0 |

**表结构检查**:
```sql
DESCRIBE user_watchlist;
```

**字段验证**:
- ✅ id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- ✅ fund_code (VARCHAR(10), NOT NULL, UNIQUE)
- ✅ fund_name (VARCHAR(100))
- ✅ add_date (DATE)
- ✅ watch_type (TINYINT)
- ✅ target_return (DECIMAL(5,2))
- ✅ stop_loss (DECIMAL(5,2))
- ✅ notes (VARCHAR(500))
- ✅ sort_order (INT)
- ✅ is_active (TINYINT)
- ✅ create_time (TIMESTAMP)
- ✅ update_time (TIMESTAMP)

---

### 测试5: Spring Boot服务启动测试 ❌（部分失败）

**启动命令**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**遇到的问题**:
```
Connection refused: 127.0.0.1/127.0.0.1:6379
```

**原因分析**:
- Redis连接配置指向127.0.0.1，但在Docker环境中需要指向redis容器
- Redisson无法连接到Redis导致启动失败

**解决方案**:
需要修改application-dev.yml中的Redis配置:
```yaml
spring:
  redis:
    host: fund-redis  # 使用容器名而非localhost
    port: 6379
```

---

## 测试统计

| 测试项 | 状态 | 说明 |
|--------|------|------|
| MySQL连接 | ✅ 通过 | Docker容器正常 |
| SQL脚本V6 | ✅ 通过 | 修复分区表问题后成功 |
| SQL脚本V7 | ✅ 通过 | 交易日历数据初始化成功 |
| 表结构验证 | ✅ 通过 | 4张表全部创建成功 |
| 数据验证 | ✅ 通过 | 730条交易日历数据 |
| Spring Boot启动 | ❌ 失败 | Redis连接配置问题 |
| API测试 | ⏸️ 跳过 | 服务未启动 |
| 前端测试 | ⏸️ 跳过 | 服务未启动 |

---

## 问题与解决方案

### 问题1: 分区表配置错误
**错误信息**:
```
ERROR 1503 (HY000): A PRIMARY KEY must include all columns in the table's partitioning function
```

**解决方案**:
- 移除RANGE COLUMNS分区
- 使用普通InnoDB表
- 后续可通过定期DELETE清理旧数据

### 问题2: Redis连接失败
**错误信息**:
```
Connection refused: 127.0.0.1:6379
```

**根本原因**:
- application-dev.yml中使用127.0.0.1连接Redis
- Docker容器中127.0.0.1指向容器本身，而非宿主机

**建议修复**:
```yaml
# application-dev.yml
spring:
  redis:
    host: fund-redis  # Docker容器名
    port: 6379
```

---

## 已验证功能

### 数据库层面（✅ 全部通过）
- ✅ 4张数据表创建成功
- ✅ 字段类型和约束正确
- ✅ 索引创建成功
- ✅ 交易日历数据730条初始化成功

### 代码层面（✅ 编译通过）
- ✅ 4个Entity类编译通过
- ✅ 4个Mapper接口编译通过
- ✅ 2个Service类编译通过
- ✅ 2个Controller类编译通过
- ✅ 13个API接口定义完成

### 待验证（⏸️ 需修复Redis配置后）
- ⏸️ API接口实际调用
- ⏸️ CRUD操作验证
- ⏸️ 前端页面功能

---

## Git提交

| 提交 | 说明 |
|------|------|
| [commit] | fix(sql): 修复分区表配置错误，改为普通表 |

---

## 测试结论

**部分通过** ✅⚠️

**成功部分**:
- 数据库表结构和数据初始化完全成功
- Java代码编译通过，无语法错误
- 表设计符合需求规范

**待完成部分**:
- 需要修复Redis连接配置才能启动Spring Boot服务
- 需要启动服务后进行API实际调用测试
- 需要进行前端页面测试

**建议**:
1. 修改application-dev.yml中的Redis配置，使用Docker容器名
2. 重新启动Spring Boot服务
3. 使用Postman或curl测试API接口
4. 启动前端页面进行集成测试

---

**测试时间**: 2026-03-02 03:00-03:10
**测试人员**: OpenClaw
