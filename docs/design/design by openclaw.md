# 基金交易决策辅助系统 - 终版架构设计 v2.0

> 版本：v2.0 Final  
> 设计：OpenClaw 架构审查（整合四份设计）  
> 日期：2026-02-28  
> 定位：个人使用的基金交易决策辅助工具  
> 原则：**渐进式架构，MVP优先，预留扩展**

---

## 一、架构审查结论

### 1.1 Design One 评审

| 维度 | 评价 | 说明 |
|------|------|------|
| 架构简洁性 | ✅ 优秀 | Python+Java+MySQL 三层结构清晰，适合个人项目 |
| 技术选型 | ⚠️ 偏旧 | Spring Boot 2.7 已接近EOL，建议升级到 3.x |
| 数据流 | ⚠️ 存在隐患 | 直接写入MySQL，缺少解耦机制，异常时难以追溯 |
| 扩展性 | ❌ 不足 | 无缓存层，数据量大时查询性能下降 |
| 信号规则 | ✅ 实用 | 基于PE分位的买卖信号逻辑清晰，适合个人决策 |

**核心问题**：缺少异步解耦机制，爬虫异常可能直接影响业务查询。

### 1.2 Design Two 评审

| 维度 | 评价 | 说明 |
|------|------|------|
| 架构完整性 | ✅ 优秀 | 引入MQ、Redis、分区表，企业级设计 |
| 技术选型 | ✅ 先进 | Java 17 + Spring Boot 3.2，长期支持 |
| 复杂度 | ❌ 过高 | 主从复制、分区表、MQ集群对个人项目过重 |
| 评分模型 | ✅ 专业 | 多维度评分算法（夏普、回撤、波动率） |
| 运维成本 | ❌ 高 | Docker Compose 4个服务，维护负担大 |

**核心问题**：过度设计，个人项目不需要主从复制和复杂MQ集群。

### 1.3 Design Three 评审

| 维度 | 评价 | 说明 |
|------|------|------|
| 数据解耦 | ✅ 优秀 | "数据库即接口" + 临时表机制，轻量解耦 |
| 数据质量 | ✅ 亮点 | 阈值报警、多源校验、完整性检查 |
| 部署简洁 | ✅ 优秀 | Docker Compose三服务，Crontab轻量调度 |
| 搜索优化 | ✅ 实用 | 拼音首字母搜索，提升用户体验 |
| 评分权重 | ⚠️ 主观 | 收益40/风险30/其他30，权重需验证 |

**核心亮点**：数据质量监控机制和拼音搜索值得采纳。

### 1.4 Design Four 评审

| 维度 | 评价 | 说明 |
|------|------|------|
| 指标详尽 | ✅ 专业 | 夏普、索提诺、卡玛、阿尔法、贝塔等全维度 |
| 表设计完善 | ✅ 优秀 | 基金经理表、持仓表、分区表设计专业 |
| 生产级考虑 | ✅ 周全 | 高可用、监控、合规风险全面考虑 |
| 复杂度 | ❌ 过高 | Celery+Redis调度、分区表对个人过重 |
| 技术栈 | ⚠️ 偏旧 | Spring Boot 2.7+，未采用3.x LTS |

**核心亮点**：详尽的指标体系和数据表设计值得采纳。

### 1.5 四份设计综合对比

| 特性 | One | Two | Three | Four | 终版取舍 |
|------|-----|-----|-------|------|----------|
| 架构复杂度 | 低 | 高 | 中 | 高 | 中（轻量但可扩展） |
| 数据质量监控 | ❌ | ⚠️ | ✅ | ⚠️ | ✅ 采纳Three的机制 |
| 多维度指标 | 基础 | 中等 | 基础 | 完整 | 完整（采纳Four） |
| 拼音搜索 | ❌ | ❌ | ✅ | ❌ | ✅ 采纳 |
| 分区表 | ❌ | ✅ | ❌ | ✅ | 可选（预留设计） |
| 定时调度 | APScheduler | APScheduler | Crontab | Celery | APScheduler（平衡） |
| Spring Boot | 2.7 | 3.2 | 未明确 | 2.7 | **3.2 LTS** |

### 1.6 终版设计原则

