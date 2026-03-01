## 基金交易决策辅助系统 - 设计文档

## 一、文档概述

### 1.1 文档目的

本文档为个人使用的基金交易决策辅助系统提供完整的设计方案，明确系统定位、核心功能、技术架构、实施计划等核心内容，作为系统开发、落地的核心指导依据。

### 1.2 系统定位

本系统为**个人基金交易决策辅助工具**，不涉及自动交易、不对外提供投资建议，核心目标是：

- 替代人工完成基金数据的采集、整理工作；
- 通过标准化指标计算和规则筛选，输出基金买入 / 持有 / 卖出的参考信号；
- 实现基金检索、优质基金筛选、交易记录复盘等功能，辅助个人做出理性的基金交易决策。

## 二、系统核心需求

### 2.1 功能需求

| 功能模块         | 核心需求                                                     |
| ---------------- | ------------------------------------------------------------ |
| 数据采集模块     | 1. 爬取天天基金 / 东方财富网的基金基础信息、业绩数据；2. 通过 AKShare 获取专业的基金估值、指数分位数据；3. 定时自动更新数据并写入 MySQL |
| 数据存储模块     | 1. 基于 MySQL 存储标准化的基金基础信息、业绩数据、交易记录；2. 保证数据唯一性和更新时效性 |
| 基金检索模块     | 1. 支持按基金代码 / 名称 / 基金经理 / 基金公司模糊搜索；2. 支持多维度筛选（基金类型、规模等） |
| 优质基金筛选模块 | 1. 基于预设规则（收益率、最大回撤、夏普比率等）筛选优质基金；2. 支持自定义筛选条件 |
| 决策信号模块     | 1. 基于估值分位、回撤、收益率等指标生成买入 / 持有 / 卖出参考信号；2. 信号规则可配置 |
| 交易记录与复盘   | 1. 手动录入交易记录（买入 / 卖出时间、金额、费率）；2. 计算单只 / 组合基金收益；3. 复盘决策信号命中率 |
| 可视化模块       | 1. 展示基金净值走势、收益率曲线、最大回撤对比；2. 展示估值分位、持仓行业分布等 |

### 2.2 非功能需求

- **易用性**：操作流程简单，检索 / 筛选结果直观展示；
- **数据准确性**：爬取的数据需校验，重复数据自动覆盖；
- **时效性**：基金净值、收益率等核心数据每日收盘后更新；
- **稳定性**：爬虫脚本具备异常重试机制，Java 业务层兼容数据缺失场景；
- **扩展性**：支持新增数据源（如基金公司年报）、新增筛选规则（如定投适配）。

## 三、技术架构设计

### 3.1 整体架构 

```
flowchart TB
    A[数据源] -->|Python采集| B[数据采集层]
    A1[天天基金网] --> A
    A2[东方财富网] --> A
    A3[AKShare API] --> A
    B -->|数据清洗/标准化| C[MySQL数据库]
    C -->|数据读取| D[Java业务层]
    D -->|功能实现| E[应用层]
    E1[基金检索] --> E
    E2[优质基金筛选] --> E
    E3[决策信号生成] --> E
    E4[交易记录复盘] --> E
    E --> F[可视化展示层]
    G[定时调度器] -->|触发数采| B
    G -->|触发数据更新| D
```

### 3.2 技术栈选型

| 分层        | 技术选型                                 | 选型说明                                                     |
| ----------- | ---------------------------------------- | ------------------------------------------------------------ |
| 数据采集层  | Python 3.9+                              | - 爬虫：Requests + BeautifulSoup（轻量）/Scrapy（进阶）- 数据处理：Pandas- MySQL 对接：PyMySQL/SQLAlchemy- 第三方 API：AKShare- 定时调度：APScheduler |
| 数据存储层  | MySQL 8.0                                | 支持结构化数据存储、复杂查询，跨 Python/Java 语言兼容，易维护 |
| Java 业务层 | Java 11 + Spring Boot 2.7 + MyBatis-Plus | 利用开发者熟悉的技术栈，提升业务逻辑开发效率，MyBatis-Plus 简化数据库操作 |
| 可视化层    | ECharts + Thymeleaf（可选）              | 前端可视化展示，也可先用 Python 的 Matplotlib/Seaborn 做基础可视化 |
| 部署环境    | Windows/Linux                            | 个人使用优先选择熟悉的操作系统，Linux 更适合定时任务长期运行 |



