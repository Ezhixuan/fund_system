# 基金智能决策系统 - 系统设计文档

**版本**：v1.0
**技术栈**：Python (akshare) + Java (Spring Boot) + MySQL + Redis + RabbitMQ
**架构模式**：异构微服务架构（数据采集与业务服务分离）

------

## 1. 项目概述

### 1.1 项目目标

构建一套支持**基金数据采集、智能检索、优质基金筛选、组合诊断**的辅助决策系统。系统采用**Python负责数据采集与计算**、**Java负责核心业务与API**的异构架构，兼顾开发效率与运行稳定性。

### 1.2 核心功能模块

| 模块             | 负责方 | 功能描述                                                  |
| ---------------- | ------ | --------------------------------------------------------- |
| **数据采集引擎** | Python | 通过akshare获取基金净值、持仓、经理信息；爬虫补充缺失数据 |
| **数据计算中心** | Python | 基金评分计算、技术指标（夏普比率、回撤）、相关性矩阵      |
| **核心业务服务** | Java   | 用户管理、基金检索、组合分析、缓存管理                    |
| **智能推荐引擎** | Java   | 基于评分模型的优质基金筛选、相似基金推荐                  |

## 2. 系统架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         前端应用层 (Web/App)                         │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Java 核心业务层 (Spring Boot)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────┐ │
│  │  Controller  │  │    Service   │  │  Repository  │  │  Cache  │ │
│  │   REST API   │  │   业务逻辑   │  │   MyBatis    │  │  Redis  │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └────┬────┘ │
└───────────────────────────────────────────────────────────────┼─────┘
                                                                │
                    ┌──────────────────┬──────────────────────┘
                    │                  │
                    ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      数据总线层 (RabbitMQ)                           │
│         Exchange: fund.data.raw          Exchange: fund.calc        │
│         Queue: fund.nav.daily            Queue: fund.score          │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Python 数据层 (Data Pipeline)                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    数据采集引擎 (akshare)                      │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │  │
│  │  │  Fund Basic  │  │  Daily NAV   │  │   Portfolio      │   │  │
│  │  │  基础信息    │  │  每日净值    │  │   持仓数据       │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    计算与清洗模块                              │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │  │
│  │  │   Pandas     │  │  NumPy       │  │   评分算法       │   │  │
│  │  │   数据清洗   │  │  指标计算    │  │   模型           │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      持久化存储层                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  MySQL 8.0 (主从复制)                                       │   │
│  │  ├─ fund_db_master (写)                                    │   │
│  │  └─ fund_db_slave (读/报表)                                 │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Redis Cluster (缓存层)                                     │   │
│  │  ├─ 热点基金数据 (近期净值)                                 │   │
│  │  └─ 检索索引缓存                                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型与版本

| 层级       | 技术组件     | 版本/说明 | 选型理由           |
| ---------- | ------------ | --------- | ------------------ |
| **采集层** | Python       | 3.9+      | akshare生态完善    |
|            | akshare      | 1.11+     | 免费、中文支持好   |
|            | requests     | 2.31+     | 补充爬虫           |
|            | pandas       | 2.0+      | 数据处理           |
|            | SQLAlchemy   | 2.0+      | ORM连接MySQL       |
|            | pika         | 1.3+      | RabbitMQ客户端     |
| **业务层** | Java         | 17 LTS    | 长期支持           |
|            | Spring Boot  | 3.2+      | 微服务标准         |
|            | MyBatis-Plus | 3.5+      | 简化CRUD           |
|            | Redisson     | 3.24+     | Redis高级特性      |
|            | Spring AMQP  | 3.2+      | RabbitMQ集成       |
| **数据层** | MySQL        | 8.0+      | JSON支持、窗口函数 |
|            | Redis        | 7.0+      | 数据结构丰富       |
|            | RabbitMQ     | 3.12+     | 可靠消息队列       |
| **运维**   | Docker       | 24.x      | 容器化部署         |
|            | Nginx        | 1.24+     | 反向代理           |

## 3. 数据采集层详细设计（Python）

### 3.1 模块架构

```
fund_collector/
├── config/
│   ├── __init__.py
│   ├── database.py       # 数据库连接配置
│   ├── mq.py            # 消息队列配置
│   └── settings.py       # 全局配置
├── core/
│   ├── __init__.py
│   ├── akshare_client.py # akshare封装
│   ├── calculator.py     # 指标计算
│   └── publisher.py      # MQ发布
├── crawlers/
│   ├── __init__.py
│   └── eastmoney.py      # 东方财富补充爬虫
├── models/
│   ├── __init__.py
│   └── schemas.py        # Pydantic数据模型
├── tasks/
│   ├── __init__.py
│   ├── daily_job.py      # 日终任务
│   └── weekly_job.py     # 周终任务
└── main.py               # 入口
```

### 3.2 核心配置

