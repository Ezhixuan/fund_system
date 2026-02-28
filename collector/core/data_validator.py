"""
数据校验器核心模块
"""
import pandas as pd
import numpy as np
from typing import Dict, List, Tuple, Callable
from dataclasses import dataclass, field
import logging

logger = logging.getLogger(__name__)


@dataclass
class ValidationRule:
    """校验规则定义"""
    name: str
    check_func: Callable
    error_msg: str
    severity: str = 'error'  # error/warning


@dataclass
class ValidationResult:
    """校验结果"""
    is_valid: bool
    passed_rules: List[str] = field(default_factory=list)
    failed_rules: List[Tuple[str, str]] = field(default_factory=list)  # (规则名, 错误信息)
    stats: Dict = field(default_factory=dict)
    failed_indices: List[int] = field(default_factory=list)  # 失败记录索引


class DataValidator:
    """数据校验器"""
    
    def __init__(self):
        self.rules: List[ValidationRule] = []
        self._rule_map: Dict[str, ValidationRule] = {}
    
    def add_rule(self, rule: ValidationRule):
        """添加校验规则"""
        self.rules.append(rule)
        self._rule_map[rule.name] = rule
        logger.debug(f"添加校验规则: {rule.name}")
    
    def validate(self, df: pd.DataFrame) -> ValidationResult:
        """
        执行所有校验规则
        
        Args:
            df: 待校验的DataFrame
            
        Returns:
            ValidationResult: 校验结果
        """
        if df.empty:
            logger.warning("校验的数据集为空")
            return ValidationResult(
                is_valid=False,
                failed_rules=[('empty_data', '数据集为空')]
            )
        
        passed = []
        failed = []
        failed_indices = set()
        
        for rule in self.rules:
            try:
                is_pass, msg, indices = self._execute_rule(rule, df)
                if is_pass:
                    passed.append(rule.name)
                    logger.debug(f"规则通过: {rule.name}")
                else:
                    failed.append((rule.name, msg or rule.error_msg))
                    if indices:
                        failed_indices.update(indices)
                    logger.warning(f"规则失败: {rule.name} - {msg}")
            except Exception as e:
                failed.append((rule.name, f"校验异常: {e}"))
                logger.error(f"规则执行异常: {rule.name} - {e}")
        
        # 生成统计信息
        stats = {
            'total_records': len(df),
            'null_counts': df.isnull().sum().to_dict(),
            'duplicated': df.duplicated().sum(),
            'failed_records': len(failed_indices)
        }
        
        # 只有error级别失败才算失败
        critical_failed = [
            f for f in failed 
            if self._get_rule_severity(f[0]) == 'error'
        ]
        
        return ValidationResult(
            is_valid=len(critical_failed) == 0,
            passed_rules=passed,
            failed_rules=failed,
            stats=stats,
            failed_indices=sorted(list(failed_indices))
        )
    
    def _execute_rule(self, rule: ValidationRule, df: pd.DataFrame) -> Tuple[bool, str, List[int]]:
        """执行单个规则"""
        result = rule.check_func(df)
        
        # 支持多种返回格式
        if isinstance(result, bool):
            return result, None, []
        elif isinstance(result, tuple) and len(result) == 2:
            return result[0], result[1], []
        elif isinstance(result, tuple) and len(result) == 3:
            return result
        else:
            return bool(result), None, []
    
    def _get_rule_severity(self, rule_name: str) -> str:
        """获取规则严重程度"""
        rule = self._rule_map.get(rule_name)
        return rule.severity if rule else 'error'
    
    def clear_rules(self):
        """清空所有规则"""
        self.rules = []
        self._rule_map = {}
        logger.debug("清空所有校验规则")
