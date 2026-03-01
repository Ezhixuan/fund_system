# Phase 5: 精准关注与智能调度 - 主计划

## 版本信息
- **版本**: v1.0
- **制定日期**: 2026-03-02
- **计划工期**: 16天（约3周）
- **依据文档**: 
  - P5-design-v1-final.md（最终版设计）
  - P5-design-v1-interaction.md（交互细节）

---

## 阶段目标

实现"只关注我想关注的，只看我需要看的"核心理念，完成以下核心能力：

1. **我的关注**: 用户自选基金管理系统
2. **准实时估值**: 10分钟间隔采集，动态分时图展示
3. **智能调度**: 前端 → Java → Python 三层调度架构
4. **手动刷新**: 实时获取最新估值，30秒冷却机制
5. **跨交易日处理**: 自动检测切换，历史分时图查看

---

## 子计划清单

| 编号 | 名称 | 工期 | 依赖 | 状态 |
|------|------|------|------|------|
| P5-01 | 关注列表与交易日历 | 4天 | - | ⏳ 待开始 |
| P5-02 | 准实时估值采集系统 | 4天 | P5-01 | ⏳ 待开始 |
| P5-03 | WebSocket实时推送 | 3天 | P5-02 | ⏳ 待开始 |
| P5-04 | 分时图与手动刷新 | 3天 | P5-03 | ⏳ 待开始 |
| P5-05 | 持仓页面集成 | 2天 | P5-04 | ⏳ 待开始 |

**总计**: 16天（约3周）

---

## 里程碑规划

### M1: 基础数据层完成（Week 1）
- [ ] 关注列表功能可用
- [ ] 交易日历数据初始化
- [ ] 准实时估值采集运行

### M2: 实时推送层完成（Week 2）
- [ ] WebSocket推送正常
- [ ] 分时图动态绘制
- [ ] 手动刷新功能可用

### M3: 前端集成完成（Week 3）
- [ ] 持仓页面实时估值
- [ ] 跨交易日处理
- [ ] 整体联调通过

---

## 技术栈确认

| 层级 | 技术组件 | 用途 |
|------|---------|------|
| 前端 | Vue3 + ECharts | 分时图绘制、WebSocket客户端 |
| 前端 | @vueuse/core | WebSocket管理 |
| 后端 | Spring Boot + WebSocket | 实时推送服务 |
| 后端 | OkHttp | Python服务调用 |
| 后端 | Redis | 最新估值缓存、冷却控制 |
| 采集 | Flask + APScheduler | Python采集服务 |
| 采集 | akshare + 备用源 | 多数据源采集 |
| 数据 | MySQL分区表 | 当日点位存储（1个月） |

---

## 关键数据表

### 新建表
1. `user_watchlist` - 用户关注列表
2. `watch_fund_config` - 关注基金采集配置
3. `trading_calendar` - 交易日历（2024-2025）
4. `fund_estimate_intraday` - 实时估值点位（分区表，保留1个月）

### 复用表
- `fund_nav` - 日终净值（已存在）
- `fund_info` - 基金信息（已存在）
- `user_portfolio` - 持仓记录（已存在）

---

## 接口清单

### 关注列表接口
```
POST   /api/watchlist/add                    # 添加关注
GET    /api/watchlist/list                   # 获取关注列表
PUT    /api/watchlist/{fundCode}             # 更新关注
DELETE /api/watchlist/{fundCode}             # 移除关注
POST   /api/watchlist/import-from-portfolio  # 从持仓导入
```

### 实时估值接口
```
GET    /api/fund/{code}/intraday             # 获取当日分时数据
GET    /api/fund/{code}/estimate             # 获取最新估值
POST   /api/fund/{code}/estimate/refresh     # 手动刷新估值
GET    /api/fund/{code}/estimate/history     # 获取历史分时（上一交易日）
```

### WebSocket订阅
```
/topic/fund/{code}/intraday                  # 基金详情页订阅
/user/queue/portfolio/intraday               # 持仓页面订阅
```

---

## 风险点与应对

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| akshare接口变动 | 高 | 准备多数据源备用 |
| WebSocket连接数过多 | 中 | 只推送用户正在查看的基金 |
| 采集频率过高被封IP | 中 | 10分钟间隔 + 异常熔断 |
| 数据准确性偏差 | 低 | 页面标注"仅供参考" |

---

## 前置准备（开发前）

- [ ] akshare实时估值接口调研确认
- [ ] 东方财富/蛋卷备用接口测试
- [ ] 2024-2025年交易日历数据准备
- [ ] MySQL分区表权限确认

---

## 详细子计划

- [P5-01 关注列表与交易日历](./P5-01-watchlist-calendar.md)
- [P5-02 准实时估值采集系统](./P5-02-intraday-collect.md)
- [P5-03 WebSocket实时推送](./P5-03-websocket-push.md)
- [P5-04 分时图与手动刷新](./P5-04-chart-refresh.md)
- [P5-05 持仓页面集成](./P5-05-portfolio-integration.md)

---

**计划制定**: OpenClaw  
**更新日期**: 2026-03-02
