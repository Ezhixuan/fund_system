# P5-01: 关注列表功能

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P5-01 |
| 名称 | 关注列表功能 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 3天 |
| 依赖 | - |

---

## 需求描述
实现"我的关注"功能，用户只添加自己持有或关注的基金，系统只采集这些基金的数据。

---

## 数据库设计

```sql
-- 关注列表表
CREATE TABLE user_watchlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    fund_name VARCHAR(100),
    add_date DATE COMMENT '添加日期',
    watch_type TINYINT COMMENT '1-持有, 2-关注',
    target_return DECIMAL(5,2) COMMENT '目标收益率%',
    stop_loss DECIMAL(5,2) COMMENT '止损线%',
    notes VARCHAR(500) COMMENT '备注',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active TINYINT DEFAULT 1,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund (fund_code)
);

-- 关注基金采集配置
CREATE TABLE watch_fund_config (
    fund_code VARCHAR(10) PRIMARY KEY,
    need_detail TINYINT DEFAULT 1,
    need_nav TINYINT DEFAULT 1,
    need_portfolio TINYINT DEFAULT 0,
    last_collect_date DATE,
    collect_interval_days INT DEFAULT 1
);
```

---

## API 设计

### 后端接口
```java
// 添加关注
POST /api/watchlist/add

// 获取关注列表
GET /api/watchlist/list?type=1

// 更新关注
PUT /api/watchlist/{fundCode}

// 移除关注
DELETE /api/watchlist/{fundCode}

// 从持仓导入
POST /api/watchlist/import-from-portfolio

// 排序调整
PUT /api/watchlist/sort
```

---

## 前端页面

### 1. 我的关注页 (/watchlist)
- 关注基金列表
- 搜索添加基金
- 拖拽排序
- 批量操作

### 2. 首页改造 (/dashboard)
- 关注基金概览
- 今日收益汇总
- 快捷操作入口

---

## 实现步骤

### Day 1: 数据库 + 后端
- [ ] 创建数据库表
- [ ] WatchlistController
- [ ] WatchlistService
- [ ] WatchlistMapper

### Day 2: 前端页面
- [ ] 我的关注页面
- [ ] 搜索添加组件
- [ ] 列表展示组件

### Day 3: 功能完善
- [ ] 从持仓导入
- [ ] 排序功能
- [ ] 首页改造

---

## 验收标准
- [ ] 可以添加关注基金
- [ ] 可以查看关注列表
- [ ] 可以从持仓导入
- [ ] 首页显示关注概览

---

## 测试计划
测试日志: P5-01-test-log.md
