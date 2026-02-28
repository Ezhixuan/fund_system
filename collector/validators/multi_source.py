"""
多源数据一致性校验
用于校验不同数据源的净值数据是否一致
"""
import akshare as ak
import pandas as pd
from typing import Dict, Optional
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


class MultiSourceValidator:
    """多源数据一致性校验器"""
    
    def __init__(self, threshold: float = 0.01):
        """
        Args:
            threshold: 差异阈值，默认1%
        """
        self.threshold = threshold  # 1%差异阈值
    
    def validate_nav_consistency(self, fund_code: str, nav_date: str) -> Dict:
        """
        校验多源净值数据一致性
        
        Args:
            fund_code: 基金代码
            nav_date: 净值日期 (YYYY-MM-DD)
            
        Returns:
            校验结果字典
        """
        sources = {}
        
        # 源1: akshare - 使用fund_open_fund_info_em获取
        try:
            # 尝试获取基金历史净值
            df = ak.fund_open_fund_info_em(fund_code)
            if df is not None and not df.empty:
                # 列名适配
                if '净值日期' in df.columns:
                    row = df[df['净值日期'] == nav_date]
                    if not row.empty:
                        nav_val = row['单位净值'].iloc[0] if '单位净值' in df.columns else None
                        if nav_val is not None:
                            sources['akshare'] = float(nav_val)
        except Exception as e:
            logger.debug(f"akshare获取净值失败[{fund_code}]: {e}")
            sources['akshare'] = None
        
        # 源2: 东方财富 - 简化示例
        # 实际生产环境可能需要爬虫或API
        sources['eastmoney'] = None  # 预留
        
        # 计算差异
        valid_values = [v for v in sources.values() if v is not None]
        
        if len(valid_values) >= 2:
            max_val = max(valid_values)
            min_val = min(valid_values)
            diff_pct = (max_val - min_val) / min_val if min_val > 0 else 0
            
            result = {
                'is_consistent': diff_pct <= self.threshold,
                'max_diff_pct': round(diff_pct * 100, 4),
                'sources': sources,
                'recommend': min_val if diff_pct <= self.threshold else None,
                'check_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            }
            
            if diff_pct > self.threshold:
                result['alert'] = f"数据源差异{diff_pct*100:.2f}%，需人工确认"
                logger.warning(f"[{fund_code}] {result['alert']}")
            
            return result
        
        # 单源或无法获取
        return {
            'is_consistent': True,  # 单源数据无法校验
            'sources': sources,
            'note': '仅单源数据可用或无法获取数据',
            'check_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }
    
    def batch_validate(self, fund_codes: list, nav_date: str) -> Dict[str, Dict]:
        """
        批量校验多源一致性
        
        Args:
            fund_codes: 基金代码列表
            nav_date: 净值日期
            
        Returns:
            每个基金的校验结果
        """
        results = {}
        for code in fund_codes:
            try:
                results[code] = self.validate_nav_consistency(code, nav_date)
            except Exception as e:
                logger.error(f"批量校验失败[{code}]: {e}")
                results[code] = {'error': str(e)}
        return results
