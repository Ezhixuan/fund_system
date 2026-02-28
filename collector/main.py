#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金数据采集器入口

用法:
    python main.py --action list                    # 采集基金列表
    python main.py --action basic --limit 50        # 更新基础信息(50只)
    python main.py --action basic --codes 005827    # 更新指定基金
    python main.py --action nav --date 2024-01-15   # 采集指定日期净值
    python main.py --action portfolio --codes 005827,161725  # 采集持仓
"""
import argparse
import sys
import logging

from config import settings
from utils.logging_config import setup_logging
from utils.database import db
from core.collector import FundCollector

logger = logging.getLogger(__name__)


def create_parser() -> argparse.ArgumentParser:
    """创建参数解析器"""
    parser = argparse.ArgumentParser(
        description='基金数据采集器',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
示例:
  %(prog)s --action list                           # 采集基金列表
  %(prog)s --action basic --limit 100              # 更新100只基金基础信息
  %(prog)s --action basic --codes 005827,161725    # 更新指定基金
  %(prog)s --action nav                            # 采集今日净值
  %(prog)s --action nav --date 2024-01-15          # 采集指定日期净值
  %(prog)s --action portfolio --codes 005827       # 采集持仓
        '''
    )
    
    parser.add_argument(
        '--action', '-a',
        choices=['list', 'basic', 'nav', 'portfolio', 'test'],
        required=True,
        help='采集类型'
    )
    
    parser.add_argument(
        '--codes', '-c',
        help='基金代码列表，逗号分隔'
    )
    
    parser.add_argument(
        '--date', '-d',
        help='日期，格式YYYY-MM-DD'
    )
    
    parser.add_argument(
        '--limit', '-l',
        type=int,
        default=100,
        help='数量限制（用于basic动作）'
    )
    
    parser.add_argument(
        '--year', '-y',
        type=int,
        help='年份（用于portfolio动作）'
    )
    
    parser.add_argument(
        '--quarter', '-q',
        type=int,
        choices=[1, 2, 3, 4],
        help='季度 1-4（用于portfolio动作）'
    )
    
    return parser


def main():
    """主函数"""
    # 配置日志
    setup_logging()
    
    # 解析参数
    parser = create_parser()
    args = parser.parse_args()
    
    logger.info("=" * 60)
    logger.info("基金数据采集器启动")
    logger.info("=" * 60)
    
    # 测试数据库连接
    if not db.test_connection():
        logger.error("数据库连接失败，请检查配置")
        return 1
    
    logger.info(f"数据库连接成功: {settings.mysql_host}:{settings.mysql_port}/{settings.mysql_db}")
    
    # 创建采集器
    collector = FundCollector()
    
    try:
        if args.action == 'list':
            # 采集基金列表
            count = collector.collect_fund_list()
            print(f"\n✅ 采集基金列表: {count}条")
            return 0 if count > 0 else 1
        
        elif args.action == 'basic':
            # 更新基金基础信息
            codes = args.codes.split(',') if args.codes else None
            count = collector.update_fund_basic(codes, limit=args.limit)
            print(f"\n✅ 更新基础信息: {count}条")
            return 0
        
        elif args.action == 'nav':
            # 采集净值数据
            count = collector.collect_daily_nav(args.date)
            print(f"\n✅ 采集净值数据: {count}条")
            return 0 if count > 0 else 1
        
        elif args.action == 'portfolio':
            # 采集持仓数据
            if not args.codes:
                logger.error("portfolio动作需要指定--codes参数")
                return 1
            codes = args.codes.split(',')
            count = collector.collect_portfolio(codes, year=args.year, quarter=args.quarter)
            print(f"\n✅ 采集持仓数据: {count}条")
            return 0
        
        elif args.action == 'test':
            # 测试模式
            print("\n🧪 测试模式")
            print(f"数据库连接: {'✅ 正常' if db.test_connection() else '❌ 失败'}")
            result = db.fetch_one("SELECT COUNT(*) as count FROM fund_info")
            print(f"fund_info表记录数: {result['count'] if result else 0}")
            return 0
            
    except KeyboardInterrupt:
        logger.info("用户中断")
        return 130
    except Exception as e:
        logger.error(f"执行失败: {e}", exc_info=True)
        return 1


if __name__ == '__main__':
    sys.exit(main())
