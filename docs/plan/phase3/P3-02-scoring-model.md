# P3-02: 评分模型实现 - 执行计划

> 工期：3天 | 依赖：P3-01

---

## 评分权重

| 维度 | 权重 | 计算依据 |
|------|------|----------|
| 收益得分 | 30 | 夏普比率、卡玛比率 |
| 风控得分 | 25 | 最大回撤、波动率 |
| 稳定得分 | 20 | 收益一致性 |
| 规模得分 | 15 | 基金规模适中 |
| 费用得分 | 10 | 管理费率低 |

---

## 实现代码

```python
# core/scoring_model.py
class ScoringModel:
    """基金评分模型"""
    
    def calculate(self, metrics: dict) -> dict:
        """计算综合评分"""
        
        # 1. 收益得分 (30分)
        sharpe = metrics.get('sharpe_ratio_1y', 0)
        calmar = metrics.get('calmar_ratio_3y', 0)
        return_score = self._score_sharpe(sharpe) + self._score_calmar(calmar)
        
        # 2. 风控得分 (25分)
        max_dd = abs(metrics.get('max_drawdown_3y', 0))
        volatility = metrics.get('volatility_1y', 0)
        risk_score = self._score_drawdown(max_dd) + self._score_volatility(volatility)
        
        # 3. 稳定得分 (20分) - 简化
        stability_score = 15 if sharpe > 1 else 10
        
        # 4. 规模得分 (15分) - 简化
        scale_score = 10
        
        # 5. 费用得分 (10分) - 简化
        fee_score = 8
        
        total = min(100, return_score + risk_score + stability_score + scale_score + fee_score)
        
        return {
            'fund_code': metrics['fund_code'],
            'calc_date': metrics['calc_date'],
            'total_score': total,
            'quality_level': self._get_level(total),
            'return_score': return_score,
            'risk_score': risk_score,
            'stability_score': stability_score,
            'scale_score': scale_score,
            'fee_score': fee_score,
            'sharpe_ratio': sharpe,
            'max_drawdown': max_dd,
        }
    
    def _score_sharpe(self, sharpe: float) -> int:
        if sharpe >= 2: return 20
        if sharpe >= 1.5: return 16
        if sharpe >= 1: return 12
        if sharpe >= 0.5: return 8
        return max(0, int(sharpe * 10))
    
    def _score_calmar(self, calmar: float) -> int:
        if calmar >= 3: return 10
        if calmar >= 2: return 8
        if calmar >= 1: return 5
        return 0
    
    def _score_drawdown(self, dd: float) -> int:
        if dd <= 10: return 15
        if dd <= 15: return 12
        if dd <= 20: return 9
        if dd <= 25: return 6
        return 0
    
    def _score_volatility(self, vol: float) -> int:
        if vol <= 12: return 10
        if vol <= 15: return 8
        if vol <= 18: return 5
        return 0
    
    def _get_level(self, score: int) -> str:
        if score >= 90: return 'S'
        if score >= 80: return 'A'
        if score >= 60: return 'B'
        if score >= 40: return 'C'
        return 'D'
```

---

## 验收清单
- [ ] 评分0-100分
- [ ] 等级S/A/B/C/D分布合理
- [ ] 评分结果可解释
