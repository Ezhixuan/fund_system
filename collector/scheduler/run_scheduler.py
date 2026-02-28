#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调度器运行入口

用法:
    python run_scheduler.py              # 前台运行
    nohup python run_scheduler.py &      # 后台运行
    
停止:
    ps aux | grep run_scheduler
    kill <pid>
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import settings
from utils.logging_config import setup_logging
from scheduler.job_scheduler import FundJobScheduler
import logging

def main():
    # 配置日志
    setup_logging()
    logger = logging.getLogger(__name__)
    
    logger.info("="*60)
    logger.info("基金数据调度器启动")
    logger.info("="*60)
    
    # 创建并配置调度器
    scheduler = FundJobScheduler()
    scheduler.add_jobs()
    
    # 打印任务列表
    jobs = scheduler.get_job_list()
    logger.info(f"已注册 {len(jobs)} 个定时任务:")
    for job in jobs:
        logger.info(f"  - {job['name']}: {job['trigger']}")
    
    # 启动调度器
    scheduler.start()

if __name__ == '__main__':
    main()