## 四、数据库设计

### 4.1 核心表结构

#### 4.1.1 基金基础信息表（fund_basic）

| 字段名         | 字段类型      | 主键 / 索引 | 注释                          |
| -------------- | ------------- | ----------- | ----------------------------- |
| fund_code      | VARCHAR(20)   | 主键        | 基金代码（唯一标识）          |
| fund_name      | VARCHAR(100)  | 普通索引    | 基金名称                      |
| fund_type      | VARCHAR(50)   | 普通索引    | 基金类型（股票型 / 混合型等） |
| fund_company   | VARCHAR(100)  | 普通索引    | 基金公司                      |
| fund_manager   | VARCHAR(50)   | 普通索引    | 基金经理                      |
| scale          | DECIMAL(10,2) | -           | 基金规模（亿元）              |
| establish_date | DATE          | -           | 成立日期                      |
| create_time    | DATETIME      | -           | 数据创建时间                  |
| update_time    | DATETIME      | -           | 数据更新时间                  |

#### 4.1.2 基金业绩数据表（fund_performance）

| 字段名        | 字段类型              | 主键 / 索引  | 注释                         |
| ------------- | --------------------- | ------------ | ---------------------------- |
| id            | BIGINT                | 主键（自增） | 主键 ID                      |
| fund_code     | VARCHAR(20)           | 联合索引     | 基金代码                     |
| nav           | DECIMAL(10,4)         | -            | 单位净值                     |
| total_nav     | DECIMAL(10,4)         | -            | 累计净值                     |
| return_1m     | DECIMAL(5,2)          | -            | 近 1 月收益率（%）           |
| return_3m     | DECIMAL(5,2)          | -            | 近 3 月收益率（%）           |
| return_1y     | DECIMAL(5,2)          | 普通索引     | 近 1 年收益率（%）           |
| max_drawdown  | DECIMAL(5,2)          | -            | 最大回撤（%）                |
| sharpe_ratio  | DECIMAL(4,2)          | -            | 夏普比率                     |
| pe_percentile | INT                   | -            | PE 估值分位（0-100）         |
| pb_percentile | INT                   | -            | PB 估值分位（0-100）         |
| data_date     | DATE                  | 联合索引     | 数据日期                     |
| update_time   | DATETIME              | -            | 数据更新时间                 |
| 联合索引      | fund_code + data_date | 唯一索引     | 避免同一基金同一日期重复数据 |

#### 4.1.3 交易记录表（fund_trade_record）

| 字段名       | 字段类型      | 主键 / 索引  | 注释                      |
| ------------ | ------------- | ------------ | ------------------------- |
| id           | BIGINT        | 主键（自增） | 主键 ID                   |
| fund_code    | VARCHAR(20)   | 普通索引     | 基金代码                  |
| trade_type   | VARCHAR(10)   | 普通索引     | 交易类型（买入 / 卖出）   |
| trade_amount | DECIMAL(10,2) | -            | 交易金额（元）            |
| trade_fee    | DECIMAL(5,2)  | -            | 交易手续费（元）          |
| trade_date   | DATE          | 普通索引     | 交易日期                  |
| remark       | VARCHAR(200)  | -            | 交易备注（如 “定投买入”） |
| create_time  | DATETIME      | -            | 记录创建时间              |

### 4.2 数据库连接配置

Python 侧：通过 PyMySQL 配置 MySQL 连接参数（主机、端口、用户名、密码、数据库名）；

Java 侧：通过 Spring Boot 的 application.yml 配置 MyBatis-Plus 连接参数，示例：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/fund_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.fund.entity
  configuration:
    map-underscore-to-camel-case: true # 下划线转驼峰
```

## 五、核心模块详细设计

### 5.1 数据采集模块

#### 5.1.1 核心职责

- 爬取天天基金 / 东方财富网的基金基础信息、业绩数据；
- 通过 AKShare 获取基金估值、指数分位等专业数据；
- 数据清洗（处理空值、异常值、格式转换）；
- 标准化写入 MySQL，重复数据自动覆盖。

#### 5.1.2 核心代码示例（Python）

```python
import akshare as ak
import pymysql
import pandas as pd
from datetime import datetime, timedelta

