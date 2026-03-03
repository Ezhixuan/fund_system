#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金基础数据同步定时调度器
每 30 天执行一次全量基金基础数据同步检查
"""

import os
import sys
import threading
from datetime import datetime
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger
from loguru import logger

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class FundSyncScheduler:
    """基金数据同步调度器"""

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        """单例模式，确保只有一个调度器实例"""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if hasattr(self, '_initialized'):
            return

        self.scheduler = BackgroundScheduler()
        self.is_running = False
        self._sync_lock = threading.Lock()  # 用于幂等性控制
        self._initialized = True

    def start(self):
        """启动调度器"""
        if self.is_running:
            logger.warning("基金同步调度器已在运行")
            return

        # 添加定时任务
        self._add_jobs()

        # 启动调度器
        self.scheduler.start()
        self.is_running = True
        logger.info("基金数据同步调度器已启动")

    def stop(self):
        """停止调度器"""
        if not self.is_running:
            return

        self.scheduler.shutdown()
        self.is_running = False
        logger.info("基金数据同步调度器已停止")

    def _add_jobs(self):
        """添加定时任务"""
        # 每 30 天执行一次全量同步检查
        self.scheduler.add_job(
            self._run_sync_job,
            IntervalTrigger(days=30),
            id='fund_basic_sync',
            name='基金基础数据全量同步',
            replace_existing=True
        )

        logger.info("基金数据同步定时任务已添加 (每30天执行一次)")

    def _run_sync_job(self):
        """
        执行同步任务（带幂等性控制）
        """
        # 使用锁确保同一时间只有一个同步任务在执行
        if not self._sync_lock.acquire(blocking=False):
            logger.warning("已有同步任务在执行，跳过本次调度")
            return

        try:
            logger.info("=" * 60)
            logger.info("开始执行定时基金数据同步任务")
            logger.info("=" * 60)

            from services.fund_sync_service import FundSyncService

            service = FundSyncService()

            try:
                # 比对数据
                compare_result = service.compare_fund_data()

                # 如果需要同步，执行同步
                if compare_result.get('sync_needed'):
                    logger.info("检测到数据不一致，开始自动同步...")
                    sync_result = service.sync_fund_basic_data()

                    if sync_result['success']:
                        logger.info(f"✅ 定时同步完成，共同步 {sync_result['total_synced']} 只基金")
                    else:
                        logger.error(f"❌ 定时同步失败: {sync_result}")
                else:
                    logger.info("✅ 数据已是最新，无需同步")

            finally:
                service.close()

        except Exception as e:
            logger.error(f"定时同步任务执行失败: {e}")

        finally:
            self._sync_lock.release()

    def get_jobs(self):
        """获取所有任务"""
        return self.scheduler.get_jobs()

    def get_next_run_time(self):
        """获取下次执行时间"""
        job = self.scheduler.get_job('fund_basic_sync')
        if job:
            return job.next_run_time
        return None


def init_fund_sync_scheduler():
    """
    初始化基金同步调度器
    在应用启动时调用
    """
    try:
        # 检查环境变量，默认启用
        enable_sync_scheduler = os.environ.get('ENABLE_FUND_SYNC_SCHEDULER', 'true').lower() == 'true'

        if not enable_sync_scheduler:
            logger.info("基金同步调度器已禁用 (ENABLE_FUND_SYNC_SCHEDULER=false)")
            return None

        scheduler = FundSyncScheduler()
        scheduler.start()
        return scheduler

    except Exception as e:
        logger.error(f"初始化基金同步调度器失败: {e}")
        return None


if __name__ == '__main__':
    # 测试调度器
    import time

    scheduler = FundSyncScheduler()
    scheduler.start()

    print("调度器已启动，等待任务执行...")
    print(f"下次执行时间: {scheduler.get_next_run_time()}")

    try:
        # 保持运行
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        scheduler.stop()
        print("调度器已停止")
