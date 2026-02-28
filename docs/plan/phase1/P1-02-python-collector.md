# P1-02: Python采集模块 - 执行计划

> 所属阶段：Phase 1 数据基建层  
> 计划工期：5天  
> 前置依赖：P1-01 数据库设计与初始化

---

## 一、任务目标
构建Python数据采集模块，实现基金基础信息、净值、持仓的自动化采集。

---

## 二、执行步骤

### Day 1: 项目结构搭建

#### 任务 1.1: 创建项目结构
```
fund-collector/
├── config/
│   ├── __init__.py
│   └── settings.py          # 数据库配置
├── core/
│   ├── __init__.py
│   ├── collector.py         # 采集器基类
│   ├── akshare_client.py    # akshare封装
│   └── eastmoney_client.py  # 东方财富爬虫（预留）
├── models/
│   ├── __init__.py
│   └── schemas.py           # Pydantic模型
├── utils/
│   ├── __init__.py
│   └── database.py          # 数据库连接
├── requirements.txt
└── main.py                  # 入口
```

**检查点**：
- [ ] 目录结构创建
- [ ] requirements.txt包含：akshare, pandas, sqlalchemy, pymysql

#### 任务 1.2: 配置文件
```python
# config/settings.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # MySQL配置
    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "fund"
    mysql_password: str = "fund123"
    mysql_db: str = "fund_system"
    
    # akshare配置
    request_delay: float = 0.5  # 请求间隔（秒）
    retry_times: int = 3        # 重试次数
    
    class Config:
        env_file = ".env"

settings = Settings()
```

---

### Day 2: 基金列表与基础信息采集

#### 任务 2.1: akshare封装
```python
# core/akshare_client.py
import akshare as ak
import pandas as pd
from typing import Optional
import logging
import time

logger = logging.getLogger(__name__)

class AkShareClient:
    """akshare客户端封装"""
    
    def __init__(self, delay: float = 0.5):
        self.delay = delay
    
    def _safe_call(self, func, *args, **kwargs):
        """带重试的请求"""
        for i in range(3):
            try:
                time.sleep(self.delay)  # 控制频率
                return func(*args, **kwargs)
            except Exception as e:
                logger.warning(f"请求失败({i+1}/3): {e}")
                if i == 2:
                    raise
        return None
    
    def get_fund_list(self) -> pd.DataFrame:
        """获取全量基金列表"""
        df = self._safe_call(ak.fund_name_em)
        if df is None:
            return pd.DataFrame()
        
        df = df.rename(columns={
            '基金代码': 'fund_code',
            '基金简称': 'fund_name',
            '基金类型': 'fund_type'
        })
        return df[['fund_code', 'fund_name', 'fund_type']]
    
    def get_fund_basic(self, fund_code: str) -> Optional[dict]:
        """获取基金基础信息"""
        try:
            df = self._safe_call(ak.fund_info_em, fund_code)
            if df is None or df.empty:
                return None
            
            # 解析DataFrame为字典
            info = {}
            for _, row in df.iterrows():
                item = row['item']
                value = row['value']
                
                if item == '基金全称':
                    info['fund_name'] = value
                elif item == '基金类型':
                    info['fund_type'] = value
                elif item == '基金公司':
                    info['company_name'] = value
                elif item == '基金经理':
                    info['manager_name'] = value
                elif item == '基金规模':
                    info['current_scale'] = float(str(value).replace('亿元', ''))
                elif item == '成立日期':
                    info['establish_date'] = value
            
            info['fund_code'] = fund_code
            return info
        except Exception as e:
            logger.error(f"获取基金{fund_code}基础信息失败: {e}")
            return None
```

**检查点**：
- [ ] akshare调用正常
- [ ] 频率控制生效
- [ ] 重试机制可用

