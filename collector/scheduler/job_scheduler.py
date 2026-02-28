"""
APScheduler定时调度器
实现基金数据的定时采集、校验、计算任务
"""
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger
from apscheduler.events import EVENT_JOB_EXECUTED, EVENT_JOB_ERROR, JobExecutionEvent
from datetime import datetime
import logging

from config import settings
from utils.database import db
from core.collector import FundCollector
from core.data_pipeline import DataPipeline
from core.alert_trigger import AlertTrigger
from utils.alerter import Alerter

logger = logging.getLogger(__name__)


class FundJobScheduler:
    """基金数据定时调度器"""
    
    def __init__(self):
        self.scheduler = BlockingScheduler(
            timezone='Asia/Shanghai',  # 使用北京时间
            job_defaults={
                'coalesce': True,  # 错过的任务合并执行
                'max_instances': 1,  # 同一任务同时只运行一个实例
                'misfire_grace_time': 3600  # 允许1小时的延迟容错
            }
        )
        
        # 添加事件监听
        self.scheduler.add_listener(
            self._on_job_event,
            EVENT_JOB_EXECUTED | EVENT_JOB_ERROR
        )
        
        logger.info("调度器初始化完成")
    
    def _on_job_event(self, event):
        """任务执行事件回调"""
        if event.exception:
            logger.error(f"任务 {event.job_id} 执行失败: {event.exception}")
            # 发送告警
            try:
                alerter = Alerter()
                alerter.alert_data_quality(
                    '定时任务失败',
                    [f"任务 {event.job_id} 执行失败", str(event.exception)[:200]],
                    0
                )
            except Exception as e:
                logger.error(f"发送告警失败: {e}")
        else:
            logger.info(f"任务 {event.job_id} 执行成功")
    
    def _daily_collection_job(self):
        """每日采集任务"""
        logger.info("="*60)
        logger.info("【定时任务】开始每日净值采集")
        logger.info("="*60)
        
        try:
            collector = FundCollector()
            count = collector.collect_daily_nav()
            logger.info(f"每日净值采集完成: {count}条")
            return count
        except Exception as e:
            logger.error(f"每日采集失败: {e}", exc_info=True)
            raise
    
    def _daily_validation_job(self):
        """每日校验任务"""
        logger.info("="*60)
        logger.info("【定时任务】开始每日数据校验")
        logger.info("="*60)
        
        try:
            pipeline = DataPipeline()
            result = pipeline.process_nav_data()
            logger.info(f"每日数据校验完成: 通过{len(result.passed_rules)}条规则")
            
            if not result.is_valid:
                logger.warning(f"数据校验发现问题: {result.failed_rules}")
            
            return result.is_valid
        except Exception as e:
            logger.error(f"每日校验失败: {e}", exc_info=True)
            raise
    
    def _daily_alert_check_job(self):
        """每日告警检查任务"""
        logger.info("="*60)
        logger.info("【定时任务】开始每日告警检查")
        logger.info("="*60)
        
        try:
            trigger = AlertTrigger()
            trigger.check_all()
            logger.info("每日告警检查完成")
            return True
        except Exception as e:
            logger.error(f"告警检查失败: {e}", exc_info=True)
            raise
    
    def _weekly_cleanup_job(self):
        """周度清理任务"""
        logger.info("="*60)
        logger.info("【定时任务】开始周度清理")
        logger.info("="*60)
        
        try:
            # 清理超过30天的临时表数据
            with db.get_connection() as conn:
                from sqlalchemy import text
                result = conn.execute(text("""
                    DELETE FROM tmp_fund_nav 
                    WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
                """))
                deleted = result.rowcount
                conn.commit()
            
            # 清理超过90天的日志
            with db.get_connection() as conn:
                result = conn.execute(text("""
                    DELETE FROM data_update_log 
                    WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY)
                """))
                deleted_logs = result.rowcount
                conn.commit()
            
            logger.info(f"周度清理完成: 临时表{deleted}条, 日志{deleted_logs}条")
            return {'temp_deleted': deleted, 'log_deleted': deleted_logs}
        except Exception as e:
            logger.error(f"周度清理失败: {e}", exc_info=True)
            raise
    
    def add_jobs(self):
        """注册所有定时任务"""
        # 1. 每日净值采集 - 工作日19:00
        self.scheduler.add_job(
            self._daily_collection_job,
            CronTrigger(hour=19, minute=0, day_of_week='mon-fri'),
            id='daily_collection',
            name='每日净值采集',
            replace_existing=True
        )
        logger.info("✅ 已注册每日采集任务: 工作日19:00")
        
        # 2. 每日数据校验 - 工作日19:30
        self.scheduler.add_job(
            self._daily_validation_job,
            CronTrigger(hour=19, minute=30, day_of_week='mon-fri'),
            id='daily_validation',
            name='每日数据校验',
            replace_existing=True
        )
        logger.info("✅ 已注册每日校验任务: 工作日19:30")
        
        # 3. 每日告警检查 - 工作日20:00
        self.scheduler.add_job(
            self._daily_alert_check_job,
            CronTrigger(hour=20, minute=0, day_of_week='mon-fri'),
            id='daily_alert_check',
            name='每日告警检查',
            replace_existing=True
        )
        logger.info("✅ 已注册每日告警检查任务: 工作日20:00")
        
        # 4. 周度清理任务 - 每周日02:00
        self.scheduler.add_job(
            self._weekly_cleanup_job,
            CronTrigger(hour=2, minute=0, day_of_week='sun'),
            id='weekly_cleanup',
            name='周度数据清理',
            replace_existing=True
        )
        logger.info("✅ 已注册周度清理任务: 每周日02:00")
    
    def start(self):
        """启动调度器"""
        logger.info("="*60)
        logger.info("调度器启动，等待任务执行...")
        logger.info("按 Ctrl+C 停止")
        logger.info("="*60)
        
        try:
            self.scheduler.start()
        except KeyboardInterrupt:
            logger.info("收到停止信号，调度器关闭中...")
            self.scheduler.shutdown()
            logger.info("调度器已停止")
    
    def get_job_list(self):
        """获取任务列表"""
        jobs = self.scheduler.get_jobs()
        return [
            {
                'id': job.id,
                'name': job.name,
                'next_run_time': job.next_run_time.strftime('%Y-%m-%d %H:%M:%S') if job.next_run_time else None,
                'trigger': str(job.trigger)
            }
            for job in jobs
        ]
