# 基金系统功能升级设计方案

## 概述

本文档包含以下功能的设计方案：
1. 交易净值简化方案（方案A）
2. 当日实时估值功能
3. 持仓编辑功能
4. 基金列表翻页修复
5. 基金类型模糊搜索
6. 板块划分功能

---

## 1. 交易净值简化方案（方案A）

### 需求
用户选择日期和份额，系统次日自动填入实际净值

### 数据流

```
用户操作: 选择日期 + 份额 → 提交交易
          ↓
系统处理: 计算净值日期（根据交易时间规则）
          ↓
          记录交易：份额确认，价格=待确认，金额=待确认
          ↓
次日任务: 自动查询实际净值
          ↓
          更新交易记录：价格=实际净值，金额=份额×价格
          ↓
          重新计算持仓成本
```

### 数据库变更

```sql
-- 添加确认状态字段
ALTER TABLE portfolio_trade 
ADD COLUMN confirm_status TINYINT DEFAULT 0 COMMENT '0=待确认,1=已确认',
ADD COLUMN nav_date DATE COMMENT '实际使用的净值日期',
ADD COLUMN trade_time TIME COMMENT '交易时间（可选）';
```

### 核心逻辑

```java
// 计算净值日期
public LocalDate calculateNavDate(LocalDate tradeDate, LocalTime tradeTime) {
    // 15:00 后或非工作日 → 下一工作日
    if (isAfterMarketClose(tradeTime) || !isWorkDay(tradeDate)) {
        return getNextWorkDay(tradeDate);
    }
    return tradeDate;
}

// 定时任务：每日 21:00 确认当日交易
@Scheduled(cron = "0 0 21 * * MON-FRI")
public void confirmTodayTrades() {
    // 查询待确认的交易
    // 查询实际净值
    // 更新交易记录
}
```

---

## 2. 当日实时估值功能

### 需求分析

| 场景 | 触发条件 | 缓存时间 | 功能 |
|------|---------|---------|------|
| 基金详情页 | 用户点击进入 | 5分钟 | 显示实时估值、涨跌幅 |
| 持仓管理页 | 打开持仓页面 | 5分钟 | 显示各基金估值、预估收益 |
| 刷新按钮 | 用户主动点击 | 立即更新 | 幂等控制防频繁刷新 |

### 技术方案

#### 2.1 估值数据来源

```python
# 使用 akshare 获取实时估值
import akshare as ak

# 获取单只基金实时估值
def get_fund_estimate(fund_code):
    """
    返回: {
        'fund_code': '011452',
        'estimate_nav': 2.1456,      # 预估净值
        'estimate_time': '14:32:00',  # 估值时间
        'daily_change': 0.98,         # 日涨跌幅%
        'source': 'akshare'
    }
    """
    df = ak.fund_value_estimate_em(symbol=fund_code)
    # 解析返回数据
    ...
```

#### 2.2 缓存策略

```java
// Redis 缓存配置
@Cacheable(value = "fund:estimate", key = "#fundCode", unless = "#result == null")
@CacheExpire(300) // 5分钟过期
public FundEstimate getEstimate(String fundCode) {
    // 查询实时估值
}

// 刷新接口 - 强制更新缓存
@PostMapping("/{fundCode}/estimate/refresh")
@RateLimiter(key = "#fundCode", rate = 1, interval = 60) // 1分钟1次
public FundEstimate refreshEstimate(@PathVariable String fundCode) {
    // 清除缓存并重新获取
    cacheManager.getCache("fund:estimate").evict(fundCode);
    return getEstimate(fundCode);
}
```

#### 2.3 API 设计

```java
// 获取基金实时估值
GET /api/funds/{fundCode}/estimate

Response:
{
    "code": 200,
    "data": {
        "fundCode": "011452",
        "estimateNav": 2.1456,        // 预估净值
        "estimateTime": "14:32:00",    // 估值时间
        "dailyChange": 0.98,           // 日涨跌幅%
        "previousNav": 2.1247,         // 昨日净值
        "updateTime": "2026-03-01T14:32:00"
    }
}

// 刷新估值（幂等控制）
POST /api/funds/{fundCode}/estimate/refresh

Response:
{
    "code": 200,
    "data": { ... },
    "message": "刷新成功"
}
```

#### 2.4 前端展示

