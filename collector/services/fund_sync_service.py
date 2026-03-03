#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金基础数据同步服务
用于比对和同步 MySQL 与 akshare 数据源之间的基金基础数据
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pymysql
import pandas as pd
from datetime import datetime
from typing import Dict
from loguru import logger

from core.akshare_client import AkShareClient
from config import settings


# 数据库配置（从统一配置读取）
DB_CONFIG = settings.get_db_config()


class FundSyncService:
    """基金数据同步服务"""

    def __init__(self):
        self.conn = None
        self.ak_client = AkShareClient()

    def get_connection(self):
        """获取数据库连接"""
        if self.conn is None or not self.conn.open:
            self.conn = pymysql.connect(**DB_CONFIG)
        return self.conn

    def close(self):
        """关闭连接"""
        if self.conn and self.conn.open:
            self.conn.close()

    def get_mysql_fund_count(self) -> int:
        """
        获取 MySQL 中基金基础数据条数

        Returns:
            基金数量
        """
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM fund_info WHERE status = 1")
            count = cursor.fetchone()[0]
            cursor.close()
            logger.info(f"MySQL 基金数量: {count}")
            return count
        except Exception as e:
            logger.error(f"获取 MySQL 基金数量失败: {e}")
            return 0

    def get_mysql_fund_codes(self) -> set:
        """
        获取 MySQL 中所有基金代码

        Returns:
            基金代码集合
        """
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT fund_code FROM fund_info WHERE status = 1")
            codes = {row[0] for row in cursor.fetchall()}
            cursor.close()
            logger.info(f"MySQL 基金代码数量: {len(codes)}")
            return codes
        except Exception as e:
            logger.error(f"获取 MySQL 基金代码失败: {e}")
            return set()

    def get_akshare_fund_list(self) -> pd.DataFrame:
        """
        获取 akshare 所有基金列表

        Returns:
            DataFrame 包含基金代码、名称、类型
        """
        try:
            df = self.ak_client.get_fund_list()
            logger.info(f"akshare 基金数量: {len(df)}")
            return df
        except Exception as e:
            logger.error(f"获取 akshare 基金列表失败: {e}")
            return pd.DataFrame()

    def compare_fund_data(self) -> Dict:
        """
        比对 MySQL 和 akshare 的基金数据

        Returns:
            比对结果字典
        """
        logger.info("=" * 60)
        logger.info("开始比对基金数据")
        logger.info("=" * 60)

        result = {
            'mysql_count': 0,
            'akshare_count': 0,
            'missing_in_mysql': [],
            'need_update': False,
            'sync_needed': False
        }

        try:
            # 获取 MySQL 数据
            mysql_count = self.get_mysql_fund_count()
            mysql_codes = self.get_mysql_fund_codes()
            result['mysql_count'] = mysql_count

            # 获取 akshare 数据
            ak_df = self.get_akshare_fund_list()
            ak_count = len(ak_df)
            result['akshare_count'] = ak_count

            if ak_df.empty:
                logger.warning("akshare 返回空数据，无法比对")
                return result

            ak_codes = set(ak_df['fund_code'].tolist())

            # 找出 MySQL 中缺失的基金
            missing_codes = ak_codes - mysql_codes
            result['missing_in_mysql'] = list(missing_codes)

            # 判断是否需要同步
            if mysql_count < ak_count * 0.95:  # 如果数量少于95%，需要同步
                result['need_update'] = True
                result['sync_needed'] = True
                logger.warning(f"数据不一致: MySQL({mysql_count}) vs akshare({ak_count}), 缺失 {len(missing_codes)} 只基金")
            else:
                logger.info(f"数据基本一致: MySQL({mysql_count}) vs akshare({ak_count})")

            return result

        except Exception as e:
            logger.error(f"比对基金数据失败: {e}")
            return result

    def sync_fund_basic_data(self, batch_size: int = 100) -> Dict:
        """
        同步基金基础数据到 MySQL

        Args:
            batch_size: 每批处理的基金数量

        Returns:
            同步结果
        """
        logger.info("=" * 60)
        logger.info("开始同步基金基础数据")
        logger.info("=" * 60)

        result = {
            'success': False,
            'total_synced': 0,
            'failed_codes': [],
            'start_time': datetime.now().isoformat(),
            'end_time': None
        }

        try:
            # 比对数据
            compare_result = self.compare_fund_data()
            missing_codes = compare_result.get('missing_in_mysql', [])

            if not missing_codes:
                logger.info("没有需要同步的基金数据")
                result['success'] = True
                result['end_time'] = datetime.now().isoformat()
                return result

            # 获取 akshare 完整数据
            ak_df = self.get_akshare_fund_list()
            missing_df = ak_df[ak_df['fund_code'].isin(missing_codes)]

            logger.info(f"需要同步 {len(missing_df)} 只基金")

            # 批量插入
            conn = self.get_connection()
            cursor = conn.cursor()

            insert_sql = """
                INSERT INTO fund_info
                (fund_code, fund_name, fund_type, status, create_time, update_time)
                VALUES (%s, %s, %s, 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    fund_name = VALUES(fund_name),
                    fund_type = VALUES(fund_type),
                    status = 1,
                    update_time = NOW()
            """

            synced_count = 0
            failed_codes = []

            for _, row in missing_df.iterrows():
                try:
                    fund_code = row['fund_code']
                    fund_name = row.get('fund_name', '')
                    fund_type = row.get('fund_type', '')

                    cursor.execute(insert_sql, (fund_code, fund_name, fund_type))
                    synced_count += 1

                    if synced_count % batch_size == 0:
                        conn.commit()
                        logger.info(f"已同步 {synced_count}/{len(missing_df)} 只基金")

                except Exception as e:
                    logger.error(f"同步基金 {fund_code} 失败: {e}")
                    failed_codes.append(fund_code)

            # 提交剩余数据
            conn.commit()
            cursor.close()

            result['success'] = True
            result['total_synced'] = synced_count
            result['failed_codes'] = failed_codes
            result['end_time'] = datetime.now().isoformat()

            logger.info(f"✅ 同步完成: 成功 {synced_count} 只, 失败 {len(failed_codes)} 只")

        except Exception as e:
            logger.error(f"同步基金基础数据失败: {e}")
            result['end_time'] = datetime.now().isoformat()

        return result

    def get_sync_status(self) -> Dict:
        """
        获取同步状态

        Returns:
            同步状态信息
        """
        try:
            mysql_count = self.get_mysql_fund_count()
            ak_df = self.get_akshare_fund_list()
            ak_count = len(ak_df)

            return {
                'mysql_count': mysql_count,
                'akshare_count': ak_count,
                'sync_rate': round(mysql_count / ak_count * 100, 2) if ak_count > 0 else 0,
                'is_synced': mysql_count >= ak_count * 0.95,
                'check_time': datetime.now().isoformat()
            }
        except Exception as e:
            logger.error(f"获取同步状态失败: {e}")
            return {
                'error': str(e),
                'check_time': datetime.now().isoformat()
            }


