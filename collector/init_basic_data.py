#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金基础数据初始化脚本
任务: P1-01-database / 任务 3.3
功能: 从akshare获取基金列表并导入fund_info表
"""

import os
import sys
import logging
from datetime import datetime

import pandas as pd
import akshare as ak
from sqlalchemy import create_engine, text
from sqlalchemy.exc import IntegrityError

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('/Users/ezhixuan/Projects/fund-system/docs/tasks/phase1/init-basic-data.log')
    ]
)
logger = logging.getLogger(__name__)

# 数据库配置
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3307,
    'user': 'fund',
    'password': 'fund123',
    'database': 'fund_system'
}


def create_engine_connection():
    """创建数据库连接引擎"""
    conn_str = (
        f"mysql+pymysql://{DB_CONFIG['user']}:{DB_CONFIG['password']}"
        f"@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"
        f"?charset=utf8mb4"
    )
    return create_engine(conn_str, pool_pre_ping=True)


def fetch_fund_list():
    """从akshare获取基金列表"""
    logger.info("开始从akshare获取基金列表...")
    try:
        df = ak.fund_name_em()
        logger.info(f"成功获取基金列表，共 {len(df)} 条记录")
        return df
    except Exception as e:
        logger.error(f"获取基金列表失败: {e}")
        raise


def transform_fund_data(df):
    """转换基金数据格式"""
    logger.info("开始转换数据格式...")
    
    # 列名映射 (根据akshare实际返回调整)
    column_mapping = {
        '基金代码': 'fund_code',
        '基金简称': 'fund_name',
        '基金类型': 'fund_type',
    }
    
    # 选择并重命名列
    available_cols = [c for c in column_mapping.keys() if c in df.columns]
    df_selected = df[available_cols].copy()
    df_selected.rename(columns=column_mapping, inplace=True)
    
    # 确保必要字段存在
    if 'fund_code' not in df_selected.columns:
        raise ValueError("缺少fund_code字段")
    if 'fund_name' not in df_selected.columns:
        df_selected['fund_name'] = '未知基金'
    if 'fund_type' not in df_selected.columns:
        df_selected['fund_type'] = None
    
    # 数据清洗
    df_selected['fund_code'] = df_selected['fund_code'].astype(str).str.strip()
    df_selected['fund_name'] = df_selected['fund_name'].astype(str).str.strip()
    
    # 去重（按fund_code）
    before_dedup = len(df_selected)
    df_selected.drop_duplicates(subset=['fund_code'], keep='first', inplace=True)
    after_dedup = len(df_selected)
    logger.info(f"数据去重: {before_dedup} -> {after_dedup}")
    
    return df_selected


def import_to_database(engine, df):
    """导入数据到数据库"""
    logger.info(f"开始导入数据到fund_info表，共 {len(df)} 条...")
    
    try:
        # 使用to_sql导入（if_exists='append'）
        df.to_sql(
            'fund_info',
            engine,
            if_exists='append',
            index=False,
            chunksize=500
        )
        logger.info("数据导入成功")
        return len(df)
    except IntegrityError as e:
        logger.warning(f"部分数据已存在，跳过重复: {e}")
        # 可以改用逐条插入处理重复
        return insert_ignore_duplicates(engine, df)
    except Exception as e:
        logger.error(f"数据导入失败: {e}")
        raise


def insert_ignore_duplicates(engine, df):
    """逐条插入，忽略重复"""
    success_count = 0
    skip_count = 0
    
    with engine.connect() as conn:
        for _, row in df.iterrows():
            try:
                sql = text("""
                    INSERT IGNORE INTO fund_info 
                    (fund_code, fund_name, fund_type, status)
                    VALUES (:fund_code, :fund_name, :fund_type, 1)
                """)
                conn.execute(sql, {
                    'fund_code': row['fund_code'],
                    'fund_name': row['fund_name'],
                    'fund_type': row.get('fund_type')
                })
                success_count += 1
            except Exception as e:
                skip_count += 1
                logger.debug(f"跳过记录 {row['fund_code']}: {e}")
        
        conn.commit()
    
    logger.info(f"逐条插入完成: 成功 {success_count}, 跳过 {skip_count}")
    return success_count


def verify_import(engine):
    """验证导入结果"""
    with engine.connect() as conn:
        result = conn.execute(text("SELECT COUNT(*) as total FROM fund_info"))
        count = result.scalar()
        logger.info(f"当前fund_info表记录数: {count}")
        return count


def log_update_status(engine, record_count, status, error_msg=None):
    """记录数据更新日志"""
    try:
        with engine.connect() as conn:
            sql = text("""
                INSERT INTO data_update_log 
                (table_name, update_date, record_count, status, error_msg, start_time, end_time)
                VALUES ('fund_info', CURDATE(), :record_count, :status, :error_msg, NOW(), NOW())
            """)
            conn.execute(sql, {
                'record_count': record_count,
                'status': status,
                'error_msg': error_msg
            })
            conn.commit()
    except Exception as e:
        logger.warning(f"记录日志失败: {e}")


def main():
    """主函数"""
    logger.info("=" * 60)
    logger.info("基金基础数据初始化开始")
    logger.info("=" * 60)
    
    start_time = datetime.now()
    record_count = 0
    
    try:
        # 1. 创建数据库连接
        engine = create_engine_connection()
        logger.info("数据库连接成功")
        
        # 2. 获取基金列表
        df_raw = fetch_fund_list()
        
        # 3. 转换数据
        df_clean = transform_fund_data(df_raw)
        
        # 4. 导入数据库
        record_count = import_to_database(engine, df_clean)
        
        # 5. 验证结果
        total_count = verify_import(engine)
        
        # 6. 记录日志
        log_update_status(engine, record_count, 'SUCCESS')
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.info("=" * 60)
        logger.info(f"初始化完成!")
        logger.info(f"本次导入: {record_count} 条")
        logger.info(f"表内总计: {total_count} 条")
        logger.info(f"耗时: {duration:.2f} 秒")
        logger.info("=" * 60)
        
        return 0 if total_count >= 1000 else 1
        
    except Exception as e:
        logger.error(f"初始化失败: {e}", exc_info=True)
        try:
            engine = create_engine_connection()
            log_update_status(engine, 0, 'FAILURE', str(e))
        except:
            pass
        return 1


if __name__ == '__main__':
    sys.exit(main())
