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
            # 使用 fund_value_estimation_em 获取实时估值
            df = ak.fund_value_estimation_em()
            
            # 查找指定基金
            fund_data = df[df['基金代码'] == fund_code]
            
            if fund_data.empty:
                logger.warning(f"akshare 未找到基金 {fund_code}")
                return None
            
            row = fund_data.iloc[0]
            
            # 获取列名（包含日期）
            columns = list(df.columns)
            
            # 找到估算值列（通常是第4列，包含"估算数据-估算值"）
            estimate_col = None
            change_col = None
            nav_col = None
            
            for col in columns:
                if '估算数据-估算值' in col:
                    estimate_col = col
                elif '估算数据-估算增长率' in col:
                    change_col = col
                elif '公布数据-单位净值' in col:
                    nav_col = col
            
            # 解析数据
            estimate_nav = self._parse_float(row.get(estimate_col)) if estimate_col else None
            change_pct = self._parse_float(row.get(change_col)) if change_col else None
            nav = self._parse_float(row.get(nav_col)) if nav_col else None
            
            # 如果没有估算值，使用最新净值
            if estimate_nav is None and nav is not None:
                estimate_nav = nav
            
            if estimate_nav is None:
                logger.warning(f"akshare {fund_code} 净值数据为空")
                return None
            
            # 计算昨日收盘（如果估算增长率为空，设为0）
            if change_pct is not None and change_pct != 0:
                pre_close = estimate_nav / (1 + change_pct / 100)
            else:
                pre_close = estimate_nav
            
            return {
                'fund_code': fund_code,
                'fund_name': row.get('基金名称', ''),
                'nav': round(estimate_nav, 4),
                'change_pct': round(change_pct, 4) if change_pct else 0.0,
                'change_amt': round(estimate_nav - pre_close, 4),
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
            # 处理百分比格式
            str_value = str(value).replace('%', '').strip()
            return float(str_value)
        except (ValueError, TypeError):
            return None