```python
# config/settings.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # 数据库
    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "fund_user"
    mysql_password: str = "fund_pass"
    mysql_db: str = "fund_db"
    
    # RabbitMQ
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "admin"
    rabbitmq_pass: str = "admin"
    mq_exchange_fund: str = "fund.data"
    mq_queue_nav: str = "fund.nav.daily"
    
    # Tushare (可选，akshare为主时可为空)
    tushare_token: str = ""
    
    # 采集配置
    nav_update_time: str = "19:00"  # 每日估值更新时间
    request_delay: float = 0.5      # 请求间隔（秒）
    
    class Config:
        env_file = ".env"

settings = Settings()
```

### 3.3 数据采集引擎实现

```python
# core/akshare_client.py
import akshare as ak
import pandas as pd
from datetime import datetime, timedelta
from typing import Optional, List
import logging
from sqlalchemy import create_engine
from config.settings import settings

logger = logging.getLogger(__name__)

class FundDataCollector:
    def __init__(self):
        self.engine = create_engine(
            f"mysql+pymysql://{settings.mysql_user}:{settings.mysql_password}"
            f"@{settings.mysql_host}:{settings.mysql_port}/{settings.mysql_db}"
        )
    
    def fetch_all_fund_list(self) -> pd.DataFrame:
        """
        获取全量基金基础列表（约1万只）
        来源：akshareFund代码搜索
        """
        try:
            df = ak.fund_name_em()
            # 标准化字段
            df = df.rename(columns={
                '基金代码': 'fund_code',
                '基金简称': 'fund_name',
                '基金类型': 'fund_type',
                '成立日期': 'establish_date'
            })
            df['created_at'] = datetime.now()
            logger.info(f"获取基金列表成功，共 {len(df)} 条")
            return df[['fund_code', 'fund_name', 'fund_type', 'establish_date', 'created_at']]
        except Exception as e:
            logger.error(f"获取基金列表失败: {e}")
            raise
    
    def fetch_daily_nav(self, date: Optional[str] = None) -> pd.DataFrame:
        """
        获取开放式基金每日净值
        支持指定日期，默认为最新交易日
        """
        if date is None:
            date = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
        
        try:
            # akshare获取实时估值（盘中）或净值（盘后）
            df = ak.fund_em_open_fund_daily()
            
            # 数据清洗
            df = df.rename(columns={
                '基金代码': 'fund_code',
                '基金简称': 'fund_name',
                '单位净值': 'unit_nav',
                '累计净值': 'accum_nav',
                '日增长率': 'daily_return',
                '申购状态': 'purchase_status',
                '赎回状态': 'redeem_status'
            })
            
            # 类型转换
            df['unit_nav'] = pd.to_numeric(df['unit_nav'], errors='coerce')
            df['accum_nav'] = pd.to_numeric(df['accum_nav'], errors='coerce')
            df['daily_return'] = pd.to_numeric(df['daily_return'].str.replace('%', ''), errors='coerce')
            df['nav_date'] = date
            df['source'] = 'akshare'
            df['created_at'] = datetime.now()
            
            # 过滤无效数据
            df = df.dropna(subset=['unit_nav', 'fund_code'])
            
            logger.info(f"获取 {date} 净值数据成功，共 {len(df)} 条")
            return df[['fund_code', 'nav_date', 'unit_nav', 'accum_nav', 
                      'daily_return', 'source', 'created_at']]
            
        except Exception as e:
            logger.error(f"获取净值数据失败 [{date}]: {e}")
            # 失败时返回空DataFrame，由上层触发爬虫补偿
            return pd.DataFrame()
    
    def fetch_fund_portfolio(self, fund_code: str, year: int, quarter: int) -> pd.DataFrame:
        """
        获取基金持仓（前十大重仓股）
        用于计算行业集中度、风格分析
        """
        try:
            df = ak.fund_portfolio_hold_em(symbol=fund_code, year=year, quarter=quarter)
            df = df.rename(columns={
                '股票代码': 'stock_code',
                '股票名称': 'stock_name',
                '占净值比例': 'ratio',
                '持股数': 'shares',
                '持仓市值': 'market_value'
            })
            df['fund_code'] = fund_code
            df['report_year'] = year
            df['report_quarter'] = quarter
            df['created_at'] = datetime.now()
            return df[['fund_code', 'stock_code', 'stock_name', 'ratio', 
                      'report_year', 'report_quarter', 'created_at']]
        except Exception as e:
            logger.warning(f"获取持仓失败 [{fund_code}]: {e}")
            return pd.DataFrame()
    
    def calculate_technical_indicators(self, fund_code: str, days: int = 252) -> dict:
        """
        计算基金技术指标（夏普比率、最大回撤、波动率）
        用于优质基金评分
        """
        query = f"""
        SELECT nav_date, unit_nav, daily_return 
        FROM fund_nav 
        WHERE fund_code = '{fund_code}' 
        ORDER BY nav_date DESC 
        LIMIT {days}
        """
        df = pd.read_sql(query, self.engine)
        
        if len(df) < 60:  # 数据不足
            return {}
        
        df = df.sort_values('nav_date')
        returns = df['daily_return'].dropna() / 100  # 转为小数
        
        # 年化收益率
        total_return = (df['unit_nav'].iloc[-1] / df['unit_nav'].iloc[0]) - 1
        annual_return = (1 + total_return) ** (252 / len(df)) - 1
        
        # 波动率（年化）
        volatility = returns.std() * (252 ** 0.5)
        
        # 夏普比率（假设无风险利率2.5%）
        risk_free_rate = 0.025
        sharpe_ratio = (annual_return - risk_free_rate) / volatility if volatility > 0 else 0
        
        # 最大回撤
        cumulative = (1 + returns).cumprod()
        running_max = cumulative.expanding().max()
        drawdown = (cumulative - running_max) / running_max
        max_drawdown = drawdown.min()
        
        return {
            'fund_code': fund_code,
            'sharpe_ratio': round(sharpe_ratio, 4),
            'annual_return': round(annual_return, 4),
            'volatility': round(volatility, 4),
            'max_drawdown': round(max_drawdown, 4),
            'calc_date': datetime.now().strftime('%Y-%m-%d')
        }
```