# ---------------------- 配置项 ----------------------
MYSQL_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '你的MySQL密码',
    'db': 'fund_system',
    'charset': 'utf8mb4'
}
# ---------------------- 数据采集 ----------------------
def crawl_fund_basic_info(fund_code):
    """爬取基金基础信息（基于AKShare）"""
    # AKShare获取基金基本信息
    fund_info_df = ak.fund_info_em(fund_code=fund_code)
    # 数据清洗：提取核心字段
    basic_info = {
        'fund_code': fund_code,
        'fund_name': fund_info_df.loc[fund_info_df['item'] == '基金全称', 'value'].iloc[0],
        'fund_type': fund_info_df.loc[fund_info_df['item'] == '基金类型', 'value'].iloc[0],
        'fund_company': fund_info_df.loc[fund_info_df['item'] == '基金公司', 'value'].iloc[0],
        'fund_manager': fund_info_df.loc[fund_info_df['item'] == '基金经理', 'value'].iloc[0],
        'scale': float(fund_info_df.loc[fund_info_df['item'] == '基金规模', 'value'].iloc[0].replace('亿元', '')),
        'establish_date': fund_info_df.loc[fund_info_df['item'] == '成立日期', 'value'].iloc[0],
        'update_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    }
    return basic_info

def get_fund_performance_data(fund_code):
    """获取基金业绩数据（基于AKShare）"""
    # 获取近1年净值数据
    nav_df = ak.fund_net_value_em(fund_code=fund_code, start_date=(datetime.now() - timedelta(days=365)).strftime('%Y-%m-%d'), end_date=datetime.now().strftime('%Y-%m-%d'))
    # 计算核心业绩指标（示例）
    nav_list = nav_df['净值'].tolist()
    max_drawdown = round((max(nav_list) - min(nav_list)) / max(nav_list) * 100, 2)  # 最大回撤
    return_1y = round((nav_list[-1] - nav_list[0]) / nav_list[0] * 100, 2)        # 近1年收益率
    
    performance_data = {
        'fund_code': fund_code,
        'nav': nav_list[-1],
        'total_nav': nav_list[-1],  # 示例：累计净值暂用单位净值替代，可根据实际数据调整
        'return_1m': round((nav_list[-1] - nav_list[-30]) / nav_list[-30] * 100, 2) if len(nav_list) >= 30 else 0.0,
        'return_1y': return_1y,
        'max_drawdown': max_drawdown,
        'data_date': datetime.now().strftime('%Y-%m-%d'),
        'update_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    }
    return performance_data

# ---------------------- 数据写入MySQL ----------------------
def insert_mysql(data, table_name):
    """写入MySQL，重复数据更新"""
    conn = pymysql.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()
    try:
        if table_name == 'fund_basic':
            sql = """
            INSERT INTO fund_basic (fund_code, fund_name, fund_type, fund_company, fund_manager, scale, establish_date, update_time)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE 
                fund_name = VALUES(fund_name),
                fund_type = VALUES(fund_type),
                fund_company = VALUES(fund_company),
                fund_manager = VALUES(fund_manager),
                scale = VALUES(scale),
                update_time = VALUES(update_time)
            """
            cursor.execute(sql, (
                data['fund_code'], data['fund_name'], data['fund_type'],
                data['fund_company'], data['fund_manager'], data['scale'],
                data['establish_date'], data['update_time']
            ))
        elif table_name == 'fund_performance':
            sql = """
            INSERT INTO fund_performance (fund_code, nav, total_nav, return_1m, return_1y, max_drawdown, data_date, update_time)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE 
                nav = VALUES(nav),
                total_nav = VALUES(total_nav),
                return_1m = VALUES(return_1m),
                return_1y = VALUES(return_1y),
                max_drawdown = VALUES(max_drawdown),
                update_time = VALUES(update_time)
            """
            cursor.execute(sql, (
                data['fund_code'], data['nav'], data['total_nav'],
                data['return_1m'], data['return_1y'], data['max_drawdown'],
                data['data_date'], data['update_time']
            ))
        conn.commit()
        print(f"基金{data['fund_code']}数据写入成功")
    except Exception as e:
        conn.rollback()
        print(f"写入失败：{e}")
    finally:
        cursor.close()
        conn.close()

# ---------------------- 执行采集流程 ----------------------
if __name__ == '__main__':
    # 示例：采集易方达蓝筹精选（005827）数据
    fund_code = '005827'
    # 1. 采集基础信息并写入
    basic_info = crawl_fund_basic_info(fund_code)
    insert_mysql(basic_info, 'fund_basic')
    # 2. 采集业绩数据并写入
    performance_data = get_fund_performance_data(fund_code)
    insert_mysql(performance_data, 'fund_performance')
```

#### 5.1.3 定时调度配置

通过 APScheduler 实现每日收盘后自动采集：

```python
from apscheduler.schedulers.blocking import BlockingScheduler

def scheduled_crawl():
    """定时采集任务：可配置多只基金代码"""
    fund_codes = ['005827', '161725', '001714']
    for code in fund_codes:
        basic_info = crawl_fund_basic_info(code)
        insert_mysql(basic_info, 'fund_basic')
        performance_data = get_fund_performance_data(code)
        insert_mysql(performance_data, 'fund_performance')

if __name__ == '__main__':
    # 配置调度器：每天18:00（收盘后）执行采集
    scheduler = BlockingScheduler()
    scheduler.add_job(scheduled_crawl, 'cron', hour=18, minute=0)
    print("定时采集任务已启动，每天18:00执行...")
    scheduler.start()
```

### 5.2 基金检索与优质筛选模块（Java）

#### 5.2.1 实体类定义

```java
// 基金基础信息实体
@Data
@TableName("fund_basic")
public class FundBasic {
    @TableId(value = "fund_code")
    private String fundCode;
    private String fundName;
    private String fundType;
    private String fundCompany;
    private String fundManager;
    private BigDecimal scale;
    private Date establishDate;
    private Date createTime;
    private Date updateTime;
}

// 基金业绩实体
@Data
@TableName("fund_performance")
public class FundPerformance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private BigDecimal nav;
    private BigDecimal totalNav;
    private BigDecimal return1m;
    private BigDecimal return1y;
    private BigDecimal maxDrawdown;
    private Integer pePercentile;
    private Date dataDate;
    private Date updateTime;
}

// 检索结果VO
@Data
public class FundVO {
    private String fundCode;
    private String fundName;
    private String fundType;
    private String fundManager;
    private BigDecimal scale;
    private BigDecimal return1y;
    private BigDecimal maxDrawdown;
    private Integer pePercentile;
}
```

#### 5.2.2 Mapper 层

```java
// FundBasicMapper
public interface FundBasicMapper extends BaseMapper<FundBasic> {
    /**
     * 模糊搜索基金（联表查询基础信息+最新业绩）
     */
    @Select("""
        SELECT b.fund_code, b.fund_name, b.fund_type, b.fund_manager, b.scale,
               p.return_1y, p.max_drawdown, p.pe_percentile
        FROM fund_basic b
        LEFT JOIN (
            SELECT * FROM fund_performance 
            WHERE data_date = (SELECT MAX(data_date) FROM fund_performance)
        ) p ON b.fund_code = p.fund_code
        WHERE b.fund_code LIKE CONCAT('%', #{keyword}, '%')
           OR b.fund_name LIKE CONCAT('%', #{keyword}, '%')
           OR b.fund_manager LIKE CONCAT('%', #{keyword}, '%')
           OR b.fund_company LIKE CONCAT('%', #{keyword}, '%')
        """)
    List<FundVO> searchFund(@Param("keyword") String keyword);

    /**
     * 筛选优质基金
     */
    @Select("""
        SELECT b.fund_code, b.fund_name, b.fund_type, b.fund_manager, b.scale,
               p.return_1y, p.max_drawdown, p.pe_percentile
        FROM fund_basic b
        LEFT JOIN (
            SELECT * FROM fund_performance 
            WHERE data_date = (SELECT MAX(data_date) FROM fund_performance)
        ) p ON b.fund_code = p.fund_code
        WHERE (b.fund_type LIKE CONCAT('%', #{fundType}, '%') OR #{fundType} IS NULL)
          AND p.return_1y >= #{minReturn1y}
          AND p.max_drawdown <= #{maxDrawdown}
          AND b.scale >= #{minScale}
        ORDER BY p.return_1y DESC
        """)
    List<FundVO> filterHighQualityFunds(
            @Param("fundType") String fundType,
            @Param("minReturn1y") BigDecimal minReturn1y,
            @Param("maxDrawdown") BigDecimal maxDrawdown,
            @Param("minScale") BigDecimal minScale);
}
```

#### 5.2.3 Service 层

```java
@Service
public class FundService {
    @Autowired
    private FundBasicMapper fundBasicMapper;

    /**
     * 基金模糊搜索
     */
    public List<FundVO> searchFund(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new ArrayList<>();
        }
        return fundBasicMapper.searchFund(keyword);
    }

    /**
     * 优质基金筛选
     * @param fundType 基金类型（可为空）
     * @param minReturn1y 近1年最低收益率（%）
     * @param maxDrawdown 最大回撤上限（%）
     * @param minScale 最小规模（亿元）
     */
    public List<FundVO> filterHighQualityFunds(String fundType, BigDecimal minReturn1y, BigDecimal maxDrawdown, BigDecimal minScale) {
        // 默认值处理
        if (minReturn1y == null) {
            minReturn1y = new BigDecimal("10.0");
        }
        if (maxDrawdown == null) {
            maxDrawdown = new BigDecimal("20.0");
        }
        if (minScale == null) {
            minScale = new BigDecimal("5.0");
        }
        return fundBasicMapper.filterHighQualityFunds(fundType, minReturn1y, maxDrawdown, minScale);
    }
}
```

#### 5.2.4 Controller 层（接口示例）

```java
@RestController
@RequestMapping("/fund")
public class FundController {
    @Autowired
    private FundService fundService;