#### 任务 2.2: 基金基础信息入库
```python
# core/collector.py
from sqlalchemy import create_engine
from core.akshare_client import AkShareClient
import pandas as pd
import logging

logger = logging.getLogger(__name__)

class FundCollector:
    """基金采集器"""
    
    def __init__(self, db_config: dict):
        self.engine = create_engine(
            f"mysql+pymysql://{db_config['user']}:{db_config['password']}"
            f"@{db_config['host']}:{db_config['port']}/{db_config['database']}"
        )
        self.client = AkShareClient()
    
    def collect_fund_list(self) -> int:
        """采集基金列表"""
        df = self.client.get_fund_list()
        if df.empty:
            logger.error("获取基金列表失败")
            return 0
        
        # 写入数据库
        df.to_sql('fund_info', self.engine, if_exists='append', index=False)
        logger.info(f"采集基金列表：{len(df)}条")
        return len(df)
    
    def update_fund_basic(self, fund_codes: list = None, limit: int = 100):
        """更新基金基础信息"""
        if fund_codes is None:
            # 获取需要更新的基金
            with self.engine.connect() as conn:
                result = conn.execute(
                    "SELECT fund_code FROM fund_info WHERE manager_name IS NULL LIMIT %s",
                    (limit,)
                )
                fund_codes = [row[0] for row in result]
        
        updated = 0
        for code in fund_codes:
            try:
                info = self.client.get_fund_basic(code)
                if info:
                    self._update_fund_info(code, info)
                    updated += 1
            except Exception as e:
                logger.error(f"更新基金{code}失败: {e}")
        
        logger.info(f"更新基金基础信息：{updated}/{len(fund_codes)}条")
        return updated
    
    def _update_fund_info(self, fund_code: str, info: dict):
        """更新单只基金信息"""
        from sqlalchemy import text
        
        sql = text("""
        UPDATE fund_info SET
            fund_name = :fund_name,
            fund_type = :fund_type,
            company_name = :company_name,
            manager_name = :manager_name,
            current_scale = :current_scale,
            establish_date = :establish_date
        WHERE fund_code = :fund_code
        """)
        
        with self.engine.connect() as conn:
            conn.execute(sql, {**info, 'fund_code': fund_code})
            conn.commit()
```

**检查点**：
- [ ] 基金列表入库>1000条
- [ ] 基础信息更新正常

---

### Day 3: 净值采集与临时表

#### 任务 3.1: 每日净值采集
```python
# 在 AkShareClient 中添加

def get_daily_nav(self, date: Optional[str] = None) -> pd.DataFrame:
    """获取每日净值"""
    df = self._safe_call(ak.fund_em_open_fund_daily)
    if df is None or df.empty:
        return pd.DataFrame()
    
    df = df.rename(columns={
        '基金代码': 'fund_code',
        '单位净值': 'unit_nav',
        '累计净值': 'accum_nav',
        '日增长率': 'daily_return'
    })
    
    # 数据清洗
    df['daily_return'] = pd.to_numeric(
        df['daily_return'].astype(str).str.replace('%', ''),
        errors='coerce'
    )
    df['nav_date'] = date or pd.Timestamp.now().strftime('%Y-%m-%d')
    df['source'] = 'akshare'
    
    # 过滤无效数据
    df = df.dropna(subset=['unit_nav', 'fund_code'])
    
    return df[['fund_code', 'nav_date', 'unit_nav', 'accum_nav', 'daily_return', 'source']]
```

#### 任务 3.2: 临时表写入
```python
# 在 FundCollector 中添加

def collect_daily_nav(self, date: Optional[str] = None) -> int:
    """
    采集每日净值到临时表
    流程：采集 -> 临时表 -> 校验 -> 正式表
    """
    # 1. 采集数据
    df = self.client.get_daily_nav(date)
    if df.empty:
        logger.error("采集净值数据失败")
        return 0
    
    # 2. 清空临时表
    with self.engine.connect() as conn:
        conn.execute("TRUNCATE TABLE tmp_fund_nav")
        conn.commit()
    
    # 3. 写入临时表
    df.to_sql('tmp_fund_nav', self.engine, if_exists='append', index=False)
    
    logger.info(f"净值数据写入临时表：{len(df)}条")
    return len(df)
```

**检查点**：
- [ ] 净值采集正常
- [ ] 临时表写入成功

---

### Day 4: 持仓数据采集

#### 任务 4.1: 持仓信息采集
```python
# 在 AkShareClient 中添加

def get_fund_portfolio(self, fund_code: str, year: int, quarter: int) -> pd.DataFrame:
    """获取基金持仓（前十大重仓股）"""
    try:
        df = self._safe_call(
            ak.fund_portfolio_hold_em,
            symbol=fund_code,
            year=year,
            quarter=quarter
        )
        if df is None or df.empty:
            return pd.DataFrame()
        
        df = df.rename(columns={
            '股票代码': 'stock_code',
            '股票名称': 'stock_name',
            '占净值比例': 'holding_ratio',
            '持股数': 'holding_amount',
            '持仓市值': 'holding_value'
        })
        
        df['fund_code'] = fund_code
        df['report_date'] = f"{year}-{'03-31' if quarter==1 else '06-30' if quarter==2 else '09-30' if quarter==3 else '12-31'}"
        df['holding_type'] = '股票'
        df['is_top10'] = 1
        
        # 只保留前10
        df = df.head(10)
        
        return df[['fund_code', 'report_date', 'stock_code', 'stock_name', 
                   'holding_amount', 'holding_value', 'holding_ratio', 
                   'holding_type', 'is_top10']]
    except Exception as e:
        logger.error(f"获取持仓失败[{fund_code}]: {e}")
        return pd.DataFrame()
```

