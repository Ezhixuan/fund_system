"""
基金采集器主类
整合各类数据采集功能
"""
import logging
from datetime import datetime, date
from typing import Optional, List

import pandas as pd
from sqlalchemy import text

from config import settings
from utils.database import db
from core.akshare_client import AkShareClient

logger = logging.getLogger(__name__)


class FundCollector:
    """基金采集器"""
    
    def __init__(self):
        self.client = AkShareClient()
        self.engine = db.get_engine()
    
    def collect_fund_list(self) -> int:
        """
        采集基金列表
        
        Returns:
            导入的记录数
        """
        logger.info("开始采集基金列表...")
        
        df = self.client.get_fund_list()
        if df.empty:
            logger.error("获取基金列表失败")
            return 0
        
        # 使用INSERT IGNORE避免重复
        success_count = 0
        with db.get_connection() as conn:
            for _, row in df.iterrows():
                try:
                    sql = text("""
                        INSERT IGNORE INTO fund_info (fund_code, fund_name, fund_type, status)
                        VALUES (:fund_code, :fund_name, :fund_type, 1)
                    """)
                    conn.execute(sql, {
                        'fund_code': row.get('fund_code'),
                        'fund_name': row.get('fund_name'),
                        'fund_type': row.get('fund_type')
                    })
                    success_count += 1
                except Exception as e:
                    logger.warning(f"插入基金记录失败: {e}")
        
        logger.info(f"基金列表采集完成: {success_count}/{len(df)}条")
        self._log_update('fund_info', date.today(), success_count, 'SUCCESS')
        return success_count
    
    def update_fund_basic(self, fund_codes: List[str] = None, limit: int = 100) -> int:
        """
        更新基金基础信息
        
        Args:
            fund_codes: 指定基金代码列表，None则自动获取待更新的
            limit: 自动获取时的数量限制
            
        Returns:
            更新的记录数
        """
        if fund_codes is None:
            # 获取需要更新的基金（经理名称为空的）
            fund_codes = db.fetch_all(
                "SELECT fund_code FROM fund_info WHERE manager_name IS NULL LIMIT :limit",
                {'limit': limit}
            )
            fund_codes = [row['fund_code'] for row in fund_codes]
        
        if not fund_codes:
            logger.info("没有需要更新的基金")
            return 0
        
        logger.info(f"开始更新{len(fund_codes)}只基金的基础信息...")
        
        updated = 0
        failed = 0
        
        for code in fund_codes:
            try:
                info = self.client.get_fund_basic(code)
                if info:
                    self._update_fund_info(code, info)
                    updated += 1
                else:
                    failed += 1
            except Exception as e:
                logger.error(f"更新基金{code}失败: {e}")
                failed += 1
        
        logger.info(f"基础信息更新完成: 成功{updated}, 失败{failed}")
        self._log_update('fund_info_update', date.today(), updated, 'SUCCESS')
        return updated
    
    def _update_fund_info(self, fund_code: str, info: dict):
        """更新单只基金信息"""
        # 构建动态SQL，只更新有值的字段
        valid_fields = []
        params = {'fund_code': fund_code}
        
        field_mapping = {
            'fund_name': 'fund_name',
            'fund_type': 'fund_type',
            'company_name': 'company_name',
            'manager_name': 'manager_name',
            'current_scale': 'current_scale',
            'establish_date': 'establish_date'
        }
        
        for src_field, db_field in field_mapping.items():
            if src_field in info and info[src_field] is not None:
                valid_fields.append(f"{db_field} = :{db_field}")
                params[db_field] = info[src_field]
        
        if not valid_fields:
            logger.warning(f"基金{fund_code}没有有效字段需要更新")
            return
        
        sql = text(f"""
            UPDATE fund_info SET
                {', '.join(valid_fields)}
            WHERE fund_code = :fund_code
        """)
        
        with db.get_connection() as conn:
            conn.execute(sql, params)
    
    def collect_daily_nav(self, nav_date: Optional[str] = None) -> int:
        """
        采集每日净值到临时表
        流程：采集 -> 临时表 -> 校验 -> 正式表
        
        Args:
            nav_date: 净值日期，默认当天
            
        Returns:
            采集的记录数
        """
        nav_date = nav_date or datetime.now().strftime('%Y-%m-%d')
        logger.info(f"开始采集{nav_date}的净值数据...")
        
        # 1. 采集数据
        df = self.client.get_daily_nav(nav_date)
        if df.empty:
            logger.error("采集净值数据失败")
            self._log_update('fund_nav', nav_date, 0, 'FAILURE', '采集返回空数据')
            return 0
        
        # 2. 清空临时表
        with db.get_connection() as conn:
            conn.execute(text("TRUNCATE TABLE tmp_fund_nav"))
        
        # 3. 写入临时表
        df.to_sql('tmp_fund_nav', self.engine, if_exists='append', index=False)
        logger.info(f"净值数据写入临时表：{len(df)}条")
        
        # 4. 数据校验和迁移到正式表
        migrated = self._migrate_nav_to_production(nav_date)
        
        self._log_update('fund_nav', nav_date, migrated, 'SUCCESS')
        return migrated
    
    def _migrate_nav_to_production(self, nav_date: str) -> int:
        """
        将临时表数据迁移到正式表
        使用INSERT IGNORE处理重复
        """
        with db.get_connection() as conn:
            # 校验：检查异常值
            result = conn.execute(text("""
                SELECT COUNT(*) as abnormal_count 
                FROM tmp_fund_nav 
                WHERE unit_nav <= 0 OR unit_nav > 1000
            """))
            abnormal = result.fetchone()[0]
            if abnormal > 0:
                logger.warning(f"发现{abnormal}条异常净值数据")
            
            # 标记校验状态
            conn.execute(text("""
                UPDATE tmp_fund_nav 
                SET check_status = CASE 
                    WHEN unit_nav > 0 AND unit_nav <= 1000 THEN 1
                    ELSE 2
                END,
                check_msg = CASE 
                    WHEN unit_nav > 0 AND unit_nav <= 1000 THEN '通过'
                    ELSE '异常值'
                END
            """))
            
            # 迁移到正式表
            result = conn.execute(text("""
                INSERT IGNORE INTO fund_nav 
                (fund_code, nav_date, unit_nav, accum_nav, daily_return, source)
                SELECT fund_code, nav_date, unit_nav, accum_nav, daily_return, source
                FROM tmp_fund_nav
                WHERE check_status = 1
            """))
            
            migrated = result.rowcount
            logger.info(f"迁移到正式表：{migrated}条")
            return migrated
    
    def collect_portfolio(self, fund_codes: List[str], year: int = None, quarter: int = None) -> int:
        """
        采集持仓数据
        
        Args:
            fund_codes: 基金代码列表
            year: 年份，默认当前年
            quarter: 季度(1-4)，默认当前季度
            
        Returns:
            采集的记录数
        """
        if not fund_codes:
            logger.warning("基金代码列表为空")
            return 0
        
        # 默认当前季度
        if year is None or quarter is None:
            now = datetime.now()
            year = year or now.year
            quarter = quarter or (now.month - 1) // 3 + 1
        
        quarter_end_dates = {1: '03-31', 2: '06-30', 3: '09-30', 4: '12-31'}
        report_date = f"{year}-{quarter_end_dates.get(quarter, '12-31')}"
        
        logger.info(f"开始采集{year}Q{quarter}持仓数据，共{len(fund_codes)}只基金...")
        
        all_data = []
        
        for code in fund_codes:
            try:
                df = self.client.get_fund_portfolio(code, year, quarter)
                if not df.empty:
                    all_data.append(df)
            except Exception as e:
                logger.error(f"采集持仓失败[{code}]: {e}")
        
        if not all_data:
            logger.warning("未采集到任何持仓数据")
            return 0
        
        combined = pd.concat(all_data, ignore_index=True)
        
        # 删除旧数据（同一报告期）
        with db.get_connection() as conn:
            conn.execute(
                text("DELETE FROM fund_holding WHERE report_date = :report_date"),
                {'report_date': report_date}
            )
        
        # 写入新数据
        combined.to_sql('fund_holding', self.engine, if_exists='append', index=False)
        
        logger.info(f"持仓数据入库：{len(combined)}条")
        self._log_update('fund_holding', report_date, len(combined), 'SUCCESS')
        return len(combined)
    
    def _log_update(self, table_name: str, update_date, record_count: int, 
                    status: str, error_msg: str = None):
        """记录数据更新日志"""
        try:
            with db.get_connection() as conn:
                sql = text("""
                    INSERT INTO data_update_log 
                    (table_name, update_date, record_count, status, error_msg, created_at)
                    VALUES (:table_name, :update_date, :record_count, :status, :error_msg, NOW())
                """)
                conn.execute(sql, {
                    'table_name': table_name,
                    'update_date': update_date,
                    'record_count': record_count,
                    'status': status,
                    'error_msg': error_msg
                })
        except Exception as e:
            logger.warning(f"记录日志失败: {e}")
