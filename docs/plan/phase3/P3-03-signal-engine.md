# P3-03: 决策信号引擎 - 执行计划

> 工期：3天 | 依赖：P3-02

---

## 双轨决策逻辑

```
输入：基金代码
    ↓
获取指标(metrics) + 评分(score)
    ↓
┌─────────────────┬─────────────────┐
│   规则引擎       │   评分模型       │
│   (直观规则)     │   (量化模型)     │
├─────────────────┼─────────────────┤
│ PE<20% → 买入   │ S级 → 优先买入   │
│ 回撤>30% → 卖出 │ D级 → 不建议买入 │
│ 收益>20% → 止盈 │                 │
└─────────────────┴─────────────────┘
    ↓
综合决策：买入/持有/卖出 + 理由
```

---

## Java实现

```java
@Service
public class SignalEngine {
    
    @Autowired
    private FundMetricsMapper metricsMapper;
    
    @Autowired
    private FundScoreMapper scoreMapper;
    
    public TradeSignal generateSignal(String fundCode) {
        FundMetrics m = metricsMapper.selectLatest(fundCode);
        FundScore s = scoreMapper.selectLatest(fundCode);
        
        if (m == null || s == null) {
            return TradeSignal.hold("数据不足");
        }
        
        // 规则评分
        int buyScore = 0, sellScore = 0;
        List<String> reasons = new ArrayList<>();
        
        // 买入规则
        if (m.getPePercentile() != null && m.getPePercentile() < 20) {
            buyScore += 3;
            reasons.add("PE估值低(" + m.getPePercentile() + "%)");
        }
        if (m.getSharpeRatio1y().compareTo(BigDecimal.ONE) > 0 
            && m.getMaxDrawdown1y().compareTo(new BigDecimal("-20")) > 0) {
            buyScore += 2;
            reasons.add("夏普>1且回撤<20%");
        }
        
        // 卖出规则
        if (m.getPePercentile() != null && m.getPePercentile() > 80) {
            sellScore += 3;
            reasons.add("PE估值高(" + m.getPePercentile() + "%)");
        }
        if (m.getReturn1y().compareTo(new BigDecimal("20")) > 0) {
            sellScore += 2;
            reasons.add("收益超20%，止盈");
        }
        
        // 评分干预
        String level = s.getQualityLevel();
        if ("D".equals(level) && buyScore > 0) {
            return TradeSignal.hold("评级D，暂不建议买入");
        }
        
        // 决策
        if (buyScore >= 5 && ("S".equals(level) || "A".equals(level))) {
            return TradeSignal.buy(String.join(";", reasons));
        }
        if (sellScore >= 5) {
            return TradeSignal.sell(String.join(";", reasons));
        }
        
        return TradeSignal.hold("估值合理，建议持有");
    }
}
```

---

## 验收清单
- [ ] 信号生成<100ms
- [ ] 买卖持有三种状态
- [ ] 信号理由清晰
- [ ] 信号历史记录
