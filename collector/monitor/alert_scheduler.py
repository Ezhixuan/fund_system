#!/usr/bin/env python3
"""
告警调度器
定时执行告警检查
"""

import os
import sys
import time
import signal
from datetime import datetime

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger

from monitor.alert_checker import AlertChecker


class AlertScheduler:
    """告警调度器"""
    
    def __init__(self, check_interval_minutes: int = 30):
        """
        初始化调度器
        
        Args:
            check_interval_minutes: 检查间隔（分钟），默认30分钟
        """
        self.check_interval = check_interval_minutes
        self.scheduler = BackgroundScheduler()
        self.checker = AlertChecker()
        self.running = False
        
        # 注册信号处理
        signal.signal(signal.SIGTERM, self._signal_handler)
        signal.signal(signal.SIGINT, self._signal_handler)
    
    def _signal_handler(self, signum, frame):
        """信号处理"""
        print(f"\n收到信号 {signum}，正在停止...")
        self.stop()
    
    def _check_job(self):
        """检查任务"""
        print(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 执行告警检查...")
        
        try:
            alerts = self.checker.check_all()
            
            if alerts:
                summary = self.checker.get_alert_summary()
                print(f"检查完成: 发现 {len(alerts)} 个告警 "
                      f"(严重:{summary['critical_count']} 警告:{summary['warning_count']})")
            else:
                print("检查完成: 无告警")
                
        except Exception as e:
            print(f"检查失败: {e}")
    
    def start(self):
        """启动调度器"""
        if self.running:
            print("调度器已在运行")
            return
        
        print(f"启动告警调度器，检查间隔: {self.check_interval}分钟")
        
        # 添加定时任务
        self.scheduler.add_job(
            self._check_job,
            trigger=IntervalTrigger(minutes=self.check_interval),
            id='alert_check',
            name='告警检查',
            replace_existing=True
        )
        
        # 立即执行一次
        self._check_job()
        
        # 启动调度器
        self.scheduler.start()
        self.running = True
        
        print("调度器已启动，按 Ctrl+C 停止")
        
        # 保持运行
        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            self.stop()
    
    def stop(self):
        """停止调度器"""
        if not self.running:
            return
        
        print("正在停止调度器...")
        self.scheduler.shutdown()
        self.running = False
        print("调度器已停止")
    
    def run_once(self):
        """执行一次检查"""
        self._check_job()


def main():
    """命令行入口"""
    import argparse
    
    parser = argparse.ArgumentParser(description='告警调度器')
    parser.add_argument(
        '--interval', '-i',
        type=int,
        default=30,
        help='检查间隔（分钟），默认30分钟'
    )
    parser.add_argument(
        '--once', '-o',
        action='store_true',
        help='只执行一次检查'
    )
    
    args = parser.parse_args()
    
    scheduler = AlertScheduler(check_interval_minutes=args.interval)
    
    if args.once:
        scheduler.run_once()
    else:
        scheduler.start()


if __name__ == '__main__':
    main()
