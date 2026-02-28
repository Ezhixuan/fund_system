"""
净值数据校验规则
6条规则：净值范围、涨跌幅范围、必填字段、重复数据、代码格式、时效性
"""
import pandas as pd
import numpy as np
import re
from datetime import datetime, timedelta
from typing import List, Tuple

from core.data_validator import ValidationRule


def create_nav_validators() -> List[ValidationRule]:
    """创建净值数据校验规则列表"""
    
    # 规则1：净值范围检查
    def check_nav_range(df: pd.DataFrame) -> Tuple[bool, str, List[int]]:
        """检查净值是否在合理范围(0, 1000]"""
        if 'unit_nav' not in df.columns:
            return True, None, []
        
        invalid_mask = (df['unit_nav'] <= 0) | (df['unit_nav'] > 1000)
        invalid = df[invalid_mask]
        
        if len(invalid) > 0:
            indices = invalid.index.tolist()
            samples = invalid[['fund_code', 'unit_nav']].head(3).to_dict('records')
            return False, f"{len(invalid)}条记录净值超出正常范围(0,1000]: {samples}", indices
        return True, None, []
    
    # 规则2：日涨跌幅范围检查
    def check_return_range(df: pd.DataFrame) -> Tuple[bool, str, List[int]]:
        """检查日涨跌幅是否在合理范围(-20%, +20%)"""
        if 'daily_return' not in df.columns:
            return True, None, []
        
        # 过滤空值
        valid_returns = df['daily_return'].dropna()
        if len(valid_returns) == 0:
            return True, None, []
        
        invalid_mask = (df['daily_return'] < -20) | (df['daily_return'] > 20)
        invalid = df[invalid_mask]
        
        if len(invalid) > 0:
            indices = invalid.index.tolist()
            samples = invalid[['fund_code', 'daily_return']].head(3).to_dict('records')
            return False, f"{len(invalid)}条记录涨跌幅超出±20%: {samples}", indices
        return True, None, []
    
    # 规则3：必填字段检查
    def check_required_fields(df: pd.DataFrame) -> Tuple[bool, str, List[int]]:
        """检查必填字段是否缺失"""
        required = ['fund_code', 'nav_date', 'unit_nav']
        missing_fields = []
        
        for field in required:
            if field not in df.columns:
                missing_fields.append(f"缺少字段:{field}")
            else:
                null_count = df[field].isnull().sum()
                if null_count > 0:
                    missing_fields.append(f"{field}有{null_count}条空值")
        
        if missing_fields:
            return False, "; ".join(missing_fields), []
        return True, None, []
    
    # 规则4：重复数据检查
    def check_duplicates(df: pd.DataFrame) -> Tuple[bool, str, List[int]]:
        """检查是否存在重复数据(fund_code + nav_date)"""
        if 'fund_code' not in df.columns or 'nav_date' not in df.columns:
            return True, None, []
        
        dup_mask = df.duplicated(['fund_code', 'nav_date'], keep=False)
        dups = df[dup_mask]
        
        if len(dups) > 0:
            indices = dups.index.tolist()
            groups = dups.groupby(['fund_code', 'nav_date']).size()
            return False, f"{len(groups)}组重复数据(fund_code+nav_date): {groups.head(3).to_dict()}", indices
        return True, None, []
    
    # 规则5：基金代码格式检查
    def check_fund_code_format(df: pd.DataFrame) -> Tuple[bool, str, List[int]]:
        """检查基金代码格式(6位数字)"""
        if 'fund_code' not in df.columns:
            return True, None, []
        
        # 6位数字格式
        pattern = r'^\d{6}$'
        invalid_mask = ~df['fund_code'].astype(str).str.match(pattern, na=False)
        invalid = df[invalid_mask]
        
        if len(invalid) > 0:
            indices = invalid.index.tolist()
            samples = invalid['fund_code'].head(5).tolist()
            return False, f"{len(invalid)}条记录基金代码格式不正确(应为6位数字): {samples}", indices
        return True, None, []
    
    # 规则6：数据时效性检查（警告级别）
    def check_data_freshness(df: pd.DataFrame) -> Tuple[bool, str, List[int]]:
        """检查数据时效性（超过3天警告）"""
        if 'nav_date' not in df.columns:
            return True, None, []
        
        try:
            latest_date = pd.to_datetime(df['nav_date']).max()
            today = datetime.now()
            days_diff = (today - latest_date).days
            
            if days_diff > 3:
                return False, f"数据可能滞后{days_diff}天，最新数据日期:{latest_date.strftime('%Y-%m-%d')}", []
            return True, None, []
        except Exception as e:
            return True, f"时效性检查异常:{e}", []
    
    return [
        ValidationRule('nav_range', check_nav_range, '净值超出正常范围(0,1000]', 'error'),
        ValidationRule('return_range', check_return_range, '日涨跌幅超出±20%', 'error'),
        ValidationRule('required_fields', check_required_fields, '必填字段缺失', 'error'),
        ValidationRule('duplicates', check_duplicates, '存在重复数据', 'error'),
        ValidationRule('fund_code_format', check_fund_code_format, '基金代码格式错误(应为6位数字)', 'error'),
        ValidationRule('data_freshness', check_data_freshness, '数据时效性警告(超过3天)', 'warning'),
    ]
