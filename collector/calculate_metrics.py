#!/usr/bin/env python3
"""
基金指标计算脚本
基于 fund_nav 历史净值数据计算全维指标
"""
import os
import sys
import pymysql
import numpy as np
from datetime import datetime, timedelta
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


def connect_db():
    """连接数据库"""
    return pymysql.connect(**DB_CONFIG)


def get_fund_list(conn):
    """获取有净值数据的基金列表"""
    with conn.cursor() as cursor:
        sql = """
        SELECT DISTINCT fund_code 
        FROM fund_nav 
        ORDER BY fund_code
        LIMIT 500
        """
        cursor.execute(sql)
        return [row[0] for row in cursor.fetchall()]


def get_nav_history(conn, fund_code, days=1095):
    """获取基金净值历史（默认3年）"""
    with conn.cursor() as cursor:
        end_date = datetime.now().date()
        start_date = end_date - timedelta(days=days)
        
        sql = """
        SELECT nav_date, unit_nav, accum_nav, daily_return
        FROM fund_nav
        WHERE fund_code = %s AND nav_date >= %s
        ORDER BY nav_date ASC
        """
        cursor.execute(sql, (fund_code, start_date))
        return cursor.fetchall()


def calculate_returns(nav_data):
    """计算各周期收益率"""
    if not nav_data or len(nav_data) < 2:
        return None, None, None, None
    
    dates = [row[0] for row in nav_data]
    navs = [float(row[1]) for row in nav_data if row[1]]
    
    if len(navs) < 2:
        return None, None, None, None
    
    end_nav = navs[-1]
    
    # 计算1月收益
    return_1m = None
    one_month_ago = datetime.now().date() - timedelta(days=30)
    for i, d in enumerate(dates):
        if d >= one_month_ago and i > 0:
            return_1m = (end_nav - navs[i]) / navs[i] * 100 if navs[i] else None
            break
    
    # 计算3月收益
    return_3m = None
    three_month_ago = datetime.now().date() - timedelta(days=90)
    for i, d in enumerate(dates):
        if d >= three_month_ago and i > 0:
            return_3m = (end_nav - navs[i]) / navs[i] * 100 if navs[i] else None
            break
    
    # 计算1年收益
    return_1y = None
    one_year_ago = datetime.now().date() - timedelta(days=365)
    for i, d in enumerate(dates):
        if d >= one_year_ago and i > 0:
            return_1y = (end_nav - navs[i]) / navs[i] * 100 if navs[i] else None
            break
    
    # 计算3年收益（年化）
    return_3y = None
    three_year_ago = datetime.now().date() - timedelta(days=1095)
    for i, d in enumerate(dates):
        if d >= three_year_ago and i > 0:
            total_return = (end_nav - navs[i]) / navs[i] if navs[i] else 0
            return_3y = ((1 + total_return) ** (1/3) - 1) * 100
            break
    
    return return_1m, return_3m, return_1y, return_3y


def calculate_risk_metrics(nav_data):
    """计算风险指标"""
    if not nav_data or len(nav_data) < 30:
        return None, None, None
    
    # 提取日收益率
    daily_returns = []
    for row in nav_data:
        if row[3]:  # daily_return 字段
            daily_returns.append(float(row[3]))
    
    if len(daily_returns) < 30:
        return None, None, None
    
    returns_array = np.array(daily_returns)
    
    # 年化波动率 (假设252个交易日)
    volatility = np.std(returns_array) * np.sqrt(252)
    
    # 最大回撤
    navs = [float(row[1]) for row in nav_data if row[1]]
    max_drawdown = 0
    peak = navs[0]
    for nav in navs:
        if nav > peak:
            peak = nav
        drawdown = (peak - nav) / peak
        if drawdown > max_drawdown:
            max_drawdown = drawdown
    
    # 夏普比率 (假设无风险利率3%)
    risk_free_rate = 0.03
    annual_return = np.mean(returns_array) * 252
    sharpe_ratio = (annual_return - risk_free_rate) / volatility if volatility > 0 else 0
    
    return round(sharpe_ratio, 4), round(max_drawdown * 100, 4), round(volatility * 100, 4)


def calculate_metrics(conn, fund_code):
    """计算单个基金的全维指标"""
    nav_data = get_nav_history(conn, fund_code)
    
    if not nav_data or len(nav_data) < 30:
        return None
    
    # 计算收益率
    return_1m, return_3m, return_1y, return_3y = calculate_returns(nav_data)
    
    # 计算风险指标
    sharpe, max_dd, vol = calculate_risk_metrics(nav_data)
    
    calc_date = nav_data[-1][0]  # 最新净值日期
    
    return {
        'fund_code': fund_code,
        'calc_date': calc_date,
        'return_1m': return_1m,
        'return_3m': return_3m,
        'return_1y': return_1y,
        'return_3y': return_3y,
        'sharpe_ratio_1y': sharpe,
        'max_drawdown_1y': max_dd,
        'volatility_1y': vol
    }


def insert_metrics(conn, metrics_list):
    """批量插入指标数据"""
    with conn.cursor() as cursor:
        sql = """
        INSERT INTO fund_metrics 
        (fund_code, calc_date, return_1m, return_3m, return_1y, return_3y,
         sharpe_ratio_1y, max_drawdown_1y, volatility_1y, update_time)
        VALUES 
        (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
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
        
        data = [
            (
                m['fund_code'], m['calc_date'], m['return_1m'], m['return_3m'],
                m['return_1y'], m['return_3y'], m['sharpe_ratio_1y'],
                m['max_drawdown_1y'], m['volatility_1y']
            )
            for m in metrics_list if m
        ]
        
        if data:
            cursor.executemany(sql, data)
            conn.commit()
            return len(data)
    return 0


def main():
    """主函数"""
    print("="*60)
    print("基金指标计算工具")
    print("="*60)
    
    try:
        conn = connect_db()
        print(f"\n✅ 数据库连接成功: {DB_CONFIG['host']}:{DB_CONFIG['port']}")
        
        # 获取基金列表
        fund_codes = get_fund_list(conn)
        print(f"📊 发现 {len(fund_codes)} 只基金有净值数据")
        
        # 计算指标
        print("\n🔄 开始计算指标...")
        metrics_list = []
        success_count = 0
        skip_count = 0
        
        for i, code in enumerate(fund_codes):
            metrics = calculate_metrics(conn, code)
            if metrics:
                metrics_list.append(metrics)
                success_count += 1
                if (i + 1) % 50 == 0:
                    print(f"   进度: {i+1}/{len(fund_codes)} ({success_count}成功, {skip_count}跳过)")
            else:
                skip_count += 1
        
        print(f"\n📈 计算完成: {success_count} 只基金指标有效")
        
        # 插入数据库
        if metrics_list:
            inserted = insert_metrics(conn, metrics_list)
            print(f"✅ 成功插入/更新 {inserted} 条指标记录")
        
        # 验证结果
        with conn.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM fund_metrics")
            total = cursor.fetchone()[0]
            print(f"\n📋 fund_metrics 表现在共有 {total} 条记录")
        
        print("\n" + "="*60)
        print("指标计算完成！")
        print("="*60)
        
    except Exception as e:
        print(f"\n❌ 错误: {e}")
        import traceback
        traceback.print_exc()
    finally:
        if 'conn' in locals():
            conn.close()


if __name__ == '__main__':
    main()
