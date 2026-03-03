#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
配置文件
"""

import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

# 根据 ENV 环境变量确定默认数据库端口
# development=3307 (本地MySQL), production=13306 (Docker MySQL)
ENV = os.environ.get('ENV', 'development')
DEFAULT_MYSQL_PORT = 3307 if ENV == 'development' else 13306
DEFAULT_MYSQL_USER = 'root' if ENV == 'development' else 'fund'
DEFAULT_MYSQL_PASSWORD = '1q2w3e4r5%' if ENV == 'development' else 'fund123'

class Config:
    """应用配置"""

    # Flask配置
    SECRET_KEY = os.environ.get('SECRET_KEY', 'fund-collector-secret-key')
    DEBUG = os.environ.get('DEBUG', 'False').lower() == 'true'

    # 数据库配置 - 根据环境自动选择默认值
    MYSQL_HOST = os.environ.get('MYSQL_HOST', '127.0.0.1')
    MYSQL_PORT = int(os.environ.get('MYSQL_PORT', DEFAULT_MYSQL_PORT))
    MYSQL_USER = os.environ.get('MYSQL_USER', DEFAULT_MYSQL_USER)
    MYSQL_PASSWORD = os.environ.get('MYSQL_PASSWORD', DEFAULT_MYSQL_PASSWORD)
    MYSQL_DATABASE = os.environ.get('MYSQL_DATABASE', 'fund_system')
    MYSQL_DB = MYSQL_DATABASE  # 别名兼容

    @classmethod
    def get_db_config(cls) -> dict:
        """获取数据库配置字典"""
        return {
            'host': cls.MYSQL_HOST,
            'port': cls.MYSQL_PORT,
            'user': cls.MYSQL_USER,
            'password': cls.MYSQL_PASSWORD,
            'database': cls.MYSQL_DATABASE,
            'charset': 'utf8mb4'
        }

    @classmethod
    def get_db_url(cls) -> str:
        """获取 SQLAlchemy 数据库连接 URL"""
        return (
            f"mysql+pymysql://{cls.MYSQL_USER}:{cls.MYSQL_PASSWORD}"
            f"@{cls.MYSQL_HOST}:{cls.MYSQL_PORT}/{cls.MYSQL_DATABASE}"
            f"?charset=utf8mb4"
        )
    
    # Redis配置
    REDIS_HOST = os.environ.get('REDIS_HOST', 'localhost')
    REDIS_PORT = int(os.environ.get('REDIS_PORT', 6379))
    REDIS_DB = int(os.environ.get('REDIS_DB', 0))
    
    # 采集配置
    COLLECT_INTERVAL = int(os.environ.get('COLLECT_INTERVAL', 10))  # 采集间隔（分钟）
    MAX_RETRY = int(os.environ.get('MAX_RETRY', 3))  # 最大重试次数

    # akshare 配置（兼容 pydantic settings）
    RETRY_TIMES = int(os.environ.get('RETRY_TIMES', 3))  # 重试次数
    RETRY_DELAY = float(os.environ.get('RETRY_DELAY', 1.0))  # 重试延迟基数
    REQUEST_DELAY = float(os.environ.get('REQUEST_DELAY', 0.5))  # 请求间隔（秒）
    
    # 数据源配置
    DATA_SOURCES = {
        'akshare': {
            'name': 'akshare',
            'enabled': True,
            'priority': 1,
            'rate_limit': '10/minute'
        },
        'eastmoney': {
            'name': 'eastmoney',
            'enabled': True,
            'priority': 2,
            'url': 'https://fundmobapi.eastmoney.com/FundMNewApi/FundMNFInfo',
            'rate_limit': '30/minute'
        },
        'danjuan': {
            'name': 'danjuan',
            'enabled': True,
            'priority': 3,
            'url': 'https://danjuanfunds.com/djapi/fund/{fund_code}',
            'rate_limit': '20/minute'
        }
    }
    
    # 交易时间配置
    TRADING_HOURS = {
        'morning': {'start': '09:30', 'end': '11:30'},
        'afternoon': {'start': '13:00', 'end': '15:00'}
    }


# 创建兼容的 settings 对象（支持 from config import settings）
class _SettingsCompat:
    """兼容层：提供与 pydantic Settings 相同的接口"""

    @property
    def mysql_host(self):
        return Config.MYSQL_HOST

    @property
    def mysql_port(self):
        return Config.MYSQL_PORT

    @property
    def mysql_user(self):
        return Config.MYSQL_USER

    @property
    def mysql_password(self):
        return Config.MYSQL_PASSWORD

    @property
    def mysql_db(self):
        return Config.MYSQL_DB

    @property
    def env(self):
        return os.environ.get('ENV', 'development')

    @property
    def request_delay(self):
        return Config.REQUEST_DELAY

    @property
    def retry_times(self):
        return Config.RETRY_TIMES

    @property
    def retry_delay(self):
        return Config.RETRY_DELAY

    @property
    def log_level(self):
        return os.environ.get('LOG_LEVEL', 'INFO')

    @property
    def collector_port(self):
        return int(os.environ.get('COLLECTOR_PORT', 5005))

    @property
    def enable_startup_sync(self):
        return os.environ.get('ENABLE_STARTUP_SYNC', 'true').lower() == 'true'

    @property
    def enable_fund_sync_scheduler(self):
        return os.environ.get('ENABLE_FUND_SYNC_SCHEDULER', 'true').lower() == 'true'

    def get_db_config(self) -> dict:
        return Config.get_db_config()

    def get_db_url(self) -> str:
        return Config.get_db_url()

    def is_development(self) -> bool:
        return self.env == 'development'

    def is_production(self) -> bool:
        return self.env == 'production'


settings = _SettingsCompat()


# 模块级别兼容函数（当直接导入 config.settings 模块时使用）
def get_db_config() -> dict:
    """获取数据库配置字典"""
    return Config.get_db_config()


def get_db_url() -> str:
    """获取 SQLAlchemy 数据库连接 URL"""
    return Config.get_db_url()
