# 基金交易决策辅助系统设计文档

## 1. 项目概述

### 1.1 项目背景

随着个人投资者对基金投资需求的增长，市场急需一个能够提供理性、数据驱动的交易决策辅助工具。本系统旨在通过量化分析和行为金融学原理，帮助投资者克服情绪化交易，实现更科学的基金投资决策。

### 1.2 核心目标

- **数据驱动**：整合多源基金数据，提供全面、及时的信息支持
- **智能分析**：通过量化指标和模型评估基金质量与市场状态
- **决策辅助**：生成可操作的投资信号和策略建议
- **风险控制**：帮助用户建立纪律化的投资执行体系

### 1.3 核心功能

- 基金数据采集与更新
- 基金多维评估与筛选
- 市场状态监控与预警
- 投资策略生成与回测
- 投资组合管理与跟踪

## 2. 系统架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                   用户层 (前端/App)                          │
├─────────────────────────────────────────────────────────────┤
│                   业务服务层 (Java Spring Boot)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │基金检索API│  │组合管理API│  │策略回测API│  │用户管理API│    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
├─────────────────────────────────────────────────────────────┤
│                   数据服务层                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                 MySQL 核心数据库                      │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐       │   │
│  │  │ 基础信息表 │ │  净值表    │ │ 指标计算表 │       │   │
│  │  └────────────┘ └────────────┘ └────────────┘       │   │
│  └──────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                   数据采集与处理层 (Python)                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐           │
│  │ 网络爬虫   │  │ AkShare API│  │ 数据清洗   │           │
│  │ (天天基金) │  │ (专业数据) │  │ 与校验     │           │
│  └────────────┘  └────────────┘  └────────────┘           │
│         │              │              │                    │
│         └──────────────┼──────────────┘                    │
│                        ▼                                   │
│                ┌────────────┐                             │
│                │ 指标计算引擎 │                             │
│                │ (Python)   │                             │
│                └────────────┘                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 架构特点

1. **混合技术栈**：Python负责数据密集型任务，Java负责高并发业务服务
2. **松耦合设计**：各层通过标准接口通信，便于独立扩展和维护
3. **数据驱动**：以MySQL为核心，确保数据一致性和可追溯性
4. **模块化**：功能模块清晰分离，支持渐进式开发

## 3. 技术栈选型

### 3.1 数据采集与处理层 (Python)

| 组件      | 选型               | 用途            | 版本            |
| --------- | ------------------ | --------------- | --------------- |
| 数据获取  | AkShare            | 专业金融数据API | 最新版          |
| 网络爬虫  | Scrapy + Requests  | 补充数据采集    | Scrapy 2.8+     |
| 数据处理  | Pandas + NumPy     | 数据清洗、计算  | Pandas 1.5+     |
| 任务调度  | Celery + Redis     | 定时任务管理    | Celery 5.2+     |
| 数据库ORM | SQLAlchemy         | MySQL操作       | SQLAlchemy 1.4+ |
| 数据校验  | Great Expectations | 数据质量检查    | 可选            |

### 3.2 业务服务层 (Java)

| 组件     | 选型            | 用途         | 版本  |
| -------- | --------------- | ------------ | ----- |
| 开发框架 | Spring Boot     | 微服务框架   | 2.7+  |
| 数据访问 | MyBatis Plus    | ORM框架      | 3.5+  |
| 缓存     | Redis           | 热点数据缓存 | 6.0+  |
| API文档  | Swagger/knife4j | 接口文档     | 3.0+  |
| 安全框架 | Spring Security | 权限控制     | 5.7+  |
| 消息队列 | RabbitMQ        | 服务间通信   | 3.10+ |

### 3.3 数据存储层

| 组件       | 选型       | 用途           | 备注           |
| ---------- | ---------- | -------------- | -------------- |
| 主数据库   | MySQL 8.0  | 核心业务数据   | 支持事务、ACID |
| 缓存数据库 | Redis 6.0+ | 会话、热点数据 | 集群部署       |
| 文件存储   | 本地/MinIO | 报表、日志     | 可选对象存储   |

### 3.4 部署与运维

| 组件   | 选型                 | 用途         |
| ------ | -------------------- | ------------ |
| 容器化 | Docker               | 应用打包     |
| 编排   | Docker Compose       | 本地开发     |
| 监控   | Prometheus + Grafana | 系统监控     |
| 日志   | ELK Stack            | 日志收集分析 |
| CI/CD  | Jenkins/GitLab CI    | 持续集成     |