#### 任务 4.2: 持仓入库
```python
# 在 FundCollector 中添加

def collect_portfolio(self, fund_codes: list, year: int, quarter: int):
    """采集持仓数据"""
    all_data = []
    
    for code in fund_codes:
        try:
            df = self.client.get_fund_portfolio(code, year, quarter)
            if not df.empty:
                all_data.append(df)
        except Exception as e:
            logger.error(f"采集持仓失败[{code}]: {e}")
    
    if all_data:
        combined = pd.concat(all_data, ignore_index=True)
        
        # 删除旧数据（同一报告期）
        with self.engine.connect() as conn:
            report_date = f"{year}-{'03-31' if quarter==1 else '06-30' if quarter==2 else '09-30' if quarter==3 else '12-31'}"
            conn.execute(
                "DELETE FROM fund_holding WHERE report_date = %s",
                (report_date,)
            )
            conn.commit()
        
        # 写入新数据
        combined.to_sql('fund_holding', self.engine, if_exists='append', index=False)
        logger.info(f"持仓数据入库：{len(combined)}条")
        return len(combined)
    
    return 0
```

**检查点**：
- [ ] 持仓采集正常
- [ ] 数据去重正确

---

### Day 5: 异常处理与日志

#### 任务 5.1: 异常处理增强
```python
# utils/exceptions.py

class CollectorException(Exception):
    """采集异常基类"""
    pass

class DataSourceException(CollectorException):
    """数据源异常"""
    pass

class ValidationException(CollectorException):
    """数据校验异常"""
    pass

# 在采集器中统一处理
from functools import wraps

def retry_on_error(max_retries=3, exceptions=(Exception,)):
    """重试装饰器"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            for i in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except exceptions as e:
                    if i == max_retries - 1:
                        raise
                    time.sleep(2 ** i)  # 指数退避
        return wrapper
    return decorator
```

#### 任务 5.2: 日志配置
```python
# config/logging_config.py
import logging
import sys
from logging.handlers import RotatingFileHandler

def setup_logging():
    """配置日志"""
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # 控制台输出
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    console_handler.setLevel(logging.INFO)
    
    # 文件输出
    file_handler = RotatingFileHandler(
        'logs/collector.log',
        maxBytes=10*1024*1024,  # 10MB
        backupCount=5
    )
    file_handler.setFormatter(formatter)
    file_handler.setLevel(logging.DEBUG)
    
    # 根日志配置
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.DEBUG)
    root_logger.addHandler(console_handler)
    root_logger.addHandler(file_handler)
```

#### 任务 5.3: 入口脚本
```python
# main.py
import argparse
from config.settings import settings
from config.logging_config import setup_logging
from core.collector import FundCollector
import logging

logger = logging.getLogger(__name__)

def main():
    setup_logging()
    
    parser = argparse.ArgumentParser(description='基金数据采集器')
    parser.add_argument('--action', choices=['list', 'basic', 'nav', 'portfolio'], 
                       required=True, help='采集类型')
    parser.add_argument('--codes', help='基金代码列表，逗号分隔')
    parser.add_argument('--date', help='日期，格式YYYY-MM-DD')
    
    args = parser.parse_args()
    
    db_config = {
        'host': settings.mysql_host,
        'port': settings.mysql_port,
        'user': settings.mysql_user,
        'password': settings.mysql_password,
        'database': settings.mysql_db
    }
    
    collector = FundCollector(db_config)
    
    if args.action == 'list':
        count = collector.collect_fund_list()
        print(f"采集基金列表：{count}条")
    
    elif args.action == 'basic':
        codes = args.codes.split(',') if args.codes else None
        count = collector.update_fund_basic(codes)
        print(f"更新基础信息：{count}条")
    
    elif args.action == 'nav':
        count = collector.collect_daily_nav(args.date)
        print(f"采集净值数据：{count}条")
    
    elif args.action == 'portfolio':
        codes = args.codes.split(',') if args.codes else []
        # 默认采集最近季度
        from datetime import datetime
        now = datetime.now()
        year = now.year
        quarter = (now.month - 1) // 3 + 1
        count = collector.collect_portfolio(codes, year, quarter)
        print(f"采集持仓数据：{count}条")

if __name__ == '__main__':
    main()
```

**验收标准**：
```bash
# 测试命令
python main.py --action list
python main.py --action basic --limit 10
python main.py --action nav
python main.py --action portfolio --codes 005827,161725
```

---

## 三、验收清单

| 检查项 | 状态 | 验证方式 |
|--------|------|----------|
| 基金列表采集>1000条 | ☐ | 数据库查询 |
| 净值采集成功 | ☐ | 临时表数据 |
| 持仓采集成功 | ☐ | fund_holding表 |
| 异常重试机制 | ☐ | 模拟失败测试 |
| 日志记录完整 | ☐ | 查看日志文件 |

---

**执行人**：待定  
**验收人**：待定  
**更新日期**：2026-02-28
