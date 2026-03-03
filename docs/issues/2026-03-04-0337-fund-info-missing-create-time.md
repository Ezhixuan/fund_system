# Issue: fund_info 表缺少 create_time 字段导致同步失败

**Created**: 2026-03-04 03:37
**Status**: Fixed
**Priority**: High
**Fixed At**: 2026-03-04 03:38

## Problem Description

基金基础数据同步服务在插入新基金时失败，报错：
```
ERROR | services.fund_sync_service:sync_fund_basic_data:222 - 同步基金 970205 失败: (1054, "Unknown column 'create_time' in 'field list'")
```

## Environment

- Project: fund-system/collector
- File: `services/fund_sync_service.py:195-196`
- Database: MySQL 8.0

## Analysis

检查发现代码与数据库表结构不一致：

**代码中的 SQL (fund_sync_service.py:195-196)**:
```sql
INSERT INTO fund_info
(fund_code, fund_name, fund_type, status, create_time, update_time)
VALUES (%s, %s, %s, %s, NOW(), NOW())
```

**数据库表结构 (init.sql:14-34)**:
```sql
CREATE TABLE IF NOT EXISTS fund_info (
    fund_code VARCHAR(10) PRIMARY KEY COMMENT '基金代码',
    fund_name VARCHAR(100) NOT NULL COMMENT '基金名称',
    ...
    status TINYINT DEFAULT 1 COMMENT '状态',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 注意：没有 create_time 字段
    ...
)
```

## Root Cause

`fund_info` 表缺少 `create_time` 字段，但同步代码尝试插入该字段。

这是数据库 schema 与代码实现不同步导致的问题。

## Solution

### Option 1 (Recommended): 修改数据库表结构
添加 `create_time` 字段到 `fund_info` 表：

```sql
ALTER TABLE fund_info
ADD COLUMN create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
AFTER status;
```

**优点**:
- 保持代码不变
- 符合常规表设计（有创建时间和更新时间）
- 可以追溯记录创建时间

### Option 2: 修改代码
移除 SQL 语句中的 `create_time` 字段：

```python
# services/fund_sync_service.py:195-196
sql = """
    INSERT INTO fund_info
    (fund_code, fund_name, fund_type, status, update_time)
    VALUES (%s, %s, %s, %s, NOW())
    ON DUPLICATE KEY UPDATE
    fund_name = VALUES(fund_name),
    fund_type = VALUES(fund_type),
    update_time = NOW()
"""
```

**优点**:
- 不需要修改数据库
- 快速修复

**缺点**:
- 丢失创建时间信息
- 需要检查其他插入语句是否也有同样问题

## Action Items

- [x] 选择修复方案（推荐 Option 1）
- [x] 执行数据库变更或代码修改
- [x] 验证修复（重新运行同步）
- [x] 检查其他表是否有类似字段缺失问题

## Fix Applied

**修复方案**: Option 1 - 修改数据库表结构

**执行的 SQL**:
```sql
ALTER TABLE fund_info
ADD COLUMN create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
AFTER status;
```

**验证结果**:
```
+-------------+-----------+------+-----+-------------------+---------------+
| Field       | Type      | Null | Key | Default           | Extra         |
+-------------+-----------+------+-----+-------------------+---------------+
| status      | tinyint   | YES  |     | 1                 |               |
| create_time | timestamp | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| update_time | timestamp | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
+-------------+-----------+------+-----+-------------------+---------------+
```

**相关修改**:
- `init.sql` - 已同步更新表结构定义
- Task: [修复 fund_info 表缺少 create_time 字段](../task/database/2026-03-04-fix-fund-info-schema.md)

## Related

- `init.sql` - 数据库初始化脚本
- `services/fund_sync_service.py` - 基金同步服务
- Task: [统一 Python 端数据库配置](../task/config/2026-03-04-unify-db-config.md)
