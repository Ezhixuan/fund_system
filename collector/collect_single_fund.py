#!/usr/bin/env python3
"""
采集单只基金历史净值并计算指标
用于测试真实数据效果
"""
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pymysql
import pandas as pd
import numpy as np
from datetime import datetime
import akshare as ak

from config import settings

# 数据库配置（从统一配置读取）
DB_CONFIG = settings.get_db_config()

FUND_CODE = '011452'

def collect_history_nav():
    """采集历史净值"""
    print(f'📥 正在采集基金 {FUND_CODE} 的历史净值...')
    
    # 从akshare获取
    df = ak.fund_open_fund_info_em(symbol=FUND_CODE, indicator='单位净值走势')
    
    if df.empty:
        print('❌ 获取失败')
        return None
    
    print(f'✅ 获取到 {len(df)} 条记录')
    print(f'   日期范围: {df.iloc[0]["净值日期"]} ~ {df.iloc[-1]["净值日期"]}')
    
    # 数据清洗
    df = df.rename(columns={
        '净值日期': 'nav_date',
        '单位净值': 'unit_nav',
        '日增长率': 'daily_return'
    })
    
    df['fund_code'] = FUND_CODE
    df['nav_date'] = pd.to_datetime(df['nav_date']).dt.strftime('%Y-%m-%d')
    df['unit_nav'] = pd.to_numeric(df['unit_nav'], errors='coerce')
    df['daily_return'] = pd.to_numeric(df['daily_return'], errors='coerce')
    df['accum_nav'] = df['unit_nav']  # 简化处理
    df['source'] = 'akshare_history'
    
    return df[['fund_code', 'nav_date', 'unit_nav', 'accum_nav', 'daily_return', 'source']]

def save_to_database(df):
    """保存到数据库"""
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    # 先删除旧数据（避免重复）
    cursor.execute(
        "DELETE FROM fund_nav WHERE fund_code = %s",
        (FUND_CODE,)
    )
    
    # 批量插入
    insert_sql = """
        INSERT INTO fund_nav 
        (fund_code, nav_date, unit_nav, accum_nav, daily_return, source)
        VALUES (%s, %s, %s, %s, %s, %s)
    """
    
    records = [
        (row['fund_code'], row['nav_date'], row['unit_nav'], 
         row['accum_nav'], row['daily_return'], row['source'])
        for _, row in df.iterrows()
    ]
    
    cursor.executemany(insert_sql, records)
    conn.commit()
    
    print(f'✅ 已保存 {len(records)} 条记录到 fund_nav')
    conn.close()

