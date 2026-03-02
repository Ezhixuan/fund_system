#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金数据完整采集服务
采集基础信息 + 净值历史 + 计算指标
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pymysql
import pandas as pd
import numpy as np
import akshare as ak
from datetime import datetime
from loguru import logger

# 数据库配置 - fund-mysql (13306)
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 13306,
    'user': 'root',
    'password': 'root123',
    'database': 'fund_system',
    'charset': 'utf8mb4'
}


class FundDataService:
    """基金数据服务"""
    
    def __init__(self):
        self.conn = None
        
    def get_connection(self):
        """获取数据库连接"""
        if self.conn is None or not self.conn.open:
            self.conn = pymysql.connect(**DB_CONFIG)
        return self.conn
        
    def close(self):
        """关闭连接"""
        if self.conn and self.conn.open:
            self.conn.close()
            
    def check_fund_exists(self, fund_code):
        """检查基金是否已存在"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM fund_info WHERE fund_code = %s", (fund_code,))
        count = cursor.fetchone()[0]
        cursor.close()
        return count > 0
        
    def collect_fund_info(self, fund_code):
        """采集基金基础信息"""
        logger.info(f"采集基金基础信息: {fund_code}")
        
        try:
            # 从akshare获取基金列表
            df = ak.fund_name_em()
            fund_info = df[df['基金代码'] == fund_code]
            
            if fund_info.empty:
                return {'success': False, 'error': f'基金 {fund_code} 不存在'}
            
            fund_name = fund_info.iloc[0]['基金简称']
            fund_type = fund_info.iloc[0].get('基金类型', '')
            
            # 插入数据库
            conn = self.get_connection()
            cursor = conn.cursor()
            
            insert_sql = """
                INSERT INTO fund_info (fund_code, fund_name, fund_type, status, update_time)
                VALUES (%s, %s, %s, 1, NOW())
                ON DUPLICATE KEY UPDATE 
                    fund_name = VALUES(fund_name),
                    fund_type = VALUES(fund_type),
                    update_time = NOW()
            """
            cursor.execute(insert_sql, (fund_code, fund_name, fund_type))
            conn.commit()
            cursor.close()
            
            logger.info(f"基金基础信息已保存: {fund_name}")
            return {'success': True, 'fund_name': fund_name, 'fund_type': fund_type}
            
        except Exception as e:
            logger.error(f"采集基金基础信息失败: {e}")
            return {'success': False, 'error': str(e)}
            
    def collect_nav_history(self, fund_code):
        """采集历史净值"""
        logger.info(f"采集历史净值: {fund_code}")
        
        try:
            # 从akshare获取历史净值
            df = ak.fund_open_fund_info_em(symbol=fund_code, indicator='单位净值走势')
            
            if df.empty:
                return {'success': False, 'error': '无净值数据'}
            
            logger.info(f"获取到 {len(df)} 条净值记录")
            
            # 数据清洗
            df = df.rename(columns={
                '净值日期': 'nav_date',
                '单位净值': 'unit_nav',
                '日增长率': 'daily_return'
            })
            
            df['fund_code'] = fund_code
            df['nav_date'] = pd.to_datetime(df['nav_date']).dt.strftime('%Y-%m-%d')
            df['unit_nav'] = pd.to_numeric(df['unit_nav'], errors='coerce')
            df['daily_return'] = pd.to_numeric(df['daily_return'], errors='coerce')
            df['accum_nav'] = df['unit_nav']
            df['source'] = 'akshare'
            
            # 删除旧数据
            conn = self.get_connection()
            cursor = conn.cursor()
            cursor.execute("DELETE FROM fund_nav WHERE fund_code = %s", (fund_code,))
            
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
            cursor.close()
            
            logger.info(f"已保存 {len(records)} 条净值记录")
            return {'success': True, 'nav_count': len(records)}
            
        except Exception as e:
            logger.error(f"采集历史净值失败: {e}")
            return {'success': False, 'error': str(e)}
            
    def calculate_metrics(self, fund_code):
        """计算基金指标"""
        logger.info(f"计算基金指标: {fund_code}")
        
        try:
            conn = self.get_connection()
            
            # 读取数据
            df = pd.read_sql(
                """
                SELECT nav_date, unit_nav, daily_return 
                FROM fund_nav 
                WHERE fund_code = %s 
                ORDER BY nav_date ASC
                """,
                conn,
                params=(fund_code,)
            )
            
            if len(df) < 60:
                logger.warning(f"数据不足，只有 {len(df)} 条记录")
                return {'success': False, 'error': '数据不足，无法计算指标'}
            
            # 数据转换
            df['nav_date'] = pd.to_datetime(df['nav_date'])
            df['unit_nav'] = df['unit_nav'].astype(float)
            df['daily_return'] = df['daily_return'].astype(float) / 100
            
            # 分离1年和3年数据
            one_year_df = df.tail(252) if len(df) >= 252 else df
            three_year_df = df.tail(756) if len(df) >= 756 else df
            
            calc_date = df['nav_date'].iloc[-1].strftime('%Y-%m-%d')
            
            # 计算指标
            risk_free_rate = 0.025
            trading_days = 252
            
            # 收益指标
            def calc_annual_return(nav_series):
                if len(nav_series) < 60:
                    return None
                total_return = nav_series.iloc[-1] / nav_series.iloc[0] - 1
                years = len(nav_series) / trading_days
                annual_return = (1 + total_return) ** (1/years) - 1
                return round(annual_return * 100, 4)
            
            return_1y = calc_annual_return(one_year_df['unit_nav'])
            return_3y = calc_annual_return(three_year_df['unit_nav'])
            
            # 夏普比率
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
            
            # 最大回撤
            def calc_max_drawdown(nav_series):
                cummax = nav_series.cummax()
                drawdown = (nav_series - cummax) / cummax
                return round(drawdown.min() * 100, 4)
            
            max_dd_1y = calc_max_drawdown(one_year_df['unit_nav'])
            max_dd_3y = calc_max_drawdown(three_year_df['unit_nav'])
            
            # 波动率
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
            cursor.execute("DELETE FROM fund_metrics WHERE fund_code = %s", (fund_code,))
            
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
                fund_code, calc_date, return_1y, return_3y,
                sharpe_1y, sharpe_3y,
                max_dd_1y, max_dd_3y,
                vol_1y, vol_3y
            ))
            
            conn.commit()
            cursor.close()
            
            logger.info("指标计算完成")
            return {
                'success': True,
                'return_1y': return_1y,
                'sharpe_1y': sharpe_1y,
                'max_drawdown_1y': max_dd_1y
            }
            
        except Exception as e:
            logger.error(f"计算指标失败: {e}")
            return {'success': False, 'error': str(e)}
            
    def collect_fund_complete(self, fund_code):
        """完整采集基金数据"""
        logger.info(f"=" * 60)
        logger.info(f"开始完整采集基金: {fund_code}")
        logger.info(f"=" * 60)
        
        result = {
            'success': False,
            'fund_code': fund_code,
            'fund_name': '',
            'nav_count': 0,
            'metrics_calculated': False
        }
        
        try:
            # 1. 采集基础信息
            info_result = self.collect_fund_info(fund_code)
            if not info_result['success']:
                return {**result, 'error': info_result.get('error', '采集基础信息失败')}
            
            result['fund_name'] = info_result.get('fund_name', '')
            
            # 2. 采集历史净值
            nav_result = self.collect_nav_history(fund_code)
            if not nav_result['success']:
                return {**result, 'error': nav_result.get('error', '采集净值失败')}
            
            result['nav_count'] = nav_result.get('nav_count', 0)
            
            # 3. 计算指标
            metrics_result = self.calculate_metrics(fund_code)
            if metrics_result['success']:
                result['metrics_calculated'] = True
            
            result['success'] = True
            logger.info(f"✅ 基金 {fund_code} 数据采集完成")
            
        except Exception as e:
            logger.error(f"采集失败: {e}")
            result['error'] = str(e)
            
        finally:
            self.close()
            
        return result


# 兼容直接调用
if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        fund_code = sys.argv[1]
    else:
        fund_code = '000001'
    
    service = FundDataService()
    result = service.collect_fund_complete(fund_code)
    print(result)