### 3.4 爬虫补充模块（东方财富）

```python
# crawlers/eastmoney.py
import requests
import json
import re
from typing import Dict, Optional

class EastMoneyCrawler:
    """
    当akshare数据缺失或需要更实时数据时使用
    注意：需控制频率，避免触发反爬
    """
    
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'http://fundf10.eastmoney.com/',
            'Accept': 'application/json, text/javascript, */*'
        })
    
    def get_realtime_nav(self, fund_code: str) -> Optional[Dict]:
        """
        获取盘中实时估值（akshare不支持时补充）
        """
        url = f"http://fundgz.1234567.com.cn/js/{fund_code}.js"
        try:
            resp = self.session.get(url, timeout=5)
            # 返回格式：jsonpgz({"fundcode":"000001","name":"华夏成长"...})
            match = re.search(r'jsonpgz\((.*?)\);', resp.text)
            if match:
                data = json.loads(match.group(1))
                return {
                    'fund_code': data['fundcode'],
                    'fund_name': data['name'],
                    'est_nav': float(data['gsz']),      # 估算净值
                    'est_growth': float(data['gszzl']), # 估算涨跌幅
                    'nav_date': data['gztime'][:10],    # 净值日期
                    'source': 'eastmoney'
                }
        except Exception as e:
            return None
        return None
    
    def get_fund_manager_info(self, fund_code: str) -> Optional[Dict]:
        """
        获取基金经理详细信息（履历、管理规模）
        """
        url = f"http://fundf10.eastmoney.com/jjjl_{fund_code}.html"
        try:
            resp = self.session.get(url)
            # 解析HTML提取信息（简化示例，实际需用BeautifulSoup）
            # ...
            return {
                'manager_name': '',
                'tenure': '',
                'total_scale': ''
            }
        except:
            return None
```

### 3.5 消息队列发布者

```python
# core/publisher.py
import pika
import json
from config.settings import settings
from typing import Dict, List
import logging

logger = logging.getLogger(__name__)

class MqPublisher:
    def __init__(self):
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=settings.rabbitmq_host,
                port=settings.rabbitmq_port,
                credentials=pika.PlainCredentials(
                    settings.rabbitmq_user, 
                    settings.rabbitmq_pass
                )
            )
        )
        self.channel = self.connection.channel()
        
        # 声明交换机和队列（持久化）
        self.channel.exchange_declare(
            exchange=settings.mq_exchange_fund, 
            exchange_type='topic', 
            durable=True
        )
        self.channel.queue_declare(
            queue=settings.mq_queue_nav, 
            durable=True
        )
        self.channel.queue_bind(
            exchange=settings.mq_exchange_fund,
            queue=settings.mq_queue_nav,
            routing_key='nav.daily'
        )
    
    def publish_nav_data(self, data_list: List[Dict]):
        """
        批量发送净值数据到MQ
        """
        for item in data_list:
            message = json.dumps(item, ensure_ascii=False, default=str)
            try:
                self.channel.basic_publish(
                    exchange=settings.mq_exchange_fund,
                    routing_key='nav.daily',
                    body=message,
                    properties=pika.BasicProperties(
                        delivery_mode=2,  # 持久化
                        content_type='application/json'
                    )
                )
            except Exception as e:
                logger.error(f"发送消息失败: {e}")
        
        logger.info(f"发送完成，共 {len(data_list)} 条消息")
    
    def close(self):
        self.connection.close()
```