```vue
<!-- 基金详情页 -->
<template>
  <div class="estimate-card">
    <div class="estimate-header">
      <span>实时估值</span>
      <button @click="refreshEstimate" :disabled="refreshing">
        {{ refreshing ? '刷新中...' : '🔄 刷新' }}
      </button>
    </div>
    <div class="estimate-value" :class="changeClass">
      {{ estimateNav }}
      <span class="change">{{ dailyChange > 0 ? '+' : '' }}{{ dailyChange }}%</span>
    </div>
    <div class="estimate-time">更新时间: {{ estimateTime }}</div>
    <div v-if="isMarketOpen" class="estimate-hint">⚠️ 估值仅供参考，以实际净值为准</div>
  </div>
</template>

<script>
const isMarketOpen = () => {
  const now = new Date();
  const hour = now.getHours();
  const minute = now.getMinutes();
  const day = now.getDay();
  
  // 工作日 9:30 - 15:00
  if (day === 0 || day === 6) return false;
  if (hour < 9 || (hour === 9 && minute < 30)) return false;
  if (hour > 15 || (hour === 15 && minute > 0)) return false;
  return true;
}
</script>
```

#### 2.5 持仓页预估收益

```java
// 持仓预估收益计算
public HoldingEstimate calculateHoldingEstimate(String fundCode, BigDecimal shares) {
    FundEstimate estimate = getEstimate(fundCode);
    if (estimate == null) return null;
    
    BigDecimal estimateValue = estimate.getEstimateNav().multiply(shares);
    BigDecimal previousValue = estimate.getPreviousNav().multiply(shares);
    BigDecimal dailyReturn = estimateValue.subtract(previousValue);
    
    return new HoldingEstimate(
        estimate.getEstimateNav(),
        estimateValue,
        dailyReturn,
        estimate.getDailyChange()
    );
}
```

---

## 3. 持仓编辑功能

### 需求
持仓列表中的基金支持编辑/删除，不再直接跳转到详情页

### UI 交互设计

```
┌─────────────────────────────────────────┐
│ 持仓明细 (3只基金)                        │
├─────────────────────────────────────────┤
│ 基金名称    份额      成本      收益    [操作]│
├─────────────────────────────────────────┤
│ 华夏成长    1500     1.228    -6.3%   [编][删]│
│ 华泰柏瑞    1000     待确认    +0.0%   [编][删]│
└─────────────────────────────────────────┘

点击[编辑]:
┌─────────────────────────────────────────┐
│ 编辑持仓                                  │
├─────────────────────────────────────────┤
│ 基金: 华夏成长证券投资基金                 │
│ 当前份额: 1500                            │
│ 修改份额: [_________]                     │
│                                         │
│ [取消]                  [保存]           │
└─────────────────────────────────────────┘
```

### API 设计

```java
// 更新持仓（修改份额）
PUT /api/portfolio/holdings/{fundCode}

Request:
{
    "totalShares": 2000,  // 新的份额
    "remark": "调整持仓"
}

// 删除持仓
delete /api/portfolio/holdings/{fundCode}

Response:
{
    "code": 200,
    "message": "删除成功"
}
```

### 实现方案

```java
// 更新持仓 - 通过添加一笔调整交易实现
@PutMapping("/portfolio/holdings/{fundCode}")
public ApiResponse<Void> updateHolding(
        @PathVariable String fundCode,
        @RequestBody HoldingUpdateRequest request) {
    
    // 计算差额
    BigDecimal currentShares = getCurrentShares(fundCode);
    BigDecimal diff = request.getTotalShares().subtract(currentShares);
    
    if (diff.compareTo(BigDecimal.ZERO) != 0) {
        // 添加调整交易
        PortfolioTrade adjustTrade = new PortfolioTrade();
        adjustTrade.setFundCode(fundCode);
        adjustTrade.setTradeDate(LocalDate.now());
        adjustTrade.setTradeType(diff.compareTo(BigDecimal.ZERO) > 0 ? 1 : 2); // 买或卖
        adjustTrade.setTradeShare(diff.abs());
        adjustTrade.setTradePrice(getLatestNav(fundCode)); // 使用最新净值
        adjustTrade.setRemark("持仓调整: " + request.getRemark());
        tradeMapper.insert(adjustTrade);
    }
    
    return ApiResponse.success();
}
```

---

## 4. 基金列表翻页修复

### 问题
翻页一直显示第一页数据

### 排查思路

```java
// 检查 FundController.listFunds 方法
@GetMapping
public ApiResponse<IPage<FundInfoVO>> listFunds(
        @RequestParam(defaultValue = "1") Integer page,  // 是否接收正确？
        @RequestParam(defaultValue = "20") Integer size,
        ...
) {
    // 检查 Page 对象是否正确传递
    Page<FundInfo> pageParam = new Page<>(page, size);
    
    // 检查返回结果是否包含分页信息
    return ApiResponse.success(fundService.listFunds(pageParam, ...));
}
```

### 修复方案

```java
// 确保前端传递正确的参数名
// 前端: ?page=2&size=20
// 后端正确接收

// 如果前端使用 pageNum 而不是 page
@RequestParam(value = "page", defaultValue = "1") Integer page
// 或
@RequestParam(value = "pageNum", defaultValue = "1") Integer page
```

