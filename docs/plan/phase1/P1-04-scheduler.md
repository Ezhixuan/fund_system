# P1-04: 定时调度与监控 - 执行计划

> 所属阶段：Phase 1 数据基建层  
> 计划工期：2天  
> 前置依赖：P1-03 数据质量与校验

---

## 一、任务目标
建立自动化定时调度系统，实现数据自动采集、校验、合并的全流程自动化。

---

## 二、执行步骤

### Day 1: APScheduler调度器

#### 任务 1.1: 调度器配置
```python
# scheduler/job_scheduler.py
from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger
from apscheduler.events import EVENT_JOB_EXECUTED, EVENT_JOB_ERROR
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

class FundJobScheduler:
    """基金数据定时调度器"""
    
    def __init__(self, db_config: dict):
        self.db_config = db_config
        self.scheduler = BlockingScheduler(
            timezone='Asia/Shanghai',  # 使用北京时间
            job_defaults={
                'coalesce': True,  # 错过的任务合并执行
                'max_instances': 1,  # 同一任务同时只运行一个实例
                'misfire_grace_time': 3600  # 允许1小时的延迟容错
            }
        )
        
        # 添加事件监听
        self.scheduler.add_listener(
            self._on_job_executed,
            EVENT_JOB_EXECUTED | EVENT_JOB_ERROR
        )
    
    def _on_job_executed(self, event):
        """任务执行回调"""
        if event.exception:
            logger.error(f"任务{event.job_id}执行失败: {event.exception}")
        else:
            logger.info(f"任务{event.job_id}执行成功")
    
    def add_daily_collection_job(self):
        """添加每日采集任务"""
        from core.collector import FundCollector
        
        def job():
            collector = FundCollector(self.db_config)
            # 采集净值到临时表
            count = collector.collect_daily_nav()
            logger.info(f"定时采集完成: {count}条净值数据")
            return count
        
        # 工作日19:00执行（收盘后）
        self.scheduler.add_job(
            job,
            CronTrigger(hour=19, minute=0, day_of_week='mon-fri'),
            id='daily_collection',
            name='每日净值采集',
            replace_existing=True
        )
        logger.info("已注册每日采集任务: 工作日19:00")
    
    def add_daily_validation_job(self):
        """添加每日校验任务"""
        from core.data_pipeline import DataPipeline
        
        def job():
            pipeline = DataPipeline(self.db_config)
            result = pipeline.process_nav_data()
            logger.info(f"定时校验完成: 通过{len(result.passed_rules)}条规则")
            return result.is_valid
        
        # 工作日19:30执行（采集后）
        self.scheduler.add_job(
            job,
            CronTrigger(hour=19, minute=30, day_of_week='mon-fri'),
            id='daily_validation',
            name='每日数据校验',
            replace_existing=True
        )
        logger.info("已注册每日校验任务: 工作日19:30")
    
    def add_daily_metrics_job(self):
        """添加每日指标计算任务"""
        from core.metrics_calculator import FundMetricsCalculator
        from sqlalchemy import create_engine
        import pandas as pd
        
        def job():
            engine = create_engine(
                f"mysql+pymysql://{self.db_config['user']}:{self.db_config['password']}"
                f"@{self.db_config['host']}:{self.db_config['port']}/{self.db_config['database']}"
            )
            
            # 获取需要计算的基金列表
            fund_codes = pd.read_sql(
                "SELECT DISTINCT fund_code FROM fund_nav WHERE nav_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)",
                engine
            )['fund_code'].tolist()
            
            calculator = FundMetricsCalculator()
            success_count = 0
            
            for code in fund_codes[:100]:  # 限制数量，避免超时
                try:
                    nav_data = pd.read_sql(f"""
                        SELECT nav_date, unit_nav, daily_return 
                        FROM fund_nav 
                        WHERE fund_code = '{code}'
                        ORDER BY nav_date DESC 
                        LIMIT 756
                    """, engine)
                    
                    if len(nav_data) >= 60:
                        metrics = calculator.calculate_all_metrics(code, nav_data)
                        # 保存到数据库
                        pd.DataFrame([metrics]).to_sql(
                            'fund_metrics', engine, if_exists='append', index=False
                        )
                        success_count += 1
                except Exception as e:
                    logger.error(f"计算指标失败[{code}]: {e}")
            
            logger.info(f"指标计算完成: {success_count}/{len(fund_codes)}只基金")
            return success_count
        
        # 工作日20:30执行（校验后）
        self.scheduler.add_job(
            job,
            CronTrigger(hour=20, minute=30, day_of_week='mon-fri'),
            id='daily_metrics',
            name='每日指标计算',
            replace_existing=True
        )
        logger.info("已注册每日指标任务: 工作日20:30")
    
    def add_weekly_full_calc_job(self):
        """添加周度全量计算任务"""
        
        def job():
            logger.info("开始周度全量计算...")
            # 全量重新计算所有指标和评分
            # 清理过期临时数据
            # 生成周报
            pass
        
        # 每周日凌晨02:00
        self.scheduler.add_job(
            job,
            CronTrigger(hour=2, minute=0, day_of_week='sun'),
            id='weekly_full_calc',
            name='周度全量计算',
            replace_existing=True
        )
        logger.info("已注册周度全量任务: 每周日02:00")
    
    def start(self):
        """启动调度器"""
        logger.info("调度器启动，等待任务执行...")
        try:
            self.scheduler.start()
        except KeyboardInterrupt:
            logger.info("调度器停止")
            self.scheduler.shutdown()
```