    /**
     * 基金搜索接口
     */
    @GetMapping("/search")
    public Result<List<FundVO>> searchFund(@RequestParam String keyword) {
        List<FundVO> result = fundService.searchFund(keyword);
        return Result.success(result);
    }

    /**
     * 优质基金筛选接口
     */
    @GetMapping("/filter")
    public Result<List<FundVO>> filterHighQualityFunds(
            @RequestParam(required = false) String fundType,
            @RequestParam(required = false) BigDecimal minReturn1y,
            @RequestParam(required = false) BigDecimal maxDrawdown,
            @RequestParam(required = false) BigDecimal minScale) {
        List<FundVO> result = fundService.filterHighQualityFunds(fundType, minReturn1y, maxDrawdown, minScale);
        return Result.success(result);
    }
}

// 通用返回结果类
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("成功");
        result.setData(data);
        return result;
    }
}
```

### 5.3 决策信号模块

#### 5.3.1 信号规则定义

| 信号类型 | 触发规则                                                     |
| -------- | ------------------------------------------------------------ |
| 买入信号 | 1. PE 估值分位 <20%；2. 近 1 年收益率>-10% 且最大回撤 < 20%；3. 基金规模 > 5 亿 |
| 持有信号 | 1. PE 估值分位 20%-80%；2. 近 1 年收益率 > 0% 且最大回撤 < 25% |
| 卖出信号 | 1. PE 估值分位 > 80%；2. 近 1 年收益率 > 15%（止盈）；3. 最大回撤 > 30%（止损） |

#### 5.3.2 信号生成代码（Java）

```java
@Service
public class FundSignalService {
    @Autowired
    private FundService fundService;

