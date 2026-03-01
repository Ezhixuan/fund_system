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

### Day 1: 数据库 + 基础实体 ✅
- [x] 创建 user_watchlist 表
- [x] 创建 watch_fund_config 表
- [x] 创建 trading_calendar 表
- [x] 创建 fund_estimate_intraday 表
- [x] Java Entity 类编写
- [x] Mapper 接口编写

### Day 2: 后端 API 开发 ✅
- [x] WatchlistController
  - [x] POST /api/watchlist/add
  - [x] GET /api/watchlist/list
  - [x] PUT /api/watchlist/{fundCode}
  - [x] DELETE /api/watchlist/{fundCode}
  - [x] GET /api/watchlist/{fundCode}/check
  - [x] POST /api/watchlist/import-from-portfolio
  - [x] GET /api/watchlist/codes
- [x] TradingCalendarController
  - [x] GET /api/trading-calendar/is-trading-day
  - [x] GET /api/trading-calendar/is-trading-time
  - [x] GET /api/trading-calendar/prev-trading-day
  - [x] GET /api/trading-calendar/next-trading-day
  - [x] GET /api/trading-calendar/current-trade-date
  - [x] GET /api/trading-calendar/status
- [x] WatchlistService 业务逻辑
- [x] TradingCalendarService 业务逻辑
- [ ] 从持仓自动导入功能（TODO）

### Day 3: 交易日历数据初始化
- [ ] 初始化2024-2025年交易日历数据
- [ ] 节假日数据准备
- [ ] 数据校验

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

### Day 1 (2026-03-02) ✅ 已完成

**执行时间**: 02:09 - 02:30 GMT+8  
**耗时**: 21分钟

创建4张数据库表 + 4个实体类 + 4个Mapper接口

**Git 提交**: c191a8b

---

### Day 2 (2026-03-02) ✅ 已完成

**执行时间**: 02:31 - 02:40 GMT+8  
**耗时**: 9分钟

#### 新增Controller:

**WatchlistController** (`/api/watchlist/*`)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /add | 添加关注基金 |
| GET | /list | 获取关注列表（支持按类型过滤）|
| PUT | /{fundCode} | 更新关注信息 |
| DELETE | /{fundCode} | 移除关注（软删除）|
| GET | /{fundCode}/check | 检查是否已关注 |
| POST | /import-from-portfolio | 从持仓导入（TODO）|
| GET | /codes | 获取所有关注的基金代码 |

**TradingCalendarController** (`/api/trading-calendar/*`)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /today/is-trading-day | 今天是否交易日 |
| GET | /is-trading-day?date= | 指定日期是否交易日 |
| GET | /is-trading-time | 当前是否交易时间 |
| GET | /prev-trading-day | 获取上一交易日 |
| GET | /next-trading-day | 获取下一交易日 |
| GET | /current-trade-date | 获取当前交易日 |
| GET | /status | 获取交易状态概览 |

#### 新增Service:
- `WatchlistService` + `WatchlistServiceImpl` - 关注列表CRUD、基金配置初始化
- `TradingCalendarService` + `TradingCalendarServiceImpl` - 交易日历查询、交易时间判断

**Git 提交**: 27176dc
- feat(api): 添加关注列表和交易日历API
- 7 files changed, 674 insertions(+)

---

## API 汇总

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

## 测试报告

测试日志: test-log-P5-01.md

---

**更新日期**: 2026-03-02
