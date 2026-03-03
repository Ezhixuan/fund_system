# Task: 修复 fund_info 表缺少 create_time 字段

**Created**: 2026-03-04 03:37
**Category**: database
**Status**: Completed
**Priority**: High

## 任务描述

基金基础数据同步服务报错：
```
ERROR | services.fund_sync_service:sync_fund_basic_data:222 -
同步基金 970205 失败: (1054, "Unknown column 'create_time' in 'field list'")
```

## 执行步骤

### Step 1: 验证问题
- [x] 检查 fund_info 表结构
- [x] 确认缺少 create_time 字段
- **实际执行**: 使用 DESCRIBE 命令查看表结构，发现确实没有 create_time 字段

### Step 2: 修复数据库
- [x] 执行 ALTER TABLE 添加 create_time 字段
- **实际执行**:
  ```sql
  ALTER TABLE fund_system.fund_info
  ADD COLUMN create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER status;
  ```
- **完成时间**: 2026-03-04 03:38

### Step 3: 更新初始化脚本
- [x] 修改 init.sql 添加 create_time 字段
- **实际执行**: 在 fund_info 表定义中 status 字段后添加 create_time 字段

### Step 4: 验证修复
- [x] 检查表结构确认字段已添加
- [x] 验证 init.sql 已更新
- **完成时间**: 2026-03-04 03:38

## 实现详情

### 文件修改

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `init.sql` | 修改 | 在 fund_info 表添加 create_time 字段 |

### 数据库变更

```sql
ALTER TABLE fund_info
ADD COLUMN create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER status;
```

## 测试验证

- [x] 数据库表结构检查通过
- [x] init.sql 一致性检查通过

**测试结果**:
```
+-------------+-----------+------+-----+-------------------+--------------------------------+
| Field       | Type      | Null | Key | Default           | Extra                          |
+-------------+-----------+------+-----+-------------------+--------------------------------+
| status      | tinyint   | YES  |     | 1                 |                                |
| create_time | timestamp | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED              |
| update_time | timestamp | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update... |
+-------------+-----------+------+-----+-------------------+--------------------------------+
```

## 执行结果

- **状态**: ✅ 成功
- **完成时间**: 2026-03-04 03:38
- **耗时**: 1 分钟

## 备注

- 现有数据会自动获得当前时间作为 create_time
- 修复后基金同步服务应该可以正常工作
