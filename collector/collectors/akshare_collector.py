#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Akshare数据采集器
"""

import akshare as ak
from datetime import datetime
from typing import Optional, Dict, Any
from loguru import logger
from .base import BaseCollector

class AkshareCollector(BaseCollector):
    """Akshare基金估值采集器"""
    
    def __init__(self):
        super().__init__('akshare')
    
    def collect_estimate(self, fund_code: str) -> Optional[Dict[str, Any]]:
        """
        使用akshare采集基金估值
        """
        try:
            # 获取基金实时估值
            df = ak.fund_em_value_estimation()
            
            # 查找指定基金
            fund_data = df[df['基金代码'] == fund_code]
            
            if fund_data.empty:
                logger.warning(f"akshare 未找到基金 {fund_code}")
                return None
            
            row = fund_data.iloc[0]
            
            # 解析数据
            nav = self._parse_float(row.get('单位净值'))
            change_pct = self._parse_float(row.get('估算涨跌'))
            
            if nav is None:
                logger.warning(f"akshare {fund_code} 净值数据为空")
                return None
            
            # 计算昨日收盘
            pre_close = nav / (1 + change_pct / 100) if change_pct else nav
            
            return {
                'fund_code': fund_code,
                'fund_name': row.get('基金名称', ''),
                'nav': round(nav, 4),
                'change_pct': round(change_pct, 4) if change_pct else 0.0,
                'change_amt': round(nav - pre_close, 4) if change_pct else 0.0,
                'pre_close': round(pre_close, 4),
                'time': datetime.now(),
                'source': 'akshare'
            }
            
        except Exception as e:
            logger.error(f"akshare 采集 {fund_code} 失败: {e}")
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
