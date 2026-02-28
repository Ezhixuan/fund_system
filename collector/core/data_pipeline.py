"""
数据管道模块
流程: 临时表 -> 校验 -> 更新状态 -> 合并到正式表
"""
import pandas as pd
from datetime import datetime
from typing import Optional
import logging

from sqlalchemy import text
from utils.database import db
from core.data_validator import DataValidator, ValidationResult
from validators import create_nav_validators

logger = logging.getLogger(__name__)


class DataPipeline:
    """
    数据管道：临时表数据校验并合并到正式表
    """
    
    def __init__(self):
        self.engine = db.get_engine()
        self.validator = DataValidator()
    
    def process_nav_data(self) -> ValidationResult:
        """
        处理净值数据
        流程：读取临时表 -> 校验 -> 更新状态 -> 合并到正式表
        
        Returns:
            ValidationResult: 校验结果
        """
        logger.info("开始处理净值数据管道...")
        
        try:
            # 1. 从临时表读取待处理数据
            df = self._read_temp_data()
            
            if df.empty:
                logger.info("临时表无待处理数据")
                return ValidationResult(is_valid=True, passed_rules=['no_data'])
            
            logger.info(f"从临时表读取 {len(df)} 条待处理数据")
            
            # 2. 配置并执行校验规则
            self._setup_nav_validators()
            result = self.validator.validate(df)
            
            logger.info(f"校验完成: 通过{len(result.passed_rules)}项, 失败{len(result.failed_rules)}项")
            if result.failed_rules:
                for name, msg in result.failed_rules:
                    logger.warning(f"  - {name}: {msg}")
            
            # 3. 更新临时表校验状态
            self._update_temp_status(df, result)
            
            # 4. 合并到正式表
            if result.is_valid:
                # 全部通过
                merged_count = self._merge_to_production(df)
                self._log_update('fund_nav', merged_count, 'SUCCESS')
                logger.info(f"数据全部通过校验，合并 {merged_count} 条到正式表")
            else:
                # 部分通过：只合并通过的数据
                valid_df = self._get_valid_records(df, result)
                if not valid_df.empty:
                    merged_count = self._merge_to_production(valid_df)
                    self._log_update('fund_nav', merged_count, 'PARTIAL', 
                                   str(result.failed_rules))
                    logger.info(f"部分数据通过校验，合并 {merged_count} 条到正式表")
                    
                    # 记录失败的数据
                    failed_count = len(df) - len(valid_df)
                    logger.warning(f"{failed_count} 条数据未通过校验")
                else:
                    self._log_update('fund_nav', 0, 'FAILURE', str(result.failed_rules))
                    logger.error("所有数据均未通过校验")
            
            return result
            
        except Exception as e:
            logger.error(f"数据管道处理异常: {e}", exc_info=True)
            self._log_update('fund_nav', 0, 'FAILURE', str(e))
            raise
    
    def _read_temp_data(self) -> pd.DataFrame:
        """从临时表读取待处理数据"""
        sql = """
            SELECT * FROM tmp_fund_nav 
            WHERE check_status = 0 
            OR check_status IS NULL
        """
        return pd.read_sql(sql, self.engine)
    
    def _setup_nav_validators(self):
        """配置净值校验规则"""
        self.validator.clear_rules()
        rules = create_nav_validators()
        for rule in rules:
            self.validator.add_rule(rule)
        logger.debug(f"已配置 {len(rules)} 条校验规则")
    
    def _update_temp_status(self, df: pd.DataFrame, result: ValidationResult):
        """更新临时表校验状态"""
        if df.empty or 'id' not in df.columns:
            return
        
        with db.get_connection() as conn:
            # 通过的记录
            if result.passed_rules and not result.failed_indices:
                # 全部通过
                ids = tuple(df['id'].tolist())
                if len(ids) == 1:
                    ids = f"({ids[0]})"
                conn.execute(text("""
                    UPDATE tmp_fund_nav 
                    SET check_status = 1, check_msg = '校验通过'
                    WHERE id IN :ids
                """), {'ids': ids})
            
            # 失败的记录单独标记
            if result.failed_indices:
                failed_ids = [int(df.iloc[i]['id']) for i in result.failed_indices if i < len(df)]
                if failed_ids:
                    # 批量更新失败状态
                    for idx in result.failed_indices:
                        if idx < len(df):
                            record_id = int(df.iloc[idx]['id'])
                            conn.execute(text("""
                                UPDATE tmp_fund_nav 
                                SET check_status = 2, check_msg = '校验失败'
                                WHERE id = :id
                            """), {'id': record_id})
            
            conn.commit()
        
        logger.debug(f"临时表状态已更新: 失败{len(result.failed_indices)}条")
    
    def _get_valid_records(self, df: pd.DataFrame, result: ValidationResult) -> pd.DataFrame:
        """获取通过校验的记录"""
        if not result.failed_indices:
            return df
        
        # 过滤掉失败的记录
        valid_mask = ~df.index.isin(result.failed_indices)
        return df[valid_mask].copy()
    
    def _merge_to_production(self, df: pd.DataFrame) -> int:
        """
        合并到正式表
        使用 INSERT IGNORE 避免重复
        """
        if df.empty:
            return 0
        
        # 选择需要的列
        required_cols = ['fund_code', 'nav_date', 'unit_nav', 'accum_nav', 'daily_return', 'source']
        available_cols = [c for c in required_cols if c in df.columns]
        
        if 'fund_code' not in available_cols or 'nav_date' not in available_cols:
            logger.error("缺少必要的列(fund_code/nav_date)")
            return 0
        
        records = df[available_cols].to_dict('records')
        
        merged_count = 0
        with db.get_connection() as conn:
            for record in records:
                try:
                    conn.execute(text("""
                        INSERT IGNORE INTO fund_nav 
                        (fund_code, nav_date, unit_nav, accum_nav, daily_return, source, created_at)
                        VALUES 
                        (:fund_code, :nav_date, :unit_nav, :accum_nav, :daily_return, :source, NOW())
                    """), record)
                    merged_count += 1
                except Exception as e:
                    logger.warning(f"插入记录失败 {record.get('fund_code')}: {e}")
            
            conn.commit()
        
        return merged_count
    
    def _log_update(self, table_name: str, record_count: int, status: str, error_msg: str = None):
        """记录更新日志"""
        try:
            with db.get_connection() as conn:
                conn.execute(text("""
                    INSERT INTO data_update_log 
                    (table_name, update_date, record_count, status, error_msg, start_time, end_time, duration_seconds)
                    VALUES (:table_name, :update_date, :record_count, :status, :error_msg, NOW(), NOW(), 0)
                """), {
                    'table_name': table_name,
                    'update_date': datetime.now().date(),
                    'record_count': record_count,
                    'status': status,
                    'error_msg': error_msg[:500] if error_msg else None
                })
                conn.commit()
        except Exception as e:
            logger.warning(f"记录日志失败: {e}")
    
    def get_temp_stats(self) -> dict:
        """获取临时表统计信息"""
        with db.get_connection() as conn:
            result = conn.execute(text("""
                SELECT 
                    check_status,
                    COUNT(*) as count
                FROM tmp_fund_nav
                GROUP BY check_status
            """))
            rows = result.fetchall()
            
        stats = {'pending': 0, 'passed': 0, 'failed': 0}
        for row in rows:
            status = row[0]
            count = row[1]
            if status == 0 or status is None:
                stats['pending'] = count
            elif status == 1:
                stats['passed'] = count
            elif status == 2:
                stats['failed'] = count
        
        return stats
