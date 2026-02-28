#!/usr/bin/env python3
"""
全维指标计算引擎
基于基金净值数据计算完整的风险调整指标
"""
import os
import sys
import pymysql
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Dict, Optional
from decimal import Decimal

# 数据库配置
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3307,
    'user': 'fund',
    'password': 'fund123',
    'database': 'fund_system',
    'charset': 'utf8mb4'
}


class MetricsEngine:
    """全维指标计算引擎"""
    
    RISK_FREE_RATE = 0.025  # 无风险利率 2.5%
    TRADING_DAYS_PER_YEAR = 252  # 年化交易日
    
    def __init__(self):
        self.conn = None
    
    def connect(self):
        """连接数据库"""
        self.conn = pymysql.connect(**DB_CONFIG)
        return self
    
    def close(self):
        """关闭连接"""
        if self.conn:
            self.conn.close()
    
    def get_nav_data(self, fund_code: str, days: int = 1095) -> Optional[pd.DataFrame]:
        """获取基金净值数据"""
        with self.conn.cursor() as cursor:
            end_date = datetime.now().date()
            start_date = end_date - timedelta(days=days)
            
            sql = """
            SELECT nav_date, unit_nav, accum_nav, daily_return
            FROM fund_nav
            WHERE fund_code = %s AND nav_date >= %s
            ORDER BY nav_date ASC
            """
            cursor.execute(sql, (fund_code, start_date))
            rows = cursor.fetchall()
            
            if len(rows) < 60:  # 至少需要60天数据
                return None
            
            df = pd.DataFrame(rows, columns=['nav_date', 'unit_nav', 'accum_nav', 'daily_return'])
            df['nav_date'] = pd.to_datetime(df['nav_date'])
            df['unit_nav'] = df['unit_nav'].astype(float)
            df['daily_return'] = df['daily_return'].astype(float) / 100  # 转换为小数
            
            return df
    
    def calculate_all_metrics(self, fund_code: str) -> Optional[Dict]:
        """计算全维指标"""
        df = self.get_nav_data(fund_code)
        if df is None or len(df) < 60:
            return None
        
        # 计算日收益率（如果没有）
        if df['daily_return'].isna().all():
            df['daily_return'] = df['unit_nav'].pct_change()
        
        df = df.dropna()
        if len(df) < 60:
            return None
        
        # 分离1年和3年数据
        one_year_df = df.tail(252) if len(df) >= 252 else df
        three_year_df = df.tail(756) if len(df) >= 756 else df
        
        calc_date = df['nav_date'].iloc[-1].strftime('%Y-%m-%d')
        
        return {
            'fund_code': fund_code,
            'calc_date': calc_date,
            
            # 收益指标
            'return_1m': self._calc_period_return(df, 21),
            'return_3m': self._calc_period_return(df, 63),
            'return_1y': self._calc_annual_return(one_year_df),
            'return_3y': self._calc_annualized_return(three_year_df),
            
            # 风险调整收益
            'sharpe_ratio_1y': self._calc_sharpe(one_year_df['daily_return']),
            'sharpe_ratio_3y': self._calc_sharpe(three_year_df['daily_return']),
            'sortino_ratio_1y': self._calc_sortino(one_year_df['daily_return']),
            'calmar_ratio_3y': self._calc_calmar(three_year_df),
            
            # 风险因子 (简化版，无基准数据)
            'alpha_1y': 0.0,
            'beta_1y': 1.0,
            'information_ratio_1y': 0.0,
            
            # 风险指标
            'max_drawdown_1y': self._calc_max_drawdown(one_year_df['unit_nav']),
            'max_drawdown_3y': self._calc_max_drawdown(three_year_df['unit_nav']),
            'volatility_1y': self._calc_volatility(one_year_df['daily_return']),
            'volatility_3y': self._calc_volatility(three_year_df['daily_return']),
            
            # 估值指标 (暂无数据)
            'pe_percentile': None,
            'pb_percentile': None
        }
    
    def _calc_period_return(self, df: pd.DataFrame, days: int) -> Optional[float]:
        """计算特定期限收益"""
        if len(df) < days:
            return None
        period_df = df.tail(days)
        start_nav = period_df['unit_nav'].iloc[0]
        end_nav = period_df['unit_nav'].iloc[-1]
        if start_nav <= 0:
            return None
        return round((end_nav / start_nav - 1) * 100, 4)
    
    def _calc_annual_return(self, df: pd.DataFrame) -> Optional[float]:
        """计算年化收益"""
        if len(df) < 60:
            return None
        start_nav = df['unit_nav'].iloc[0]
        end_nav = df['unit_nav'].iloc[-1]
        if start_nav <= 0:
            return None
        total_return = end_nav / start_nav - 1
        years = len(df) / self.TRADING_DAYS_PER_YEAR
        annual_return = (1 + total_return) ** (1/years) - 1
        return round(annual_return * 100, 4)
    
    def _calc_annualized_return(self, df: pd.DataFrame) -> Optional[float]:
        """计算3年年化收益"""
        return self._calc_annual_return(df)
    
    def _calc_sharpe(self, returns: pd.Series) -> Optional[float]:
        """计算夏普比率"""
        if len(returns) < 60:
            return None
        
        mean_return = returns.mean() * self.TRADING_DAYS_PER_YEAR
        std_return = returns.std() * np.sqrt(self.TRADING_DAYS_PER_YEAR)
        
        if std_return == 0:
            return 0
        
        sharpe = (mean_return - self.RISK_FREE_RATE) / std_return
        return round(sharpe, 4)
    
    def _calc_sortino(self, returns: pd.Series) -> Optional[float]:
        """计算索提诺比率"""
        if len(returns) < 60:
            return None
        
        mean_return = returns.mean() * self.TRADING_DAYS_PER_YEAR
        
        # 下行标准差
        downside_returns = returns[returns < 0]
        if len(downside_returns) < 10:
            return None
        
        downside_std = downside_returns.std() * np.sqrt(self.TRADING_DAYS_PER_YEAR)
        if downside_std == 0:
            return None
        
        sortino = (mean_return - self.RISK_FREE_RATE) / downside_std
        return round(sortino, 4)
    
    def _calc_max_drawdown(self, nav: pd.Series) -> Optional[float]:
        """计算最大回撤"""
        if len(nav) < 30:
            return None
        
        cummax = nav.cummax()
        drawdown = (nav - cummax) / cummax
        max_dd = drawdown.min()
        return round(max_dd * 100, 4)  # 转为百分比，负值
    
    def _calc_calmar(self, df: pd.DataFrame) -> Optional[float]:
        """计算卡玛比率"""
        if len(df) < 252:
            return None
        
        annual_return = self._calc_annual_return(df)
        max_dd = self._calc_max_drawdown(df['unit_nav'])
        
        if annual_return is None or max_dd is None or max_dd >= 0:
            return None
        
        calmar = annual_return / abs(max_dd)
        return round(calmar, 4)
    
    def _calc_volatility(self, returns: pd.Series) -> Optional[float]:
        """计算年化波动率"""
        if len(returns) < 30:
            return None
        
        vol = returns.std() * np.sqrt(self.TRADING_DAYS_PER_YEAR)
        return round(vol * 100, 4)
    
    def save_metrics(self, metrics: Dict) -> bool:
        """保存指标到数据库"""
        if not metrics:
            return False
        
        with self.conn.cursor() as cursor:
            sql = """
            INSERT INTO fund_metrics 
            (fund_code, calc_date, return_1m, return_3m, return_1y, return_3y,
             sharpe_ratio_1y, sharpe_ratio_3y, sortino_ratio_1y, calmar_ratio_3y,
             alpha_1y, beta_1y, information_ratio_1y,
             max_drawdown_1y, max_drawdown_3y, volatility_1y, volatility_3y,
             pe_percentile, pb_percentile, update_time)
            VALUES 
            (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
            ON DUPLICATE KEY UPDATE
            return_1m = VALUES(return_1m),
            return_3m = VALUES(return_3m),
            return_1y = VALUES(return_1y),
            return_3y = VALUES(return_3y),
            sharpe_ratio_1y = VALUES(sharpe_ratio_1y),
            sharpe_ratio_3y = VALUES(sharpe_ratio_3y),
            sortino_ratio_1y = VALUES(sortino_ratio_1y),
            calmar_ratio_3y = VALUES(calmar_ratio_3y),
            alpha_1y = VALUES(alpha_1y),
            beta_1y = VALUES(beta_1y),
            information_ratio_1y = VALUES(information_ratio_1y),
            max_drawdown_1y = VALUES(max_drawdown_1y),
            max_drawdown_3y = VALUES(max_drawdown_3y),
            volatility_1y = VALUES(volatility_1y),
            volatility_3y = VALUES(volatility_3y),
            pe_percentile = VALUES(pe_percentile),
            pb_percentile = VALUES(pb_percentile),
            update_time = NOW()
            """
            
            cursor.execute(sql, (
                metrics['fund_code'], metrics['calc_date'],
                metrics['return_1m'], metrics['return_3m'],
                metrics['return_1y'], metrics['return_3y'],
                metrics['sharpe_ratio_1y'], metrics['sharpe_ratio_3y'],
                metrics['sortino_ratio_1y'], metrics['calmar_ratio_3y'],
                metrics['alpha_1y'], metrics['beta_1y'],
                metrics['information_ratio_1y'],
                metrics['max_drawdown_1y'], metrics['max_drawdown_3y'],
                metrics['volatility_1y'], metrics['volatility_3y'],
                metrics['pe_percentile'], metrics['pb_percentile']
            ))
            self.conn.commit()
            return True
    
    def batch_calculate(self, limit: int = 100) -> tuple:
        """批量计算指标"""
        with self.conn.cursor() as cursor:
            # 获取有净值数据的基金
            cursor.execute("""
                SELECT DISTINCT fund_code FROM fund_nav
                WHERE nav_date >= DATE_SUB(CURDATE(), INTERVAL 3 YEAR)
                ORDER BY fund_code
                LIMIT %s
            """, (limit,))
            
            fund_codes = [row[0] for row in cursor.fetchall()]
        
        print(f"开始计算 {len(fund_codes)} 只基金的指标...")
        
        success_count = 0
        fail_count = 0
        
        for i, code in enumerate(fund_codes):
            try:
                metrics = self.calculate_all_metrics(code)
                if metrics and self.save_metrics(metrics):
                    success_count += 1
                else:
                    fail_count += 1
                
                if (i + 1) % 10 == 0:
                    print(f"  进度: {i+1}/{len(fund_codes)} (成功:{success_count}, 失败:{fail_count})")
            except Exception as e:
                print(f"  计算 {code} 失败: {e}")
                fail_count += 1
        
        return success_count, fail_count


