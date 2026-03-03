#!/usr/bin/env python3
"""
基金评分模型
五维评分体系：收益/风控/稳定/规模/费用
"""
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pymysql
from typing import Dict, Optional
from dataclasses import dataclass

from config import settings

# 数据库配置（从统一配置读取）
DB_CONFIG = settings.get_db_config()


@dataclass
class ScoreResult:
    """评分结果"""
    fund_code: str
    calc_date: str
    total_score: int
    quality_level: str
    return_score: int
    risk_score: int
    stability_score: int
    scale_score: int
    fee_score: int
    sharpe_ratio: float
    max_drawdown: float


class ScoringModel:
    """基金评分模型"""
    
    # 权重配置
    WEIGHTS = {
        'return': 30,
        'risk': 25,
        'stability': 20,
        'scale': 15,
        'fee': 10
    }
    
    def calculate(self, metrics: dict) -> Optional[ScoreResult]:
        """计算综合评分"""
        if not metrics:
            return None
        
        sharpe = metrics.get('sharpe_ratio_1y') or 0
        calmar = metrics.get('calmar_ratio_3y') or 0
        max_dd = metrics.get('max_drawdown_1y') or 0
        volatility = metrics.get('volatility_1y') or 0
        scale = metrics.get('current_scale')
        fee = metrics.get('management_fee')
        
        # 1. 收益得分 (30分) - 夏普16 + 卡玛14
        sharpe_score = self._score_sharpe(sharpe)
        calmar_score = self._score_calmar(calmar)
        return_score = min(30, sharpe_score + calmar_score)
        
        # 2. 风控得分 (25分) - 回撤15 + 波动10
        dd_score = self._score_drawdown(abs(max_dd))
        vol_score = self._score_volatility(volatility)
        risk_score = min(25, dd_score + vol_score)
        
        # 3. 稳定得分 (20分) - 基于夏普稳定性
        stability_score = self._score_stability(sharpe, volatility)
        
        # 4. 规模得分 (15分) - 规模适中
        scale_score = self._score_scale(scale)
        
        # 5. 费用得分 (10分) - 费率越低越好
        fee_score = self._score_fee(fee)
        
        # 总分
        total = return_score + risk_score + stability_score + scale_score + fee_score
        total = min(100, max(0, total))
        
        return ScoreResult(
            fund_code=metrics['fund_code'],
            calc_date=metrics['calc_date'],
            total_score=total,
            quality_level=self._get_level(total),
            return_score=return_score,
            risk_score=risk_score,
            stability_score=stability_score,
            scale_score=scale_score,
            fee_score=fee_score,
            sharpe_ratio=sharpe,
            max_drawdown=max_dd
        )
    
    def _score_sharpe(self, sharpe: float) -> int:
        """夏普比率得分 (16分满)"""
        if sharpe >= 2.5: return 16
        if sharpe >= 2.0: return 14
        if sharpe >= 1.5: return 12
        if sharpe >= 1.0: return 9
        if sharpe >= 0.5: return 6
        return max(0, int(sharpe * 8))
    
    def _score_calmar(self, calmar: float) -> int:
        """卡玛比率得分 (14分满)"""
        if calmar >= 3: return 14
        if calmar >= 2: return 11
        if calmar >= 1.5: return 8
        if calmar >= 1: return 5
        if calmar >= 0.5: return 2
        return 0
    
    def _score_drawdown(self, dd: float) -> int:
        """最大回撤得分 (15分满) - 越小越好"""
        if dd <= 10: return 15
        if dd <= 15: return 12
        if dd <= 20: return 9
        if dd <= 25: return 6
        if dd <= 30: return 3
        return 0
    
    def _score_volatility(self, vol: float) -> int:
        """波动率得分 (10分满) - 越小越好"""
        if vol <= 10: return 10
        if vol <= 15: return 8
        if vol <= 20: return 5
        if vol <= 25: return 2
        return 0
    
    def _score_stability(self, sharpe: float, vol: float) -> int:
        """稳定性得分 (20分满)"""
        score = 0
        # 夏普稳定性
        if sharpe >= 1.5: score += 12
        elif sharpe >= 1.0: score += 8
        elif sharpe >= 0.5: score += 4
        
        # 波动稳定性
        if vol <= 15: score += 8
        elif vol <= 20: score += 5
        elif vol <= 25: score += 2
        
        return min(20, score)
    
    def _score_scale(self, scale) -> int:
        """规模得分 (15分满) - 适中最好"""
        if scale is None:
            return 8  # 默认值
        
        scale_val = float(scale) if scale else 0
        # 2-50亿最佳
        if 2 <= scale_val <= 50:
            return 15
        elif 0.5 <= scale_val < 2:
            return 12
        elif 50 < scale_val <= 100:
            return 10
        elif scale_val > 200:  # 太大
            return 5
        return 8
    
    def _score_fee(self, fee) -> int:
        """费用得分 (10分满) - 越低越好"""
        if fee is None:
            return 5  # 默认值
        
        fee_val = float(fee) if fee else 1.5
        if fee_val <= 0.5: return 10
        if fee_val <= 1.0: return 8
        if fee_val <= 1.5: return 6
        if fee_val <= 2.0: return 3
        return 0
    
    def _get_level(self, score: int) -> str:
        """获取等级"""
        if score >= 90: return 'S'
        if score >= 80: return 'A'
        if score >= 60: return 'B'
        if score >= 40: return 'C'
        return 'D'


