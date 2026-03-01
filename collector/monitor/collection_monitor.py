#!/usr/bin/env python3
"""
数据采集监控模块
用于监控各表数据新鲜度和采集状态
"""

import os
import sys
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from dataclasses import dataclass

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker


@dataclass
class TableStatus:
    """表状态信息"""
    table_name: str
    latest_date: Optional[str]
    record_count: int
    is_fresh: bool  # 数据是否新鲜（延迟<=1天）
    delay_days: int  # 延迟天数


@dataclass
class CollectionStats:
    """采集统计信息"""
    date: str
    total_funds: int
    collected_funds: int
    success_rate: float
    failed_count: int


class CollectionMonitor:
    """数据采集监控器"""
    
    def __init__(self, db_url: str = None):
        """
        初始化监控器
        
        Args:
            db_url: 数据库连接URL，默认从环境变量读取
        """
        if db_url is None:
            db_url = os.getenv('DB_URL', 'mysql+pymysql://fund:fund123@127.0.0.1:3307/fund_system')
        
        self.engine = create_engine(db_url, pool_pre_ping=True)
        self.Session = sessionmaker(bind=self.engine)
    
    def get_table_status(self) -> List[TableStatus]:
        """
        获取各表数据新鲜度状态
        
        Returns:
            表状态列表
        """
        tables = [
            ('fund_nav', 'nav_date'),
            ('fund_metrics', 'calc_date'),
            ('fund_score', 'calc_date'),
        ]
        
        results = []
        today = datetime.now().date()
        
        with self.Session() as session:
            for table_name, date_column in tables:
                try:
                    # 查询最新日期
                    query = text(f"""
                        SELECT MAX({date_column}) as latest_date, COUNT(*) as total_count
                        FROM {table_name}
                    """)
                    result = session.execute(query).fetchone()
                    
                    latest_date = result[0]
                    record_count = result[1]
                    
                    # 计算延迟天数
                    if latest_date:
                        if isinstance(latest_date, str):
                            latest_date = datetime.strptime(latest_date, '%Y-%m-%d').date()
                        delay_days = (today - latest_date).days
                        is_fresh = delay_days <= 1
                    else:
                        delay_days = -1
                        is_fresh = False
                    
                    results.append(TableStatus(
                        table_name=table_name,
                        latest_date=latest_date.isoformat() if latest_date else None,
                        record_count=record_count,
                        is_fresh=is_fresh,
                        delay_days=delay_days
                    ))
                    
                except Exception as e:
                    print(f"查询表 {table_name} 失败: {e}")
                    results.append(TableStatus(
                        table_name=table_name,
                        latest_date=None,
                        record_count=0,
                        is_fresh=False,
                        delay_days=-1
                    ))
        
        return results
    
    def get_collection_stats(self, date: str = None) -> CollectionStats:
        """
        获取指定日期的采集统计
        
        Args:
            date: 日期字符串，默认今天
            
        Returns:
            采集统计信息
        """
        if date is None:
            date = datetime.now().strftime('%Y-%m-%d')
        
        with self.Session() as session:
            # 查询总基金数
            total_query = text("SELECT COUNT(*) FROM fund_info")
            total_funds = session.execute(total_query).scalar()
            
            # 查询今日采集数
            collected_query = text("""
                SELECT COUNT(DISTINCT fund_code) 
                FROM fund_nav 
                WHERE nav_date = :date
            """)
            collected_funds = session.execute(collected_query, {'date': date}).scalar() or 0
            
            # 计算成功率
            success_rate = (collected_funds / total_funds * 100) if total_funds > 0 else 0
            failed_count = total_funds - collected_funds
            
            return CollectionStats(
                date=date,
                total_funds=total_funds,
                collected_funds=collected_funds,
                success_rate=round(success_rate, 2),
                failed_count=failed_count
            )
    
    def get_data_quality_report(self) -> Dict:
        """
        获取数据质量报告
        
        Returns:
            数据质量报告字典
        """
        with self.Session() as session:
            # 数据校验规则检查
            checks = []
            
            # 1. 检查净值是否为正
            nav_check = session.execute(text("""
                SELECT COUNT(*) FROM fund_nav WHERE unit_nav <= 0
            """)).scalar()
            checks.append({
                'rule': '净值大于0',
                'passed': nav_check == 0,
                'failed_count': nav_check
            })
            
            # 2. 检查累计净值是否大于等于单位净值
            acc_nav_check = session.execute(text("""
                SELECT COUNT(*) FROM fund_nav WHERE accum_nav < unit_nav
            """)).scalar()
            checks.append({
                'rule': '累计净值>=单位净值',
                'passed': acc_nav_check == 0,
                'failed_count': acc_nav_check
            })
            
            # 3. 检查必填字段
            null_check = session.execute(text("""
                SELECT COUNT(*) FROM fund_info 
                WHERE fund_code IS NULL OR fund_name IS NULL
            """)).scalar()
            checks.append({
                'rule': '必填字段不为空',
                'passed': null_check == 0,
                'failed_count': null_check
            })
            
            return {
                'checks': checks,
                'total_checks': len(checks),
                'passed_checks': sum(1 for c in checks if c['passed'])
            }
    
    def get_health_status(self) -> Dict:
        """
        获取系统健康状态（用于健康检查接口）
        
        Returns:
            健康状态字典
        """
        table_status = self.get_table_status()
        collection_stats = self.get_collection_stats()
        quality_report = self.get_data_quality_report()
        
        # 判断是否健康
        is_healthy = all(s.is_fresh for s in table_status)
        
        return {
            'status': 'healthy' if is_healthy else 'warning',
            'timestamp': datetime.now().isoformat(),
            'tables': [
                {
                    'name': s.table_name,
                    'latest_date': s.latest_date,
                    'is_fresh': s.is_fresh,
                    'delay_days': s.delay_days
                }
                for s in table_status
            ],
            'collection': {
                'date': collection_stats.date,
                'success_rate': collection_stats.success_rate,
                'collected': collection_stats.collected_funds,
                'total': collection_stats.total_funds
            },
            'quality': {
                'passed': quality_report['passed_checks'],
                'total': quality_report['total_checks']
            }
        }


def main():
    """命令行测试"""
    monitor = CollectionMonitor()
    
    print("=" * 60)
    print("数据采集监控报告")
    print("=" * 60)
    
    # 表状态
    print("\n【数据表状态】")
    for status in monitor.get_table_status():
        fresh_mark = "✓" if status.is_fresh else "✗"
        print(f"  {fresh_mark} {status.table_name}: "
              f"最新 {status.latest_date or 'N/A'}, "
              f"记录数 {status.record_count}, "
              f"延迟 {status.delay_days} 天")
    
    # 采集统计
    print("\n【今日采集统计】")
    stats = monitor.get_collection_stats()
    print(f"  日期: {stats.date}")
    print(f"  基金总数: {stats.total_funds}")
    print(f"  已采集: {stats.collected_funds}")
    print(f"  成功率: {stats.success_rate}%")
    print(f"  失败数: {stats.failed_count}")
    
    # 数据质量
    print("\n【数据质量检查】")
    quality = monitor.get_data_quality_report()
    for check in quality['checks']:
        mark = "✓" if check['passed'] else "✗"
        print(f"  {mark} {check['rule']}: "
              f"{check['failed_count']} 条记录未通过")
    
    print("\n" + "=" * 60)


if __name__ == '__main__':
    main()
