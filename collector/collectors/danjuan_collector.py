#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
蛋卷基金数据采集器
"""

import requests
from datetime import datetime
from typing import Optional, Dict, Any
from loguru import logger
from .base import BaseCollector

class DanjuanCollector(BaseCollector):
    """蛋卷基金估值采集器"""
    
    def __init__(self):
        super().__init__('danjuan')
        self.base_url = 'https://danjuanfunds.com/djapi/fund/{fund_code}'
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Accept': 'application/json'
        }
    
    def collect_estimate(self, fund_code: str) -> Optional[Dict[str, Any]]:
        """
        使用蛋卷API采集基金估值
        """
        try:
            url = self.base_url.format(fund_code=fund_code)
            
            response = requests.get(
                url,
                headers=self.headers,
                timeout=10
            )
            response.raise_for_status()
            
            data = response.json()
            
            if data.get('result_code') != 0:
                logger.warning(f"danjuan API错误 {fund_code}: {data.get('result_msg')}")
                return None
            
            fund_data = data.get('data', {})
            
            # 解析净值和涨跌幅
            nav = self._parse_float(fund_data.get('nav'))
            change_pct = self._parse_float(fund_data.get('nav_grtd'))
            
            if nav is None:
                logger.warning(f"danjuan {fund_code} 净值数据为空")
                return None
            
            # 计算昨日收盘
            pre_close = nav / (1 + change_pct / 100) if change_pct else nav
            
            return {
                'fund_code': fund_code,
                'fund_name': fund_data.get('name', ''),
                'nav': round(nav, 4),
                'change_pct': round(change_pct, 4) if change_pct else 0.0,
                'change_amt': round(nav - pre_close, 4) if change_pct else 0.0,
                'pre_close': round(pre_close, 4),
                'time': datetime.now(),
                'source': 'danjuan'
            }
            
        except requests.RequestException as e:
            logger.error(f"danjuan 请求失败 {fund_code}: {e}")
            self.record_error()
            return None
        except Exception as e:
            logger.error(f"danjuan 采集 {fund_code} 失败: {e}")
            self.record_error()
            return None
    
    def _parse_float(self, value) -> Optional[float]:
        """解析浮点数"""
        if value is None or value == '-' or value == '':
            return None
        try:
            return float(str(value).replace('%', ''))
        except (ValueError, TypeError):
            return None
