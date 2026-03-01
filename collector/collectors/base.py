#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
采集器基类
"""

from abc import ABC, abstractmethod
from datetime import datetime
from typing import Optional, Dict, Any
from loguru import logger

class BaseCollector(ABC):
    """基金数据采集器基类"""
    
    def __init__(self, name: str):
        self.name = name
        self.enabled = True
        self.error_count = 0
        self.max_errors = 5
    
    @abstractmethod
    def collect_estimate(self, fund_code: str) -> Optional[Dict[str, Any]]:
        """
        采集单只基金估值
        
        Args:
            fund_code: 基金代码
            
        Returns:
            {
                'fund_code': str,
                'fund_name': str,
                'nav': float,           # 预估净值
                'change_pct': float,    # 涨跌幅%
                'change_amt': float,    # 涨跌额
                'pre_close': float,     # 昨日收盘
                'time': datetime,
                'source': str           # 数据来源
            }
        """
        pass
    
    def is_available(self) -> bool:
        """检查采集器是否可用"""
        return self.enabled and self.error_count < self.max_errors
    
    def record_error(self):
        """记录错误"""
        self.error_count += 1
        if self.error_count >= self.max_errors:
            self.enabled = False
            logger.warning(f"{self.name} 采集器已禁用，错误次数过多")
    
    def reset_error(self):
        """重置错误计数"""
        self.error_count = 0
        self.enabled = True
