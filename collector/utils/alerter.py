"""
告警通知器
支持钉钉和邮件通知
"""
import requests
import smtplib
from email.mime.text import MIMEText
from typing import List, Optional
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


class Alerter:
    """告警通知器"""
    
    def __init__(self, 
                 dingtalk_webhook: Optional[str] = None, 
                 email_config: Optional[dict] = None):
        """
        Args:
            dingtalk_webhook: 钉钉机器人webhook地址
            email_config: 邮件配置 {
                'smtp_server': 'smtp.example.com',
                'smtp_port': 587,
                'username': 'user@example.com',
                'password': 'password',
                'from': 'alert@example.com',
                'alert_recipients': ['admin@example.com']
            }
        """
        self.dingtalk_webhook = dingtalk_webhook
        self.email_config = email_config or {}
    
    def send_dingtalk(self, title: str, message: str) -> bool:
        """
        发送钉钉通知
        
        Args:
            title: 消息标题
            message: 消息内容(Markdown格式)
            
        Returns:
            发送成功返回True
        """
        if not self.dingtalk_webhook:
            logger.debug("钉钉webhook未配置，跳过发送")
            return False
        
        try:
            payload = {
                'msgtype': 'markdown',
                'markdown': {
                    'title': title,
                    'text': f"## {title}\n\n{message}\n\n---\n*发送时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*"
                }
            }
            
            response = requests.post(
                self.dingtalk_webhook,
                json=payload,
                timeout=10
            )
            response.raise_for_status()
            result = response.json()
            
            if result.get('errcode') == 0:
                logger.info(f"钉钉通知发送成功: {title}")
                return True
            else:
                logger.error(f"钉钉通知发送失败: {result}")
                return False
                
        except Exception as e:
            logger.error(f"钉钉通知发送异常: {e}")
            return False
    
    def send_email(self, subject: str, body: str, to_addrs: Optional[List[str]] = None) -> bool:
        """
        发送邮件通知
        
        Args:
            subject: 邮件主题
            body: 邮件正文
            to_addrs: 收件人列表，默认使用配置中的alert_recipients
            
        Returns:
            发送成功返回True
        """
        if not self.email_config.get('smtp_server'):
            logger.debug("邮件配置未设置，跳过发送")
            return False
        
        recipients = to_addrs or self.email_config.get('alert_recipients', [])
        if not recipients:
            logger.warning("没有配置收件人")
            return False
        
        try:
            msg = MIMEText(body, 'plain', 'utf-8')
            msg['Subject'] = subject
            msg['From'] = self.email_config.get('from', 'alert@fund-system')
            msg['To'] = ', '.join(recipients)
            
            server = smtplib.SMTP(
                self.email_config['smtp_server'], 
                self.email_config.get('smtp_port', 587),
                timeout=10
            )
            
            if self.email_config.get('use_tls', True):
                server.starttls()
            
            if self.email_config.get('username') and self.email_config.get('password'):
                server.login(self.email_config['username'], self.email_config['password'])
            
            server.sendmail(msg['From'], recipients, msg.as_string())
            server.quit()
            
            logger.info(f"邮件发送成功: {subject} -> {recipients}")
            return True
            
        except Exception as e:
            logger.error(f"邮件发送失败: {e}")
            return False
    
    def alert_data_quality(self, table_name: str, errors: List[str], record_count: int = 0):
        """
        数据质量告警
        
        Args:
            table_name: 表名
            errors: 错误信息列表
            record_count: 影响记录数
        """
        title = f"[基金系统] 数据质量异常 - {table_name}"
        
        message = f"""
**表名**: {table_name}

**时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

**影响记录数**: {record_count}

**异常详情**:
{"".join([f"- {e}\\n" for e in errors])}

请尽快检查数据管道状态。
        """.strip()
        
        # 发送钉钉
        self.send_dingtalk(title, message)
        
        # 发送邮件
        if self.email_config:
            self.send_email(title, message)
    
    def alert_data_delay(self, delays: List[str]):
        """
        数据延迟告警
        
        Args:
            delays: 延迟信息列表
        """
        title = "[基金系统] 数据更新延迟告警"
        
        message = f"""
**告警类型**: 数据更新延迟

**时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

**延迟详情**:
{"".join([f"- {d}\\n" for d in delays])}

请检查数据采集任务是否正常执行。
        """.strip()
        
        self.send_dingtalk(title, message)
        
        if self.email_config:
            self.send_email(title, message)
