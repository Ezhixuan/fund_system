#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
任务监控面板

用法:
    python monitor.py              # 显示监控面板
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd
from datetime import datetime, timedelta
from sqlalchemy import text
import logging

from config import settings
from utils.database import db

logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger(__name__)


class JobMonitor:
    """任务监控面板"""
    
    def __init__(self):
        self.engine = db.get_engine()
    
    def get_recent_updates(self, hours: int = 24) -> pd.DataFrame:
        """获取最近更新记录"""
        try:
            sql = f"""
            SELECT 
                table_name,
                update_date,
                record_count,
                status,
                error_msg,
                created_at as end_time
            FROM data_update_log
            WHERE created_at > DATE_SUB(NOW(), INTERVAL {hours} HOUR)
            ORDER BY created_at DESC
            LIMIT 20
            """
            return pd.read_sql(sql, self.engine)
        except Exception as e:
            logger.error(f"查询最近更新失败: {e}")
            return pd.DataFrame()
    
    def get_update_stats(self, days: int = 7) -> dict:
        """获取更新统计"""
        try:
            sql = f"""
            SELECT 
                DATE(created_at) as date,
                table_name,
                COUNT(*) as job_count,
                SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
                SUM(CASE WHEN status IN ('FAILURE', 'PARTIAL') THEN 1 ELSE 0 END) as failure_count,
                SUM(record_count) as total_records
            FROM data_update_log
            WHERE created_at > DATE_SUB(CURDATE(), INTERVAL {days} DAY)
            GROUP BY DATE(created_at), table_name
            ORDER BY date DESC
            """
            df = pd.read_sql(sql, self.engine)
            
            if df.empty:
                return {
                    'daily_stats': [],
                    'total_jobs': 0,
                    'success_rate': 0
                }
            
            return {
                'daily_stats': df.to_dict('records'),
                'total_jobs': int(df['job_count'].sum()),
                'success_rate': round(
                    df['success_count'].sum() / df['job_count'].sum() * 100, 2
                ) if df['job_count'].sum() > 0 else 0
            }
        except Exception as e:
            logger.error(f"查询统计失败: {e}")
            return {'daily_stats': [], 'total_jobs': 0, 'success_rate': 0}
    
    def get_data_freshness(self) -> pd.DataFrame:
        """获取数据新鲜度"""
        try:
            sql = """
            SELECT 
                'fund_info' as table_name,
                COUNT(*) as total_records,
                MAX(update_time) as latest_time,
                DATEDIFF(NOW(), MAX(update_time)) as days_delay
            FROM fund_info
            UNION ALL
            SELECT 
                'fund_nav',
                COUNT(*),
                MAX(nav_date),
                DATEDIFF(CURDATE(), MAX(nav_date))
            FROM fund_nav
            UNION ALL
            SELECT 
                'tmp_fund_nav',
                COUNT(*),
                MAX(created_at),
                DATEDIFF(NOW(), MAX(created_at))
            FROM tmp_fund_nav
            """
            return pd.read_sql(sql, self.engine)
        except Exception as e:
            logger.error(f"查询数据新鲜度失败: {e}")
            return pd.DataFrame()
    
    def get_temp_table_stats(self) -> dict:
        """获取临时表统计"""
        try:
            with db.get_connection() as conn:
                result = conn.execute(text("""
                    SELECT 
                        COALESCE(check_status, 0) as status,
                        COUNT(*) as count
                    FROM tmp_fund_nav
                    GROUP BY check_status
                """))
                rows = result.fetchall()
            
            stats = {'pending': 0, 'passed': 0, 'failed': 0}
            for row in rows:
                status = row[0]
                count = row[1]
                if status == 0 or status is None:
                    stats['pending'] = count
                elif status == 1:
                    stats['passed'] = count
                elif status == 2:
                    stats['failed'] = count
            
            return stats
        except Exception as e:
            logger.error(f"查询临时表统计失败: {e}")
            return {'pending': 0, 'passed': 0, 'failed': 0}
    
    def print_dashboard(self):
        """打印监控面板"""
        print("\n" + "="*70)
        print("🖥️  基金数据采集系统 - 监控面板")
        print("="*70)
        print(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 1. 数据新鲜度
        print("\n📊 【数据新鲜度】")
        freshness = self.get_data_freshness()
        if not freshness.empty:
            for _, row in freshness.iterrows():
                delay = row['days_delay']
                if pd.isna(delay):
                    status = "⚪"
                    delay_str = "无数据"
                elif delay <= 0:
                    status = "🟢"
                    delay_str = "最新"
                elif delay <= 1:
                    status = "🟡"
                    delay_str = f"延迟{int(delay)}天"
                else:
                    status = "🔴"
                    delay_str = f"延迟{int(delay)}天"
                
                print(f"  {status} {row['table_name']}: {row['total_records']:,}条, {delay_str}")
        else:
            print("  暂无数据")
        
        # 2. 临时表状态
        print("\n📝 【临时表状态】")
        temp_stats = self.get_temp_table_stats()
        total = sum(temp_stats.values())
        print(f"  总计: {total:,}条")
        print(f"    🟡 待处理: {temp_stats['pending']:,}条")
        print(f"    🟢 已通过: {temp_stats['passed']:,}条")
        print(f"    🔴 已失败: {temp_stats['failed']:,}条")
        
        # 3. 最近更新
        print("\n🔄 【最近24小时更新记录】")
        recent = self.get_recent_updates(24)
        if not recent.empty:
            for _, row in recent.head(10).iterrows():
                status_icon = "✅" if row['status'] == 'SUCCESS' else "⚠️" if row['status'] == 'PARTIAL' else "❌"
                time_str = row['end_time'].strftime('%m-%d %H:%M') if pd.notna(row['end_time']) else '未知'
                print(f"  {status_icon} [{time_str}] {row['table_name']}: {row['record_count']:,}条 [{row['status']}]")
                if row['error_msg'] and pd.notna(row['error_msg']):
                    print(f"     错误: {row['error_msg'][:50]}")
        else:
            print("  无更新记录")
        
        # 4. 成功率统计
        print("\n📈 【近7天统计】")
        stats = self.get_update_stats(7)
        print(f"  总任务数: {stats['total_jobs']}")
        print(f"  成功率: {stats['success_rate']}%")
        
        print("="*70)


def main():
    monitor = JobMonitor()
    monitor.print_dashboard()


if __name__ == '__main__':
    main()
