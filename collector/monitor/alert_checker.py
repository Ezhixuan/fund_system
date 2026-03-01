#!/usr/bin/env python3
"""
告警检查模块
用于检查监控指标并触发告警
"""

import os
import sys
from datetime import datetime
from typing import List, Dict, Optional
from dataclasses import dataclass
from enum import Enum

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from monitor.collection_monitor import CollectionMonitor


class AlertLevel(Enum):
    """告警级别"""
    CRITICAL = "critical"  # 严重
    WARNING = "warning"    # 警告
    INFO = "info"          # 信息


@dataclass
class AlertRule:
    """告警规则"""
    name: str
    level: AlertLevel
    condition: str
    threshold: float
    message_template: str


@dataclass
class Alert:
    """告警信息"""
    rule_name: str
    level: AlertLevel
    message: str
    timestamp: datetime
    data: Dict


class AlertChecker:
    """告警检查器"""
    
    # 默认告警规则
    DEFAULT_RULES = [
        AlertRule(
            name="collection_failure_rate",
            level=AlertLevel.CRITICAL,
            condition="success_rate < threshold",
            threshold=95.0,
            message_template="采集成功率过低: {success_rate}% (< {threshold}%)"
        ),
        AlertRule(
            name="data_delay",
            level=AlertLevel.CRITICAL,
            condition="delay_days > threshold",
            threshold=1,
            message_template="数据延迟: {table} 延迟 {delay_days} 天"
        ),
        AlertRule(
            name="api_slow",
            level=AlertLevel.WARNING,
            condition="p99 > threshold",
            threshold=500,
            message_template="API响应慢: {api} P99={p99}ms (> {threshold}ms)"
        ),
        AlertRule(
            name="api_error_rate",
            level=AlertLevel.WARNING,
            condition="error_rate > threshold",
            threshold=5.0,
            message_template="API错误率高: {api} 错误率={error_rate}% (> {threshold}%)"
        ),
    ]
    
    def __init__(self, db_url: str = None):
        """
        初始化检查器
        
        Args:
            db_url: 数据库连接URL
        """
        self.monitor = CollectionMonitor(db_url)
        self.rules = self.DEFAULT_RULES.copy()
        self.alerts: List[Alert] = []
    
    def check_all(self) -> List[Alert]:
        """
        执行所有检查
        
        Returns:
            触发的告警列表
        """
        self.alerts = []
        
        # 检查数据采集
        self._check_collection()
        
        # 检查数据新鲜度
        self._check_data_freshness()
        
        return self.alerts
    
    def _check_collection(self):
        """检查采集成功率"""
        stats = self.monitor.get_collection_stats()
        
        rule = self._get_rule("collection_failure_rate")
        if stats.success_rate < rule.threshold:
            alert = Alert(
                rule_name=rule.name,
                level=rule.level,
                message=rule.message_template.format(
                    success_rate=stats.success_rate,
                    threshold=rule.threshold
                ),
                timestamp=datetime.now(),
                data={
                    "date": stats.date,
                    "total_funds": stats.total_funds,
                    "collected_funds": stats.collected_funds,
                    "success_rate": stats.success_rate,
                    "failed_count": stats.failed_count
                }
            )
            self.alerts.append(alert)
            self._notify(alert)
    
    def _check_data_freshness(self):
        """检查数据新鲜度"""
        table_status = self.monitor.get_table_status()
        
        rule = self._get_rule("data_delay")
        for status in table_status:
            if not status.is_fresh and status.delay_days > rule.threshold:
                alert = Alert(
                    rule_name=rule.name,
                    level=rule.level,
                    message=rule.message_template.format(
                        table=status.table_name,
                        delay_days=status.delay_days
                    ),
                    timestamp=datetime.now(),
                    data={
                        "table": status.table_name,
                        "latest_date": status.latest_date,
                        "delay_days": status.delay_days,
                        "record_count": status.record_count
                    }
                )
                self.alerts.append(alert)
                self._notify(alert)
    
    def _get_rule(self, name: str) -> Optional[AlertRule]:
        """获取规则"""
        for rule in self.rules:
            if rule.name == name:
                return rule
        return None
    
    def _notify(self, alert: Alert):
        """
        发送告警通知
        
        目前实现：
        1. 控制台输出
        2. 日志记录
        
        可扩展：
        - 钉钉通知
        - 邮件通知
        - 短信通知
        """
        level_mark = {
            AlertLevel.CRITICAL: "🔴",
            AlertLevel.WARNING: "🟡",
            AlertLevel.INFO: "🟢"
        }.get(alert.level, "⚪")
        
        print(f"\n{level_mark} [{alert.level.value.upper()}] {alert.timestamp.strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"   规则: {alert.rule_name}")
        print(f"   消息: {alert.message}")
        print(f"   数据: {alert.data}")
        print()
        
        # 这里可以添加钉钉/邮件通知代码
        # self._send_dingtalk(alert)
        # self._send_email(alert)
    
    def _send_dingtalk(self, alert: Alert):
        """
        发送钉钉通知（预留接口）
        
        需要在环境变量配置：
        - DINGTALK_WEBHOOK: 钉钉机器人Webhook地址
        - DINGTALK_SECRET: 钉钉机器人密钥
        """
        webhook = os.getenv("DINGTALK_WEBHOOK")
        if not webhook:
            return
        
        # TODO: 实现钉钉通知
        pass
    
    def _send_email(self, alert: Alert):
        """
        发送邮件通知（预留接口）
        
        需要在环境变量配置：
        - SMTP_HOST: SMTP服务器地址
        - SMTP_PORT: SMTP端口
        - SMTP_USER: 邮箱账号
        - SMTP_PASSWORD: 邮箱密码
        - ALERT_EMAIL: 接收告警的邮箱
        """
        smtp_host = os.getenv("SMTP_HOST")
        if not smtp_host:
            return
        
        # TODO: 实现邮件通知
        pass
    
    def get_alert_summary(self) -> Dict:
        """获取告警摘要"""
        return {
            "total_alerts": len(self.alerts),
            "critical_count": sum(1 for a in self.alerts if a.level == AlertLevel.CRITICAL),
            "warning_count": sum(1 for a in self.alerts if a.level == AlertLevel.WARNING),
            "info_count": sum(1 for a in self.alerts if a.level == AlertLevel.INFO),
        }


def main():
    """命令行测试"""
    print("=" * 60)
    print("告警检查")
    print("=" * 60)
    
    checker = AlertChecker()
    alerts = checker.check_all()
    
    if alerts:
        print(f"\n共发现 {len(alerts)} 个告警")
        summary = checker.get_alert_summary()
        print(f"严重: {summary['critical_count']}, 警告: {summary['warning_count']}, 信息: {summary['info_count']}")
    else:
        print("\n✅ 所有检查通过，无告警")
    
    print("=" * 60)


if __name__ == '__main__':
    main()
