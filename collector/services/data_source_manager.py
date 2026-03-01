#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据源管理器
支持多数据源自动切换
"""

from typing import Optional, Dict, Any, List
from loguru import logger

from collectors.akshare_collector import AkshareCollector
from collectors.eastmoney_collector import EastmoneyCollector
from collectors.danjuan_collector import DanjuanCollector
from utils.validator import DataValidator

class DataSourceManager:
    """数据源管理器"""
    
    def __init__(self):
        self.collectors = [
            AkshareCollector(),      # 优先级 1
            EastmoneyCollector(),    # 优先级 2
            DanjuanCollector()       # 优先级 3
        ]
        self.validator = DataValidator()
    
    def collect_with_fallback(self, fund_code: str) -> Optional[Dict[str, Any]]:
        """
        带备用切换的采集
        
        依次尝试所有数据源，直到成功或全部失败
        """
        for collector in self.collectors:
            if not collector.is_available():
                logger.debug(f"{collector.name} 不可用，跳过")
                continue
            
            try:
                logger.info(f"尝试使用 {collector.name} 采集 {fund_code}")
                data = collector.collect_estimate(fund_code)
                
                if data and self.validator.validate_estimate(data):
                    logger.info(f"{collector.name} 采集 {fund_code} 成功")
                    return data
                else:
                    logger.warning(f"{collector.name} 采集 {fund_code} 数据无效")
                    
            except Exception as e:
                logger.error(f"{collector.name} 采集 {fund_code} 失败: {e}")
                collector.record_error()
                continue
        
        logger.error(f"所有数据源均采集失败 {fund_code}")
        return None
    
    def collect_batch_with_fallback(self, fund_codes: List[str]) -> Dict[str, Any]:
        """
        批量采集（带备用切换）
        """
        results = []
        errors = []
        
        for fund_code in fund_codes:
            data = self.collect_with_fallback(fund_code)
            if data:
                results.append(data)
            else:
                errors.append(fund_code)
        
        return {
            'success': len(results),
            'failed': len(errors),
            'results': results,
            'errors': errors
        }
    
    def get_data_source_status(self) -> List[Dict[str, Any]]:
        """获取数据源状态"""
        return [
            {
                'name': c.name,
                'enabled': c.enabled,
                'available': c.is_available(),
                'error_count': c.error_count
            }
            for c in self.collectors
        ]
    
    def reset_all_collectors(self):
        """重置所有采集器"""
        for collector in self.collectors:
            collector.reset_error()
        logger.info("所有采集器已重置")