    /**
     * 生成基金交易参考信号
     */
    public Map<String, String> generateTradeSignal(String fundCode) {
        Map<String, String> signalMap = new HashMap<>();
        // 1. 获取基金最新数据
        List<FundVO> fundList = fundService.searchFund(fundCode);
        if (fundList.isEmpty()) {
            signalMap.put("code", "error");
            signalMap.put("msg", "基金数据不存在");
            return signalMap;
        }
        FundVO fund = fundList.get(0);
        
        // 2. 判定信号
        String signal = "持有";
        // 买入信号判定
        if (fund.getPePercentile() != null && fund.getPePercentile() < 20 
                && fund.getReturn1y().compareTo(new BigDecimal("-10")) > 0 
                && fund.getMaxDrawdown().compareTo(new BigDecimal("20")) < 0 
                && fund.getScale().compareTo(new BigDecimal("5")) > 0) {
            signal = "买入";
        }
        // 卖出信号判定
        else if ((fund.getPePercentile() != null && fund.getPePercentile() > 80) 
                || fund.getReturn1y().compareTo(new BigDecimal("15")) > 0 
                || fund.getMaxDrawdown().compareTo(new BigDecimal("30")) > 0) {
            signal = "卖出";
        }
        
        // 3. 返回结果
        signalMap.put("fundCode", fundCode);
        signalMap.put("fundName", fund.getFundName());
        signalMap.put("signal", signal);
        signalMap.put("reason", getSignalReason(fund, signal));
        return signalMap;
    }

