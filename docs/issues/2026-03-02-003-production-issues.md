# Issue 2026-03-02-003: 生产环境功能验证与监控需求

## 部署状态

✅ **Docker 全服务容器化部署已完成**
- fund-mysql: Healthy
- fund-redis: Healthy  
- fund-collect: Healthy
- fund-api: Healthy
- fund-nginx: 运行中

---

## 问题列表及处理状态

### 问题1: 关注列表功能未展示 ✅ 已修复

**描述**: 
P6阶段实现的关注列表功能在页面上没有显示

**原因**: 
App.vue 导航菜单缺少关注列表入口

**修复**: 
在 App.vue 的 navItems 中添加 `{ path: '/watchlist', name: '关注列表' }`

**验证**: 
- 页面访问: http://localhost:8888/watchlist ✅
- 导航菜单显示正常 ✅

---

### 问题2: 数据缺失问题 - 待排查

**描述**: 
基金详情页仍然存在数据缺失的情况

**需要用户提供**:
1. 具体哪些基金代码缺失数据？
2. 缺失哪些类型的数据？（基本信息/指标/NAV历史/实时估值）
3. 是数据库中就没有，还是页面没显示？

**排查方向**:
- [ ] Python采集未覆盖所有基金
- [ ] 数据库中数据未同步
- [ ] 新实现的实时采集逻辑未触发
- [ ] 空值缓存导致短期内无法重新采集

**排查步骤**:
```bash
# 1. 检查具体基金代码在数据库中是否存在
SELECT * FROM fund_info WHERE fund_code = '缺失的代码';
SELECT * FROM fund_metrics WHERE fund_code = '缺失的代码';
SELECT COUNT(*) FROM fund_nav WHERE fund_code = '缺失的代码';

# 2. 检查Python采集日志
docker logs fund-collect | grep "缺失的代码"

# 3. 检查Java数据补全日志
docker logs fund-api | grep "触发采集"

# 4. 检查Redis空值缓存
redis-cli KEYS "fund:empty:*"
```

---

### 问题3: 监控面板开发 - 待设计

**描述**: 
需要开发监控面板，用于查看API调用链路和Python采集的原始数据

**功能需求**:

#### 3.1 API调用链路监控
- 展示 Java API 调用 Python 采集服务的链路
- 记录每次调用的耗时
- 显示调用成功/失败状态
- 展示请求参数和返回结果摘要

#### 3.2 Python原始数据展示
- 展示 Python 采集服务获取的原始数据
- 包括: 基金基本信息、指标、NAV历史、实时估值等
- 数据格式化展示（JSON视图）
- 支持按基金代码查询

#### 3.3 实时监控指标
- 采集服务健康状态
- API调用成功率
- 平均响应时间
- 数据采集覆盖率

**技术方案待确定**:
```
方案1: 在现有前端增加监控页面
- /monitor/api-trace - API链路监控
- /monitor/raw-data - 原始数据查看
- /monitor/dashboard - 监控仪表盘

方案2: 独立监控服务
- 使用 Prometheus + Grafana
- 或自建轻量级监控面板
```

**接口设计**:
```java
// 监控接口
@GetMapping("/monitor/api-calls")
List<ApiCallLog> getRecentApiCalls();

@GetMapping("/monitor/raw-data/{fundCode}")
RawDataResponse getPythonRawData(@PathVariable String fundCode);

@GetMapping("/monitor/stats")
MonitorStats getSystemStats();
```

---

## 待处理事项

| 事项 | 优先级 | 状态 | 需要用户确认 |
|------|--------|------|--------------|
| 数据缺失排查 | 🔴 高 | 待排查 | ✅ 需要提供具体基金代码 |
| 监控面板设计 | 🟡 中 | 待设计 | ✅ 需要确认技术方案 |
| 关注列表功能 | 🟢 低 | ✅ 已完成 | 无需 |

---

## 下一步行动

### 立即处理
1. **数据缺失问题**
   - 请提供具体缺失数据的基金代码
   - 说明缺失哪些类型的数据
   
2. **监控面板需求**
   - 选择技术方案（前端页面 / Prometheus+Grafana）
   - 确定优先级和紧急程度

### 后续优化
- 完善监控面板功能
- 优化数据采集覆盖率
- 增加更多监控指标

---

**记录时间**: 2026-03-02 23:55 GMT+8  
**更新记录**: 
- 23:55 - 问题1已修复（关注列表导航）
- 23:55 - 等待用户提供问题2、3的详细信息
