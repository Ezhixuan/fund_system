#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
东方财富数据采集器
"""

import requests
import json
from datetime import datetime
from typing import Optional, Dict, Any
from loguru import logger
from .base import BaseCollector

class EastmoneyCollector(BaseCollector):
    """东方财富基金估值采集器"""
    
    def __init__(self):
        super().__init__('eastmoney')
        self.base_url = 'https://fundmobapi.eastmoney.com/FundMNewApi/FundMNFInfo'
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
    
    def collect_estimate(self, fund_code: str) -> Optional[Dict[str, Any]]:
        """
        使用东方财富API采集基金估值
        """
        try:
            params = {
                'pageIndex': '1',
                'pageSize': '1',
                'appType': 'ttjj',
                'product': 'EFund',
                'plat': 'Android',
                'deviceid': '123',
                'Version': '1',
                'Fcodes': fund_code
            }
            
            response = requests.get(
                self.base_url, 
                params=params, 
                headers=self.headers,
                timeout=10
            )
            response.raise_for_status()
            
            data = response.json()
            
            if not data.get('Datas'):
                logger.warning(f"eastmoney 未找到基金 {fund_code}")
                return None
            
            fund_data = data['Datas'][0]
            
            # 解析净值和涨跌幅
            nav = self._parse_float(fund_data.get('NAV'))
            change_pct = self._parse_float(fund_data.get('NAVCHGRT'))
            
            if nav is None:
                logger.warning(f"eastmoney {fund_code} 净值数据为空")
                return None
            
            # 计算昨日收盘
            pre_close = nav / (1 + change_pct / 100) if change_pct else nav
            
            return {
                'fund_code': fund_code,
                'fund_name': fund_data.get('NAME', ''),
                'nav': round(nav, 4),
                'change_pct': round(change_pct, 4) if change_pct else 0.0,
                'change_amt': round(nav - pre_close, 4) if change_pct else 0.0,
                'pre_close': round(pre_close, 4),
                'time': datetime.now(),
                'source': 'eastmoney'
            }
            
        except requests.RequestException as e:
            logger.error(f"eastmoney 请求失败 {fund_code}: {e}")
            self.record_error()
            return None
        except Exception as e:
            logger.error(f"eastmoney 采集 {fund_code} 失败: {e}")
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
