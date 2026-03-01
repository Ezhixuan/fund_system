# Task P5-01: 关注列表与交易日历

## 任务信息
| 属性 | 值 |
|------|------|
| Task ID | P5-01 |
| 任务名称 | 关注列表与交易日历 |
| 计划工期 | 4天 |
| 开始日期 | 2026-03-02 |
| 状态 | 🔄 进行中 |

---

## 执行内容

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

## 执行记录

### Day 1 (2026-03-02)

#### 步骤1: 创建数据库表
**执行时间**: 02:09 GMT+8

创建 user_watchlist 表:
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

**Git 提交**: 
- 待提交

---

## 测试报告

测试日志: test-log-P5-01.md

---

**更新日期**: 2026-03-02
