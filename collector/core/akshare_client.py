"""
akshare客户端封装
带频率控制和重试机制
"""
import time
import logging
from typing import Optional, Callable
from functools import wraps

import pandas as pd
import akshare as ak

from config import settings

logger = logging.getLogger(__name__)


def retry_on_error(max_retries: int = None, delay: float = None):
    """重试装饰器"""
    max_retries = max_retries or settings.retry_times
    delay = delay or settings.retry_delay
    
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_exception = None
            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    last_exception = e
                    wait_time = delay * (2 ** attempt)  # 指数退避
                    logger.warning(f"请求失败({attempt + 1}/{max_retries}): {e}, {wait_time:.1f}s后重试")
                    time.sleep(wait_time)
            
            logger.error(f"请求失败，已重试{max_retries}次: {last_exception}")
            raise last_exception
        return wrapper
    return decorator


class AkShareClient:
    """akshare客户端封装"""
    
    def __init__(self, delay: float = None):
        self.delay = delay or settings.request_delay
        self._last_request_time = 0
    
    def _rate_limit(self):
        """频率控制"""
        elapsed = time.time() - self._last_request_time
        if elapsed < self.delay:
            time.sleep(self.delay - elapsed)
        self._last_request_time = time.time()
    
    def _safe_call(self, func: Callable, *args, **kwargs) -> Optional[pd.DataFrame]:
        """带频率控制的请求"""
        self._rate_limit()
        return func(*args, **kwargs)
    
    @retry_on_error()
    def get_fund_list(self) -> pd.DataFrame:
        """
        获取全量基金列表
        
        Returns:
            DataFrame包含: fund_code, fund_name, fund_type
        """
        df = self._safe_call(ak.fund_name_em)
        if df is None or df.empty:
            logger.error("获取基金列表返回空数据")
            return pd.DataFrame()
        
        # 列名映射
        column_mapping = {
            '基金代码': 'fund_code',
            '基金简称': 'fund_name',
            '基金类型': 'fund_type',
        }
        
        # 只保留存在的列
        available_cols = {k: v for k, v in column_mapping.items() if k in df.columns}
        df = df.rename(columns=available_cols)
        
        # 确保必要字段
        if 'fund_code' not in df.columns:
            raise ValueError("获取基金列表缺少fund_code字段")
        
        # 数据清洗
        df['fund_code'] = df['fund_code'].astype(str).str.strip()
        if 'fund_name' in df.columns:
            df['fund_name'] = df['fund_name'].astype(str).str.strip()
        
        return df[list(available_cols.values())]
    
    @retry_on_error()
    def get_fund_basic(self, fund_code: str) -> Optional[dict]:
        """
        获取基金基础信息
        
        Args:
            fund_code: 基金代码
            
        Returns:
            基金信息字典，失败返回None
        """
        try:
            df = self._safe_call(ak.fund_individual_basic_info_xq, symbol=fund_code)
            if df is None or df.empty:
                logger.warning(f"基金{fund_code}基础信息返回空数据")
                return None
            
            # 解析为字典
            info = {'fund_code': fund_code}
            
            for _, row in df.iterrows():
                item = str(row.get('item', ''))
                value = row.get('value')
                
                if not item or value is None:
                    continue
                
                try:
                    if '基金全称' in item:
                        info['fund_name'] = str(value)
                    elif '基金类型' in item:
                        info['fund_type'] = str(value)
                    elif '基金公司' in item:
                        info['company_name'] = str(value)
                    elif '基金经理' in item:
                        info['manager_name'] = str(value)
                    elif '基金规模' in item:
                        scale_str = str(value).replace('亿元', '').replace('亿', '').strip()
                        info['current_scale'] = float(scale_str) if scale_str else None
                    elif '成立日期' in item:
                        info['establish_date'] = str(value)
                except (ValueError, TypeError) as e:
                    logger.debug(f"解析字段失败[{item}]: {e}")
                    continue
            
            return info if len(info) > 1 else None
            
        except Exception as e:
            logger.error(f"获取基金{fund_code}基础信息失败: {e}")
            return None
    
    @retry_on_error()
    def get_daily_nav(self, date: Optional[str] = None) -> pd.DataFrame:
        """
        获取每日净值
        
        Args:
            date: 日期，格式YYYY-MM-DD，默认当天
            
        Returns:
            DataFrame包含净值数据
        """
        # 使用开放式基金每日数据
        df = self._safe_call(ak.fund_open_fund_daily_em)
        
        if df is None or df.empty:
            logger.error("获取净值数据返回空")
            return pd.DataFrame()
        
        logger.debug(f"净值接口返回列: {df.columns.tolist()}")
        
        # 适配动态日期列名，如 '2026-02-27-单位净值'
        import re
        
        fund_code_col = None
        unit_nav_col = None
        accum_nav_col = None
        daily_return_col = None
        
        for col in df.columns:
            if col == '基金代码':
                fund_code_col = col
            elif '单位净值' in col and unit_nav_col is None:
                # 取第一个包含单位净值的列（通常是最新的）
                unit_nav_col = col
            elif '累计净值' in col and accum_nav_col is None:
                accum_nav_col = col
            elif col == '日增长率':
                daily_return_col = col
        
        if not fund_code_col or not unit_nav_col:
            logger.error(f"缺少必要列，fund_code={fund_code_col}, unit_nav={unit_nav_col}")
            return pd.DataFrame()
        
        # 提取日期（从列名中）
        if date is None:
            match = re.search(r'(\d{4}-\d{2}-\d{2})', unit_nav_col)
            date = match.group(1) if match else datetime.now().strftime('%Y-%m-%d')
        
        # 重命名列
        result_df = pd.DataFrame()
        result_df['fund_code'] = df[fund_code_col].astype(str).str.strip()
        result_df['unit_nav'] = pd.to_numeric(df[unit_nav_col], errors='coerce')
        
        if accum_nav_col:
            result_df['accum_nav'] = pd.to_numeric(df[accum_nav_col], errors='coerce')
        else:
            result_df['accum_nav'] = None
            
        if daily_return_col:
            result_df['daily_return'] = pd.to_numeric(
                df[daily_return_col].astype(str).str.replace('%', ''),
                errors='coerce'
            )
        else:
            result_df['daily_return'] = None
        
        result_df['nav_date'] = date
        result_df['source'] = 'akshare'
        
        # 过滤无效数据
        result_df = result_df.dropna(subset=['fund_code', 'unit_nav'])
        
        logger.info(f"成功解析净值数据: {len(result_df)}条")
        return result_df
    
    @retry_on_error()
    def get_fund_portfolio(self, fund_code: str, year: int, quarter: int) -> pd.DataFrame:
        """
        获取基金持仓（前十大重仓股）
        
        Args:
            fund_code: 基金代码
            year: 年份
            quarter: 季度 (1-4)
            
        Returns:
            DataFrame包含持仓数据
        """
        try:
            # akshare接口参数是date(年份字符串)
            df = self._safe_call(
                ak.fund_portfolio_hold_em,
                symbol=fund_code,
                date=str(year)
            )
            
            if df is None or df.empty:
                return pd.DataFrame()
            
            # 季度筛选
            quarter_str = f"{year}年{quarter}季度"
            df = df[df['季度'].str.contains(quarter_str, na=False)]
            
            if df.empty:
                logger.warning(f"基金{fund_code}没有找到{year}Q{quarter}的持仓数据")
                return pd.DataFrame()
            
            # 列名映射
            column_mapping = {
                '股票代码': 'stock_code',
                '股票名称': 'stock_name',
                '占净值比例': 'holding_ratio',
                '持股数': 'holding_amount',
                '持仓市值': 'holding_value',
            }
            
            available_cols = {k: v for k, v in column_mapping.items() if k in df.columns}
            df = df.rename(columns=available_cols)
            
            # 添加基金代码和报告期
            df['fund_code'] = fund_code
            quarter_end_dates = {1: '03-31', 2: '06-30', 3: '09-30', 4: '12-31'}
            df['report_date'] = f"{year}-{quarter_end_dates.get(quarter, '12-31')}"
            df['holding_type'] = '股票'
            df['is_top10'] = 1
            
            # 只保留前10
            df = df.head(10)
            
            # 数据类型转换
            if 'holding_amount' in df.columns:
                df['holding_amount'] = pd.to_numeric(df['holding_amount'], errors='coerce')
                df['holding_amount'] = df['holding_amount'].fillna(0).astype(int)
            if 'holding_value' in df.columns:
                df['holding_value'] = pd.to_numeric(df['holding_value'], errors='coerce')
            if 'holding_ratio' in df.columns:
                df['holding_ratio'] = pd.to_numeric(df['holding_ratio'], errors='coerce')
            
            return df[['fund_code', 'report_date', 'stock_code', 'stock_name',
                      'holding_amount', 'holding_value', 'holding_ratio',
                      'holding_type', 'is_top10']]
            
        except Exception as e:
            logger.error(f"获取持仓失败[{fund_code} {year}Q{quarter}]: {e}")
            return pd.DataFrame()