## 4. 模块详细设计

### 4.1 数据采集模块 (Python)

#### 4.1.1 AkShare数据源配置

```python
# config/akshare_config.py
AKSHARE_CONFIG = {
    # 基金基础信息
    'fund_basic': {
        'function': 'fund_em_open_fund_info',
        'params': {'fund': '基金代码', 'indicator': '全部'},
        'schedule': 'daily',  # 每日更新
        'priority': 1
    },
    # 基金净值
    'fund_nav': {
        'function': 'fund_em_open_fund_daily',
        'params': {'fund': '基金代码'},
        'schedule': 'daily_after_market',  # 收盘后
        'priority': 1
    },
    # 基金经理信息
    'fund_manager': {
        'function': 'fund_manager',
        'schedule': 'weekly',
        'priority': 2
    },
    # 市场指数数据
    'index_daily': {
        'function': 'stock_zh_index_daily',
        'params': {'symbol': '指数代码'},
        'schedule': 'daily',
        'priority': 1
    }
}
```

#### 4.1.2 爬虫模块设计

```python
# spiders/eastmoney_spider.py
import scrapy
from sqlalchemy import create_engine
import pandas as pd
from datetime import datetime

class EastMoneyFundSpider(scrapy.Spider):
    name = 'eastmoney_fund'
    
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # MySQL连接
        self.engine = create_engine('mysql+pymysql://user:pass@localhost:3306/fund_db')
        # 从数据库获取需要更新的基金列表
        self.fund_codes = self.get_active_fund_codes()
        
    def start_requests(self):
        """生成爬取请求"""
        base_url = 'https://fund.eastmoney.com/{code}.html'
        for code in self.fund_codes[:100]:  # 限制数量，避免被封
            url = base_url.format(code=code)
            yield scrapy.Request(
                url=url,
                callback=self.parse_fund_detail,
                meta={'fund_code': code},
                headers=self.get_headers(),
                dont_filter=True
            )
    
    def parse_fund_detail(self, response):
        """解析基金详情页"""
        fund_code = response.meta['fund_code']
        
        # 使用XPath或CSS选择器提取数据
        data = {
            'fund_code': fund_code,
            'fund_name': response.css('.fundDetail-tit::text').get(),
            'latest_nav': response.css('.dataItem01 .dataNums span::text').get(),
            'daily_change': response.css('.dataItem02 .dataNums span::text').get(),
            'update_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }
        
        # 数据清洗和校验
        cleaned_data = self.clean_data(data)
        
        # 保存到数据库
        self.save_to_database(cleaned_data)
        
        # 如果需要，可以继续爬取持仓信息等
        holding_url = f'https://fund.eastmoney.com/f10/ccmx_{fund_code}.html'
        yield scrapy.Request(
            url=holding_url,
            callback=self.parse_holdings,
            meta={'fund_code': fund_code}
        )
    
    def clean_data(self, data):
        """数据清洗"""
        # 移除空值、格式化数字等
        if data['latest_nav']:
            data['latest_nav'] = float(data['latest_nav'].replace(',', ''))
        if data['daily_change']:
            data['daily_change'] = float(data['daily_change'].strip('%')) / 100
        return data
    
    def save_to_database(self, data):
        """保存到MySQL"""
        df = pd.DataFrame([data])
        df.to_sql('fund_daily_data', self.engine, if_exists='append', index=False)
```

#### 4.1.3 数据质量监控

```python
# quality/data_validator.py
import pandas as pd
from datetime import datetime, timedelta

class DataValidator:
    def __init__(self):
        self.rules = {
            'nav_value': {'min': 0.1, 'max': 100, 'not_null': True},
            'daily_return': {'min': -0.2, 'max': 0.2},  # 日涨跌幅在±20%内
            'update_time': {'max_delay_hours': 48}  # 数据延迟不超过48小时
        }
    
    def validate_fund_data(self, df):
        """验证基金数据质量"""
        errors = []
        
        # 检查净值范围
        nav_errors = df[(df['unit_nav'] < self.rules['nav_value']['min']) | 
                       (df['unit_nav'] > self.rules['nav_value']['max'])]
        if not nav_errors.empty:
            errors.append(f"净值异常: {len(nav_errors)}条记录")
        
        # 检查涨跌幅范围
        return_errors = df[(df['daily_return'] < self.rules['daily_return']['min']) | 
                          (df['daily_return'] > self.rules['daily_return']['max'])]
        if not return_errors.empty:
            errors.append(f"涨跌幅异常: {len(return_errors)}条记录")
        
        # 检查数据时效性
        now = datetime.now()
        time_errors = df[df['update_time'] < now - timedelta(
            hours=self.rules['update_time']['max_delay_hours'])]
        if not time_errors.empty:
            errors.append(f"数据延迟: {len(time_errors)}条记录")
        
        return errors
```

