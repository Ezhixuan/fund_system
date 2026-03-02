# Issue 2026-03-02-001: 基金详情页数据缺失问题

## 问题描述

访问基金详情页 http://localhost:8888/fund/011452 时，多个 API 返回错误或空数据。

## 错误信息

| API 端点 | 状态码 | 修复前 | 修复后 |
|----------|--------|--------|--------|
| GET /api/funds/011452 | 200 | ✅ 正常 | ✅ 正常 |
| GET /api/funds/011452/metrics | 404 | 指标数据不存在 | ✅ 正常 |
| GET /api/funds/011452/nav/recent | 500 | 系统繁忙 | ✅ 正常 |
| GET /api/funds/011452/estimate | 500 | 系统繁忙 | ✅ 正常 |

## 修复内容

### 1. 数据缺失修复
- 触发基金 011452 完整数据采集
- 采集净值历史 1220 条
- 计算基金指标完成

### 2. 代码修复
- **FundNavMapper.java**: 修复 `daily_change` -> `daily_return` 字段名错误
- **FundController.java**: 删除重复的 estimate 端点（与 EstimateController 冲突）

## 修复时间

2026-03-02 13:44 GMT+8

## 状态

✅ **已修复**

