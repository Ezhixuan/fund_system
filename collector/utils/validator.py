#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据校验工具
"""

from typing import Dict, Any
from loguru import logger

class DataValidator:
    """数据校验器"""
    
    def validate_estimate(self, data: Dict[str, Any]) -> bool:
        """
        校验估值数据
        
        校验规则:
        1. 涨跌幅绝对值不超过 15%
        2. 净值在合理范围 (0.1 - 100)
        3. 基金代码不为空
        """
        try:
            # 检查基金代码
            if not data.get('fund_code'):
                logger.warning("基金代码为空")
                return False
            
            # 检查净值
            nav = data.get('nav')
            if nav is None:
                logger.warning(f"{data['fund_code']} 净值为空")
                return False
            
            if nav < 0.1 or nav > 100:
                logger.warning(f"{data['fund_code']} 净值异常: {nav}")
                return False
            
            # 检查涨跌幅
            change_pct = data.get('change_pct', 0)
            if abs(change_pct) > 15:
                logger.warning(f"{data['fund_code']} 涨跌幅异常: {change_pct}%")
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"数据校验失败: {e}")
            return False