### 4.2 指标计算模块 (Python)

#### 4.2.1 核心指标计算

```python
# metrics/calculator.py
import pandas as pd
import numpy as np
from scipy import stats

class FundMetricsCalculator:
    def __init__(self, risk_free_rate=0.02):
        self.risk_free_rate = risk_free_rate  # 无风险利率，年化2%
    
    def calculate_sharpe_ratio(self, returns, period='daily'):
        """计算夏普比率"""
        if period == 'daily':
            annual_factor = np.sqrt(252)
        elif period == 'monthly':
            annual_factor = np.sqrt(12)
        else:
            annual_factor = 1
        
        excess_returns = returns - self.risk_free_rate / annual_factor
        if len(excess_returns) < 2:
            return np.nan
        
        sharpe = np.mean(excess_returns) / np.std(excess_returns) * annual_factor
        return sharpe
    
    def calculate_max_drawdown(self, nav_series):
        """计算最大回撤"""
        if len(nav_series) < 2:
            return 0
        
        # 计算累计最大值
        cumulative_max = np.maximum.accumulate(nav_series)
        # 计算回撤
        drawdown = (cumulative_max - nav_series) / cumulative_max
        return np.max(drawdown)
    
    def calculate_calmar_ratio(self, annual_return, max_drawdown):
        """计算Calmar比率"""
        if max_drawdown == 0:
            return np.nan
        return annual_return / max_drawdown
    
    def calculate_sortino_ratio(self, returns, target_return=0, period='daily'):
        """计算索提诺比率"""
        if period == 'daily':
            annual_factor = np.sqrt(252)
        else:
            annual_factor = 1
        
        # 只考虑下行风险
        downside_returns = returns[returns < target_return]
        if len(downside_returns) < 2:
            return np.nan
        
        downside_risk = np.std(downside_returns) * annual_factor
        excess_return = np.mean(returns - target_return) * annual_factor
        
        if downside_risk == 0:
            return np.nan
        return excess_return / downside_risk
    
    def calculate_all_metrics(self, fund_code, nav_data):
        """计算所有指标"""
        # 计算日收益率
        nav_data = nav_data.sort_values('nav_date')
        nav_data['daily_return'] = nav_data['unit_nav'].pct_change()
        
        # 获取不同时间窗口的数据
        one_year_data = nav_data.tail(252)  # 约1年交易日
        three_year_data = nav_data.tail(756)  # 约3年交易日
        
        metrics = {
            'fund_code': fund_code,
            'calc_date': datetime.now().date(),
            # 收益指标
            'return_1y': self._calculate_annual_return(one_year_data),
            'return_3y': self._calculate_annual_return(three_year_data),
            # 风险调整后收益
            'sharpe_ratio_1y': self.calculate_sharpe_ratio(one_year_data['daily_return'].dropna()),
            'sharpe_ratio_3y': self.calculate_sharpe_ratio(three_year_data['daily_return'].dropna()),
            # 风险指标
            'max_drawdown_1y': self.calculate_max_drawdown(one_year_data['unit_nav']),
            'max_drawdown_3y': self.calculate_max_drawdown(three_year_data['unit_nav']),
            'volatility_1y': one_year_data['daily_return'].std() * np.sqrt(252),
            # 其他指标
            'calmar_ratio_3y': self.calculate_calmar_ratio(
                self._calculate_annual_return(three_year_data),
                self.calculate_max_drawdown(three_year_data['unit_nav'])
            ),
            'sortino_ratio_1y': self.calculate_sortino_ratio(one_year_data['daily_return'].dropna())
        }
        
        return metrics
```

### 4.3 基金检索服务 (Java)

#### 4.3.1 实体类设计