```
┌─────────────────────────────────────────────────────────────┐
│                    终版设计核心原则                          │
├─────────────────────────────────────────────────────────────┤
│  1. 【渐进式架构】MVP优先，保留扩展接口，避免过度设计          │
│  2. 【数据质量优先】融入临时表机制、校验规则、异常告警         │
│  3. 【全维指标体系】夏普/索提诺/卡玛/阿尔法/贝塔/信息比率      │
│  4. 【技术升级】Java 17 + Spring Boot 3.2 LTS                │
│  5. 【轻量运维】单节点+容器化可选，分区表作为预留设计          │
│  6. 【体验优化】拼音搜索、决策信号双轨制（规则+评分）          │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、终版系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                          前端展示层                                  │
│              Vue 3 + ECharts（个人使用推荐轻量方案）                  │
└───────────────────────────────────┬─────────────────────────────────┘
                                    │ REST API
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Java 核心业务层 (Spring Boot 3.2)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────┐ │
│  │  REST API    │  │   Service    │  │   Repository │  │  Cache  │ │
│  │  Controller  │  │   业务逻辑   │  │   MyBatis-Plus│  │  Redis  │ │
│  └──────────────┘  └──────┬───────┘  └──────────────┘  └────┬────┘ │
│                           │                                  │      │
│  ┌────────────────────────┼──────────────────────────────────┼────┐ │
│  │     决策信号引擎        │         基金评分计算器          │    │ │
│  │  ┌────────────────┐    │    ┌────────────────────────┐   │    │ │
│  │  │ 估值分位规则    │    │    │ 多维度评分算法         │   │    │ │
│  │  │ 收益率/回撤规则 │◄───┘    │ (夏普/索提诺/卡玛/阿尔│◄──┘    │ │
│  │  └────────────────┘         │ 法/贝塔/信息比率)      │          │ │
│  └─────────────────────────────└────────────────────────┘          │ │
└───────────────────────────────────┬─────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
┌─────────────────────────────────┐   ┌─────────────────────────────────┐
│     数据质量与解耦层             │   │         持久化存储层             │
│  ┌─────────────────────────┐   │   │  ┌─────────────────────────────┐│
│  │ 临时表 (tmp_)           │   │   │  │  MySQL 8.0 (单节点)         ││
│  │ 数据校验引擎            │   │   │  │  ├─ fund_info (基础信息)    ││
│  │ 更新日志表              │   │   │  │  ├─ fund_nav (净值历史)     ││
│  │ 异常告警 (钉钉/邮件)    │   │   │  │  ├─ fund_metrics (全维指标) ││
│  └─────────────────────────┘   │   │  │  ├─ fund_score (评分)       ││
│                                │   │  │  ├─ fund_manager (经理)     ││
│  Python 采集 ──► 校验 ──► 合并──┘   │  │  ├─ fund_holding (持仓)     ││
│                                     │  │  ├─ user_portfolio (持仓)   ││
│                                     │  │  ├─ trade_record (交易)     ││
│                                     │  │  └─ signal_history (信号)   ││
│                                     │  └─────────────────────────────┘│
│                                     │         ↑ 可选分区表设计        │
└─────────────────────────────────────┘  └───────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Python 数据采集与计算层                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │  akshare 采集    │  │  全维指标计算    │  │  数据质量检查    │  │
│  │  ├─ 基金列表     │  │  ├─ 夏普/索提诺  │  │  ├─ 阈值校验     │  │
│  │  ├─ 每日净值     │  │  ├─ 卡玛/阿尔法  │  │  ├─ 多源比对     │  │
│  │  ├─ 持仓数据     │  │  ├─ 贝塔/信息比率│  │  ├─ 异常检测     │  │
│  │  └─ 指数估值     │  │  └─ 最大回撤     │  │  └─ 告警通知     │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    定时调度 (APScheduler)                     │  │
│  │         交易日 19:00 执行采集  │  周末 02:00 执行评分重算       │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 技术栈终版选型

| 分层 | 技术组件 | 版本 | 选型理由 | 来源 |
|------|----------|------|----------|------|
| **采集层** | Python | 3.11+ | 性能更好，akshare兼容 | One/Two |
| | akshare | 1.15+ | 中文基金数据最全 | All |
| | pandas | 2.0+ | 数据处理标准库 | All |
| | APScheduler | 3.10+ | 轻量定时任务，比Celery简单 | One/Two |
| | SQLAlchemy | 2.0+ | ORM，支持临时表机制 | Three/Four |
| **业务层** | Java | 17 LTS | 长期支持，性能优 | Two |
| | Spring Boot | 3.2.x | 最新LTS，生态完善 | Two |
| | MyBatis-Plus | 3.5+ | 简化CRUD | One/Two |
| | Redisson | 3.25+ | Redis客户端 | Two |
| **数据层** | MySQL | 8.0+ | JSON支持，窗口函数 | All |
| | Redis | 7.0+ | 缓存 + 轻量队列 | Two/Three |
| **部署** | Docker | 24.x | 可选容器化 | Three/Four |

---

## 三、数据库设计（终版）

### 3.1 核心表结构（融合四份设计精华）

```sql
-- ============================================
-- 1. 基金基础信息表（融合Four的详细设计）
-- ============================================
CREATE TABLE fund_info (
    fund_code VARCHAR(10) PRIMARY KEY COMMENT '基金代码',
    fund_name VARCHAR(100) NOT NULL COMMENT '基金名称',
    name_pinyin VARCHAR(100) COMMENT '名称拼音首字母（如：招商->zs）- 来自Design Three',
    fund_type VARCHAR(20) COMMENT '类型：股票型/债券型/混合型/指数型/QDII/FOF',
    invest_style VARCHAR(20) COMMENT '投资风格：成长/价值/平衡',
    manager_code VARCHAR(20) COMMENT '基金经理代码',
    manager_name VARCHAR(50) COMMENT '基金经理姓名',
    company_code VARCHAR(20) COMMENT '基金公司代码',
    company_name VARCHAR(100) COMMENT '基金公司名称',
    establish_date DATE COMMENT '成立日期',
    benchmark VARCHAR(200) COMMENT '业绩比较基准',
    management_fee DECIMAL(5,4) COMMENT '管理费率',
    custody_fee DECIMAL(5,4) COMMENT '托管费率',
    risk_level TINYINT COMMENT '风险等级：1-5',
    current_scale DECIMAL(15,2) COMMENT '最新规模（亿元）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，2-暂停申购，3-暂停赎回，0-清盘',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (fund_type),
    INDEX idx_manager (manager_name),
    INDEX idx_company (company_name),
    INDEX idx_pinyin (name_pinyin) COMMENT '拼音搜索优化 - Design Three'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金基础信息表';

-- ============================================
-- 2. 基金净值历史表（简化版，预留分区扩展）
-- ============================================
CREATE TABLE fund_nav (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    nav_date DATE NOT NULL COMMENT '净值日期',
    unit_nav DECIMAL(10,4) NOT NULL COMMENT '单位净值',
    accum_nav DECIMAL(10,4) COMMENT '累计净值（用于计算真实收益）',
    adjust_nav DECIMAL(10,4) COMMENT '复权净值 - Design Four',
    daily_return DECIMAL(8,4) COMMENT '日增长率%',
    source VARCHAR(20) DEFAULT 'akshare' COMMENT '数据来源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_date (fund_code, nav_date),
    INDEX idx_date (nav_date),
    INDEX idx_code_date (fund_code, nav_date)
    -- 备注：当数据量>500万时，建议改为分区表（见下方预留设计）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金净值历史表';

-- 【预留】分区表设计（数据量大时启用）
/*
CREATE TABLE fund_nav_partitioned (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    nav_date DATE NOT NULL,
    unit_nav DECIMAL(10,4) NOT NULL,
    accum_nav DECIMAL(10,4),
    daily_return DECIMAL(8,4),
    source VARCHAR(20) DEFAULT 'akshare',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, nav_date),
    UNIQUE KEY uk_fund_date (fund_code, nav_date),
    INDEX idx_date (nav_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY RANGE (YEAR(nav_date)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
) COMMENT='基金净值历史表-分区版';
*/

-- ============================================
-- 3. 基金指标表（全维度，融合Four的详尽设计）
-- ============================================
CREATE TABLE fund_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    calc_date DATE NOT NULL COMMENT '计算日期',
    
    -- 收益指标
    return_1m DECIMAL(8,4) COMMENT '近1月收益率%',
    return_3m DECIMAL(8,4) COMMENT '近3月收益率%',
    return_1y DECIMAL(8,4) COMMENT '近1年收益率%',
    return_3y DECIMAL(8,4) COMMENT '近3年收益率%',
    return_5y DECIMAL(8,4) COMMENT '近5年收益率%',
    
    -- 风险调整后收益（融合Four的全维度设计）
    sharpe_ratio_1y DECIMAL(10,4) COMMENT '近1年夏普比率',
    sharpe_ratio_3y DECIMAL(10,4) COMMENT '近3年夏普比率',
    sortino_ratio_1y DECIMAL(10,4) COMMENT '近1年索提诺比率 - Design Four',
    calmar_ratio_3y DECIMAL(10,4) COMMENT '近3年卡玛比率 - Design Four',
    information_ratio_1y DECIMAL(10,4) COMMENT '近1年信息比率 - Design Four',
    
    -- 风险指标
    max_drawdown_1y DECIMAL(8,4) COMMENT '近1年最大回撤%',
    max_drawdown_3y DECIMAL(8,4) COMMENT '近3年最大回撤%',
    volatility_1y DECIMAL(8,4) COMMENT '近1年波动率%',
    volatility_3y DECIMAL(8,4) COMMENT '近3年波动率%',
    
    -- 风险因子（融合Four的专业指标）
    alpha_1y DECIMAL(8,4) COMMENT '近1年阿尔法（超额收益）',
    beta_1y DECIMAL(8,4) COMMENT '近1年贝塔（系统性风险）',
    tracking_error_1y DECIMAL(8,4) COMMENT '近1年跟踪误差',
    
    -- 估值指标（融合One/Three）
    pe_percentile INT COMMENT 'PE估值分位0-100',
    pb_percentile INT COMMENT 'PB估值分位0-100',
    
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_calc_date (fund_code, calc_date),
    INDEX idx_calc_date (calc_date),
    INDEX idx_sharpe_1y (sharpe_ratio_1y),
    INDEX idx_max_dd_3y (max_drawdown_3y),
    INDEX idx_return_1y (return_1y)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金全维指标表';

-- ============================================
-- 4. 基金评分表（融合Two的评分模型 + Four的详尽指标）
-- ============================================
CREATE TABLE fund_score (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    calc_date DATE NOT NULL COMMENT '计算日期',
    total_score INT COMMENT '总分0-100',
    quality_level CHAR(1) COMMENT '等级：S/A/B/C/D',
    
    -- 分项得分（融合Two的权重设计）
    return_score INT COMMENT '收益得分(30) - 基于夏普/卡玛',
    risk_score INT COMMENT '风控得分(25) - 基于最大回撤',
    stability_score INT COMMENT '稳定性(20) - 基于波动率',
    scale_score INT COMMENT '规模适配(15) - 基于基金规模',
    fee_score INT COMMENT '费用得分(10) - 基于管理费率',
    
    -- 关键指标快照
    sharpe_ratio DECIMAL(6,4) COMMENT '夏普比率',
    sortino_ratio DECIMAL(6,4) COMMENT '索提诺比率 - Design Four',
    calmar_ratio DECIMAL(6,4) COMMENT '卡玛比率 - Design Four',
    max_drawdown DECIMAL(6,4) COMMENT '最大回撤',
    volatility DECIMAL(6,4) COMMENT '波动率',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_date (fund_code, calc_date),
    INDEX idx_score (total_score),
    INDEX idx_level (quality_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金评分表';

-- ============================================
-- 5. 基金经理表（来自Design Four）
-- ============================================
CREATE TABLE fund_manager (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    manager_code VARCHAR(20) COMMENT '基金经理代码',
    manager_name VARCHAR(50) NOT NULL COMMENT '基金经理姓名',
    company_name VARCHAR(100) COMMENT '所属公司',
    start_date DATE COMMENT '任职开始日期',
    end_date DATE COMMENT '任职结束日期（NULL表示在职）',
    total_funds INT COMMENT '管理基金总数',
    total_scale DECIMAL(15,2) COMMENT '管理总规模(亿元)',
    best_return DECIMAL(8,4) COMMENT '最佳任职回报%',
    tenure_years DECIMAL(4,1) COMMENT '任职年限',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_manager_name (manager_name),
    INDEX idx_company (company_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金经理信息表';

-- ============================================
-- 6. 基金持仓表（来自Design Four）
-- ============================================
CREATE TABLE fund_holding (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    report_date DATE NOT NULL COMMENT '报告期（季报日期）',
    stock_code VARCHAR(10) COMMENT '股票代码',
    stock_name VARCHAR(100) COMMENT '股票名称',
    holding_amount BIGINT COMMENT '持股数量(股)',
    holding_value DECIMAL(15,2) COMMENT '持股市值(元)',
    holding_ratio DECIMAL(8,4) COMMENT '占净值比例%',
    holding_type VARCHAR(20) COMMENT '持仓类型:股票/债券/现金/其他',
    is_top10 TINYINT DEFAULT 0 COMMENT '是否前十大重仓：1-是，0-否',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_report_stock (fund_code, report_date, stock_code),
    INDEX idx_report_date (report_date),
    INDEX idx_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金持仓明细表';

-- ============================================
-- 7. 用户持仓表（融合Three/Four的设计）
-- ============================================
CREATE TABLE user_portfolio (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    trade_type TINYINT NOT NULL COMMENT '1-买入, 2-卖出',
    trade_date DATE NOT NULL COMMENT '交易日期',
    nav_price DECIMAL(10,4) NOT NULL COMMENT '成交净值',
    trade_share DECIMAL(15,4) COMMENT '成交份额',
    trade_amount DECIMAL(15,2) COMMENT '交易金额（元）',
    trade_fee DECIMAL(10,2) COMMENT '交易手续费',
    remark VARCHAR(200) COMMENT '备注（如：定投买入）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fund (fund_code),
    INDEX idx_date (trade_date),
    INDEX idx_fund_date (fund_code, trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持仓记录表';

-- ============================================
-- 8. 决策信号历史表（用于复盘命中率）
-- ============================================
CREATE TABLE signal_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    signal_date DATE NOT NULL COMMENT '信号日期',
    signal_type TINYINT NOT NULL COMMENT '1-买入, 2-持有, 3-卖出',
    reason VARCHAR(500) COMMENT '信号理由',
    
    -- 当时指标快照
    pe_percentile INT COMMENT '当时PE分位',
    return_1y DECIMAL(8,4) COMMENT '当时1年收益',
    max_drawdown DECIMAL(8,4) COMMENT '当时最大回撤',
    sharpe_ratio DECIMAL(6,4) COMMENT '当时夏普比率',
    total_score INT COMMENT '当时综合评分',
    
    -- 复盘结果
    is_hit TINYINT DEFAULT 0 COMMENT '是否命中：0-未知, 1-命中, 2-未命中',
    hit_check_date DATE COMMENT '复盘验证日期',
    actual_return DECIMAL(8,4) COMMENT '信号后实际收益%',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_date (fund_code, signal_date),
    INDEX idx_signal (signal_type, signal_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='决策信号历史表';

-- ============================================
-- 9. 数据更新日志表（融合Three的数据质量机制）
-- ============================================
CREATE TABLE data_update_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL COMMENT '更新的表名',
    update_date DATE NOT NULL COMMENT '数据日期',
    record_count INT COMMENT '更新记录数',
    status VARCHAR(20) COMMENT '状态：SUCCESS/FAILURE/PARTIAL',
    error_msg TEXT COMMENT '错误信息',
    start_time TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP COMMENT '结束时间',
    duration_seconds INT COMMENT '耗时(秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_table_date (table_name, update_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据更新日志表';

-- ============================================
-- 10. 临时数据表（融合Three的临时表机制）
-- ============================================
CREATE TABLE tmp_fund_nav (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    nav_date DATE NOT NULL,
    unit_nav DECIMAL(10,4) NOT NULL,
    accum_nav DECIMAL(10,4),
    daily_return DECIMAL(8,4),
    source VARCHAR(20),
    check_status TINYINT DEFAULT 0 COMMENT '0-待校验,1-校验通过,2-校验失败',
    check_msg VARCHAR(200) COMMENT '校验信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (check_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='净值临时表（数据校验用）';
```

### 3.2 索引优化策略

```sql
-- 拼音搜索优化（Design Three亮点）
CREATE INDEX idx_fund_pinyin ON fund_info(name_pinyin, fund_type);

-- 复合检索优化
CREATE INDEX idx_fund_search ON fund_info(fund_type, status, current_scale);

-- 指标排序优化
CREATE INDEX idx_metrics_sort ON fund_metrics(sharpe_ratio_1y DESC, max_drawdown_3y ASC, return_1y DESC);

-- 时间序列查询优化
CREATE INDEX idx_nav_timeseries ON fund_nav(fund_code, nav_date DESC);

-- 覆盖索引（减少回表）
CREATE INDEX idx_metrics_covering ON fund_metrics(fund_code, calc_date, sharpe_ratio_1y, max_drawdown_3y, return_1y);
```

---

## 四、数据质量机制（融合Design Three）

### 4.1 数据采集流程（临时表机制）

```python
# fund_collector/core/data_pipeline.py
import pandas as pd
from datetime import datetime
from sqlalchemy import create_engine
import logging

logger = logging.getLogger(__name__)

class DataPipeline:
    """数据管道：临时表机制确保数据质量"""
    
    def __init__(self, db_config: dict):
        self.engine = create_engine(
            f"mysql+pymysql://{db_config['user']}:{db_config['password']}"
            f"@{db_config['host']}:{db_config['port']}/{db_config['database']}"
        )
    
    def collect_and_validate(self, df: pd.DataFrame, table_name: str) -> bool:
        """
        数据采集与校验流程：
        1. 写入临时表
        2. 数据校验
        3. 校验通过则合并到正式表
        4. 记录更新日志
        """
        try:
            # 1. 清空并写入临时表
            self._clear_temp_table(table_name)
            df.to_sql(f'tmp_{table_name}', self.engine, if_exists='append', index=False)
            
            # 2. 数据校验
            validation_result = self._validate_data(table_name)
            
            if validation_result['is_valid']:
                # 3. 合并到正式表
                self._merge_to_production(table_name)
                self._log_update(table_name, len(df), 'SUCCESS')
                logger.info(f"数据更新成功: {table_name}, {len(df)}条记录")
                return True
            else:
                # 校验失败
                self._log_update(table_name, len(df), 'FAILURE', validation_result['errors'])
                self._send_alert(f"数据校验失败: {table_name}", validation_result['errors'])
                return False
                
        except Exception as e:
            self._log_update(table_name, 0, 'FAILURE', str(e))
            self._send_alert(f"数据采集异常: {table_name}", str(e))
            return False
    
    def _validate_data(self, table_name: str) -> dict:
        """数据校验规则（融合Three/Four的设计）"""
        errors = []
        
        if table_name == 'fund_nav':
            # 校验规则1：净值范围
            sql = """
            SELECT COUNT(*) as cnt FROM tmp_fund_nav 
            WHERE unit_nav <= 0 OR unit_nav > 1000
            """
            result = pd.read_sql(sql, self.engine)
            if result['cnt'].iloc[0] > 0:
                errors.append(f"净值异常: {result['cnt'].iloc[0]}条记录超出正常范围")
            
            # 校验规则2：日涨跌幅范围
            sql = """
            SELECT COUNT(*) as cnt FROM tmp_fund_nav 
            WHERE daily_return < -20 OR daily_return > 20
            """
            result = pd.read_sql(sql, self.engine)
            if result['cnt'].iloc[0] > 0:
                errors.append(f"涨跌幅异常: {result['cnt'].iloc[0]}条记录超出±20%")
            
            # 校验规则3：数据完整性
            sql = """
            SELECT COUNT(*) as cnt FROM tmp_fund_nav 
            WHERE fund_code IS NULL OR nav_date IS NULL OR unit_nav IS NULL
            """
            result = pd.read_sql(sql, self.engine)
            if result['cnt'].iloc[0] > 0:
                errors.append(f"数据缺失: {result['cnt'].iloc[0]}条记录关键字段为空")
            
            # 校验规则4：重复数据检查
            sql = """
            SELECT fund_code, nav_date, COUNT(*) as cnt 
            FROM tmp_fund_nav 
            GROUP BY fund_code, nav_date HAVING cnt > 1
            """
            result = pd.read_sql(sql, self.engine)
            if len(result) > 0:
                errors.append(f"重复数据: {len(result)}组重复记录")
        
        return {
            'is_valid': len(errors) == 0,
            'errors': '; '.join(errors) if errors else None
        }
    
    def _merge_to_production(self, table_name: str):
        """合并临时表到正式表"""
        if table_name == 'fund_nav':
            sql = """
            INSERT INTO fund_nav (fund_code, nav_date, unit_nav, accum_nav, daily_return, source)
            SELECT fund_code, nav_date, unit_nav, accum_nav, daily_return, source
            FROM tmp_fund_nav
            WHERE check_status = 0 OR check_status = 1
            ON DUPLICATE KEY UPDATE
                unit_nav = VALUES(unit_nav),
                accum_nav = VALUES(accum_nav),
                daily_return = VALUES(daily_return),
                source = VALUES(source)
            """
            with self.engine.connect() as conn:
                conn.execute(sql)
    
    def _log_update(self, table_name: str, record_count: int, status: str, error_msg: str = None):
        """记录更新日志"""
        sql = """
        INSERT INTO data_update_log 
        (table_name, update_date, record_count, status, error_msg, end_time)
        VALUES (%s, %s, %s, %s, %s, NOW())
        """
        with self.engine.connect() as conn:
            conn.execute(sql, (table_name, datetime.now().date(), record_count, status, error_msg))
    
    def _send_alert(self, title: str, message: str):
        """发送告警（可接入钉钉/邮件）"""
        logger.error(f"[ALERT] {title}: {message}")
        # TODO: 接入钉钉webhook或邮件SMTP
```

### 4.2 多源数据校验

```python
# fund_collector/core/data_validator.py
import akshare as ak
import pandas as pd
from typing import Dict, List

class MultiSourceValidator:
    """多源数据校验（融合Three的多源校验思想）"""
    
    def __init__(self):
        self.sources = ['akshare', 'eastmoney']
    
    def validate_nav_consistency(self, fund_code: str, date: str) -> Dict:
        """
        多源校验净值数据一致性
        如果差异过大，触发人工确认
        """
        results = {}
        
        # 源1：akshare
        try:
            df = ak.fund_net_value_em(fund_code)
            if not df.empty:
                row = df[df['净值日期'] == date]
                if not row.empty:
                    results['akshare'] = float(row['单位净值'].iloc[0])
        except Exception as e:
            results['akshare'] = None
        
        # 源2：东方财富爬虫（简化示例）
        # results['eastmoney'] = self._crawl_eastmoney(fund_code, date)
        
        # 校验一致性
        valid_values = [v for v in results.values() if v is not None]
        if len(valid_values) >= 2:
            max_diff = max(valid_values) - min(valid_values)
            max_diff_pct = max_diff / min(valid_values) * 100
            
            if max_diff_pct > 1:  # 差异超过1%
                return {
                    'is_consistent': False,
                    'max_diff_pct': max_diff_pct,
                    'sources': results,
                    'suggestion': '数据源差异过大，需人工确认'
                }
        
        return {
            'is_consistent': True,
            'sources': results
        }
```

---

## 五、全维指标计算（融合Design Four）

### 5.1 指标计算引擎

```python
# fund_collector/core/metrics_calculator.py
import pandas as pd
import numpy as np
from typing import Dict, Optional
from scipy import stats

class FundMetricsCalculator:
    """
    基金全维指标计算引擎
    融合Design Four的详尽指标设计
    """
    
    def __init__(self, risk_free_rate: float = 0.025):
        self.risk_free_rate = risk_free_rate  # 无风险利率2.5%
    
    def calculate_all_metrics(self, fund_code: str, nav_data: pd.DataFrame) -> Dict:
        """
        计算所有指标（1年/3年窗口）
        """
        if len(nav_data) < 60:
            return {}
        
        nav_data = nav_data.sort_values('nav_date')
        nav_data['daily_return'] = nav_data['unit_nav'].pct_change()
        returns = nav_data['daily_return'].dropna()
        
        # 1年/3年数据
        one_year = nav_data.tail(252)
        three_year = nav_data.tail(756) if len(nav_data) >= 756 else nav_data
        
        metrics = {
            'fund_code': fund_code,
            'calc_date': pd.Timestamp.now().strftime('%Y-%m-%d'),
            
            # 收益指标
            **self._calculate_returns(nav_data, one_year, three_year),
            
            # 风险调整后收益
            **self._calculate_risk_adjusted_returns(one_year, three_year),
            
            # 风险指标
            **self._calculate_risk_metrics(one_year, three_year),
            
            # 风险因子（Design Four亮点）
            **self._calculate_risk_factors(one_year)
        }
        
        return metrics
    
    def _calculate_returns(self, full_data: pd.DataFrame, 
                          one_year: pd.DataFrame, 
                          three_year: pd.DataFrame) -> Dict:
        """计算收益指标"""
        
        def annual_return(nav_series):
            total = (nav_series.iloc[-1] / nav_series.iloc[0]) - 1
            years = len(nav_series) / 252
            return (1 + total) ** (1/years) - 1 if years > 0 else 0
        
        returns_1m = self._period_return(full_data, 21)
        returns_3m = self._period_return(full_data, 63)
        
        return {
            'return_1m': round(returns_1m * 100, 4),
            'return_3m': round(returns_3m * 100, 4),
            'return_1y': round(annual_return(one_year) * 100, 4),
            'return_3y': round(annual_return(three_year) * 100, 4),
        }
    
    def _calculate_risk_adjusted_returns(self, one_year: pd.DataFrame, 
                                          three_year: pd.DataFrame) -> Dict:
        """计算风险调整后收益指标（Design Four全维度）"""
        
        returns_1y = one_year['daily_return'].dropna()
        returns_3y = three_year['daily_return'].dropna()
        
        return {
            # 夏普比率
            'sharpe_ratio_1y': round(self._sharpe_ratio(returns_1y), 4),
            'sharpe_ratio_3y': round(self._sharpe_ratio(returns_3y), 4),
            
            # 索提诺比率（只考虑下行风险）
            'sortino_ratio_1y': round(self._sortino_ratio(returns_1y), 4),
            
            # 卡玛比率（收益/最大回撤）
            'calmar_ratio_3y': round(self._calmar_ratio(three_year), 4),
            
            # 信息比率（相对基准）- 简化计算
            'information_ratio_1y': round(self._information_ratio(returns_1y), 4),
        }
    
    def _calculate_risk_metrics(self, one_year: pd.DataFrame, 
                                three_year: pd.DataFrame) -> Dict:
        """计算风险指标"""
        returns_1y = one_year['daily_return'].dropna()
        returns_3y = three_year['daily_return'].dropna()
        
        return {
            'max_drawdown_1y': round(self._max_drawdown(one_year['unit_nav']) * 100, 4),
            'max_drawdown_3y': round(self._max_drawdown(three_year['unit_nav']) * 100, 4),
            'volatility_1y': round(returns_1y.std() * np.sqrt(252) * 100, 4),
            'volatility_3y': round(returns_3y.std() * np.sqrt(252) * 100, 4),
        }
    
    def _calculate_risk_factors(self, one_year: pd.DataFrame) -> Dict:
        """计算风险因子（阿尔法/贝塔）"""
        # 简化计算，实际应使用市场基准
        returns = one_year['daily_return'].dropna()
        
        # 阿尔法和贝塔（简化版）
        alpha, beta = self._alpha_beta(returns)
        
        return {
            'alpha_1y': round(alpha, 4),
            'beta_1y': round(beta, 4),
            'tracking_error_1y': round(returns.std() * np.sqrt(252), 4),
        }
    
    # ==================== 基础计算方法 ====================
    
    def _sharpe_ratio(self, returns: pd.Series) -> float:
        """夏普比率"""
        if len(returns) < 2:
            return np.nan
        excess = returns.mean() * 252 - self.risk_free_rate
        volatility = returns.std() * np.sqrt(252)
        return excess / volatility if volatility > 0 else 0
    
    def _sortino_ratio(self, returns: pd.Series, target: float = 0) -> float:
        """索提诺比率（只考虑下行风险）- Design Four"""
        if len(returns) < 2:
            return np.nan
        
        downside = returns[returns < target]
        if len(downside) < 2:
            return np.nan
        
        downside_risk = downside.std() * np.sqrt(252)
        excess_return = returns.mean() * 252 - self.risk_free_rate
        
        return excess_return / downside_risk if downside_risk > 0 else 0
    
    def _calmar_ratio(self, nav_data: pd.DataFrame) -> float:
        """卡玛比率（年化收益/最大回撤）- Design Four"""
        annual_return = (nav_data['unit_nav'].iloc[-1] / nav_data['unit_nav'].iloc[0]) ** (252/len(nav_data)) - 1
        max_dd = self._max_drawdown(nav_data['unit_nav'])
        return annual_return / abs(max_dd) if max_dd != 0 else 0
    
    def _max_drawdown(self, nav_series: pd.Series) -> float:
        """最大回撤"""
        cumulative = (1 + nav_series.pct_change().fillna(0)).cumprod()
        running_max = cumulative.expanding().max()
        drawdown = (cumulative - running_max) / running_max
        return drawdown.min()
    
    def _information_ratio(self, returns: pd.Series) -> float:
        """信息比率（简化计算）"""
        # 假设基准收益为0（实际应传入基准指数收益）
        active_return = returns.mean() * 252
        tracking_error = returns.std() * np.sqrt(252)
        return active_return / tracking_error if tracking_error > 0 else 0
    
    def _alpha_beta(self, returns: pd.Series) -> tuple:
        """阿尔法/贝塔（简化计算）"""
        # 简化：假设市场收益率为日收益的均值
        market_return = returns.mean()
        
        # 贝塔 = Cov(股票收益, 市场收益) / Var(市场收益)
        # 简化处理：假设与市场完全相关
        beta = 1.0
        
        # 阿尔法 = 实际收益 - 预期收益
        expected_return = self.risk_free_rate/252 + beta * (market_return - self.risk_free_rate/252)
        alpha = returns.mean() - expected_return
        
        return alpha * 252, beta  # 年化阿尔法
    
    def _period_return(self, nav_data: pd.DataFrame, days: int) -> float:
        """计算N日收益率"""
        if len(nav_data) < days:
            return 0
        recent = nav_data.tail(days)
        return (recent['unit_nav'].iloc[-1] / recent['unit_nav'].iloc[0]) - 1
```

---

## 六、决策信号与评分模型（融合所有设计）

### 6.1 双轨决策引擎

```java
@Service
public class FundDecisionEngine {
    
    @Autowired
    private FundMetricsMapper metricsMapper;
    
    @Autowired
    private FundScoreMapper scoreMapper;
    
    /**
     * 双轨决策引擎：规则 + 评分模型
     * 融合One的直观规则、Two的评分权重、Four的全维指标
     */
    public TradeSignal generateSignal(String fundCode) {
        FundMetrics metrics = metricsMapper.selectLatest(fundCode);
        FundScore score = scoreMapper.selectLatest(fundCode);
        
        if (metrics == null || score == null) {
            return TradeSignal.hold("数据不足");
        }
        
        // 轨道1：规则引擎（来自Design One，简单直观）
        RuleResult ruleResult = evaluateRules(metrics);
        
        // 轨道2：评分模型（来自Design Two，量化严谨）
        ScoreResult scoreResult = evaluateScore(score);
        
        // 双轨融合决策
        return combineDecision(ruleResult, scoreResult, metrics, score);
    }
    
    private RuleResult evaluateRules(FundMetrics m) {
        int buyScore = 0, sellScore = 0;
        List<String> reasons = new ArrayList<>();
        
        // 买入规则
        if (m.getPePercentile() != null && m.getPePercentile() < 20) {
            buyScore += 3;
            reasons.add("PE估值分位低(" + m.getPePercentile() + "%)");
        }
        if (m.getSharpeRatio1y() != null && m.getSharpeRatio1y().compareTo(new BigDecimal("1.0")) > 0
                && m.getMaxDrawdown1y().compareTo(new BigDecimal("-20")) > 0) {
            buyScore += 2;
            reasons.add("夏普>1.0且回撤<20%");
        }
        if (m.getSortinoRatio1y() != null && m.getSortinoRatio1y().compareTo(new BigDecimal("1.5")) > 0) {
            buyScore += 2;
            reasons.add("索提诺比率优秀");
        }
        
        // 卖出规则
        if (m.getPePercentile() != null && m.getPePercentile() > 80) {
            sellScore += 3;
            reasons.add("PE估值分位高(" + m.getPePercentile() + "%)");
        }
        if (m.getReturn1y() != null && m.getReturn1y().compareTo(new BigDecimal("20")) > 0) {
            sellScore += 2;
            reasons.add("年化收益>20%，考虑止盈");
        }
        if (m.getMaxDrawdown1y() != null && m.getMaxDrawdown1y().compareTo(new BigDecimal("-30")) < 0) {
            sellScore += 3;
            reasons.add("回撤超30%，止损");
        }
        
        return new RuleResult(buyScore, sellScore, reasons);
    }
    
    private ScoreResult evaluateScore(FundScore s) {
        String level = s.getQualityLevel();
        int total = s.getTotalScore();
        
        return new ScoreResult(level, total, 
            "S".equals(level) || "A".equals(level),
            "D".equals(level) || "C".equals(level));
    }
    
    private TradeSignal combineDecision(RuleResult rule, ScoreResult score, 
                                        FundMetrics m, FundScore s) {
        // 评分强干预
        if (score.isPoorQuality() && rule.getBuyScore() > 0) {
            return TradeSignal.hold("质量评级" + s.getQualityLevel() + 
                "(" + s.getTotalScore() + "分)，不满足买入标准");
        }
        
        // 规则主导
        if (rule.getBuyScore() >= 5 && score.isGoodQuality()) {
            return TradeSignal.buy("规则分" + rule.getBuyScore() + 
                "，评级" + s.getQualityLevel() + "，建议买入");
        }
        if (rule.getSellScore() >= 5) {
            return TradeSignal.sell(String.join(";", rule.getReasons()));
        }
        
        return TradeSignal.hold("估值合理，建议持有");
    }
}
```

### 6.2 评分模型（融合Two/Four）

```python
def calculate_comprehensive_score(metrics: dict) -> dict:
    """
    综合评分模型（0-100分）
    权重设计融合Design Two和Four
    """
    sharpe = metrics.get('sharpe_ratio_1y', 0)
    sortino = metrics.get('sortino_ratio_1y', 0)
    calmar = metrics.get('calmar_ratio_3y', 0)
    max_dd = abs(metrics.get('max_drawdown_3y', 0))
    volatility = metrics.get('volatility_1y', 0)
    
    # 1. 收益得分 (30分) - 基于夏普、索提诺、卡玛
    return_score = 0
    if sharpe >= 2.0: return_score += 15
    elif sharpe >= 1.5: return_score += 12
    elif sharpe >= 1.0: return_score += 9
    elif sharpe >= 0.5: return_score += 5
    
    if sortino >= 2.5: return_score += 15
    elif sortino >= 2.0: return_score += 12
    elif sortino >= 1.5: return_score += 9
    elif sortino >= 1.0: return_score += 5
    
    # 2. 风控得分 (25分) - 基于最大回撤和卡玛
    risk_score = 0
    if max_dd <= 10: risk_score += 15
    elif max_dd <= 15: risk_score += 12
    elif max_dd <= 20: risk_score += 9
    elif max_dd <= 25: risk_score += 5
    
    if calmar >= 3.0: risk_score += 10
    elif calmar >= 2.0: risk_score += 8
    elif calmar >= 1.0: risk_score += 5
    
    # 3. 稳定性得分 (20分) - 基于波动率
    stability_score = 0
    if volatility <= 12: stability_score = 20
    elif volatility <= 15: stability_score = 16
    elif volatility <= 18: stability_score = 12
    elif volatility <= 22: stability_score = 8
    else: stability_score = max(0, 20 - int((volatility - 22) / 2))
    
    # 4. 规模得分 (15分) - 简化
    scale_score = 10  # 默认值
    
    # 5. 费用得分 (10分) - 简化
    fee_score = 8  # 默认值
    
    total = return_score + risk_score + stability_score + scale_score + fee_score
    
    return {
        'total_score': total,
        'quality_level': 'S' if total >= 90 else 'A' if total >= 80 else 'B' if total >= 60 else 'C' if total >= 40 else 'D',
        'return_score': return_score,
        'risk_score': risk_score,
        'stability_score': stability_score,
        'scale_score': scale_score,
        'fee_score': fee_score
    }
```

---

## 七、API接口设计

### 7.1 核心接口列表（融合所有设计）

| 接口 | 方法 | 描述 | 缓存 | 来源 |
|------|------|------|------|------|
| `GET /api/funds` | 检索 | 关键词搜索（支持拼音） | 5min | Three |
| `GET /api/funds/{code}` | 详情 | 基金详情+全维指标 | 1min | Four |
| `GET /api/funds/{code}/nav` | 净值 | 历史净值时间序列 | 1hour | All |
| `GET /api/funds/{code}/signal` | 信号 | 买卖决策建议 | 实时 | One+Two |
| `GET /api/funds/recommend` | 推荐 | TOP N优质基金 | 12hour | Two |
| `POST /api/funds/filter` | 筛选 | 多条件组合筛选 | 5min | Four |
| `POST /api/portfolio/trade` | 交易 | 录入买卖记录 | - | Three |
| `GET /api/portfolio/analysis` | 分析 | 持仓收益风险分析 | 5min | Three/Four |
| `GET /api/signals/history` | 复盘 | 历史信号命中率 | 1hour | One |
| `GET /api/data/status` | 状态 | 数据更新状态 | 1min | Three |

### 7.2 拼音搜索（Design Three亮点）

```java
@Service
public class FundSearchService {
    
    /**
     * 拼音搜索优化（融合Design Three）
     */
    public List<FundVO> searchWithPinyin(String keyword) {
        // 支持：基金代码、基金名称、拼音首字母
        // 如：输入 "zs" 可匹配 "招商..."
        
        QueryWrapper<FundInfo> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w
            .like("fund_code", keyword)
            .or()
            .like("fund_name", keyword)
            .or()
            .like("name_pinyin", keyword.toLowerCase())  // 拼音搜索
        );
        
        return fundInfoMapper.selectList(wrapper)
            .stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
}
```

---

## 八、部署方案（融合Three的简洁设计）

### 8.1 Docker Compose（三服务精简版）

```yaml
# docker-compose.yml（融合Design Three的简洁部署）
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: fund-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASS:-root123}
      MYSQL_DATABASE: fund_system
      MYSQL_USER: fund
      MYSQL_PASSWORD: ${DB_PASS:-fund123}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password
    networks:
      - fund_net

  redis:
    image: redis:7-alpine
    container_name: fund-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - fund_net

  java-backend:
    build: ./fund-service
    container_name: fund-api
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - REDIS_HOST=redis
      - DB_PASS=${DB_PASS:-fund123}
    depends_on:
      - mysql
      - redis
    networks:
      - fund_net

  # Python采集可选容器化，也可宿主机Crontab运行（推荐Crontab，更简单）
  # python-collector:
  #   build: ./fund-collector
  #   container_name: fund-collector
  #   environment:
  #     - DB_HOST=mysql
  #   depends_on:
  #     - mysql
  #   networks:
  #     - fund_net

volumes:
  mysql_data:
  redis_data:

networks:
  fund_net:
    driver: bridge
```

### 8.2 定时任务（推荐APScheduler，比Celery轻量）

```python
# fund_collector/scheduler.py
from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger

scheduler = BlockingScheduler()

# 每日19:00采集净值（收盘后）
scheduler.add_job(
    daily_collection_job,
    CronTrigger(hour=19, minute=0, day_of_week='mon-fri'),
    id='daily_collection',
    replace_existing=True
)

# 每日20:30计算指标
scheduler.add_job(
    daily_metrics_job,
    CronTrigger(hour=20, minute=30, day_of_week='mon-fri'),
    id='daily_metrics',
    replace_existing=True
)

# 每周日凌晨02:00重新计算全量评分
scheduler.add_job(
    weekly_full_calc_job,
    CronTrigger(hour=2, minute=0, day_of_week='sun'),
    id='weekly_full_calc',
    replace_existing=True
)

if __name__ == '__main__':
    scheduler.start()
```

---

## 九、风险与合规（融合Four的全面考虑）

| 风险类别 | 风险项 | 应对措施 |
|----------|--------|----------|
| **技术风险** | akshare接口变更 | 封装抽象层，预留爬虫补偿 |
| | 数据质量异常 | 临时表+校验机制，差异>1%告警 |
| | 数据量增长 | 预留分区表设计，按年归档 |
| **合规风险** | 被误认为荐股 | 界面显著免责声明，仅限个人使用 |
| | 数据采集合规 | 遵守robots.txt，频率<1次/秒 |
| | 用户数据安全 | 敏感数据加密，本地部署 |
| **业务风险** | 策略失效 | 信号历史复盘，定期优化规则 |
| | 数据滞后 | 持仓数据T+90提示，估值仅供参考 |
| | 情绪化交易 | 强制冷静期，信号需二次确认 |

---

## 十、实施路线图（6-8周）

### Phase 1: 数据基建（2周）
- [ ] MySQL建表（10张核心表）
- [ ] Python采集脚本（akshare+临时表机制）
- [ ] 数据质量校验与日志

### Phase 2: 后端核心（2周）
- [ ] Spring Boot 3.2项目搭建
- [ ] 基金检索API（含拼音搜索）
- [ ] 全维指标查询接口

### Phase 3: 智能决策（2周）
- [ ] 全维指标计算（Python）
- [ ] 评分模型与信号引擎
- [ ] 持仓管理与分析

### Phase 4: 可视化与优化（2周）
- [ ] Vue3前端界面
- [ ] 数据监控告警
- [ ] 性能优化与文档

---

## 十一、总结：四份设计融合终版

### 各设计贡献总结

```
┌─────────────────────────────────────────────────────────────────┐
│                     终版设计成分溯源                            │
├─────────────────────────────────────────────────────────────────┤
│  Design One 贡献：                                              │
│    ✅ 买卖信号规则（PE分位/回撤/收益率）                        │
│    ✅ 交易记录与信号复盘机制                                    │
│    ✅ 轻量架构思想                                              │
├─────────────────────────────────────────────────────────────────┤
│  Design Two 贡献：                                              │
│    ✅ Java 17 + Spring Boot 3.2 LTS                            │
│    ✅ Redis缓存架构                                             │
│    ✅ 评分模型权重设计（30/25/20/15/10）                        │
├─────────────────────────────────────────────────────────────────┤
│  Design Three 贡献：                                            │
│    ✅ 数据库即接口（临时表+校验+合并）                          │
│    ✅ 数据质量监控与告警机制                                    │
│    ✅ 拼音首字母搜索优化                                        │
│    ✅ 三服务简洁Docker部署                                      │
├─────────────────────────────────────────────────────────────────┤
│  Design Four 贡献：                                             │
│    ✅ 全维指标体系（夏普/索提诺/卡玛/阿尔法/贝塔/信息比率）    │
│    ✅ 详尽的表设计（基金经理/持仓/分区表预留）                  │
│    ✅ 生产级考虑（高可用/监控/合规）                            │
└─────────────────────────────────────────────────────────────────┘
```

### 关键设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| Spring Boot版本 | **3.2.x** | 采纳Two的LTS建议，放弃Four的2.7 |
| 定时调度 | **APScheduler** | 比Celery轻量，比Crontab灵活 |
| 数据解耦 | **临时表机制** | 采纳Three的方案，轻量有效 |
| 分区表 | **预留但不启用** | 数据量<500万时单表即可 |
| 指标维度 | **全维指标** | 采纳Four的详尽设计 |
| 搜索优化 | **拼音首字母** | 采纳Three的体验优化 |

### 架构演进预留

```
Phase 1 (MVP)           Phase 2 (数据增长)        Phase 3 (高可用)
    │                         │                       │
    ▼                         ▼                       ▼
┌─────────┐             ┌─────────────┐        ┌─────────────┐
│ 单表    │ ──────────► │ 分区表      │ ─────► │ 主从复制    │
│ 无缓存  │             │ 冷热分离    │        │ 读写分离    │
│ 单节点  │             │ Redis缓存   │        │ MQ集群      │
└─────────┘             └─────────────┘        └─────────────┘
```

---

**设计完成时间**：2026-02-28  
**架构师**：OpenClaw  
**版本**：v2.0 Final（整合四份设计精华）