def run_startup_sync():
    """
    启动时执行同步检查
    在独立线程中运行，不阻塞主服务启动
    """
    import threading

    def _sync_task():
        logger.info("启动后台同步检查任务...")
        try:
            service = FundSyncService()

            # 先比对数据
            compare_result = service.compare_fund_data()

            # 如果需要同步，执行同步
            if compare_result.get('sync_needed'):
                logger.info("检测到数据不一致，开始自动同步...")
                sync_result = service.sync_fund_basic_data()
                if sync_result['success']:
                    logger.info(f"✅ 启动同步完成，共同步 {sync_result['total_synced']} 只基金")
                else:
                    logger.error("❌ 启动同步失败")
            else:
                logger.info("✅ 数据已是最新，无需同步")

            service.close()

        except Exception as e:
            logger.error(f"启动同步任务失败: {e}")

    # 启动后台线程
    thread = threading.Thread(target=_sync_task, daemon=True)
    thread.start()
    logger.info("后台同步检查线程已启动")


if __name__ == '__main__':
    # 测试同步服务
    service = FundSyncService()

    # 测试比对
    result = service.compare_fund_data()
    print(f"比对结果: {result}")

    # 测试同步
    sync_result = service.sync_fund_basic_data()
    print(f"同步结果: {sync_result}")

    service.close()