### 3.6 定时任务调度

```python
# tasks/daily_job.py
from apscheduler.schedulers.blocking import BlockingScheduler
from core.akshare_client import FundDataCollector
from core.publisher import MqPublisher
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def daily_collection_job():
    """
    每日19:30执行（交易日）
    1. 采集当日净值
    2. 计算技术指标
    3. 发送到MQ供Java消费
    """
    collector = FundDataCollector()
    publisher = MqPublisher()
    
    try:
        # 1. 采集净值
        df = collector.fetch_daily_nav()
        if df.empty:
            logger.error("未获取到数据，任务终止")
            return
        
        # 2. 转换为消息格式
        records = df.to_dict('records')
        
        # 3. 批量发送（分批避免MQ压力过大）
        batch_size = 500
        for i in range(0, len(records), batch_size):
            batch = records[i:i+batch_size]
            publisher.publish_nav_data(batch)
        
        logger.info(f"日终任务完成，处理 {len(records)} 条数据")
        
    except Exception as e:
        logger.error(f"日终任务异常: {e}")
    finally:
        publisher.close()

if __name__ == '__main__':
    scheduler = BlockingScheduler()
    # 交易日19:30执行（实际生产建议用cron表达式精确控制）
    scheduler.add_job(
        daily_collection_job, 
        'cron', 
        hour=19, 
        minute=30,
        day_of_week='mon-fri'
    )
    logger.info("调度器启动，等待执行任务...")
    scheduler.start()
```

## 4. 核心业务层设计（Java）

### 4.1 项目结构

```
fund-service/
├── src/main/java/com/fund/
│   ├── controller/          # REST API
│   │   ├── FundController.java
│   │   └── PortfolioController.java
│   ├── service/             # 业务逻辑
│   │   ├── FundQueryService.java
│   │   ├── FundScoreService.java
│   │   └── PortfolioService.java
│   ├── repository/          # 数据访问
│   │   ├── FundRepository.java
│   │   └── NavRepository.java
│   ├── entity/              # 实体类
│   │   ├── FundInfo.java
│   │   └── FundNav.java
│   ├── dto/                 # 传输对象
│   │   ├── FundDTO.java
│   │   └── FundSearchRequest.java
│   ├── consumer/            # MQ消费者
│   │   └── FundDataConsumer.java
│   └── config/              # 配置
│       ├── RabbitConfig.java
│       └── RedisConfig.java
└── src/main/resources/
    ├── application.yml
    └── mapper/              # MyBatis XML
```

### 4.2 核心配置

```yaml
# application.yml
spring:
  datasource:
    master:
      url: jdbc:mysql://localhost:3306/fund_db?useUnicode=true&characterEncoding=utf8
      username: fund_user
      password: fund_pass
      driver-class-name: com.mysql.cj.jdbc.Driver
    slave:
      url: jdbc:mysql://localhost:3307/fund_db?useUnicode=true&characterEncoding=utf8
      username: fund_user_readonly
      password: readonly_pass
      
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
    listener:
      simple:
        concurrency: 5
        max-concurrency: 20
        acknowledge-mode: manual  # 手动ACK
        
  redis:
    host: localhost
    port: 6379
    database: 0
    lettuce:
      pool:
        max-active: 8
        
server:
  port: 8080

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:/mapper/**/*.xml
```

### 4.3 实体类定义

```java
// entity/FundInfo.java
@Data
@TableName("fund_info")
public class FundInfo {
    @TableId(type = IdType.INPUT)
    private String fundCode;
    private String fundName;
    private String fundType;
    private String managerCode;
    private String companyCode;
    private LocalDate establishDate;
    private BigDecimal managementFee;
    private Integer riskLevel;
    private Integer status;
    private LocalDateTime updateTime;
    
    // 冗余字段（加速查询）
    @TableField(exist = false)
    private BigDecimal latestNav;
    @TableField(exist = false)
    private BigDecimal yearReturn;
    @TableField(exist = false)
    private BigDecimal sharpeRatio;
}

// entity/FundNav.java
@Data
@TableName("fund_nav")
public class FundNav {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private LocalDate navDate;
    private BigDecimal unitNav;
    private BigDecimal accumNav;
    private BigDecimal dailyReturn;
    private String source;
    private LocalDateTime createdAt;
    
    // 分区键（用于分表路由）
    @TableField(exist = false)
    private Integer year;
}
```

### 4.4 MQ消费者实现