```java
// entity/FundBasic.java
@Data
@TableName("fund_basic")
public class FundBasic {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("fund_code")
    private String fundCode;
    
    @TableField("fund_name")
    private String fundName;
    
    @TableField("fund_type")
    private String fundType;
    
    @TableField("establish_date")
    private Date establishDate;
    
    @TableField("fund_scale")
    private BigDecimal fundScale;
    
    @TableField("management_fee")
    private BigDecimal managementFee;
    
    @TableField("status")
    private Integer status;
    
    @TableField("update_time")
    private Date updateTime;
}

// entity/FundMetrics.java
@Data
@TableName("fund_metrics")
public class FundMetrics {
    @TableId
    private Long id;
    
    @TableField("fund_code")
    private String fundCode;
    
    @TableField("calc_date")
    private Date calcDate;
    
    @TableField("sharpe_ratio_1y")
    private BigDecimal sharpeRatio1y;
    
    @TableField("max_drawdown_3y")
    private BigDecimal maxDrawdown3y;
    
    @TableField("volatility_1y")
    private BigDecimal volatility1y;
    
    @TableField("calmar_ratio_3y")
    private BigDecimal calmarRatio3y;
}
```

#### 4.3.2 检索服务实现

```java
// service/impl/FundSearchServiceImpl.java
@Service
@Slf4j
public class FundSearchServiceImpl implements FundSearchService {
    
    @Autowired
    private FundBasicMapper fundBasicMapper;
    
    @Autowired
    private FundMetricsMapper fundMetricsMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    @Cacheable(value = "fundSearch", key = "#condition.hashCode()")
    public PageResult<FundVO> searchFunds(FundSearchCondition condition) {
        log.info("执行基金检索，条件: {}", condition);
        
        // 构建查询条件
        QueryWrapper<FundBasic> wrapper = this.buildQueryWrapper(condition);
        
        // 执行分页查询
        Page<FundBasic> page = new Page<>(condition.getPageNo(), condition.getPageSize());
        Page<FundBasic> resultPage = fundBasicMapper.selectPage(page, wrapper);
        
        // 转换为VO并补充指标数据
        List<FundVO> fundVOs = this.convertToVO(resultPage.getRecords());
        
        return new PageResult<>(
            fundVOs,
            resultPage.getTotal(),
            condition.getPageNo(),
            condition.getPageSize()
        );
    }
    
    private QueryWrapper<FundBasic> buildQueryWrapper(FundSearchCondition condition) {
        QueryWrapper<FundBasic> wrapper = new QueryWrapper<>();
        
        // 基础条件
        if (StringUtils.isNotBlank(condition.getFundCode())) {
            wrapper.eq("fund_code", condition.getFundCode());
        }
        if (StringUtils.isNotBlank(condition.getFundName())) {
            wrapper.like("fund_name", condition.getFundName());
        }
        if (StringUtils.isNotBlank(condition.getFundType())) {
            wrapper.eq("fund_type", condition.getFundType());
        }
        
        // 指标条件（通过子查询实现）
        if (condition.getMinSharpeRatio() != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM fund_metrics fm WHERE " +
                         "fm.fund_code = fund_basic.fund_code AND " +
                         "fm.calc_date = (SELECT MAX(calc_date) FROM fund_metrics WHERE fund_code = fm.fund_code) AND " +
                         "fm.sharpe_ratio_1y >= {0})", condition.getMinSharpeRatio());
        }
        
        if (condition.getMaxDrawdown() != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM fund_metrics fm WHERE " +
                         "fm.fund_code = fund_basic.fund_code AND " +
                         "fm.calc_date = (SELECT MAX(calc_date) FROM fund_metrics WHERE fund_code = fm.fund_code) AND " +
                         "fm.max_drawdown_3y <= {0})", condition.getMaxDrawdown());
        }
        
        // 排序
        if (StringUtils.isNotBlank(condition.getOrderBy())) {
            if ("sharpe_ratio".equals(condition.getOrderBy())) {
                wrapper.orderByDesc("(SELECT sharpe_ratio_1y FROM fund_metrics fm WHERE " +
                                   "fm.fund_code = fund_basic.fund_code AND " +
                                   "fm.calc_date = (SELECT MAX(calc_date) FROM fund_metrics WHERE fund_code = fm.fund_code))");
            } else if ("return_1y".equals(condition.getOrderBy())) {
                wrapper.orderByDesc("(SELECT return_1y FROM fund_metrics fm WHERE " +
                                   "fm.fund_code = fund_basic.fund_code AND " +
                                   "fm.calc_date = (SELECT MAX(calc_date) FROM fund_metrics WHERE fund_code = fm.fund_code))");
            }
        }
        
        return wrapper;
    }
    
    private List<FundVO> convertToVO(List<FundBasic> fundBasics) {
        return fundBasics.stream().map(fund -> {
            FundVO vo = new FundVO();
            BeanUtils.copyProperties(fund, vo);
            
            // 获取最新指标
            FundMetrics metrics = fundMetricsMapper.selectLatestByFundCode(fund.getFundCode());
            if (metrics != null) {
                vo.setSharpeRatio(metrics.getSharpeRatio1y());
                vo.setMaxDrawdown(metrics.getMaxDrawdown3y());
                vo.setVolatility(metrics.getVolatility1y());
                vo.setCalmarRatio(metrics.getCalmarRatio3y());
            }
            
            // 计算综合评分（示例）
            vo.setCompositeScore(this.calculateCompositeScore(vo));
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    private BigDecimal calculateCompositeScore(FundVO fund) {
        // 简单的加权评分模型
        BigDecimal score = BigDecimal.ZERO;
        
        if (fund.getSharpeRatio() != null) {
            score = score.add(fund.getSharpeRatio().multiply(new BigDecimal("0.3")));
        }
        if (fund.getMaxDrawdown() != null) {
            // 最大回撤越小越好，所以用1-回撤
            score = score.add(BigDecimal.ONE.subtract(fund.getMaxDrawdown())
                          .multiply(new BigDecimal("0.25")));
        }
        if (fund.getCalmarRatio() != null) {
            score = score.add(fund.getCalmarRatio().multiply(new BigDecimal("0.2")));
        }
        // 其他指标...
        
        return score.setScale(4, RoundingMode.HALF_UP);
    }
}
```