def main():
    """主函数"""
    print("="*60)
    print("全维指标计算引擎")
    print("="*60)
    
    engine = MetricsEngine().connect()
    
    try:
        # 批量计算
        success, fail = engine.batch_calculate(limit=100)
        
        print(f"\n✅ 计算完成: {success} 只基金成功, {fail} 只失败")
        
        # 验证结果
        with engine.conn.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM fund_metrics")
            total = cursor.fetchone()[0]
            print(f"📊 fund_metrics 表现在共有 {total} 条记录")
            
            # 统计
            cursor.execute("""
                SELECT 
                    AVG(sharpe_ratio_1y) as avg_sharpe,
                    AVG(max_drawdown_1y) as avg_dd,
                    AVG(volatility_1y) as avg_vol
                FROM fund_metrics
                WHERE sharpe_ratio_1y IS NOT NULL
            """)
            stats = cursor.fetchone()
            print(f"\n📈 指标统计:")
            print(f"   平均夏普: {stats[0]:.4f}" if stats[0] else "   平均夏普: N/A")
            print(f"   平均回撤: {stats[1]:.2f}%" if stats[1] else "   平均回撤: N/A")
            print(f"   平均波动: {stats[2]:.2f}%" if stats[2] else "   平均波动: N/A")
        
    finally:
        engine.close()
    
    print("\n" + "="*60)


if __name__ == '__main__':
    main()