class ScoreService:
    """评分服务"""
    
    def __init__(self):
        self.conn = pymysql.connect(**DB_CONFIG)
        self.model = ScoringModel()
    
    def close(self):
        self.conn.close()
    
    def get_metrics(self, fund_code: str) -> Optional[dict]:
        """获取基金指标"""
        with self.conn.cursor(pymysql.cursors.DictCursor) as cursor:
            sql = """
            SELECT m.*, f.current_scale, f.management_fee
            FROM fund_metrics m
            LEFT JOIN fund_info f ON m.fund_code = f.fund_code
            WHERE m.fund_code = %s
            ORDER BY m.calc_date DESC
            LIMIT 1
            """
            cursor.execute(sql, (fund_code,))
            return cursor.fetchone()
    
    def calculate_score(self, fund_code: str) -> Optional[ScoreResult]:
        """计算单只基金评分"""
        metrics = self.get_metrics(fund_code)
        if not metrics:
            return None
        return self.model.calculate(metrics)
    
    def batch_calculate(self, limit: int = 100) -> tuple:
        """批量计算评分"""
        with self.conn.cursor() as cursor:
            cursor.execute("SELECT DISTINCT fund_code FROM fund_metrics LIMIT %s", (limit,))
            fund_codes = [row[0] for row in cursor.fetchall()]
        
        print(f"开始计算 {len(fund_codes)} 只基金的评分...")
        
        results = []
        for i, code in enumerate(fund_codes):
            score = self.calculate_score(code)
            if score:
                results.append(score)
            if (i + 1) % 20 == 0:
                print(f"  进度: {i+1}/{len(fund_codes)}")
        
        return results
    
    def analyze_distribution(self, results: list) -> dict:
        """分析评分分布"""
        if not results:
            return {}
        
        total = len(results)
        levels = {'S': 0, 'A': 0, 'B': 0, 'C': 0, 'D': 0}
        
        for r in results:
            levels[r.quality_level] = levels.get(r.quality_level, 0) + 1
        
        return {
            'total': total,
            'average_score': sum(r.total_score for r in results) / total,
            'distribution': {k: f"{v/total*100:.1f}%" for k, v in levels.items()},
            'counts': levels
        }


def main():
    """主函数"""
    print("="*60)
    print("基金评分模型")
    print("="*60)
    
    service = ScoreService()
    
    try:
        # 批量计算
        results = service.batch_calculate(limit=200)
        print(f"\n✅ 成功计算 {len(results)} 只基金评分")
        
        # 分析分布
        dist = service.analyze_distribution(results)
        print("\n📊 评分分布:")
        print(f"   总数: {dist['total']}")
        print(f"   平均分: {dist['average_score']:.1f}")
        print(f"   分布: {dist['distribution']}")
        
        # 显示TOP10
        print("\n🏆 TOP 10 基金:")
        top10 = sorted(results, key=lambda x: x.total_score, reverse=True)[:10]
        for i, r in enumerate(top10, 1):
            print(f"   {i}. {r.fund_code} - {r.total_score}分 ({r.quality_level}级) "
                  f"夏普:{r.sharpe_ratio:.2f} 回撤:{r.max_drawdown:.1f}%")
        
    finally:
        service.close()
    
    print("\n" + "="*60)


if __name__ == '__main__':
    main()