```java
// consumer/FundDataConsumer.java
@Slf4j
@Component
public class FundDataConsumer {
    
    @Autowired
    private FundNavService fundNavService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private FundScoreCalculator scoreCalculator;
    
    @RabbitListener(queues = "${spring.rabbitmq.queues.nav}", containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackFor = Exception.class)
    public void handleNavMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            FundNavMessage navMessage = JSON.parseObject(message, FundNavMessage.class);
            
            // 1. 幂等性检查（Redis去重）
            String dupKey = "fund:nav:processed:" + navMessage.getFundCode() + ":" + navMessage.getNavDate();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dupKey, "1", Duration.ofHours(24));
            if (!Boolean.TRUE.equals(isNew)) {
                log.warn("重复消息，跳过: {}", dupKey);
                channel.basicAck(tag, false);
                return;
            }
            
            // 2. 数据校验
            if (navMessage.getUnitNav() == null || navMessage.getUnitNav().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("非法净值数据: " + navMessage.getFundCode());
            }
            
            // 3. 保存到MySQL（主库）
            FundNav entity = convertToEntity(navMessage);
            fundNavService.saveOrUpdate(entity);  // 存在则更新
            
            // 4. 更新缓存（最新净值）
            updateLatestNavCache(navMessage);
            
            // 5. 触发异步评分计算（如果满足条件）
            if (isTradingDayEnd(navMessage.getNavDate())) {
                scoreCalculator.asyncCalculateScore(navMessage.getFundCode());
            }
            
            channel.basicAck(tag, false);
            log.debug("处理成功: {}", navMessage.getFundCode());
            
        } catch (Exception e) {
            log.error("处理消息失败: {}", message, e);
            try {
                channel.basicNack(tag, false, true); // 重试
            } catch (IOException ioException) {
                log.error("Nack失败", ioException);
            }
        }
    }
    
    private void updateLatestNavCache(FundNavMessage message) {
        String key = "fund:latest:" + message.getFundCode();
        Map<String, String> data = new HashMap<>();
        data.put("unitNav", message.getUnitNav().toString());
        data.put("navDate", message.getNavDate());
        data.put("dailyReturn", message.getDailyReturn().toString());
        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, Duration.ofDays(7));
    }
}
```

### 4.5 基金检索服务（核心功能）

```java
// service/FundQueryService.java
@Service
public class FundQueryService {
    
    @Autowired
    private FundInfoMapper fundInfoMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 多维度基金检索（支持分页、排序、筛选）
     */
    public PageResult<FundVO> searchFunds(FundSearchRequest request) {
        // 1. 构建查询条件（MyBatis-Plus动态SQL）
        LambdaQueryWrapper<FundInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(request.getKeyword())) {
            wrapper.and(w -> w.like(FundInfo::getFundName, request.getKeyword())
                           .or()
                           .like(FundInfo::getFundCode, request.getKeyword()));
        }
        
        if (request.getFundTypes() != null && !request.getFundTypes().isEmpty()) {
            wrapper.in(FundInfo::getFundType, request.getFundTypes());
        }
        
        if (request.getRiskLevels() != null && !request.getRiskLevels().isEmpty()) {
            wrapper.in(FundInfo::getRiskLevel, request.getRiskLevels());
        }
        
        // 2. 业绩筛选（子查询或JOIN）
        if (request.getMinSharpe() != null) {
            wrapper.inSql(FundInfo::getFundCode,
                "SELECT fund_code FROM fund_score WHERE sharpe_ratio >= " + request.getMinSharpe());
        }
        
        // 3. 排序（支持多字段）
        if (request.getSortField() != null) {
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
            switch (request.getSortField()) {
                case "yearReturn":
                    // 需关联查询，这里简化处理
                    wrapper.orderBy(true, isAsc, FundInfo::getFundCode); // 占位，实际需自定义SQL
                    break;
                default:
                    wrapper.orderBy(true, isAsc, FundInfo::getEstablishDate);
            }
        }
        
        // 4. 执行查询（从库）
        Page<FundInfo> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<FundInfo> resultPage = fundInfoMapper.selectPage(page, wrapper);
        
        // 5. 补充实时数据（Redis）
        List<FundVO> voList = resultPage.getRecords().stream()
            .map(this::enrichFundData)
            .collect(Collectors.toList());
            
        return new PageResult<>(resultPage.getTotal(), voList);
    }
    
    /**
     * 优质基金推荐（基于评分模型）
     */
    public List<FundVO> recommendQualityFunds(String fundType, int limit) {
        String cacheKey = "fund:recommend:" + fundType;
        
        // 尝试读取缓存（每日更新）
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return JSON.parseArray(cached, FundVO.class);
        }
        
        // 查询高分基金（综合评分>80，夏普>1.0，回撤<20%）
        List<FundScore> topFunds = fundInfoMapper.selectQualityFunds(fundType, limit);
        
        List<FundVO> result = topFunds.stream()
            .map(score -> {
                FundVO vo = new FundVO();
                BeanUtil.copyProperties(score, vo);
                vo.setTags(List.of("高夏普", "低回撤", "业绩稳定"));
                return vo;
            })
            .collect(Collectors.toList());
            
        // 缓存12小时
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result), Duration.ofHours(12));
        
        return result;
    }
    
    private FundVO enrichFundData(FundInfo fund) {
        FundVO vo = new FundVO();
        BeanUtil.copyProperties(fund, vo);
        
        // 从Redis读取最新净值
        String navKey = "fund:latest:" + fund.getFundCode();
        Map<Object, Object> navData = redisTemplate.opsForHash().entries(navKey);
        if (!navData.isEmpty()) {
            vo.setLatestNav(new BigDecimal((String) navData.get("unitNav")));
            vo.setDailyReturn(new BigDecimal((String) navData.get("dailyReturn")));
        }
        
        return vo;
    }
}
```

