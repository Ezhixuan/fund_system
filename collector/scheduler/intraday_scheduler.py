#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
日内估值定时调度器
"""

import os
import sys
from datetime import datetime
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from loguru import logger

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config.settings import Config

class IntradayScheduler:
    """日内估值采集调度器"""
    
    def __init__(self, data_manager):
        self.data_manager = data_manager
        self.scheduler = BackgroundScheduler()
        self.is_running = False
    
    def start(self):
        """启动调度器"""
        if self.is_running:
            logger.warning("调度器已在运行")
            return
        
        # 添加定时任务
        self._add_jobs()
        
        # 启动调度器
        self.scheduler.start()
        self.is_running = True
        logger.info("日内估值采集调度器已启动")
    
    def stop(self):
        """停止调度器"""
        if not self.is_running:
            return
        
        self.scheduler.shutdown()
        self.is_running = False
        logger.info("日内估值采集调度器已停止")
    
    def _add_jobs(self):
        """添加定时任务"""
        # 交易日上午 9:30-11:30，每10分钟
        self.scheduler.add_job(
            self._collect_intraday,
            CronTrigger(
                day_of_week='mon-fri',
                hour='9-11',
                minute='*/10',
                second='0'
            ),
            id='morning_session',
            name='上午交易时段采集'
        )
        
        # 交易日下午 13:00-15:00，每10分钟
        self.scheduler.add_job(
            self._collect_intraday,
            CronTrigger(
                day_of_week='mon-fri',
                hour='13-14',
                minute='*/10',
                second='0'
            ),
            id='afternoon_session',
            name='下午交易时段采集'
        )
        
        # 开盘点 9:30
        self.scheduler.add_job(
            self._collect_intraday,
            CronTrigger(
                day_of_week='mon-fri',
                hour='9',
                minute='30'
            ),
            id='market_open',
            name='开盘采集'
        )
        
        # 上午收盘 11:30
        self.scheduler.add_job(
            self._collect_intraday,
            CronTrigger(
                day_of_week='mon-fri',
                hour='11',
                minute='30'
            ),
            id='morning_close',
            name='上午收盘采集'
        )
        
        # 下午开盘 13:00
        self.scheduler.add_job(
            self._collect_intraday,
            CronTrigger(
                day_of_week='mon-fri',
                hour='13',
                minute='0'
            ),
            id='afternoon_open',
            name='下午开盘采集'
        )
        
        # 收盘 15:00
        self.scheduler.add_job(
            self._collect_intraday,
            CronTrigger(
                day_of_week='mon-fri',
                hour='15',
                minute='0'
            ),
            id='market_close',
            name='收盘采集'
        )
        
        logger.info("定时任务已添加")
    
    def _collect_intraday(self):
        """执行日内采集"""
        try:
            logger.info(f"开始执行定时采集任务: {datetime.now()}")
            
            # TODO: 从数据库获取关注列表
            # 这里暂时使用测试数据
            test_funds = ['005827', '000001', '110011']
            
            # 批量采集
            result = self.data_manager.collect_batch_with_fallback(test_funds)
            
            logger.info(f"定时采集完成: 成功 {result['success']}, 失败 {result['failed']}")
            
            # TODO: 保存到数据库并推送WebSocket
            
        except Exception as e:
            logger.error(f"定时采集任务失败: {e}")
    
    def get_jobs(self):
        """获取所有任务"""
        return self.scheduler.get_jobs()
