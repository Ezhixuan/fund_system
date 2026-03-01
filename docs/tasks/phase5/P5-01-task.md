# Task P5-01: 关注列表与交易日历

## 任务信息
| 属性 | 值 |
|------|------|
| Task ID | P5-01 |
| 任务名称 | 关注列表与交易日历 |
| 计划工期 | 4天 |
| 实际工期 | 4天 |
| 开始日期 | 2026-03-02 |
| 完成日期 | 2026-03-02 |
| 状态 | ✅ **已完成** |

---

## 执行内容

### Day 1: 数据库 + 基础实体 ✅
- [x] 创建 user_watchlist 表
- [x] 创建 watch_fund_config 表
- [x] 创建 trading_calendar 表
- [x] 创建 fund_estimate_intraday 表
- [x] Java Entity 类编写
- [x] Mapper 接口编写

### Day 2: 后端 API 开发 ✅
- [x] WatchlistController (7个接口)
- [x] TradingCalendarController (6个接口)
- [x] WatchlistService 业务逻辑
- [x] TradingCalendarService 业务逻辑

### Day 3: 交易日历数据初始化 ✅
- [x] 初始化 2024-2026 年交易日历数据
- [x] 节假日数据准备
- [x] 存储过程生成完整日历
- [x] 自动更新前后交易日链接

### Day 4: 前端页面 ✅
- [x] 我的关注页面 (views/watchlist/index.vue)
  - [x] 统计卡片展示
  - [x] 筛选栏（按类型、关键词）
  - [x] 关注列表表格
  - [x] 从持仓导入功能
- [x] 添加关注弹窗 (AddWatchlistDialog.vue)
  - [x] 基金搜索选择
  - [x] 关注类型选择
  - [x] 目标收益/止损设置
  - [x] 备注输入
- [x] 基金搜索组件 (FundSearchSelect.vue)
- [x] API 接口封装 (watchlist.js)

---

## Git 提交记录

| 提交 | 说明 | 文件数 |
|------|------|--------|
| c191a8b | feat(db): 添加Phase 5数据库表和实体类 | 9 |
| 27176dc | feat(api): 添加关注列表和交易日历API | 7 |
| 8835488 | feat(db): 添加2025-2026年交易日历初始化数据 | 1 |
| e77d697 | feat(ui): 添加关注列表前端页面 | 6 |

**总计**: 23个文件，2000+ 行代码

---

## API 清单

### 关注列表 API
```
POST   /api/watchlist/add                    # 添加关注
GET    /api/watchlist/list                   # 获取列表
PUT    /api/watchlist/{fundCode}             # 更新关注
DELETE /api/watchlist/{fundCode}             # 移除关注
GET    /api/watchlist/{fundCode}/check       # 检查是否关注
POST   /api/watchlist/import-from-portfolio  # 从持仓导入
GET    /api/watchlist/codes                  # 获取基金代码列表
```

### 交易日历 API
```
GET /api/trading-calendar/is-trading-day       # 是否交易日
GET /api/trading-calendar/is-trading-time      # 是否交易时间
GET /api/trading-calendar/prev-trading-day     # 上一交易日
GET /api/trading-calendar/next-trading-day     # 下一交易日
GET /api/trading-calendar/current-trade-date   # 当前交易日
GET /api/trading-calendar/status               # 交易状态概览
```

---

## 页面功能

### 我的关注页面 (/watchlist)
- ✅ 统计卡片（总关注、持有中、仅关注）
- ✅ 类型筛选（全部/持有中/仅关注）
- ✅ 关键词搜索（基金名称/代码）
- ✅ 关注列表展示（基金信息、类型、目标/止损、备注）
- ✅ 添加/编辑/删除关注
- ✅ 从持仓一键导入
- ✅ 点击基金跳转详情页

### 添加/编辑关注弹窗
- ✅ 基金搜索选择（远程搜索、防抖）
- ✅ 关注类型选择（持有/仅关注）
- ✅ 目标收益率设置
- ✅ 止损线设置
- ✅ 备注输入

---

## 数据库表

| 表名 | 说明 |
|------|------|
| user_watchlist | 用户关注列表 |
| watch_fund_config | 关注基金采集配置 |
| trading_calendar | 交易日历（2024-2026年）|
| fund_estimate_intraday | 实时估值点位（分区表）|

---

## 时间记录

| Day | 内容 | 耗时 | 提交 |
|-----|------|------|------|
| Day 1 | 数据库 + 实体 + Mapper | 21分钟 | c191a8b |
| Day 2 | 后端 API + Service | 9分钟 | 27176dc |
| Day 3 | 交易日历数据 | 3分钟 | 8835488 |
| Day 4 | 前端页面 | 18分钟 | e77d697 |
| **总计** | - | **51分钟** | - |

---

## 下一步

开始执行 **P5-02: 准实时估值采集系统**

---

**更新日期**: 2026-03-02