### 4.6 基金评分算法（Java实现）

```java
// service/FundScoreCalculator.java
@Component
@Slf4j
public class FundScoreCalculator {
    
    @Autowired
    private FundNavMapper navMapper;
    
    @Autowired
    private FundScoreRepository scoreRepository;
    
    /**
     * 多维度基金评分算法（0-100分）
     * 每日夜间批量计算
     */
    @Async("taskExecutor")
    public void asyncCalculateScore(String fundCode) {
        try {
            FundScore score = calculateScore(fundCode);
            scoreRepository.save(score);
            log.info("评分计算完成: {} - 总分: {}", fundCode, score.getTotalScore());
        } catch (Exception e) {
            log.error("评分计算失败: {}", fundCode, e);
        }
    }
    
    public FundScore calculateScore(String fundCode) {
        // 获取近1年、3年数据
        List<FundNav> navList = navMapper.selectRecentNav(fundCode, 756); // 3年交易日
        
        if (navList.size() < 60) {
            throw new IllegalStateException("数据不足，无法评分");
        }
        
        FundScore score = new FundScore();
        score.setFundCode(fundCode);
        score.setCalcDate(LocalDate.now());
        
        // 1. 收益能力 (30分) - 基于夏普比率
        double sharpe = calculateSharpe(navList);
        score.setSharpeRatio(BigDecimal.valueOf(sharpe));
        score.setReturnScore(calcReturnScore(sharpe));
        
        // 2. 风险控制 (25分) - 基于最大回撤和卡玛比率
        double maxDrawdown = calculateMaxDrawdown(navList);
        double calmar = calculateCalmar(navList, maxDrawdown);
        score.setMaxDrawdown(BigDecimal.valueOf(maxDrawdown));
        score.setRiskScore(calcRiskScore(calmar, maxDrawdown));
        
        // 3. 稳定性 (20分) - 收益波动率
        double volatility = calculateVolatility(navList);
        score.setVolatility(BigDecimal.valueOf(volatility));
        score.setStabilityScore(calcStabilityScore(volatility));
        
        // 4. 规模适配 (15分) - 查询基金规模数据
        BigDecimal scale = getFundScale(fundCode);
        score.setScaleScore(calcScaleScore(scale));
        
        // 5. 费用性价比 (10分)
        BigDecimal fee = getManagementFee(fundCode);
        score.setFeeScore(calcFeeScore(fee));
        
        // 总分
        int total = score.getReturnScore() + score.getRiskScore() + 
                   score.getStabilityScore() + score.getScaleScore() + score.getFeeScore();
        score.setTotalScore(total);
        score.setQualityLevel(total >= 80 ? "A" : total >= 60 ? "B" : "C");
        
        return score;
    }
    
    private double calculateSharpe(List<FundNav> navList) {
        // 简化计算：年化收益 / 年化波动
        double[] returns = navList.stream()
            .mapToDouble(n -> n.getDailyReturn().doubleValue())
            .toArray();
            
        double avgReturn = Arrays.stream(returns).average().orElse(0) * 252; // 年化
        double stdDev = Math.sqrt(Arrays.stream(returns)
            .map(r -> Math.pow(r - avgReturn/252, 2))
            .average().orElse(0)) * Math.sqrt(252);
            
        return stdDev == 0 ? 0 : (avgReturn - 0.025) / stdDev; // 假设无风险利率2.5%
    }
    
    private int calcReturnScore(double sharpe) {
        if (sharpe >= 2.0) return 30;
        if (sharpe >= 1.5) return 25;
        if (sharpe >= 1.0) return 20;
        if (sharpe >= 0.5) return 15;
        return Math.max(0, (int)(sharpe * 20));
    }
    
    // 其他计算方法省略...
}
```

## 5. 数据库设计（MySQL）

### 5.1 核心表结构

