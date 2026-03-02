# Issue 2026-03-02-003: 生产环境问题诊断与处理

## 部署状态

✅ **Docker 全服务容器化部署已完成**
- fund-mysql: Healthy
- fund-redis: Healthy  
- fund-collect: Healthy
- fund-api: Healthy
- fund-nginx: 运行中

---

## 问题诊断结果

### 问题1: 关注列表功能未展示 ✅ 已修复

**修复**: App.vue 导航菜单添加关注列表入口

**验证**: http://localhost:8888/watchlist

---

### 问题2: 数据缺失 - 已诊断出根本原因

#### 诊断结果

**数据现状**（以基金011452为例）：
```json
{
  "基金基本信息": {
    "fundCode": "011452",
    "fundName": "华泰柏瑞质量成长C",
    "fundType": "混合型-偏股",
    "managerName": null,      // ❌ 缺失
    "companyName": null,      // ❌ 缺失
    "riskLevel": null,        // ❌ 缺失
    "establishDate": null     // ❌ 缺失
  },
  "基金指标": {
    "return1y": 162.4058,     // ✅ 有数据
    "sharpeRatio1y": 2.1892,  // ✅ 有数据
    "maxDrawdown1y": -28.6014 // ✅ 有数据
  },
  "NAV历史": [                // ✅ 有14条数据
    {"navDate": "2026-02-27", "unitNav": 2.1247}
  ],
  "实时估值": {               // ✅ 有数据
    "estimateNav": 2.209,
    "dailyChange": 3.97
  }
}
```

#### 根本原因

**1. 数据库中只有基础信息**
- fund_info 表有基金代码和名称
- 但缺少：基金经理、基金公司、风险等级等详细信息

**2. 数据补全逻辑未触发**
- 新实现的 `FundDataFetchService` 只在数据**完全不存在**时触发采集
- 数据库中有基础记录，所以未触发补全
- **关键字段为null时应该触发补全，但当前逻辑未处理**

**3. 初始数据导入不完整**
- 数据库初始化时只导入了基础信息
- 详细信息和历史数据需要通过Python采集补充

---

## 解决方案

### 立即修复方案（推荐）

#### 方案A: 手动触发数据补全

为缺失基本信息的基金触发采集：

```bash
# 1. 进入监控页面查询缺失数据的基金
http://localhost:8888/monitor

# 2. 在"原始数据查询"中输入基金代码
# 查看哪些字段为null

# 3. 调用强制刷新接口补充数据
curl -X POST "http://localhost:18080/api/funds/{fundCode}/refresh"
```

#### 方案B: 修改数据补全逻辑

修改 `FundDataFetchService.java`，在关键字段为null时也触发采集：

```java
public FundInfo getFundInfo(String fundCode) {
    // 1. 检查空值缓存
    ...
    
    // 2. 查询本地数据库
    FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
    
    // 3. 【修改】数据不完整时也触发采集
    if (fundInfo == null || isInfoIncomplete(fundInfo)) {
        // 触发Python采集
        return fetchAndSaveFromPython(fundCode);
    }
    
    return fundInfo;
}

private boolean isInfoIncomplete(FundInfo info) {
    return StringUtils.isEmpty(info.getManagerName())
        || StringUtils.isEmpty(info.getCompanyName())
        || info.getRiskLevel() == null;
}
```

#### 方案C: 批量数据补充脚本

创建脚本批量补充缺失数据：

```sql
-- 查询缺失基本信息的基金
SELECT fund_code, fund_name 
FROM fund_info 
WHERE manager_name IS NULL 
   OR company_name IS NULL
LIMIT 100;
```

然后批量调用采集接口。

---

### 已完成的优化

✅ **监控面板已上线**（方案A - 前端监控页面）
- 地址: http://localhost:8888/monitor
- 功能:
  - 服务状态监控
  - 数据覆盖情况统计
  - API链路监控
  - 原始数据查询（支持查看缺失字段）
  - 操作日志

---

## 下一步行动

### 需要你确认

1. **选择数据补全方案**：
   - 方案A: 通过监控页面手动补充（适合少量基金）
   - 方案B: 修改代码自动补全（推荐，长期解决）
   - 方案C: 批量脚本补充（适合大量缺失）

2. **确认缺失范围**：
   - 是所有基金都缺失基本信息？
   - 还是只有部分基金？
   - 请在监控页面查询几个基金代码验证

### 建议操作步骤

```bash
# 1. 访问监控页面
open http://localhost:8888/monitor

# 2. 在"原始数据查询"中测试几个基金代码
# 例如: 000001, 011452, 000016

# 3. 确认哪些字段缺失

# 4. 根据缺失情况选择补全方案
```

---

## 监控页面使用说明

### 访问地址
http://localhost:8888/monitor

### 功能模块

1. **服务状态**
   - 实时显示MySQL/Redis/Python/Java/Nginx状态
   - 自动每30秒刷新

2. **数据覆盖情况**
   - 基金总数统计
   - 基本信息/指标/NAV完整度百分比
   - 低于阈值时黄色警告

3. **API链路监控**
   - Java -> Python 内部调用状态
   - 响应时间显示

4. **原始数据查询**
   - 输入基金代码查询完整数据
   - 自动检测缺失字段并提示
   - 支持切换基本信息/指标/NAV/估值标签

5. **操作日志**
   - 记录最近20条查询操作
   - 信息/成功/警告/错误分类显示

---

**记录时间**: 2026-03-03 00:05 GMT+8  
**更新记录**: 
- 00:00 - 问题诊断：数据缺失根因分析
- 00:05 - 监控页面上线，支持数据查询和缺失检测
