# P5-02: 准实时估值采集系统

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P5-02 |
| 名称 | 准实时估值采集系统 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 4天 |
| 依赖 | P5-01（关注列表）|

---

## 需求描述

实现10分钟间隔的准实时估值采集系统：
1. 只采集用户自选基金的估值（而非全市场26000+）
2. 支持多数据源（akshare + 东方财富 + 蛋卷）自动切换
3. 节假日/非交易时间自动跳过
4. 数据校验和异常处理

---

## 数据库设计

### 实时估值点位表（分区表）
```sql
CREATE TABLE fund_estimate_intraday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    estimate_time DATETIME NOT NULL COMMENT '采集时间（每10分钟）',
    estimate_nav DECIMAL(10,4) COMMENT '预估净值',
    estimate_change_pct DECIMAL(8,4) COMMENT '预估涨跌幅%（当日点位）',
    estimate_change_amt DECIMAL(10,4) COMMENT '预估涨跌额',
    pre_close_nav DECIMAL(10,4) COMMENT '昨日收盘净值',
    trade_date DATE NOT NULL COMMENT '交易日期',
    data_source VARCHAR(20) DEFAULT 'akshare' COMMENT '数据来源',
    is_trading_time TINYINT DEFAULT 1 COMMENT '是否为交易时间采集',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_time (fund_code, estimate_time),
    INDEX idx_fund_date (fund_code, trade_date),
    INDEX idx_time (estimate_time)
) PARTITION BY RANGE COLUMNS(trade_date) (
    PARTITION p202403 VALUES LESS THAN ('2024-04-01'),
    PARTITION p202404 VALUES LESS THAN ('2024-05-01'),
    PARTITION p202405 VALUES LESS THAN ('2024-06-01'),
    PARTITION p_future VALUES LESS THAN (MAXVALUE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时估值点位';
```

---

## 实现步骤

### Day 1: 数据库 + Python基础服务
- [ ] 创建 fund_estimate_intraday 分区表
- [ ] Python Flask 服务搭建
  - [ ] app.py 基础框架
  - [ ] 健康检查接口 /health
  - [ ] 配置管理 config/
- [ ] 数据源配置
  - [ ] akshare 配置
  - [ ] 东方财富API配置
  - [ ] 蛋卷API配置

### Day 2: 多数据源采集
- [ ] 数据源基类设计
- [ ] AkshareCollector 实现
- [ ] EastmoneyCollector 实现
- [ ] DanjuanCollector 实现
- [ ] 数据源管理器（自动切换）
- [ ] 数据校验逻辑
  - [ ] 涨跌幅范围校验（<15%）
  - [ ] 净值范围校验（0.1-100）

### Day 3: 定时调度
- [ ] APScheduler 配置
  - [ ] 交易日判断
  - [ ] 交易时间调度（9:30-11:30, 13:00-15:00）
  - [ ] 每10分钟触发
- [ ] 采集主逻辑
  - [ ] 获取关注列表
  - [ ] 批量采集
  - [ ] 数据保存
- [ ] 异常处理和日志

### Day 4: API接口 + 部署
- [ ] Flask API接口
  ```python
  POST /api/collect/estimate      # 单只基金采集
  POST /api/collect/batch         # 批量采集
  POST /api/collect/pipeline      # 执行数据管道
  GET  /api/collect/health        # 健康检查
  ```
- [ ] Dockerfile 更新
- [ ] docker-compose 集成
- [ ] 测试验证

---

## 核心代码设计

### 数据源配置
```python
# config/data_source.py
DATA_SOURCES = {
    'akshare': {
        'name': 'akshare',
        'module': 'akshare.fund_em_value_estimation',
        'rate_limit': '10/minute',
        'priority': 1
    },
    'eastmoney': {
        'name': 'eastmoney',
        'url': 'https://fundmobapi.eastmoney.com/FundMNewApi/FundMNFInfo',
        'headers': {'User-Agent': '...'},
        'rate_limit': '30/minute',
        'priority': 2
    },
    'danjuan': {
        'name': 'danjuan',
        'url': 'https://danjuanfunds.com/djapi/fund/{fund_code}',
        'rate_limit': '20/minute',
        'priority': 3
    }
}
```

### 采集器基类
```python
from abc import ABC, abstractmethod

class BaseCollector(ABC):
    @abstractmethod
    def collect_estimate(self, fund_code: str) -> dict:
        """
        采集单只基金估值
        Returns: {
            'fund_code': str,
            'nav': float,
            'change_pct': float,
            'pre_close': float,
            'time': datetime
        }
        """
        pass
```

### 数据源管理器
```python
class DataSourceManager:
    def __init__(self):
        self.collectors = [
            AkshareCollector(),
            EastmoneyCollector(),
            DanjuanCollector()
        ]
    
    def collect_with_fallback(self, fund_code: str) -> dict:
        """带备用切换的采集"""
        for collector in self.collectors:
            try:
                data = collector.collect_estimate(fund_code)
                if self.validate(data):
                    return data
            except Exception as e:
                logger.warning(f"{collector.name} 失败: {e}")
                continue
        
        raise Exception("所有数据源均失败")
    
    def validate(self, data: dict) -> bool:
        """数据校验"""
        if abs(data['change_pct']) > 15:
            return False
        if data['nav'] < 0.1 or data['nav'] > 100:
            return False
        return True
```

### APScheduler 调度
```python
from apscheduler.schedulers.background import BackgroundScheduler

scheduler = BackgroundScheduler()

# 交易日每10分钟执行
scheduler.add_job(
    collect_intraday,
    'cron',
    day_of_week='mon-fri',
    hour='9-11,13-14',
    minute='*/10',
    second='30'
)

# 开盘和收盘特殊时间点
scheduler.add_job(collect_intraday, 'cron', day_of_week='mon-fri', hour='9', minute='30')
scheduler.add_job(collect_intraday, 'cron', day_of_week='mon-fri', hour='11', minute='30')
scheduler.add_job(collect_intraday, 'cron', day_of_week='mon-fri', hour='13', minute='0')
scheduler.add_job(collect_intraday, 'cron', day_of_week='mon-fri', hour='15', minute='0')
```

---

## Flask API 接口

### 单只基金采集
```python
@app.route('/api/collect/estimate', methods=['POST'])
def collect_estimate():
    fund_code = request.json.get('fundCode')
    
    try:
        manager = DataSourceManager()
        data = manager.collect_with_fallback(fund_code)
        
        # 保存到数据库
        db.save_estimate(data)
        
        return jsonify({
            'success': True,
            'data': {
                'fundCode': fund_code,
                'estimateTime': data['time'].isoformat(),
                'estimateNav': data['nav'],
                'estimateChangePct': data['change_pct'],
                'dataSource': data.get('source', 'unknown')
            }
        })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
```

---

## 验收标准

- [ ] Python Flask 服务可启动
- [ ] 单只基金采集成功（akshare）
- [ ] 备用数据源切换正常
- [ ] 定时调度按10分钟触发
- [ ] 节假日自动跳过
- [ ] 数据校验通过
- [ ] API接口可调用

---

## 测试计划

测试日志: P5-02-test-log.md

---

**制定日期**: 2026-03-02