#### 4.3.3 REST API设计

```java
// controller/FundController.java
@RestController
@RequestMapping("/api/v1/funds")
@Api(tags = "基金服务")
public class FundController {
    
    @Autowired
    private FundSearchService fundSearchService;
    
    @PostMapping("/search")
    @ApiOperation("基金多条件检索")
    public ApiResponse<PageResult<FundVO>> searchFunds(
            @RequestBody @Valid FundSearchCondition condition) {
        return ApiResponse.success(fundSearchService.searchFunds(condition));
    }
    
    @GetMapping("/{fundCode}")
    @ApiOperation("获取基金详情")
    @Cacheable(value = "fundDetail", key = "#fundCode")
    public ApiResponse<FundDetailVO> getFundDetail(
            @PathVariable @NotBlank String fundCode) {
        FundDetailVO detail = fundSearchService.getFundDetail(fundCode);
        return ApiResponse.success(detail);
    }
    
    @GetMapping("/{fundCode}/nav-history")
    @ApiOperation("获取基金净值历史")
    public ApiResponse<List<FundNavVO>> getNavHistory(
            @PathVariable String fundCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        List<FundNavVO> navHistory = fundSearchService.getNavHistory(fundCode, startDate, endDate);
        return ApiResponse.success(navHistory);
    }
    
    @GetMapping("/screening/conditions")
    @ApiOperation("获取筛选条件选项")
    public ApiResponse<Map<String, List<String>>> getScreeningConditions() {
        Map<String, List<String>> conditions = new HashMap<>();
        
        // 基金类型
        conditions.put("fundTypes", Arrays.asList(
            "股票型", "混合型", "债券型", "指数型", "QDII", "FOF"
        ));
        
        // 风险等级
        conditions.put("riskLevels", Arrays.asList(
            "低风险", "中低风险", "中风险", "中高风险", "高风险"
        ));
        
        // 排序选项
        conditions.put("orderOptions", Arrays.asList(
            "sharpe_ratio", "return_1y", "max_drawdown", "calmar_ratio", "composite_score"
        ));
        
        return ApiResponse.success(conditions);
    }
}
```

## 5. 数据库设计

### 5.1 核心表结构

#### 5.1.1 基金基本信息表