```sql
-- 1. 基金基础信息表
CREATE TABLE fund_info (
    fund_code VARCHAR(10) PRIMARY KEY COMMENT '基金代码',
    fund_name VARCHAR(100) NOT NULL COMMENT '基金名称',
    fund_type VARCHAR(20) COMMENT '基金类型：股票型/债券型/混合型/指数型/QDII/FOF',
    manager_code VARCHAR(20) COMMENT '基金经理代码',
    manager_name VARCHAR(50) COMMENT '基金经理姓名（冗余）',
    company_code VARCHAR(20) COMMENT '基金公司代码',
    company_name VARCHAR(100) COMMENT '基金公司名称（冗余）',
    establish_date DATE COMMENT '成立日期',
    benchmark VARCHAR(200) COMMENT '业绩比较基准',
    invest_style VARCHAR(20) COMMENT '投资风格：成长/价值/平衡',
    management_fee DECIMAL(5,4) COMMENT '管理费率',
    custody_fee DECIMAL(5,4) COMMENT '托管费率',
    risk_level TINYINT COMMENT '风险等级：1-5',
    current_scale DECIMAL(15,2) COMMENT '最新规模（亿元）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，2-暂停申购，3-暂停赎回，0-清盘',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (fund_type),
    INDEX idx_manager (manager_code),
    INDEX idx_company (company_code),
    INDEX idx_risk (risk_level)
) ENGINE=InnoDB COMMENT='基金基础信息表';

-- 2. 基金净值历史表（按年分区）
CREATE TABLE fund_nav (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    nav_date DATE NOT NULL,
    unit_nav DECIMAL(8,4) NOT NULL COMMENT '单位净值',
    accum_nav DECIMAL(8,4) NOT NULL COMMENT '累计净值',
    daily_return DECIMAL(5,2) COMMENT '日涨跌幅%',
    adj_factor DECIMAL(10,6) DEFAULT 1.000000 COMMENT '复权因子',
    source VARCHAR(20) DEFAULT 'akshare' COMMENT '数据来源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fund_date (fund_code, nav_date),
    INDEX idx_date (nav_date),
    INDEX idx_code_date (fund_code, nav_date)
) ENGINE=InnoDB PARTITION BY RANGE (YEAR(nav_date)) (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p_future VALUES LESS THAN MAXVALUE
) COMMENT='基金净值历史表';

-- 3. 基金持仓明细表（季报数据）
CREATE TABLE fund_portfolio (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    stock_code VARCHAR(10) NOT NULL,
    stock_name VARCHAR(50),
    ratio DECIMAL(5,2) COMMENT '占净值比例',
    shares BIGINT COMMENT '持股数',
    market_value DECIMAL(15,2) COMMENT '持仓市值（万元）',
    report_year INT,
    report_quarter TINYINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fund_stock_quarter (fund_code, stock_code, report_year, report_quarter),
    INDEX idx_fund_date (fund_code, report_year, report_quarter)
) ENGINE=InnoDB COMMENT='基金持仓表';

-- 4. 基金评分表（每日更新）
CREATE TABLE fund_score (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    calc_date DATE NOT NULL,
    total_score INT COMMENT '总分0-100',
    quality_level CHAR(1) COMMENT '等级：A/B/C/D',
    -- 分项得分
    return_score INT COMMENT '收益得分(30)',
    risk_score INT COMMENT '风控得分(25)',
    stability_score INT COMMENT '稳定性(20)',
    scale_score INT COMMENT '规模适配(15)',
    fee_score INT COMMENT '费用得分(10)',
    -- 关键指标
    sharpe_ratio DECIMAL(6,4) COMMENT '夏普比率',
    annual_return DECIMAL(6,4) COMMENT '年化收益',
    max_drawdown DECIMAL(6,4) COMMENT '最大回撤',
    volatility DECIMAL(6,4) COMMENT '波动率',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fund_date (fund_code, calc_date),
    INDEX idx_score (total_score),
    INDEX idx_sharpe (sharpe_ratio)
) ENGINE=InnoDB COMMENT='基金评分表';

-- 5. 用户持仓组合表
CREATE TABLE user_portfolio (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    portfolio_name VARCHAR(50),
    fund_code VARCHAR(10) NOT NULL,
    holding_share DECIMAL(15,4) COMMENT '持有份额',
    holding_cost DECIMAL(10,4) COMMENT '持仓成本',
    purchase_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_fund (user_id, fund_code, purchase_date),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='用户持仓表';
```

### 5.2 关键索引策略

- **查询优化**：`fund_nav`表采用**复合索引**(fund_code, nav_date)支持快速时间序列查询
- **分区策略**：按年份分区，历史数据查询限定分区范围，避免全表扫描
- **冗余设计**：`fund_info`中冗余manager_name和company_name，避免联表查询

## 6. 接口与集成方案

### 6.1 Python与Java通信协议

**数据流转协议**：

