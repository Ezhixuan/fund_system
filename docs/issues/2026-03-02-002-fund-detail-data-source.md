# Issue 2026-03-02-002: 基金详情页数据源优化

## 问题描述

打开基金详情页时，如果数据库中缺少以下信息，当前实现仅返回空值或错误：
- 基金指标信息（metrics）
- 基金走势信息（NAV历史）
- 基金基本信息

## 预期行为

当本地数据库缺少上述数据时，Java 后端服务应主动调用 Python 采集服务获取数据，而非简单返回空值。

## 当前行为

- 直接查询数据库，若无数据则返回 404 或空结果
- 未触发后台数据补全机制

## 建议方案

### 方案一：实时触发采集（推荐）
```
用户请求基金详情
    ↓
Java 服务查询本地数据库
    ↓
数据缺失？
    ↓ 是
调用 Python 采集服务 API
    ↓
Python 服务异步采集数据
    ↓
Java 服务等待/轮询结果
    ↓
返回完整数据给用户
```

### 方案二：异步队列模式
```
用户请求基金详情
    ↓
Java 服务查询本地数据库
    ↓
数据缺失 → 发送采集任务到消息队列
    ↓
返回"数据准备中"提示
    ↓
Python 服务消费队列完成任务
```

## 影响范围

| 模块 | 文件 | 说明 |
|------|------|------|
| Controller | `FundController.java` | 详情查询入口 |
| Service | `FundService.java` | 业务逻辑层 |
| Client | `CollectClient.java` | Python 服务调用 |

## 相关代码位置

- `fund-service/src/main/java/com/fund/controller/FundController.java`
- `fund-service/src/main/java/com/fund/service/FundService.java`
- `fund-service/src/main/java/com/fund/client/CollectClient.java`

## 优先级

🔶 **Medium** - 影响用户体验，需在产品稳定后优化

## 状态

📝 **待处理**

## 备注

TODO 已在 `EstimateService.java` 中添加相关注释，可作为参考实现模式。

---
**记录时间**: 2026-03-02 16:21 GMT+8  
**记录人**: Ezhixuan
