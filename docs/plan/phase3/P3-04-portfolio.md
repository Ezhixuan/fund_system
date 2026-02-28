# P3-04: 持仓管理与分析 - 执行计划

> 工期：3天 | 依赖：P2-01

---

## 核心功能

### 1. 交易记录
- 买入/卖出记录
- 成本计算（多次买入摊薄）
- 手续费记录

### 2. 持仓分析
- 当前持仓市值
- 累计收益/收益率
- 持仓基金数
- 组合风险分布

---

## API设计

```java
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    
    @PostMapping("/trade")
    public ApiResponse<Void> recordTrade(@RequestBody TradeRequest request) {
        // 记录交易
        return ApiResponse.success(null);
    }
    
    @GetMapping("/holdings")
    public ApiResponse<List<HoldingVO>> getHoldings() {
        // 当前持仓
        return ApiResponse.success(portfolioService.getHoldings());
    }
    
    @GetMapping("/analysis")
    public ApiResponse<PortfolioAnalysis> analyze() {
        // 组合分析
        return ApiResponse.success(portfolioService.analyze());
    }
}
```

---

## 收益计算

```java
// 成本法计算
public BigDecimal calculateReturn(List<TradeRecord> trades) {
    BigDecimal totalCost = BigDecimal.ZERO;
    BigDecimal totalShares = BigDecimal.ZERO;
    
    for (TradeRecord trade : trades) {
        if (trade.getTradeType() == 1) { // 买入
            totalCost = totalCost.add(trade.getTradeAmount()).add(trade.getTradeFee());
            totalShares = totalShares.add(trade.getTradeShare());
        }
    }
    
    BigDecimal avgCost = totalCost.divide(totalShares, 4, RoundingMode.HALF_UP);
    BigDecimal currentNav = getLatestNav(fundCode);
    BigDecimal currentValue = currentNav.multiply(totalShares);
    
    // 收益率
    return currentValue.subtract(totalCost)
        .divide(totalCost, 4, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
}
```

---

## 验收清单
- [ ] 交易记录CRUD
- [ ] 成本计算准确
- [ ] 收益计算正确
- [ ] 组合分析维度完整
