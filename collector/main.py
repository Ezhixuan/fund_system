#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金数据采集器入口

用法:
    python main.py --action list                    # 采集基金列表
    python main.py --action basic --limit 50        # 更新基础信息(50只)
    python main.py --action nav --date 2024-01-15   # 采集指定日期净值
    python main.py --action portfolio --codes 005827,161725  # 采集持仓
    python main.py --action pipeline                # 执行数据管道(校验+合并)
    python main.py --action validate                # 执行数据校验
    python main.py --action health                  # 检查系统健康状态
    python main.py --action alert                   # 执行告警检查
"""
import argparse
import sys
import logging

from config import settings
from utils.logging_config import setup_logging
from utils.database import db
from utils.alerter import Alerter
from core.collector import FundCollector
from core.data_pipeline import DataPipeline
from core.alert_trigger import AlertTrigger

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
  %(prog)s --action pipeline                       # 执行数据管道(校验+合并)
  %(prog)s --action validate                       # 仅执行数据校验
  %(prog)s --action health                         # 检查系统健康状态
  %(prog)s --action alert                          # 执行告警检查
        '''
    )
    
    parser.add_argument(
        '--action', '-a',
        choices=['list', 'basic', 'nav', 'portfolio', 'pipeline', 'validate', 'health', 'alert', 'test'],
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


def print_health_status(health: dict):
    """打印健康状态"""
    print(f"\n📊 系统健康状态 ({health['timestamp']})")
    print("=" * 50)
    
    for check_name, check_result in health['checks'].items():
        status_icon = '✅' if check_result['status'] == 'ok' else '⚠️' if check_result['status'] == 'warning' else '❌'
        print(f"{status_icon} {check_name}: {check_result['message']}")
    
    print("=" * 50)
    overall_icon = '✅' if health['overall'] == 'ok' else '⚠️' if health['overall'] == 'warning' else '❌'
    print(f"整体状态: {overall_icon} {health['overall'].upper()}")


def main():
    """主函数"""
    # 配置日志
    setup_logging()
    
    # 解析参数
    parser = create_parser()
    args = parser.parse_args()
    
    logger.info("=" * 60)
    logger.info("基金数据采集器启动")
    logger.info(f"动作: {args.action}")
    logger.info("=" * 60)
    
    # 测试数据库连接
    if not db.test_connection():
        logger.error("数据库连接失败，请检查配置")
        return 1
    
    logger.info(f"数据库连接成功: {settings.mysql_host}:{settings.mysql_port}/{settings.mysql_db}")
    
    try:
        if args.action == 'list':
            collector = FundCollector()
            count = collector.collect_fund_list()
            print(f"\n✅ 采集基金列表: {count}条")
            return 0 if count > 0 else 1
        
        elif args.action == 'basic':
            collector = FundCollector()
            codes = args.codes.split(',') if args.codes else None
            count = collector.update_fund_basic(codes, limit=args.limit)
            print(f"\n✅ 更新基础信息: {count}条")
            return 0
        
        elif args.action == 'nav':
            collector = FundCollector()
            count = collector.collect_daily_nav(args.date)
            print(f"\n✅ 采集净值数据: {count}条")
            return 0 if count > 0 else 1
        
        elif args.action == 'portfolio':
            collector = FundCollector()
            if not args.codes:
                logger.error("portfolio动作需要指定--codes参数")
                return 1
            codes = args.codes.split(',')
            count = collector.collect_portfolio(codes, year=args.year, quarter=args.quarter)
            print(f"\n✅ 采集持仓数据: {count}条")
            return 0
        
        elif args.action == 'pipeline':
            # 执行数据管道
            pipeline = DataPipeline()
            result = pipeline.process_nav_data()
            
            print(f"\n📊 数据管道执行结果")
            print(f"  是否通过: {'✅' if result.is_valid else '❌'}")
            print(f"  通过规则: {len(result.passed_rules)}项")
            print(f"  失败规则: {len(result.failed_rules)}项")
            
            if result.failed_rules:
                print("\n  失败详情:")
                for name, msg in result.failed_rules:
                    severity = '⚠️' if 'warning' in name else '❌'
                    print(f"    {severity} {name}: {msg}")
            
            if result.stats:
                print(f"\n  统计信息:")
                print(f"    总记录数: {result.stats.get('total_records', 0)}")
                print(f"    失败记录数: {result.stats.get('failed_records', 0)}")
            
            return 0 if result.is_valid else 1
        
        elif args.action == 'validate':
            # 仅执行校验（不合并）
            from core.data_validator import DataValidator
            from validators import create_nav_validators
            
            pipeline = DataPipeline()
            df = pipeline._read_temp_data()
            
            if df.empty:
                print("\n⚠️ 临时表无待处理数据")
                return 0
            
            print(f"\n🧪 校验 {len(df)} 条数据...")
            
            validator = DataValidator()
            rules = create_nav_validators()
            for rule in rules:
                validator.add_rule(rule)
            
            result = validator.validate(df)
            
            print(f"\n📊 校验结果")
            print(f"  是否通过: {'✅' if result.is_valid else '❌'}")
            print(f"  通过规则: {', '.join(result.passed_rules)}")
            
            if result.failed_rules:
                print(f"\n  失败规则:")
                for name, msg in result.failed_rules:
                    print(f"    ❌ {name}: {msg}")
            
            return 0 if result.is_valid else 1
        
        elif args.action == 'health':
            # 检查系统健康状态
            trigger = AlertTrigger()
            health = trigger.get_system_health()
            print_health_status(health)
            return 0 if health['overall'] == 'ok' else 1
        
        elif args.action == 'alert':
            # 执行告警检查
            trigger = AlertTrigger()
            trigger.check_all()
            print("\n✅ 告警检查执行完成")
            return 0
        
        elif args.action == 'test':
            # 测试模式
            print("\n🧪 测试模式")
            print(f"数据库连接: {'✅ 正常' if db.test_connection() else '❌ 失败'}")
            result = db.fetch_one("SELECT COUNT(*) as count FROM fund_info")
            print(f"fund_info表记录数: {result['count'] if result else 0}")
            
            # 检查临时表状态
            pipeline = DataPipeline()
            stats = pipeline.get_temp_stats()
            print(f"临时表状态: 待处理{stats['pending']}, 通过{stats['passed']}, 失败{stats['failed']}")
            
            return 0
            
    except KeyboardInterrupt:
        logger.info("用户中断")
        return 130
    except Exception as e:
        logger.error(f"执行失败: {e}", exc_info=True)
        return 1


if __name__ == '__main__':
    sys.exit(main())
