# Task P5-03: WebSocket 实时推送

## 任务信息
| 属性 | 值 |
|------|------|
| Task ID | P5-03 |
| 任务名称 | WebSocket 实时推送 |
| 计划工期 | 3天 |
| 开始日期 | 2026-03-02 |
| 状态 | 🔄 进行中 |

---

## 执行内容

### Day 1: Spring Boot WebSocket 配置
- [ ] WebSocketConfig 配置类
  - [ ] STOMP协议配置
  - [ ] Endpoint注册
  - [ ] 消息代理配置
- [ ] WebSocketSecurity 安全配置
- [ ] 跨域配置

### Day 2: 推送服务与订阅管理
- [ ] IntradayPushService
  - [ ] pushToFundDetail() - 推送到详情页
  - [ ] pushToPortfolio() - 推送到持仓页
  - [ ] broadcastUpdate() - 广播更新
- [ ] WebSocketSessionManager
  - [ ] subscribeFund() - 订阅基金
  - [ ] unsubscribeFund() - 取消订阅
  - [ ] hasSubscribers() - 检查订阅者
- [ ] Redis缓存集成
  - [ ] 最新估值缓存
  - [ ] 缓存更新逻辑

### Day 3: Python推送触发 + 前端接入
- [ ] Python采集完成后触发推送
  - [ ] HTTP回调Java API
- [ ] 前端 WebSocket 客户端
  - [ ] 详情页订阅
  - [ ] 持仓页订阅
  - [ ] 重连机制

---

## 执行记录

### Day 1 (2026-03-02)

#### 步骤1: 添加WebSocket依赖
**执行时间**: 02:50 GMT+8

