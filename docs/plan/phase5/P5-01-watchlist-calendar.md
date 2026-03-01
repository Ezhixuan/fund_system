# P5-01: 关注列表与交易日历

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P5-01 |
| 名称 | 关注列表与交易日历 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 4天 |
| 依赖 | - |

---

## 需求描述

建立用户自选基金管理体系，实现：
1. 用户只关注自己持有或感兴趣的基金（预计20-50只）
2. 系统只针对这些基金进行数据采集
3. 完整的交易日历支持节假日判断

---

## 数据库设计

### 1. 关注列表表
```sql
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
    UNIQUE KEY uk_fund (fund_code),
    INDEX idx_type (watch_type),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注列表';
```

### 2. 关注基金配置表
```sql
CREATE TABLE watch_fund_config (
    fund_code VARCHAR(10) PRIMARY KEY,
    need_detail TINYINT DEFAULT 1 COMMENT '是否需要详细信息',
    need_nav TINYINT DEFAULT 1 COMMENT '是否需要净值',
    need_intraday TINYINT DEFAULT 1 COMMENT '是否需要实时估值',
    need_portfolio TINYINT DEFAULT 0 COMMENT '是否需要持仓',
    last_collect_date DATE COMMENT '最后采集日期',
    last_intraday_time DATETIME COMMENT '最后估值时间',
    collect_interval_minutes INT DEFAULT 10 COMMENT '估值采集间隔',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注基金采集配置';
```

### 3. 交易日历表
```sql
CREATE TABLE trading_calendar (
    trade_date DATE PRIMARY KEY,
    is_trading_day TINYINT DEFAULT 1 COMMENT '是否为交易日',
    is_holiday TINYINT DEFAULT 0 COMMENT '是否为节假日',
    holiday_name VARCHAR(50) COMMENT '节假日名称',
    prev_trading_day DATE COMMENT '上一交易日',
    next_trading_day DATE COMMENT '下一交易日',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易日历';
```

---

## 实现步骤

### Day 1: 数据库 + 基础实体
- [ ] 创建 user_watchlist 表
- [ ] 创建 watch_fund_config 表
- [ ] 创建 trading_calendar 表
- [ ] Java Entity 类编写
- [ ] Mapper 接口编写

### Day 2: 后端 API 开发
- [ ] WatchlistController
  - [ ] POST /api/watchlist/add
  - [ ] GET /api/watchlist/list
  - [ ] PUT /api/watchlist/{fundCode}
  - [ ] DELETE /api/watchlist/{fundCode}
  - [ ] POST /api/watchlist/import-from-portfolio
- [ ] WatchlistService 业务逻辑
- [ ] 从持仓自动导入功能

### Day 3: 交易日历工具
- [ ] TradingCalendarService
  - [ ] isTradingDay() - 判断是否为交易日
  - [ ] isTradingTime() - 判断是否为交易时间
  - [ ] getPrevTradingDay() - 获取上一交易日
  - [ ] getNextTradingDay() - 获取下一交易日
- [ ] 初始化2024-2025年交易日历数据
- [ ] 节假日数据准备

### Day 4: 前端页面
- [ ] 我的关注页面
  - [ ] 关注基金列表展示
  - [ ] 搜索添加基金功能
  - [ ] 拖拽排序功能
  - [ ] 批量操作（删除、分类）
- [ ] 添加关注弹窗
  - [ ] 基金搜索组件
  - [ ] 关注类型选择
  - [ ] 目标收益/止损设置
- [ ] 首页关注概览组件

---

## API 详细设计

### 添加关注
```java
POST /api/watchlist/add
Request:
{
    "fundCode": "005827",
    "fundName": "易方达蓝筹精选混合",
    "watchType": 1,        // 1-持有, 2-关注
    "targetReturn": 20.0,  // 目标收益率20%
    "stopLoss": -10.0,     // 止损线-10%
    "notes": "张坤管理，长期看好"
}

Response:
{
    "code": 200,
    "message": "添加成功",
    "data": {
        "id": 1,
        "fundCode": "005827",
        ...
    }
}
```

### 获取关注列表
```java
GET /api/watchlist/list?type=1&page=1&size=20
Response:
{
    "code": 200,
    "data": {
        "total": 15,
        "list": [
            {
                "fundCode": "005827",
                "fundName": "易方达蓝筹精选混合",
                "watchType": 1,
                "targetReturn": 20.0,
                "stopLoss": -10.0,
                "addDate": "2024-03-01",
                "sortOrder": 1
            }
        ]
    }
}
```

---

## 交易日历初始化数据

```sql
-- 2024年节假日（示例）
INSERT INTO trading_calendar (trade_date, is_trading_day, is_holiday, holiday_name) VALUES
('2024-01-01', 0, 1, '元旦'),
('2024-02-09', 0, 1, '除夕'),
('2024-02-10', 0, 1, '春节'),
('2024-02-11', 0, 1, '春节'),
('2024-02-12', 0, 1, '春节'),
('2024-02-13', 0, 1, '春节'),
('2024-02-14', 0, 1, '春节'),
('2024-02-15', 0, 1, '春节'),
('2024-02-16', 0, 1, '春节'),
('2024-04-04', 0, 1, '清明节'),
('2024-04-05', 0, 1, '清明节'),
('2024-04-06', 0, 1, '清明节'),
('2024-05-01', 0, 1, '劳动节'),
('2024-05-02', 0, 1, '劳动节'),
('2024-05-03', 0, 1, '劳动节'),
('2024-05-04', 0, 1, '劳动节'),
('2024-05-05', 0, 1, '劳动节'),
('2024-06-10', 0, 1, '端午节'),
('2024-09-15', 0, 1, '中秋节'),
('2024-09-16', 0, 1, '中秋节'),
('2024-09-17', 0, 1, '中秋节'),
('2024-10-01', 0, 1, '国庆节'),
('2024-10-02', 0, 1, '国庆节'),
('2024-10-03', 0, 1, '国庆节'),
('2024-10-04', 0, 1, '国庆节'),
('2024-10-05', 0, 1, '国庆节'),
('2024-10-06', 0, 1, '国庆节'),
('2024-10-07', 0, 1, '国庆节');

-- 更新前后交易日
-- 需要执行存储过程或程序更新 prev_trading_day 和 next_trading_day
```

---

## 验收标准

- [ ] 可以添加关注基金
- [ ] 可以查看关注列表（分页、排序）
- [ ] 可以从持仓自动导入
- [ ] 可以更新/删除关注
- [ ] 交易日历可以判断交易日和交易时间
- [ ] 可以获取上一/下一交易日

---

## 测试计划

测试日志: P5-01-test-log.md

---

**制定日期**: 2026-03-02