```json
// FundNavMessage 净值消息格式
{
  "fundCode": "000001",
  "fundName": "华夏成长",
  "navDate": "2024-01-15",
  "unitNav": 1.2345,
  "accumNav": 3.4567,
  "dailyReturn": 0.52,
  "source": "akshare",
  "timestamp": "2024-01-15T19:35:00Z"
}

// FundScoreMessage 评分消息格式
{
  "fundCode": "000001",
  "calcDate": "2024-01-15",
  "totalScore": 85,
  "sharpeRatio": 1.45,
  "maxDrawdown": -0.12,
  "volatility": 0.18
}
```

### 6.2 API接口规范（Java提供）

| 接口                               | 方法     | 描述                                |
| ---------------------------------- | -------- | ----------------------------------- |
| `GET /api/funds`                   | 检索基金 | 支持keyword、type、risk、sort等参数 |
| `GET /api/funds/{code}`            | 基金详情 | 基础信息+最新净值+评分              |
| `GET /api/funds/{code}/nav`        | 净值历史 | 支持dateRange，返回时间序列         |
| `GET /api/funds/{code}/portfolio`  | 持仓分析 | 最新季报持仓+行业分布               |
| `POST /api/funds/search/advanced`  | 高级检索 | 多条件组合筛选                      |
| `GET /api/funds/recommend/quality` | 优质推荐 | 基于评分模型返回TOP N               |
| `POST /api/portfolio/analyze`      | 组合诊断 | 传入持仓列表，返回分析报告          |

## 7. 部署与运维方案

### 7.1 Docker Compose配置

```yaml
version: '3.8'
services:
  mysql-master:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_pass
      MYSQL_DATABASE: fund_db
      MYSQL_USER: fund_user
      MYSQL_PASSWORD: fund_pass
    volumes:
      - mysql_master_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  fund-collector:
    build: ./fund-collector
    depends_on:
      - mysql-master
      - rabbitmq
    environment:
      - MYSQL_HOST=mysql-master
      - RABBITMQ_HOST=rabbitmq
      - TZ=Asia/Shanghai
    # 定时任务通过宿主机的cron或k8s cronjob触发

  fund-api:
    build: ./fund-service
    ports:
      - "8080:8080"
    depends_on:
      - mysql-master
      - redis
      - rabbitmq
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - TZ=Asia/Shanghai

volumes:
  mysql_master_data:
  redis_data:
  rabbitmq_data:
```

### 7.2 监控与告警

- **数据质量监控**：Python采集异常率>5%触发告警
- **消息堆积监控**：RabbitMQ队列堆积>1000条触发扩容
- **业务指标监控**：Java服务QPS、P99延迟、错误率

------

## 8. 安全与合规

### 8.1 数据安全

- **传输加密**：MQ使用TLS，MySQL启用SSL
- **敏感数据**：用户持仓数据AES加密存储
- **访问控制**：MySQL用户最小权限原则（采集用户仅INSERT，业务用户仅SELECT/UPDATE）

### 8.2 合规边界

- **免责声明**：所有接口返回包含风险提示"历史业绩不代表未来"
- **数据授权**：akshare数据为公开数据，符合爬虫协议（遵守robots.txt，请求频率<1次/秒）
- **禁止行为**：不提供自动交易接口，不涉及投资建议算法（仅提供数据分析）

------

## 9. 实施路线图

### Phase 1: 基础数据（2周）

- [ ] Python搭建akshare采集脚本（基金列表+每日净值）
- [ ] MySQL表结构创建（fund_info, fund_nav分区表）
- [ ] Java基础项目搭建（Spring Boot + MyBatis-Plus）
- [ ] 简单REST API（基金查询、净值查询）

### Phase 2: 管道集成（1周）

- [ ] RabbitMQ部署与配置
- [ ] Python接入MQ Producer
- [ ] Java接入MQ Consumer
- [ ] 实现端到端数据流转（采集→MQ→存储→API）

### Phase 3: 核心功能（2周）

- [ ] 基金评分算法（Python计算/Java存储）
- [ ] 高级检索功能（多条件筛选、排序）
- [ ] 优质基金推荐接口
- [ ] Redis缓存层（热点数据、评分排行榜）

### Phase 4: 优化与监控（1周）

- [ ] 数据库性能优化（索引、慢查询治理）
- [ ] 异常处理与重试机制
- [ ] 监控告警系统（Prometheus + Grafana）
- [ ] 数据质量校验（完整性、一致性检查）

------

## 10. 风险与应对

| 风险点                | 影响 | 应对措施                                 |
| --------------------- | ---- | ---------------------------------------- |
| **akshare接口变更**   | 高   | 封装抽象层，预留爬虫兜底，多数据源备份   |
| **MySQL单点性能瓶颈** | 中   | 读写分离，历史数据归档，分库分表（按年） |
| **MQ消息堆积**        | 中   | 消费者横向扩展，死信队列，监控告警       |
| **基金数据延迟**      | 低   | T+1数据预期管理，实时估值用爬虫补充      |
| **反爬封禁**          | 中   | 请求频率控制，User-Agent轮换，IP代理池   |