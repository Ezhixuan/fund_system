# P1-01: 数据库设计与初始化 - 执行计划

> 所属阶段：Phase 1 数据基建层  
> 计划工期：3天  
> 前置依赖：无

---

## 一、任务目标
创建完整的MySQL数据库结构，包含10张核心表和优化索引。

---

## 二、执行步骤

### Day 1: 基础表创建

#### 任务 1.1: 创建数据库
```sql
CREATE DATABASE fund_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**检查点**：
- [ ] 数据库创建成功
- [ ] 字符集为utf8mb4

#### 任务 1.2: 创建 fund_info 表
```sql
CREATE TABLE fund_info (
    fund_code VARCHAR(10) PRIMARY KEY COMMENT '基金代码',
    fund_name VARCHAR(100) NOT NULL COMMENT '基金名称',
    name_pinyin VARCHAR(100) COMMENT '拼音首字母',
    fund_type VARCHAR(20) COMMENT '类型',
    invest_style VARCHAR(20) COMMENT '投资风格',
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
    status TINYINT DEFAULT 1 COMMENT '状态',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (fund_type),
    INDEX idx_manager (manager_name),
    INDEX idx_company (company_name),
    INDEX idx_pinyin (name_pinyin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金基础信息表';
```

**检查点**：
- [ ] 表创建成功
- [ ] 所有字段类型正确
- [ ] 索引生效

#### 任务 1.3: 创建 fund_nav 表
```sql
CREATE TABLE fund_nav (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    nav_date DATE NOT NULL COMMENT '净值日期',
    unit_nav DECIMAL(10,4) NOT NULL COMMENT '单位净值',
    accum_nav DECIMAL(10,4) COMMENT '累计净值',
    adjust_nav DECIMAL(10,4) COMMENT '复权净值',
    daily_return DECIMAL(8,4) COMMENT '日增长率%',
    source VARCHAR(20) DEFAULT 'akshare' COMMENT '数据来源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_date (fund_code, nav_date),
    INDEX idx_date (nav_date),
    INDEX idx_code_date (fund_code, nav_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金净值历史表';
```

**检查点**：
- [ ] 联合唯一索引生效
- [ ] 支持重复数据更新

---

### Day 2: 指标与评分表

#### 任务 2.1: 创建 fund_metrics 表
```sql
CREATE TABLE fund_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    calc_date DATE NOT NULL COMMENT '计算日期',
    return_1m DECIMAL(8,4) COMMENT '近1月收益率%',
    return_3m DECIMAL(8,4) COMMENT '近3月收益率%',
    return_1y DECIMAL(8,4) COMMENT '近1年收益率%',
    return_3y DECIMAL(8,4) COMMENT '近3年收益率%',
    return_5y DECIMAL(8,4) COMMENT '近5年收益率%',
    sharpe_ratio_1y DECIMAL(10,4) COMMENT '近1年夏普比率',
    sharpe_ratio_3y DECIMAL(10,4) COMMENT '近3年夏普比率',
    sortino_ratio_1y DECIMAL(10,4) COMMENT '近1年索提诺比率',
    calmar_ratio_3y DECIMAL(10,4) COMMENT '近3年卡玛比率',
    information_ratio_1y DECIMAL(10,4) COMMENT '近1年信息比率',
    max_drawdown_1y DECIMAL(8,4) COMMENT '近1年最大回撤%',
    max_drawdown_3y DECIMAL(8,4) COMMENT '近3年最大回撤%',
    volatility_1y DECIMAL(8,4) COMMENT '近1年波动率%',
    volatility_3y DECIMAL(8,4) COMMENT '近3年波动率%',
    alpha_1y DECIMAL(8,4) COMMENT '近1年阿尔法',
    beta_1y DECIMAL(8,4) COMMENT '近1年贝塔',
    tracking_error_1y DECIMAL(8,4) COMMENT '近1年跟踪误差',
    pe_percentile INT COMMENT 'PE估值分位0-100',
    pb_percentile INT COMMENT 'PB估值分位0-100',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_calc_date (fund_code, calc_date),
    INDEX idx_calc_date (calc_date),
    INDEX idx_sharpe_1y (sharpe_ratio_1y),
    INDEX idx_max_dd_3y (max_drawdown_3y),
    INDEX idx_return_1y (return_1y)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金全维指标表';
```

**检查点**：
- [ ] 全维指标字段完整
- [ ] 索引覆盖常用查询

#### 任务 2.2: 创建 fund_score 表
```sql
CREATE TABLE fund_score (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    calc_date DATE NOT NULL COMMENT '计算日期',
    total_score INT COMMENT '总分0-100',
    quality_level CHAR(1) COMMENT '等级：S/A/B/C/D',
    return_score INT COMMENT '收益得分(30)',
    risk_score INT COMMENT '风控得分(25)',
    stability_score INT COMMENT '稳定性(20)',
    scale_score INT COMMENT '规模适配(15)',
    fee_score INT COMMENT '费用得分(10)',
    sharpe_ratio DECIMAL(6,4) COMMENT '夏普比率',
    sortino_ratio DECIMAL(6,4) COMMENT '索提诺比率',
    calmar_ratio DECIMAL(6,4) COMMENT '卡玛比率',
    max_drawdown DECIMAL(6,4) COMMENT '最大回撤',
    volatility DECIMAL(6,4) COMMENT '波动率',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_date (fund_code, calc_date),
    INDEX idx_score (total_score),
    INDEX idx_level (quality_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金评分表';
```

**检查点**：
- [ ] 评分权重字段正确

---

### Day 3: 业务表与临时表

#### 任务 3.1: 创建 fund_manager / fund_holding 表
```sql
-- 基金经理表
CREATE TABLE fund_manager (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    manager_code VARCHAR(20) COMMENT '基金经理代码',
    manager_name VARCHAR(50) NOT NULL COMMENT '基金经理姓名',
    company_name VARCHAR(100) COMMENT '所属公司',
    start_date DATE COMMENT '任职开始日期',
    end_date DATE COMMENT '任职结束日期',
    total_funds INT COMMENT '管理基金总数',
    total_scale DECIMAL(15,2) COMMENT '管理总规模(亿元)',
    best_return DECIMAL(8,4) COMMENT '最佳任职回报%',
    tenure_years DECIMAL(4,1) COMMENT '任职年限',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_manager_name (manager_name),
    INDEX idx_company (company_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金经理信息表';

-- 基金持仓表
CREATE TABLE fund_holding (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    report_date DATE NOT NULL COMMENT '报告期',
    stock_code VARCHAR(10) COMMENT '股票代码',
    stock_name VARCHAR(100) COMMENT '股票名称',
    holding_amount BIGINT COMMENT '持股数量(股)',
    holding_value DECIMAL(15,2) COMMENT '持股市值(元)',
    holding_ratio DECIMAL(8,4) COMMENT '占净值比例%',
    holding_type VARCHAR(20) COMMENT '持仓类型',
    is_top10 TINYINT DEFAULT 0 COMMENT '是否前十大重仓',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_report_stock (fund_code, report_date, stock_code),
    INDEX idx_report_date (report_date),
    INDEX idx_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金持仓明细表';
```

**检查点**：
- [ ] 基金经理表可用
- [ ] 持仓表可用

#### 任务 3.2: 创建业务与临时表
```sql
-- 用户持仓表
CREATE TABLE user_portfolio (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    trade_type TINYINT NOT NULL COMMENT '1-买入, 2-卖出',
    trade_date DATE NOT NULL COMMENT '交易日期',
    nav_price DECIMAL(10,4) NOT NULL COMMENT '成交净值',
    trade_share DECIMAL(15,4) COMMENT '成交份额',
    trade_amount DECIMAL(15,2) COMMENT '交易金额',
    trade_fee DECIMAL(10,2) COMMENT '交易手续费',
    remark VARCHAR(200) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fund (fund_code),
    INDEX idx_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持仓记录表';

-- 决策信号历史表
CREATE TABLE signal_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    signal_date DATE NOT NULL COMMENT '信号日期',
    signal_type TINYINT NOT NULL COMMENT '1-买入, 2-持有, 3-卖出',
    reason VARCHAR(500) COMMENT '信号理由',
    pe_percentile INT COMMENT '当时PE分位',
    return_1y DECIMAL(8,4) COMMENT '当时1年收益',
    max_drawdown DECIMAL(8,4) COMMENT '当时最大回撤',
    sharpe_ratio DECIMAL(6,4) COMMENT '当时夏普比率',
    total_score INT COMMENT '当时综合评分',
    is_hit TINYINT DEFAULT 0 COMMENT '是否命中',
    hit_check_date DATE COMMENT '复盘验证日期',
    actual_return DECIMAL(8,4) COMMENT '信号后实际收益%',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_date (fund_code, signal_date),
    INDEX idx_signal (signal_type, signal_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='决策信号历史表';

-- 数据更新日志表
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

-- 临时数据表
CREATE TABLE tmp_fund_nav (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    nav_date DATE NOT NULL,
    unit_nav DECIMAL(10,4) NOT NULL,
    accum_nav DECIMAL(10,4),
    daily_return DECIMAL(8,4),
    source VARCHAR(20),
    check_status TINYINT DEFAULT 0 COMMENT '0-待校验,1-通过,2-失败',
    check_msg VARCHAR(200) COMMENT '校验信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (check_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='净值临时表';
```

**检查点**：
- [ ] 所有10张表创建成功
- [ ] 临时表机制可用

#### 任务 3.3: 初始化基础数据
```python
# init_basic_data.py
import akshare as ak
from sqlalchemy import create_engine

engine = create_engine('mysql+pymysql://fund:fund123@localhost:3306/fund_system')

# 采集基金基础列表
df = ak.fund_name_em()
df = df.rename(columns={
    '基金代码': 'fund_code',
    '基金简称': 'fund_name',
    '基金类型': 'fund_type'
})
# 插入数据库
df[['fund_code', 'fund_name', 'fund_type']].to_sql(
    'fund_info', engine, if_exists='append', index=False
)
```

**验收标准**：
- [ ] 基金基础数据>1000条
- [ ] 数据查询正常

---

## 三、验收清单

| 检查项 | 状态 | 验证方式 |
|--------|------|----------|
| 10张表创建成功 | ☐ | SHOW TABLES |
| 索引生效 | ☐ | EXPLAIN查询 |
| 基础数据>1000条 | ☐ | SELECT COUNT |
| 字符集utf8mb4 | ☐ | SHOW CREATE TABLE |

---

**执行人**：待定  
**验收人**：待定  
**更新日期**：2026-02-28