**检查点**：
- [ ] 调度器配置正确
- [ ] 时区设置为Asia/Shanghai
- [ ] 任务监听可用

#### 任务 1.2: 任务执行入口
```python
# scheduler/run_scheduler.py
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config.settings import settings
from config.logging_config import setup_logging
from scheduler.job_scheduler import FundJobScheduler
import logging

def main():
    setup_logging()
    logger = logging.getLogger(__name__)
    
    db_config = {
        'host': settings.mysql_host,
        'port': settings.mysql_port,
        'user': settings.mysql_user,
        'password': settings.mysql_password,
        'database': settings.mysql_db
    }
    
    scheduler = FundJobScheduler(db_config)
    
    # 注册所有任务
    scheduler.add_daily_collection_job()
    scheduler.add_daily_validation_job()
    scheduler.add_daily_metrics_job()
    scheduler.add_weekly_full_calc_job()
    
    logger.info("所有任务已注册，启动调度器...")
    scheduler.start()

if __name__ == '__main__':
    main()
```

**运行方式**：
```bash
# 前台运行
python scheduler/run_scheduler.py

# 后台运行（生产环境）
nohup python scheduler/run_scheduler.py > logs/scheduler.log 2>&1 &
echo $! > scheduler.pid

# 停止
kill `cat scheduler.pid`
```

---

### Day 2: 监控与运维

#### 任务 2.1: 任务监控面板
```python
# scheduler/monitor.py
from sqlalchemy import create_engine, text
import pandas as pd
from datetime import datetime, timedelta
import logging

logger = logging.getLogger(__name__)

class JobMonitor:
    """任务监控面板"""
    
    def __init__(self, db_config: dict):
        self.engine = create_engine(
            f"mysql+pymysql://{db_config['user']}:{db_config['password']}"
            f"@{db_config['host']}:{db_config['port']}/{db_config['database']}"
        )
    
    def get_recent_updates(self, hours: int = 24) -> pd.DataFrame:
        """获取最近更新记录"""
        sql = """
        SELECT 
            table_name,
            update_date,
            record_count,
            status,
            duration_seconds,
            end_time
        FROM data_update_log
        WHERE end_time > DATE_SUB(NOW(), INTERVAL %s HOUR)
        ORDER BY end_time DESC
        LIMIT 50
        """
        return pd.read_sql(sql, self.engine, params=(hours,))
    
    def get_update_stats(self, days: int = 7) -> dict:
        """获取更新统计"""
        sql = """
        SELECT 
            DATE(end_time) as date,
            table_name,
            COUNT(*) as job_count,
            SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
            SUM(CASE WHEN status = 'FAILURE' THEN 1 ELSE 0 END) as failure_count,
            SUM(record_count) as total_records,
            AVG(duration_seconds) as avg_duration
        FROM data_update_log
        WHERE end_time > DATE_SUB(CURDATE(), INTERVAL %s DAY)
        GROUP BY DATE(end_time), table_name
        ORDER BY date DESC
        """
        df = pd.read_sql(sql, self.engine, params=(days,))
        
        return {
            'daily_stats': df.to_dict('records'),
            'total_jobs': int(df['job_count'].sum()),
            'success_rate': round(
                df['success_count'].sum() / df['job_count'].sum() * 100, 2
            ) if len(df) > 0 else 0
        }
    
    def get_data_freshness(self) -> pd.DataFrame:
        """获取数据新鲜度"""
        sql = """
        SELECT 
            'fund_nav' as table_name,
            COUNT(*) as total_records,
            MAX(nav_date) as latest_date,
            DATEDIFF(CURDATE(), MAX(nav_date)) as days_delay
        FROM fund_nav
        UNION ALL
        SELECT 
            'fund_metrics',
            COUNT(*),
            MAX(calc_date),
            DATEDIFF(CURDATE(), MAX(calc_date))
        FROM fund_metrics
        """
        return pd.read_sql(sql, self.engine)
    
    def print_dashboard(self):
        """打印监控面板"""
        print("\n" + "="*60)
        print("基金数据采集监控面板")
        print("="*60)
        
        # 数据新鲜度
        freshness = self.get_data_freshness()
        print("\n【数据新鲜度】")
        for _, row in freshness.iterrows():
            status = "✓" if row['days_delay'] <= 1 else "✗"
            print(f"{status} {row['table_name']}: 最新{row['latest_date']}, 延迟{row['days_delay']}天")
        
        # 最近更新
        recent = self.get_recent_updates(24)
        print("\n【最近24小时更新】")
        for _, row in recent.head(5).iterrows():
            status_icon = "✓" if row['status'] == 'SUCCESS' else "✗"
            print(f"{status_icon} {row['table_name']}({row['update_date']}): "
                  f"{row['record_count']}条, {row['duration_seconds']}秒")
        
        # 统计
        stats = self.get_update_stats(7)
        print("\n【近7天统计】")
        print(f"总任务数: {stats['total_jobs']}")
        print(f"成功率: {stats['success_rate']}%")
        print("="*60)

if __name__ == '__main__':
    from config.settings import settings
    
    db_config = {
        'host': settings.mysql_host,
        'port': settings.mysql_port,
        'user': settings.mysql_user,
        'password': settings.mysql_password,
        'database': settings.mysql_db
    }
    
    monitor = JobMonitor(db_config)
    monitor.print_dashboard()
```

