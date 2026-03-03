#!/usr/bin/env python3
"""
基金指标数据初始化脚本
由于 fund_nav 只有1天数据，生成基础指标记录用于测试
"""
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pymysql
from datetime import datetime
import random

from config import settings

# 数据库配置（从统一配置读取）
DB_CONFIG = settings.get_db_config()


def generate_sample_metrics():
    """生成模拟指标数据用于测试"""
    conn = pymysql.connect(**DB_CONFIG)
    
    try:
        with conn.cursor() as cursor:
            # 获取基金列表
            cursor.execute("SELECT fund_code, fund_name FROM fund_info WHERE status = 1 LIMIT 1000")
            funds = cursor.fetchall()
            
            print(f"准备为 {len(funds)} 只基金生成测试指标数据...")
            
            calc_date = datetime.now().date()
            inserted = 0
            
            for fund_code, fund_name in funds:
                # 生成模拟指标数据
                return_1m = round(random.uniform(-5, 8), 4)
                return_3m = round(random.uniform(-10, 15), 4)
                return_1y = round(random.uniform(-20, 30), 4)
                return_3y = round(random.uniform(-30, 50), 4)
                sharpe = round(random.uniform(-1, 3), 4)
                max_dd = round(random.uniform(-30, -5), 4)
                volatility = round(random.uniform(10, 25), 4)
                
                sql = """
                INSERT INTO fund_metrics 
                (fund_code, calc_date, return_1m, return_3m, return_1y, return_3y,
                 sharpe_ratio_1y, max_drawdown_1y, volatility_1y, 
                 sortino_ratio_1y, calmar_ratio_3y, alpha_1y, beta_1y,
                 pe_percentile, pb_percentile, update_time)
                VALUES 
                (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
                ON DUPLICATE KEY UPDATE
                return_1m = VALUES(return_1m),
                return_3m = VALUES(return_3m),
                return_1y = VALUES(return_1y),
                return_3y = VALUES(return_3y),
                sharpe_ratio_1y = VALUES(sharpe_ratio_1y),
                max_drawdown_1y = VALUES(max_drawdown_1y),
                volatility_1y = VALUES(volatility_1y),
                update_time = NOW()
                """
                
                cursor.execute(sql, (
                    fund_code, calc_date, return_1m, return_3m, return_1y, return_3y,
                    sharpe, max_dd, volatility,
                    sharpe * 1.2,  # sortino
                    abs(return_3y / max_dd) if max_dd != 0 else 0,  # calmar
                    round(random.uniform(-5, 5), 4),  # alpha
                    round(random.uniform(0.8, 1.2), 4),  # beta
                    random.randint(10, 90),  # pe_percentile
                    random.randint(10, 90)   # pb_percentile
                ))
                inserted += 1
                
                if inserted % 100 == 0:
                    print(f"  已插入 {inserted} 条...")
            
            conn.commit()
            print(f"\n✅ 成功插入 {inserted} 条指标记录")
            
            # 验证
            cursor.execute("SELECT COUNT(*) FROM fund_metrics")
            total = cursor.fetchone()[0]
            print(f"📊 fund_metrics 表现在共有 {total} 条记录")
            
            # 显示一些统计
            cursor.execute("""
                SELECT 
                    COUNT(*) as total,
                    AVG(sharpe_ratio_1y) as avg_sharpe,
                    MAX(sharpe_ratio_1y) as max_sharpe,
                    MIN(sharpe_ratio_1y) as min_sharpe
                FROM fund_metrics
            """)
            stats = cursor.fetchone()
            print(f"\n📈 夏普比率统计:")
            print(f"   平均: {stats[1]:.4f}")
            print(f"   最高: {stats[2]:.4f}")
            print(f"   最低: {stats[3]:.4f}")
            
    finally:
        conn.close()


if __name__ == '__main__':
    print("="*60)
    print("基金指标数据初始化")
    print("="*60)
    generate_sample_metrics()
    print("="*60)
