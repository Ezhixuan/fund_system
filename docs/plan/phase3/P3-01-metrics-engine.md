# P3-01: 全维指标计算引擎 - 执行计划

> 工期：4天 | 依赖：P1-02

---

## 核心指标清单

| 指标 | 窗口 | 说明 |
|------|------|------|
| 夏普比率 | 1年/3年 | 收益/波动率 |
| 索提诺比率 | 1年 | 收益/下行风险 |
| 卡玛比率 | 3年 | 收益/最大回撤 |
| 阿尔法 | 1年 | 超额收益 |
| 贝塔 | 1年 | 系统性风险 |
| 信息比率 | 1年 | 主动管理能力 |
| 最大回撤 | 1年/3年 | 历史最大亏损 |
| 波动率 | 1年/3年 | 收益标准差 |

---

## 实现代码

```python
# core/metrics_engine.py
import pandas as pd
import numpy as np
from scipy import stats
from typing import Dict

class MetricsEngine:
    """全维指标计算引擎"""
    
    RISK_FREE = 0.025  # 无风险利率2.5%
    
    def calculate(self, fund_code: str, nav_df: pd.DataFrame) -> Dict:
        """计算全维指标"""
        nav_df = nav_df.sort_values('nav_date')
        nav_df['daily_return'] = nav_df['unit_nav'].pct_change()
        returns = nav_df['daily_return'].dropna()
        
        # 1年/3年数据
        one_year = nav_df.tail(252)
        three_year = nav_df.tail(756) if len(nav_df) >= 756 else nav_df
        
        return {
            'fund_code': fund_code,
            'calc_date': pd.Timestamp.now().strftime('%Y-%m-%d'),
            
            # 收益
            'return_1y': self._annual_return(one_year),
            'return_3y': self._annual_return(three_year),
            
            # 风险调整收益
            'sharpe_ratio_1y': self._sharpe(one_year['daily_return']),
            'sharpe_ratio_3y': self._sharpe(three_year['daily_return']),
            'sortino_ratio_1y': self._sortino(one_year['daily_return']),
            'calmar_ratio_3y': self._calmar(three_year),
            
            # 风险因子
            'alpha_1y': self._alpha(one_year),
            'beta_1y': self._beta(one_year),
            'information_ratio_1y': self._info_ratio(one_year),
            
            # 风险指标
            'max_drawdown_1y': self._max_dd(one_year['unit_nav']),
            'max_drawdown_3y': self._max_dd(three_year['unit_nav']),
            'volatility_1y': self._volatility(one_year['daily_return']),
        }
    
    def _sharpe(self, returns: pd.Series) -> float:
        if len(returns) < 60: return np.nan
        excess = returns.mean() * 252 - self.RISK_FREE
        return excess / (returns.std() * np.sqrt(252)) if returns.std() > 0 else 0
    
    def _sortino(self, returns: pd.Series) -> float:
        downside = returns[returns < 0]
        if len(downside) < 30: return np.nan
        return (returns.mean() * 252 - self.RISK_FREE) / (downside.std() * np.sqrt(252))
    
    def _max_dd(self, nav: pd.Series) -> float:
        cummax = nav.cummax()
        dd = (cummax - nav) / cummax
        return -dd.max()
    
    def _calmar(self, nav_df: pd.DataFrame) -> float:
        ann_return = (nav_df['unit_nav'].iloc[-1] / nav_df['unit_nav'].iloc[0]) ** (252/len(nav_df)) - 1
        max_dd = abs(self._max_dd(nav_df['unit_nav']))
        return ann_return / max_dd if max_dd > 0 else 0
    
    def _alpha(self, nav_df: pd.DataFrame) -> float:
        # 简化：假设市场收益为平均收益
        return 0  # 需接入真实基准指数
    
    def _beta(self, nav_df: pd.DataFrame) -> float:
        return 1  # 简化处理
    
    def _info_ratio(self, nav_df: pd.DataFrame) -> float:
        return 0  # 需接入真实基准指数
    
    def _annual_return(self, nav_df: pd.DataFrame) -> float:
        total = nav_df['unit_nav'].iloc[-1] / nav_df['unit_nav'].iloc[0] - 1
        years = len(nav_df) / 252
        return (1 + total) ** (1/years) - 1 if years > 0 else 0
    
    def _volatility(self, returns: pd.Series) -> float:
        return returns.std() * np.sqrt(252)
```

---

## 验收清单
- [ ] 8+指标计算正确
- [ ] 100只基金计算<5分钟
- [ ] 结果入库fund_metrics