**检查点**：
- [ ] 监控面板可显示
- [ ] 数据新鲜度检测
- [ ] 成功率统计

#### 任务 2.2: 健康检查接口
```python
# scheduler/health_check.py
from flask import Flask, jsonify
from sqlalchemy import create_engine, text
import logging

logger = logging.getLogger(__name__)
app = Flask(__name__)

def create_health_app(db_config: dict):
    """创建健康检查应用"""
    engine = create_engine(
        f"mysql+pymysql://{db_config['user']}:{db_config['password']}"
        f"@{db_config['host']}:{db_config['port']}/{db_config['database']}"
    )
    
    @app.route('/health')
    def health():
        """健康检查"""
        try:
            # 检查数据库连接
            with engine.connect() as conn:
                result = conn.execute(text("SELECT 1"))
                
            # 检查数据新鲜度
            nav_count = conn.execute(text("SELECT COUNT(*) FROM fund_nav WHERE nav_date = CURDATE()")).scalar()
            
            return jsonify({
                'status': 'healthy',
                'database': 'connected',
                'today_nav_count': nav_count,
                'timestamp': datetime.now().isoformat()
            })
        except Exception as e:
            return jsonify({
                'status': 'unhealthy',
                'error': str(e)
            }), 500
    
    @app.route('/metrics')
    def metrics():
        """指标数据"""
        try:
            with engine.connect() as conn:
                # 获取统计数据
                nav_total = conn.execute(text("SELECT COUNT(*) FROM fund_nav")).scalar()
                fund_count = conn.execute(text("SELECT COUNT(*) FROM fund_info")).scalar()
                
            return jsonify({
                'nav_records': nav_total,
                'fund_count': fund_count,
                'timestamp': datetime.now().isoformat()
            })
        except Exception as e:
            return jsonify({'error': str(e)}), 500
    
    return app

if __name__ == '__main__':
    from config.settings import settings
    
    db_config = {
        'host': settings.mysql_host,
        'port': settings.mysql_port,
        'user': settings.mysql_user,
        'password': settings.mysql_password,
        'database': settings.mysql_db
    }
    
    app = create_health_app(db_config)
    app.run(host='0.0.0.0', port=5000)
```

**检查点**：
- [ ] /health接口可用
- [ ] /metrics接口可用

---

## 三、验收清单

| 检查项 | 状态 | 验证方式 |
|--------|------|----------|
| 定时任务按时触发 | ☐ | 查看日志 |
| 采集->校验->计算流程 | ☐ | 完整测试 |
| 监控面板显示正确 | ☐ | python monitor.py |
| 健康检查接口可用 | ☐ | curl localhost:5000/health |
| 异常自动告警 | ☐ | 模拟失败场景 |

---

## 四、运维命令

```bash
# 启动调度器
python scheduler/run_scheduler.py

# 查看监控面板
python scheduler/monitor.py

# 启动健康检查服务
python scheduler/health_check.py

# 手动触发采集
python main.py --action nav

# 查看日志
tail -f logs/collector.log
tail -f logs/scheduler.log
```

---

**执行人**：待定  
**验收人**：待定  
**更新日期**：2026-02-28