---

## 5. 基金类型模糊搜索

### 问题
搜索"股票型"无数据，实际类型可能是"股票型-普通股票"

### 修复方案

```java
// 修改 FundServiceImpl.listFunds
if (StringUtils.hasText(fundType)) {
    // 改为模糊匹配
    wrapper.like(FundInfo::getFundType, fundType);
    // 而不是 wrapper.eq(FundInfo::getFundType, fundType);
}
```

### 类型映射优化

```java
// 提供标准化类型选项
public static final Map<String, List<String>> TYPE_MAPPING = Map.of(
    "股票型", List.of("股票型", "股票型-普通股票", "股票型-指数型"),
    "混合型", List.of("混合型", "混合型-偏股", "混合型-偏债", "混合型-灵活配置"),
    "债券型", List.of("债券型", "债券型-纯债", "债券型-混合债"),
    "货币型", List.of("货币型", "货币型-普通货币"),
    "QDII", List.of("QDII", "QDII-普通股票", "QDII-混合型")
);

// 搜索时展开类型
if (TYPE_MAPPING.containsKey(fundType)) {
    wrapper.in(FundInfo::getFundType, TYPE_MAPPING.get(fundType));
} else {
    wrapper.like(FundInfo::getFundType, fundType);
}
```

---

## 6. 板块划分功能

### 需求
通过板块（行业/主题）划分基金，比单纯搜索更好挑选

### 设计方案

#### 6.1 数据库设计

```sql
-- 板块表
CREATE TABLE fund_sector (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sector_code VARCHAR(20) NOT NULL COMMENT '板块代码',
    sector_name VARCHAR(50) NOT NULL COMMENT '板块名称',
    sector_type VARCHAR(20) COMMENT '板块类型: industry=行业, theme=主题, style=风格',
    parent_id BIGINT COMMENT '父板块ID',
    description VARCHAR(200),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sector_code (sector_code)
);

-- 基金-板块关联表
CREATE TABLE fund_sector_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    sector_code VARCHAR(20) NOT NULL,
    weight DECIMAL(5,2) COMMENT '基金在该板块的权重',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_sector (fund_code, sector_code)
);
```

#### 6.2 板块分类示例

| 板块类型 | 板块示例 |
|---------|---------|
| 行业板块 | 消费、医药、科技、金融、新能源 |
| 主题板块 | 人工智能、芯片、5G、碳中和 |
| 风格板块 | 价值、成长、均衡、大盘、小盘 |
| 市场板块 | A股、港股、美股、全球市场 |

#### 6.3 板块数据填充

```python
# 通过持仓股票反推板块
# 1. 获取基金持仓
# 2. 查询股票所属行业
# 3. 按权重计算基金板块分布
# 4. 存入 fund_sector_mapping
```

#### 6.4 API 设计

```java
// 获取所有板块
GET /api/sectors

Response:
{
    "code": 200,
    "data": [
        {
            "sectorCode": "tech",
            "sectorName": "科技",
            "sectorType": "industry",
            "fundCount": 156,
            "avgReturn1y": 25.6
        }
    ]
}

// 获取板块内的基金
GET /api/sectors/{sectorCode}/funds?page=1&size=20

// 获取基金所属板块
GET /api/funds/{fundCode}/sectors
```

#### 6.5 前端展示

```vue
<!-- 基金列表页增加板块筛选 -->
<div class="sector-filter">
  <div class="sector-group">
    <span class="group-title">热门板块</span>
    <div class="sector-tags">
      <span 
        v-for="sector in hotSectors" 
        :key="sector.code"
        class="sector-tag"
        :class="{ active: selectedSector === sector.code }"
        @click="filterBySector(sector.code)"
      >
        {{ sector.name }}
        <small>{{ sector.fundCount }}只</small>
      </span>
    </div>
  </div>
</div>
```

---

## 实施优先级

| 功能 | 优先级 | 预计工时 | 依赖 |
|------|--------|---------|------|
| 4. 翻页修复 | P0 | 30分钟 | 无 |
| 5. 类型模糊搜索 | P0 | 30分钟 | 无 |
| 2. 当日估值 | P1 | 3-4小时 | 无 |
| 3. 持仓编辑 | P1 | 2小时 | 无 |
| 1. 方案A | P2 | 3小时 | 无 |
| 6. 板块划分 | P2 | 4-6小时 | 需要持仓数据 |

---

## 下一步

1. ✅ 设计方案已完成
2. ⏳ 等待确认优先级
3. ⏳ 开始实施

**请确认：**
1. 实施顺序是否调整？
2. 当日估值功能是否需要预估净值计算，还是仅展示已有数据？
3. 板块划分是否需要先做数据调研（统计现有基金类型分布）？
