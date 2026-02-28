"""
告警触发器
定期检查数据状态和触发告警
"""
import pandas as pd
from datetime import datetime, timedelta
from typing import List, Optional
import logging

from sqlalchemy import text
from utils.database import db
from utils.alerter import Alerter

logger = logging.getLogger(__name__)


class AlertTrigger:
    """告警触发器"""
    
    def __init__(self, alerter: Optional[Alerter] = None):
        self.engine = db.get_engine()
        self.alerter = alerter or Alerter()
    
    def check_all(self):
        """执行所有检查"""
        logger.info("开始执行告警检查...")
        
        # 1. 检查数据更新延迟
        self.check_data_delay()
        
        # 2. 检查校验失败记录
        self.check_validation_failures()
        
        # 3. 检查临时表堆积
        self.check_temp_table_backlog()
        
        logger.info("告警检查完成")
    
    def check_data_delay(self, hours: int = 24):
        """
        检查数据更新延迟
        
        Args:
            hours: 检查最近多少小时内的更新
        """
        try:
            df = pd.read_sql(f"""
                SELECT 
                    table_name, 
                    MAX(update_date) as last_update_date,
                    MAX(end_time) as last_update_time,
                    MAX(CASE WHEN status = 'SUCCESS' THEN record_count ELSE 0 END) as last_count
                FROM data_update_log
                WHERE end_time > DATE_SUB(NOW(), INTERVAL {hours} HOUR)
                GROUP BY table_name
            """, self.engine)
            
            if df.empty:
                logger.warning(f"最近{hours}小时无数据更新记录")
                return
            
            alerts = []
            for _, row in df.iterrows():
                table_name = row['table_name']
                last_update = row['last_update_date']
                
                if last_update is None:
                    continue
                
                # 计算延迟天数
                if isinstance(last_update, str):
                    last_update = datetime.strptime(last_update, '%Y-%m-%d').date()
                
                today = datetime.now().date()
                days_diff = (today - last_update).days
                
                if days_diff > 1:
                    alerts.append(f"{table_name} 已 {days_diff} 天未更新 (最后更新: {last_update})")
            
            if alerts:
                self.alerter.alert_data_delay(alerts)
                logger.warning(f"发现{len(alerts)}项数据延迟")
            else:
                logger.info("数据更新正常，无延迟")
                
        except Exception as e:
            logger.error(f"检查数据延迟失败: {e}")
    
    def check_validation_failures(self, hours: int = 24):
        """
        检查校验失败记录
        
        Args:
            hours: 检查最近多少小时内的失败记录
        """
        try:
            df = pd.read_sql(f"""
                SELECT 
                    table_name, 
                    update_date, 
                    record_count, 
                    status,
                    error_msg
                FROM data_update_log
                WHERE status IN ('FAILURE', 'PARTIAL')
                AND end_time > DATE_SUB(NOW(), INTERVAL {hours} HOUR)
                ORDER BY end_time DESC
            """, self.engine)
            
            if df.empty:
                logger.info("最近无校验失败记录")
                return
            
            errors = []
            for _, row in df.iterrows():
                table = row['table_name']
                date = row['update_date']
                status = row['status']
                msg = row['error_msg'] or '未知错误'
                errors.append(f"{table}({date})[{status}]: {msg[:100]}")
            
            self.alerter.alert_data_quality(
                '数据校验失败', 
                errors, 
                len(df)
            )
            logger.warning(f"发现{len(df)}条校验失败记录")
            
        except Exception as e:
            logger.error(f"检查校验失败记录失败: {e}")
    
    def check_temp_table_backlog(self, threshold: int = 10000):
        """
        检查临时表堆积情况
        
        Args:
            threshold: 堆积阈值，超过则告警
        """
        try:
            stats = self._get_temp_table_stats()
            pending = stats.get('pending', 0)
            
            if pending > threshold:
                msg = f"临时表堆积 {pending} 条待处理数据，超过阈值 {threshold}"
                logger.warning(msg)
                
                self.alerter.alert_data_quality(
                    '临时表堆积告警',
                    [msg, '请检查数据管道是否正常执行'],
                    pending
                )
            else:
                logger.info(f"临时表状态正常: 待处理{pending}条")
                
        except Exception as e:
            logger.error(f"检查临时表状态失败: {e}")
    
    def _get_temp_table_stats(self) -> dict:
        """获取临时表统计信息"""
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
            if status == 0:
                stats['pending'] = count
            elif status == 1:
                stats['passed'] = count
            elif status == 2:
                stats['failed'] = count
        
        return stats
    
    def get_system_health(self) -> dict:
        """
        获取系统健康状态
        
        Returns:
            系统状态字典
        """
        health = {
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'checks': {}
        }
        
        # 1. 数据库连接
        try:
            with db.get_connection() as conn:
                conn.execute(text("SELECT 1"))
            health['checks']['database'] = {'status': 'ok', 'message': '连接正常'}
        except Exception as e:
            health['checks']['database'] = {'status': 'error', 'message': str(e)}
        
        # 2. 临时表状态
        try:
            stats = self._get_temp_table_stats()
            total = sum(stats.values())
            if stats['pending'] > 10000:
                status = 'warning'
            else:
                status = 'ok'
            health['checks']['temp_table'] = {
                'status': status,
                'message': f"总计{total}条(待处理{stats['pending']},通过{stats['passed']},失败{stats['failed']})"
            }
        except Exception as e:
            health['checks']['temp_table'] = {'status': 'error', 'message': str(e)}
        
        # 3. 最近更新
        try:
            df = pd.read_sql("""
                SELECT table_name, MAX(end_time) as last_time
                FROM data_update_log
                WHERE status = 'SUCCESS'
                GROUP BY table_name
            """, self.engine)
            
            updates = []
            for _, row in df.iterrows():
                updates.append(f"{row['table_name']}: {row['last_time']}")
            
            health['checks']['last_updates'] = {
                'status': 'ok',
                'message': '; '.join(updates) if updates else '无记录'
            }
        except Exception as e:
            health['checks']['last_updates'] = {'status': 'error', 'message': str(e)}
        
        # 整体状态
        errors = [c for c in health['checks'].values() if c['status'] == 'error']
        warnings = [c for c in health['checks'].values() if c['status'] == 'warning']
        
        if errors:
            health['overall'] = 'error'
        elif warnings:
            health['overall'] = 'warning'
        else:
            health['overall'] = 'ok'
        
        return health