def calculate_metrics():
    """计算指标"""
    print('')
    print('📊 正在计算指标...')
    
    conn = pymysql.connect(**DB_CONFIG)
    
    # 读取数据
    df = pd.read_sql(
        """
        SELECT nav_date, unit_nav, daily_return 
        FROM fund_nav 
        WHERE fund_code = %s 
        ORDER BY nav_date ASC
        """,
        conn,
        params=(FUND_CODE,)
    )
    
    if len(df) < 60:
        print(f'❌ 数据不足，只有 {len(df)} 条记录')
        conn.close()
        return
    
    # 数据转换
    df['nav_date'] = pd.to_datetime(df['nav_date'])
    df['unit_nav'] = df['unit_nav'].astype(float)
    df['daily_return'] = df['daily_return'].astype(float) / 100  # 转为小数
    
    # 分离1年和3年数据
    one_year_df = df.tail(252) if len(df) >= 252 else df
    three_year_df = df.tail(756) if len(df) >= 756 else df
    
    calc_date = df['nav_date'].iloc[-1].strftime('%Y-%m-%d')
    
    # 计算指标
    risk_free_rate = 0.025
    trading_days = 252
    
    # 1. 收益指标
    def calc_annual_return(nav_series):
        if len(nav_series) < 60:
            return None
        total_return = nav_series.iloc[-1] / nav_series.iloc[0] - 1
        years = len(nav_series) / trading_days
        annual_return = (1 + total_return) ** (1/years) - 1
        return round(annual_return * 100, 4)
    
    return_1y = calc_annual_return(one_year_df['unit_nav'])
    return_3y = calc_annual_return(three_year_df['unit_nav'])
    
    # 2. 夏普比率
    def calc_sharpe(returns):
        if len(returns) < 60:
            return None
        mean_return = returns.mean() * trading_days
        std_return = returns.std() * np.sqrt(trading_days)
        if std_return == 0:
            return 0
        sharpe = (mean_return - risk_free_rate) / std_return
        return round(sharpe, 4)
    
    sharpe_1y = calc_sharpe(one_year_df['daily_return'])
    sharpe_3y = calc_sharpe(three_year_df['daily_return'])
    
    # 3. 最大回撤
    def calc_max_drawdown(nav_series):
        cummax = nav_series.cummax()
        drawdown = (nav_series - cummax) / cummax
        return round(drawdown.min() * 100, 4)
    
    max_dd_1y = calc_max_drawdown(one_year_df['unit_nav'])
    max_dd_3y = calc_max_drawdown(three_year_df['unit_nav'])
    
    # 4. 年化波动率
    def calc_volatility(returns):
        if len(returns) < 30:
            return None
        vol = returns.std() * np.sqrt(trading_days)
        return round(vol * 100, 4)
    
    vol_1y = calc_volatility(one_year_df['daily_return'])
    vol_3y = calc_volatility(three_year_df['daily_return'])
    
    # 保存指标
    cursor = conn.cursor()
    
    # 先删除旧指标
    cursor.execute("DELETE FROM fund_metrics WHERE fund_code = %s", (FUND_CODE,))
    
    # 插入新指标
    insert_sql = """
        INSERT INTO fund_metrics 
        (fund_code, calc_date, return_1y, return_3y,
         sharpe_ratio_1y, sharpe_ratio_3y,
         max_drawdown_1y, max_drawdown_3y,
         volatility_1y, volatility_3y,
         update_time)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
    """
    
    cursor.execute(insert_sql, (
        FUND_CODE, calc_date, return_1y, return_3y,
        sharpe_1y, sharpe_3y,
        max_dd_1y, max_dd_3y,
        vol_1y, vol_3y
    ))
    
    conn.commit()
    conn.close()
    
    print('✅ 指标计算完成')
    print('')
    
    return {
        'fund_code': FUND_CODE,
        'calc_date': calc_date,
        'data_points': len(df),
        'return_1y': return_1y,
        'return_3y': return_3y,
        'sharpe_1y': sharpe_1y,
        'sharpe_3y': sharpe_3y,
        'max_drawdown_1y': max_dd_1y,
        'max_drawdown_3y': max_dd_3y,
        'volatility_1y': vol_1y,
        'volatility_3y': vol_3y
    }

def print_results(metrics):
    """打印结果"""
    print('=' * 60)
    print(f'📈 基金 {FUND_CODE} 真实数据计算结果')
    print('=' * 60)
    print(f'数据点数: {metrics["data_points"]} 天')
    print(f'计算日期: {metrics["calc_date"]}')
    print('')
    print('【收益指标】')
    print(f'  近1年收益: {metrics["return_1y"]:.2f}%')
    print(f'  近3年收益: {metrics["return_3y"]:.2f}%')
    print('')
    print('【风险调整收益】')
    print(f'  1年夏普率: {metrics["sharpe_1y"]:.4f} {"✅优秀" if metrics["sharpe_1y"] > 1.5 else "⚠️一般"}')
    print(f'  3年夏普率: {metrics["sharpe_3y"]:.4f}')
    print('')
    print('【风险指标】')
    print(f'  1年最大回撤: {metrics["max_drawdown_1y"]:.2f}%')
    print(f'  3年最大回撤: {metrics["max_drawdown_3y"]:.2f}%')
    print(f'  1年波动率: {metrics["volatility_1y"]:.2f}%')
    print(f'  3年波动率: {metrics["volatility_3y"]:.2f}%')
    print('=' * 60)

def main():
    print('=' * 60)
    print(f'🎯 采集基金 {FUND_CODE} 真实历史数据')
    print('=' * 60)
    print('')
    
    # 1. 采集历史数据
    df = collect_history_nav()
    if df is None:
        return
    
    # 2. 保存到数据库
    save_to_database(df)
    
    # 3. 计算指标
    metrics = calculate_metrics()
    if metrics:
        print_results(metrics)
        print('')
        print(f'✅ 完成！现在可以在前端查看 {FUND_CODE} 的完整数据了')
        print(f'   访问: http://localhost:5173/fund/{FUND_CODE}')

if __name__ == '__main__':
    main()