```sql
-- 创建主表
CREATE TABLE `fund_nav` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(10) NOT NULL COMMENT '基金代码',
  `nav_date` date NOT NULL COMMENT '净值日期',
  `unit_nav` decimal(10,4) NOT NULL COMMENT '单位净值',
  `accumulated_nav` decimal(10,4) DEFAULT NULL COMMENT '累计净值',
  `daily_return` decimal(8,4) DEFAULT NULL COMMENT '日增长率',
  `adjust_nav` decimal(10,4) DEFAULT NULL COMMENT '复权净值',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`, `nav_date`),
  UNIQUE KEY `uk_fund_date` (`fund_code`, `nav_date`),
  KEY `idx_nav_date` (`nav_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金净值表'
PARTITION BY RANGE (YEAR(nav_date)) (
  PARTITION p2023 VALUES LESS THAN (2024),
  PARTITION p2024 VALUES LESS THAN (2025),
  PARTITION p2025 VALUES LESS THAN (2026),
  PARTITION p2026 VALUES LESS THAN (2027),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### 5.1.2 基金净值表（分区表设计）

```sql
-- 创建主表
CREATE TABLE `fund_nav` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(10) NOT NULL COMMENT '基金代码',
  `nav_date` date NOT NULL COMMENT '净值日期',
  `unit_nav` decimal(10,4) NOT NULL COMMENT '单位净值',
  `accumulated_nav` decimal(10,4) DEFAULT NULL COMMENT '累计净值',
  `daily_return` decimal(8,4) DEFAULT NULL COMMENT '日增长率',
  `adjust_nav` decimal(10,4) DEFAULT NULL COMMENT '复权净值',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`, `nav_date`),
  UNIQUE KEY `uk_fund_date` (`fund_code`, `nav_date`),
  KEY `idx_nav_date` (`nav_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金净值表'
PARTITION BY RANGE (YEAR(nav_date)) (
  PARTITION p2023 VALUES LESS THAN (2024),
  PARTITION p2024 VALUES LESS THAN (2025),
  PARTITION p2025 VALUES LESS THAN (2026),
  PARTITION p2026 VALUES LESS THAN (2027),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### 5.1.3 基金指标表

```sql
CREATE TABLE `fund_metrics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(10) NOT NULL COMMENT '基金代码',
  `calc_date` date NOT NULL COMMENT '计算日期',
  `return_1y` decimal(8,4) DEFAULT NULL COMMENT '近1年收益率',
  `return_3y` decimal(8,4) DEFAULT NULL COMMENT '近3年收益率',
  `return_5y` decimal(8,4) DEFAULT NULL COMMENT '近5年收益率',
  `sharpe_ratio_1y` decimal(10,4) DEFAULT NULL COMMENT '近1年夏普比率',
  `sharpe_ratio_3y` decimal(10,4) DEFAULT NULL COMMENT '近3年夏普比率',
  `max_drawdown_1y` decimal(8,4) DEFAULT NULL COMMENT '近1年最大回撤',
  `max_drawdown_3y` decimal(8,4) DEFAULT NULL COMMENT '近3年最大回撤',
  `volatility_1y` decimal(8,4) DEFAULT NULL COMMENT '近1年波动率',
  `calmar_ratio_3y` decimal(10,4) DEFAULT NULL COMMENT '近3年Calmar比率',
  `sortino_ratio_1y` decimal(10,4) DEFAULT NULL COMMENT '近1年索提诺比率',
  `alpha_1y` decimal(8,4) DEFAULT NULL COMMENT '近1年阿尔法',
  `beta_1y` decimal(8,4) DEFAULT NULL COMMENT '近1年贝塔',
  `tracking_error_1y` decimal(8,4) DEFAULT NULL COMMENT '近1年跟踪误差',
  `information_ratio_1y` decimal(10,4) DEFAULT NULL COMMENT '近1年信息比率',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fund_calc_date` (`fund_code`, `calc_date`),
  KEY `idx_calc_date` (`calc_date`),
  KEY `idx_sharpe_ratio` (`sharpe_ratio_1y`),
  KEY `idx_max_drawdown` (`max_drawdown_3y`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金衍生指标表';
```

#### 5.1.4 基金经理表

```sql
CREATE TABLE `fund_manager` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `manager_code` varchar(20) DEFAULT NULL COMMENT '基金经理代码',
  `manager_name` varchar(50) NOT NULL COMMENT '基金经理姓名',
  `company` varchar(100) DEFAULT NULL COMMENT '所属公司',
  `start_date` date DEFAULT NULL COMMENT '任职开始日期',
  `end_date` date DEFAULT NULL COMMENT '任职结束日期',
  `total_funds` int(11) DEFAULT NULL COMMENT '管理基金总数',
  `total_scale` decimal(15,2) DEFAULT NULL COMMENT '管理总规模(亿元)',
  `best_return` decimal(8,4) DEFAULT NULL COMMENT '最佳任职回报',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_manager_name` (`manager_name`),
  KEY `idx_company` (`company`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金经理信息表';
```

#### 5.1.5 基金持仓表

```sql
CREATE TABLE `fund_holding` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(10) NOT NULL COMMENT '基金代码',
  `report_date` date NOT NULL COMMENT '报告期',
  `stock_code` varchar(10) DEFAULT NULL COMMENT '股票代码',
  `stock_name` varchar(100) DEFAULT NULL COMMENT '股票名称',
  `holding_amount` bigint(20) DEFAULT NULL COMMENT '持股数量(股)',
  `holding_value` decimal(15,2) DEFAULT NULL COMMENT '持股市值(元)',
  `holding_ratio` decimal(8,4) DEFAULT NULL COMMENT '占净值比例',
  `holding_type` varchar(20) DEFAULT NULL COMMENT '持仓类型:股票/债券/现金',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fund_report_stock` (`fund_code`, `report_date`, `stock_code`),
  KEY `idx_report_date` (`report_date`),
  KEY `idx_stock_code` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金持仓明细表';
```

### 5.2 索引优化策略

1. **复合索引设计**：

```sql
-- 基金检索常用查询
CREATE INDEX idx_fund_search ON fund_basic(fund_type, status, fund_scale);

-- 净值查询优化
CREATE INDEX idx_nav_query ON fund_nav(fund_code, nav_date DESC);

-- 指标排序优化
CREATE INDEX idx_metrics_sort ON fund_metrics(sharpe_ratio_1y DESC, max_drawdown_3y ASC);
```

**分区策略**：

- `fund_nav`表按年份分区，便于历史数据归档和查询
- 超过3年的净值数据可迁移到历史表或数据仓库



**查询优化建议**：

```sql
-- 避免全表扫描
EXPLAIN SELECT * FROM fund_nav WHERE fund_code = '000001' AND nav_date >= '2025-01-01';

-- 使用覆盖索引
CREATE INDEX idx_covering ON fund_metrics(fund_code, calc_date, sharpe_ratio_1y, max_drawdown_3y);
```

## 6. 接口设计规范

### 6.1 API统一响应格式

```java
// common/ApiResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
```

#### 6.2 基金检索接口规范

```yaml
# 请求参数
FundSearchCondition:
  type: object
  properties:
    fundCode:
      type: string
      description: 基金代码
    fundName:
      type: string
      description: 基金名称（模糊匹配）
    fundType:
      type: string
      description: 基金类型
    minSharpeRatio:
      type: number
      format: float
      description: 最小夏普比率
    maxDrawdown:
      type: number
      format: float
      description: 最大回撤上限
    minAnnualReturn:
      type: number
      format: float
      description: 最小年化收益率
    orderBy:
      type: string
      enum: [sharpe_ratio, return_1y, max_drawdown, calmar_ratio]
      description: 排序字段
    pageNo:
      type: integer
      default: 1
    pageSize:
      type: integer
      default: 20

# 响应数据
FundVO:
  type: object
  properties:
    fundCode:
      type: string
    fundName:
      type: string
    fundType:
      type: string
    establishDate:
      type: string
      format: date
    fundScale:
      type: number
      format: float
    sharpeRatio:
      type: number
      format: float
    maxDrawdown:
      type: number
      format: float
    annualReturn:
      type: number
      format: float
    compositeScore:
      type: number
      format: float
```

## 7. 部署方案

### 7.1 开发环境部署

```yaml
# docker-compose-dev.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: fund-mysql-dev
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: fund_db
      MYSQL_USER: fund_user
      MYSQL_PASSWORD: fund_pass123
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:6.2-alpine
    container_name: fund-redis-dev
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  python-worker:
    build:
      context: ./data-pipeline
      dockerfile: Dockerfile.python
    container_name: fund-python-worker
    depends_on:
      - mysql
      - redis
    volumes:
      - ./data-pipeline:/app
      - ./logs:/app/logs
    environment:
      - REDIS_HOST=redis
      - MYSQL_HOST=mysql
    command: celery -A tasks worker --loglevel=info

  java-app:
    build:
      context: ./fund-service
      dockerfile: Dockerfile.java
    container_name: fund-java-app
    depends_on:
      - mysql
      - redis
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=mysql
      - REDIS_HOST=redis
    volumes:
      - ./fund-service:/app

volumes:
  mysql-data:
  redis-data:
```

### 7.2 生产环境部署建议

#### 7.2.1 服务器规划

| 服务器         | 配置建议             | 数量     | 用途                 |
| -------------- | -------------------- | -------- | -------------------- |
| 应用服务器     | 4核8G内存，100G SSD  | 2+       | Java Spring Boot应用 |
| 数据采集服务器 | 4核8G内存，200G SSD  | 1        | Python爬虫和数据处理 |
| MySQL数据库    | 8核16G内存，500G SSD | 主从架构 | 核心数据存储         |
| Redis缓存      | 4核8G内存，50G内存   | 哨兵模式 | 缓存和会话           |
| 文件服务器     | 4核8G内存，1T HDD    | 1        | 日志和报表存储       |

#### 7.2.2 高可用设计

```yaml
# 生产环境MySQL主从配置
# 主库 my.cnf
[mysqld]
server-id=1
log-bin=mysql-bin
binlog-format=ROW
gtid-mode=ON
enforce-gtid-consistency=ON

# 从库 my.cnf
[mysqld]
server-id=2
relay-log=mysql-relay-bin
read-only=1
super-read-only=1
```

#### 7.2.3 监控告警配置

```yaml
# prometheus/prometheus.yml
scrape_configs:
  - job_name: 'fund-java-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['java-app-1:8080', 'java-app-2:8080']
        labels:
          application: 'fund-service'
  
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

# 告警规则
groups:
  - name: fund-service-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status="500"}[5m]) > 0.01
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "高错误率: {{ $labels.instance }}"
          
      - alert: SlowResponse
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
```

## 8. 风险与注意事项

### 8.1 技术风险

1. **数据源稳定性**：
   - AkShare API可能变更或限制频率
   - 网站爬虫可能因反爬机制失效
   - 解决方案：建立多数据源备份，监控数据更新状态
2. **系统性能**：
   - 净值数据量增长迅速（预计每年新增1000万+记录）
   - 复杂指标计算耗时
   - 解决方案：分区表、读写分离、异步计算
3. **数据一致性**：
   - Python和Java服务间数据同步延迟
   - 解决方案：使用消息队列确保最终一致性

### 8.2 合规风险

1. **数据采集合规**：
   - 遵守网站robots.txt协议
   - 控制爬取频率（建议≥5秒/请求）
   - 添加User-Agent标识
2. **投资建议边界**：
   - 系统必须明确是"辅助决策工具"
   - 不能承诺收益或提供具体买卖建议
   - 必须包含充分的风险提示
3. **用户数据安全**：
   - 用户持仓等敏感数据加密存储
   - 遵守GDPR等数据保护法规
   - 定期安全审计

### 8.3 业务风险

1. **策略失效风险**：
   - 历史有效的策略未来可能失效
   - 解决方案：多策略组合、定期回测评估
2. **数据滞后性**：
   - 基金持仓数据严重滞后（季报）
   - 解决方案：使用高频数据辅助判断，明确提示数据时效
3. **用户行为风险**：
   - 用户可能过度依赖系统
   - 解决方案：加强投资者教育，设置风险承受能力评估

## 9. 开发计划（建议）

### 第一阶段：MVP版本（4-6周）

1. **数据管道搭建**（2周）
   - AkShare基础数据采集
   - MySQL表结构设计
   - 基础数据清洗入库
2. **核心检索功能**（2周）
   - Java Spring Boot项目初始化
   - 基金基础信息查询API
   - 简单条件筛选
3. **基础指标计算**（1周）
   - 收益率、波动率等基础指标
   - 定时计算任务
4. **前端展示**（1周）
   - 简单Web界面
   - 基金列表和详情展示

### 第二阶段：功能完善（8-10周）

1. **数据源扩展**（2周）

   - 增加天天基金爬虫
   - 补充持仓、经理等详细信息
   - 数据质量监控

2. 

   **高级检索功能**（3周）

   - 

     多维度复合筛选

   - 

     排序和分页优化

   - 

     缓存策略实现

3. **指标体系完善**（2周）

   - 夏普比率、最大回撤等高级指标
   - 基金评分模型
   - 批量计算优化

4. **用户体验优化**（1周）

   - 响应式设计
   - 性能优化
   - 错误处理和提示

### 第三阶段：智能化扩展（持续迭代）

1. **策略引擎**（4周+）
   - 定投策略实现
   - 回测框架搭建
   - 信号生成系统
2. **个性化推荐**（4周+）
   - 用户画像构建
   - 个性化基金推荐
   - 风险适配模型
3. **移动端应用**（6周+）
   - 移动App开发
   - 推送通知
   - 离线功能

## 10. 总结

本设计文档提供了一个完整的基金交易决策辅助系统架构方案，采用Python+Java混合技术栈，充分发挥各自优势：

1. **Python端**：专注于数据采集、清洗和指标计算，利用AkShare和爬虫获取多源数据
2. **Java端**：构建稳定、高性能的业务服务，提供基金检索、组合管理等核心功能
3. **MySQL**：作为核心数据存储