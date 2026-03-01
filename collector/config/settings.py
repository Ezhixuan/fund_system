#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
配置文件
"""

import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

class Config:
    """应用配置"""
    
    # Flask配置
    SECRET_KEY = os.environ.get('SECRET_KEY', 'fund-collector-secret-key')
    DEBUG = os.environ.get('DEBUG', 'False').lower() == 'true'
    
    # 数据库配置
    MYSQL_HOST = os.environ.get('MYSQL_HOST', 'localhost')
    MYSQL_PORT = int(os.environ.get('MYSQL_PORT', 3306))
    MYSQL_USER = os.environ.get('MYSQL_USER', 'root')
    MYSQL_PASSWORD = os.environ.get('MYSQL_PASSWORD', '123456')
    MYSQL_DATABASE = os.environ.get('MYSQL_DATABASE', 'fund_system')
    
    # Redis配置
    REDIS_HOST = os.environ.get('REDIS_HOST', 'localhost')
    REDIS_PORT = int(os.environ.get('REDIS_PORT', 6379))
    REDIS_DB = int(os.environ.get('REDIS_DB', 0))
    
    # 采集配置
    COLLECT_INTERVAL = int(os.environ.get('COLLECT_INTERVAL', 10))  # 采集间隔（分钟）
    MAX_RETRY = int(os.environ.get('MAX_RETRY', 3))  # 最大重试次数
    
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
