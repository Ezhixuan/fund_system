# 持仓管理功能修复计划

## 问题清单

### P1: 交易净值计算逻辑缺陷
**问题描述**: 用户需要手动输入交易净值，但基金交易规则复杂：
- 工作日 15:00 前买入：使用当日净值（但当日净值要晚上才公布）
- 工作日 15:00 后买入：使用下一交易日净值
- 非工作日买入：使用下一交易日净值

**当前实现**: 用户手动输入净值，容易出错

**修复方案**:
1. **添加交易时间字段** - 记录具体交易时间点
2. **后端自动计算净值日期** - 根据交易时间确定使用哪天的净值
3. **延迟确认机制** - 当日 15:00 前的交易标记为"待确认"，等净值公布后再更新
4. **预估净值** - 使用基金的历史数据预估当日净值（可选）

**实现步骤**:
```sql
-- 1. 修改 portfolio_trade 表结构
ALTER TABLE portfolio_trade ADD COLUMN trade_time TIME AFTER trade_date;
ALTER TABLE portfolio_trade ADD COLUMN nav_confirm_status TINYINT DEFAULT 0 COMMENT '0=待确认,1=已确认';
ALTER TABLE portfolio_trade ADD COLUMN nav_date DATE COMMENT '实际使用的净值日期';
```

```java
// 2. 后端服务计算净值日期
public LocalDate calculateNavDate(LocalDate tradeDate, LocalTime tradeTime) {
    // 判断是否为工作日
    if (!isWorkDay(tradeDate)) {
        return getNextWorkDay(tradeDate);
    }
    // 15:00 后交易
    if (tradeTime != null && tradeTime.isAfter(LocalTime.of(15, 0))) {
        return getNextWorkDay(tradeDate);
    }
    // 15:00 前交易，但当日净值未公布
    return tradeDate; // 标记为待确认
}
```

---

### P2: 添加记录后持仓明细不显示
**问题描述**: 成功添加交易记录后，持仓列表页面空白，不显示任何数据

**可能原因**:
1. 后端接口返回数据格式错误
2. 前端数据解析失败
3. 持仓计算逻辑异常

**排查步骤**:
1. 检查 `/api/portfolio/holdings` 接口返回
2. 检查浏览器控制台错误
3. 检查后端计算持仓的逻辑

**修复方案**:
1. 添加详细日志记录
2. 修复前端空值处理
3. 确保持仓计算正确处理首笔交易

---

### P3: 基金代码未验证
**问题描述**: 可以随意输入不存在的基金代码，系统未做校验

**修复方案**:
1. **前端验证** - 输入时实时查询基金是否存在
2. **后端验证** - 提交时再次验证基金代码
3. **UX优化** - 提供基金搜索选择器，而非纯文本输入

**实现步骤**:
```java
// 后端验证
@PostMapping("/portfolio/trade")
public ApiResponse<?> recordTrade(@RequestBody TradeRequest request) {
    // 验证基金是否存在
    FundInfo fund = fundInfoMapper.selectById(request.getFundCode());
    if (fund == null) {
        return ApiResponse.badRequest("基金代码不存在");
    }
    // ... 继续处理
}
```

```vue
<!-- 前端优化：使用搜索选择器 -->
<el-select 
  v-model="tradeForm.fundCode" 
  filterable
  remote
  :remote-method="searchFund"
  placeholder="输入基金代码或名称"
>
  <el-option 
    v-for="fund in fundOptions" 
    :key="fund.fundCode"
    :label="fund.fundName" 
    :value="fund.fundCode"
  />
</el-select>
```

---

## 修复进度

| 问题 | 优先级 | 状态 | 提交 |
|------|--------|------|------|
| P3 基金代码验证 | P0 | ✅ 已修复 | 5106940 |
| P2 持仓不显示 | P0 | ✅ 已修复 | 5106940 |
| P1 净值计算逻辑 | P1 | ⏳ 待开发 | - |

## 已完成修复

### P2: 持仓不显示 ✅
**原因**: `calculateHolding` 方法中计算收益率时 `totalCost` 为 0 导致除以零异常

**修复**: 添加判断 `totalCost.compareTo(BigDecimal.ZERO) > 0`

### P3: 基金代码验证 ✅
**修复**: 在 `recordTrade` 方法中添加基金代码存在性检查
```java
FundInfo fundInfo = fundInfoMapper.selectById(request.getFundCode());
if (fundInfo == null) {
    throw new IllegalArgumentException("基金代码不存在: " + request.getFundCode());
}
```

## 待修复

### P1: 净值计算逻辑 ⏳
**状态**: 需要进一步讨论实现方案

---

**记录时间**: 2026-03-01
**作者**: OpenClaw