    /**
     * 生成信号判定理由
     */
    private String getSignalReason(FundVO fund, String signal) {
        StringBuilder reason = new StringBuilder();
        switch (signal) {
            case "买入":
                reason.append("PE估值分位").append(fund.getPePercentile()).append("%（低于20%），");
                reason.append("近1年收益率").append(fund.getReturn1y()).append("%，");
                reason.append("最大回撤").append(fund.getMaxDrawdown()).append("%（低于20%），");
                reason.append("基金规模").append(fund.getScale()).append("亿（大于5亿），符合买入条件");
                break;
            case "卖出":
                if (fund.getPePercentile() != null && fund.getPePercentile() > 80) {
                    reason.append("PE估值分位").append(fund.getPePercentile()).append("%（高于80%），触发卖出");
                } else if (fund.getReturn1y().compareTo(new BigDecimal("15")) > 0) {
                    reason.append("近1年收益率").append(fund.getReturn1y()).append("%（高于15%），止盈卖出");
                } else {
                    reason.append("最大回撤").append(fund.getMaxDrawdown()).append("%（高于30%），止损卖出");
                }
                break;
            case "持有":
                reason.append("估值、收益率、回撤均处于合理区间，建议持有");
                break;
        }
        return reason.toString();
    }
}
```

## 六、实施计划

### 6.1 阶段划分（按优先级）

| 阶段                   | 周期   | 核心任务                                                     |
| ---------------------- | ------ | ------------------------------------------------------------ |
| 阶段 1（MVP）          | 1-2 周 | 1. 搭建 MySQL 数据库，创建核心表；2. 实现 Python 爬虫采集单只基金数据并写入 MySQL；3. 实现 Java 基础搜索接口 |
| 阶段 2（功能完善）     | 2-3 周 | 1. 扩展爬虫支持多只基金、多数据源；2. 实现优质基金筛选、决策信号生成功能；3. 新增交易记录入库功能 |
| 阶段 3（可视化与复盘） | 2 周   | 1. 实现基金净值、收益率等可视化图表；2. 开发交易记录录入、收益计算功能；3. 优化定时调度稳定性 |
| 阶段 4（迭代优化）     | 长期   | 1. 完善反爬机制，提升爬虫稳定性；2. 扩展筛选规则（如定投适配、行业偏好）；3. 优化界面交互体验 |

### 6.2 环境准备

1. 安装 Python 3.9+，安装依赖：`pip install akshare pymysql pandas apscheduler`；
2. 安装 MySQL 8.0，创建数据库`fund_system`；
3. 安装 Java 11+、Maven，引入 Spring Boot、MyBatis-Plus 依赖；
4. 注册 AKShare 账号（无需付费，基础接口免费）。

## 七、风险与注意事项

### 7.1 技术风险

- **爬虫反爬**：天天基金 / 东方财富网可能封禁 IP，解决方案：添加请求头、随机延迟、使用代理池（可选）；
- **数据格式变更**：网站改版导致爬虫失效，解决方案：定期校验数据采集结果，及时调整爬虫逻辑；
- **数据一致性**：Python 写入和 Java 读取数据类型不一致，解决方案：严格对齐 MySQL 表结构和实体类字段类型。

### 7.2 业务风险

- **合规风险**：系统仅用于个人决策，不对外提供投资建议、不做自动交易；
- **数据准确性**：免费数据源可能存在延迟 / 误差，重要决策需交叉验证（如对比基金公司官网数据）；
- **策略局限性**：决策信号基于历史数据，无法预测未来市场，需结合自身风险承受能力做最终决策。

## 八、总结

### 8.1 核心要点

1. 系统采用**Python（数采）+ Java（业务）+ MySQL（存储）** 的混合技术栈，兼顾数采效率和业务开发优势，完全适配个人使用场景；
2. 核心流程为：Python 采集标准化数据写入 MySQL → Java 读取数据实现检索 / 筛选 / 决策信号 → 可视化展示辅助决策；
3. 实施时优先完成 MVP 版本（单基金数采 + 基础搜索），再逐步扩展功能，降低开发复杂度。

### 8.2 后续扩展建议

- 接入更多数据源（如基金季报 / 年报、宏观经济数据）；
- 优化决策信号规则（如引入机器学习模型做趋势预测，适合有进阶需求后尝试）；
- 开发简单的前端界面（如 Vue+ECharts），提升操作体验；
- 增加数据备份功能，避免 MySQL 数据丢失。