# Phase 5: 精准关注与智能调度 - 修订版

## 版本信息
- **版本**: v1.1 (修订版)
- **修订日期**: 2026-03-02
- **修订说明**: 根据产品讨论，增加实时估值和当日走势功能

---

## 设计原则

### 核心理念
**"我只关心我想关心的，我只看我需要看的"**

### 系统定位
**V1.0**: 基于规则的辅助决策系统  
**V2.0**: 规则回测优化系统（未来）

---

## 功能矩阵（更新）

| 功能 | 蚂蚁/天天基金 | 本系统定位 |
|------|--------------|-----------|
| 全市场实时行情 | ✅ 有（延时） | ❌ **不做** - 只关注自选 |
| 自选实时估值 | ⚠️ 延时15分钟+ | ✅ **准实时** - 10分钟间隔 |
| 当日分时走势 | ✅ 有 | ✅ **有** - 基于估值数据 |
| 板块分析 | ✅ 详细 | ❌ **不做** - 跳转外部 |
| 持仓管理 | ⚠️ 基础 | ✅ **重点** - 精细化成本 |
| 定投分析 | ❌ 很少 | ✅ **支持** - XIRR计算 |
| 基于规则的决策 | ❌ 主观 | ✅ **核心** - 信号辅助 |
| 规则回测 | ❌ 无 | ✅ **V2核心** - 历史验证 |

---

## 核心功能设计

### 1. 我的关注列表（Watchlist）

#### 功能描述
- 用户只添加自己**真实持有**或**正在关注**的基金（预计20-50只）
- 系统**只针对这些基金**提供：
  - 准实时估值（10分钟间隔）
  - 当日分时走势
  - 详细数据分析

#### 数据模型

```sql
-- 关注列表表
CREATE TABLE user_watchlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    fund_name VARCHAR(100),
    add_date DATE,
    watch_type TINYINT COMMENT '1-持有, 2-关注',
    target_return DECIMAL(5,2),
    stop_loss DECIMAL(5,2),
    notes VARCHAR(500),
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund (fund_code)
);

-- 关注基金配置表
CREATE TABLE watch_fund_config (
    fund_code VARCHAR(10) PRIMARY KEY,
    need_detail TINYINT DEFAULT 1,
    need_nav TINYINT DEFAULT 1,
    need_intraday TINYINT DEFAULT 1 COMMENT '是否需要实时估值',
    need_portfolio TINYINT DEFAULT 0,
    last_collect_date DATE,
    last_intraday_time DATETIME COMMENT '最后估值时间',
    collect_interval_minutes INT DEFAULT 10 COMMENT '估值采集间隔（分钟）'
);
```

---

### 2. 准实时估值系统

#### 功能描述
- **采集频率**: 每10分钟（交易日 9:30-11:30, 13:00-15:00）
- **覆盖范围**: 只拉取用户自选基金（不是全市场）
- **数据用途**: 
  - 显示实时估值
  - 绘制当日分时走势
  - 计算当日预估收益

#### 数据模型

```sql
-- 实时估值快照表（分区表）
CREATE TABLE fund_estimate_intraday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    estimate_time DATETIME NOT NULL,
    estimate_nav DECIMAL(10,4),
    estimate_change_pct DECIMAL(8,4) COMMENT '估值涨跌幅%',
    estimate_change_amt DECIMAL(10,4) COMMENT '估值涨跌额',
    actual_nav DECIMAL(10,4) COMMENT '实际净值（收盘后更新）',
    pre_close_nav DECIMAL(10,4) COMMENT '昨日收盘净值',
    trade_date DATE NOT NULL,
    data_source VARCHAR(20) DEFAULT 'akshare',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_time (fund_code, estimate_time),
    INDEX idx_fund_date (fund_code, trade_date),
    INDEX idx_time (estimate_time)
) PARTITION BY RANGE (YEAR(trade_date)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 当日汇总表（收盘后更新）
CREATE TABLE fund_intraday_summary (
    fund_code VARCHAR(10) NOT NULL,
    trade_date DATE NOT NULL,
    open_estimate DECIMAL(10,4),
    high_estimate DECIMAL(10,4),
    low_estimate DECIMAL(10,4),
    close_estimate DECIMAL(10,4),
    close_actual DECIMAL(10,4),
    avg_estimate DECIMAL(10,4),
    volatility DECIMAL(6,4) COMMENT '当日波动率',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (fund_code, trade_date)
);
```

#### 数据量估算

```
用户关注基金数: 50只
采集频率: 每10分钟
交易日时长: 4小时 (9:30-11:30, 13:00-15:00)
每日采集次数: 6次/小时 × 4小时 = 24次/只

日数据量: 50只 × 24次 = 1200条
年数据量: 1200 × 250交易日 = 30万条
存储空间: 约 50MB/年（含索引）

→ 分区表设计，保留2年数据足够
```

#### 采集流程

```python
# 实时估值采集调度器
class IntradayCollector:
    def __init__(self):
        self.scheduler = BackgroundScheduler()
    
    def start(self):
        """启动定时采集"""
        # 交易日每10分钟执行
        self.scheduler.add_job(
            self.collect_intraday,
            'cron',
            day_of_week='mon-fri',
            hour='9-11,13-14',
            minute='*/10',
            second='30'  # 延迟30秒，避免整点高峰
        )
        # 开盘和收盘特殊时间点
        self.scheduler.add_job(
            self.collect_intraday,
            'cron',
            day_of_week='mon-fri',
            hour='9',
            minute='30'
        )
        self.scheduler.add_job(
            self.collect_intraday,
            'cron',
            day_of_week='mon-fri',
            hour='11',
            minute='30'
        )
        self.scheduler.start()
    
    def collect_intraday(self):
        """执行采集"""
        # 1. 获取用户自选基金列表
        watchlist = self.get_watchlist()
        
        # 2. 批量获取实时估值
        for fund_code in watchlist:
            try:
                estimate = ak.fund_em_value_estimation(fund_code)
                self.save_estimate(fund_code, estimate)
            except Exception as e:
                logger.error(f"采集失败 {fund_code}: {e}")
        
        # 3. 推送WebSocket通知
        self.notify_frontend()
```

---

### 3. 当日分时走势图

#### 功能描述
- 基于实时估值数据绘制当日走势
- 显示：时间-估值曲线
- 对比：昨日收盘线（水平参考线）

#### 前端实现

```vue
<template>
  <div class="intraday-chart">
    <v-chart :option="chartOption" />
    <div class="realtime-info">
      <span class="estimate-nav">{{ currentEstimate.nav }}</span>
      <span :class="['change-pct', changePct > 0 ? 'up' : 'down']">
        {{ changePct > 0 ? '+' : '' }}{{ changePct }}%
      </span>
      <span class="update-time">{{ lastUpdateTime }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useWebSocket } from '@vueuse/core'

const props = defineProps({
  fundCode: String
})

// WebSocket 实时接收估值更新
const { data } = useWebSocket(`ws://localhost:8080/ws/intraday/${props.fundCode}`)

// ECharts 配置
const chartOption = ref({
  xAxis: {
    type: 'time',
    min: '09:30',
    max: '15:00',
    splitLine: { show: false }
  },
  yAxis: {
    type: 'value',
    scale: true,
    splitLine: { lineStyle: { type: 'dashed' } }
  },
  series: [{
    type: 'line',
    data: [], // 实时估值数据
    smooth: true,
    symbol: 'none',
    lineStyle: { width: 2 }
  }, {
    type: 'line',
    markLine: {
      data: [{ yAxis: preCloseNav }], // 昨日收盘参考线
      lineStyle: { type: 'dashed', color: '#999' }
    }
  }]
})

// 接收WebSocket推送，更新图表
watch(data, (newData) => {
  const estimate = JSON.parse(newData)
  chartOption.value.series[0].data.push([
    estimate.time,
    estimate.nav
  ])
})
</script>
```

---

### 4. V2.0 规则回测系统（规划）

#### 目标
针对不同的规则组合进行历史回测，寻找最优的买卖信号参数。

#### 可回测规则

| 规则类型 | 参数示例 |
|---------|---------|
| 估值分位 | 买入阈值: 20% / 30% / 40% |
| 回撤控制 | 止损线: -10% / -15% / -20% |
| 目标收益 | 止盈线: 10% / 20% / 30% |
| 评分权重 | 收益40%+风险30% / 收益50%+风险20% |
| 持有时间 | 最小持有: 7天 / 30天 / 90天 |

#### 回测流程

```python
# 回测引擎
class BacktestEngine:
    def backtest(self, fund_code, strategy_config, start_date, end_date):
        """
        执行回测
        
        Args:
            fund_code: 基金代码
            strategy_config: 策略配置（规则参数）
            start_date: 回测开始日期
            end_date: 回测结束日期
        
        Returns:
            回测结果：收益率、最大回撤、交易次数、胜率等
        """
        # 1. 获取历史数据
        nav_history = self.get_nav_history(fund_code, start_date, end_date)
        
        # 2. 模拟交易
        trades = []
        position = 0  # 0-空仓, 1-持仓
        
        for date, nav in nav_history:
            signal = self.generate_signal(date, nav, strategy_config)
            
            if signal == 'buy' and position == 0:
                trades.append({'type': 'buy', 'date': date, 'price': nav})
                position = 1
            elif signal == 'sell' and position == 1:
                trades.append({'type': 'sell', 'date': date, 'price': nav})
                position = 0
        
        # 3. 计算指标
        total_return = self.calc_return(trades)
        max_drawdown = self.calc_max_drawdown(trades)
        win_rate = self.calc_win_rate(trades)
        
        return {
            'total_return': total_return,
            'max_drawdown': max_drawdown,
            'win_rate': win_rate,
            'trade_count': len(trades) // 2,
            'trades': trades
        }
```

#### 输出示例

```json
{
  "strategy": "估值分位20%+止损15%",
  "period": "2020-01-01 ~ 2024-12-31",
  "total_return": "45.2%",
  "annualized_return": "9.8%",
  "max_drawdown": "-12.5%",
  "win_rate": "68%",
  "trade_count": 12,
  "recommendation": "推荐",
  "comparison": {
    "buy_and_hold": "38.5%",
    "this_strategy": "45.2%",
    "outperform": "+6.7%"
  }
}
```

---

## 技术架构

### 实时数据采集流程

```
交易日时间线
09:30 ──10min──┬──10min──┬──10min──┬──10min──┬──10min──┐
              09:40    09:50    10:00    10:10    10:20
               │         │         │         │         │
               ↓         ↓         ↓         ↓         ↓
            Python调度器（APScheduler）
               │         │         │         │         │
               ↓         ↓         ↓         ↓         ↓
            获取自选基金列表（MySQL）
               │         │         │         │         │
               ↓         ↓         ↓         ↓         ↓
            批量调用akshare获取估值
               │         │         │         │         │
               ↓         ↓         ↓         ↓         ↓
            写入fund_estimate_intraday
               │         │         │         │         │
               ↓         ↓         ↓         ↓         ↓
            推送WebSocket通知
               │         │         │         │         │
               ↓         ↓         ↓         ↓         ↓
            前端更新分时图
```

### 存储策略

| 数据类型 | 存储位置 | 说明 |
|---------|---------|------|
| 最新估值 | Redis | TTL 15分钟，快速读取 |
| 当日点位 | MySQL | 分区表，持久化存储 |
| 历史估值 | MySQL | 保留90天，用于对比 |
| 日终净值 | MySQL | 长期保留 |

---

## 待确认事项

1. **实时估值数据源**
   - [ ] akshare 接口调研确认
   - [ ] 备用数据源准备（天天基金/蛋卷）
   - [ ] 接口限流策略

2. **WebSocket推送策略**
   - [ ] 推送给所有在线用户？
   - [ ] 还是只推送给打开了详情页的用户？

3. **历史数据保留**
   - [ ] 当日点位保留90天？
   - [ ] 是否需要保留历史分时用于对比？

4. **V2回测细节**
   - [ ] 回测时间范围（建议3-5年）
   - [ ] 支持的规则组合数量
   - [ ] 回测结果展示形式

---

## 实施计划

### Phase 5-1: 关注列表（3天）
- [ ] 数据库表设计
- [ ] 后端API开发
- [ ] 前端页面开发

### Phase 5-2: 实时估值（4天）
- [ ] 实时估值采集调度
- [ ] 数据存储和缓存
- [ ] WebSocket推送

### Phase 5-3: 当日走势图（3天）
- [ ] 前端分时图组件
- [ ] 实时数据接入
- [ ] 详情页集成

### Phase 5-4: Java调度服务（3天）
- [ ] Python API封装
- [ ] 触发式调度
- [ ] 前端刷新功能

### Phase 5-5: 定投分析（3天）
- [ ] 定投计划管理
- [ ] XIRR计算
- [ ] 收益分析

**总计**: 16天（约3周）

---

## 版本规划

### V1.0（当前Phase 5）
- 我的关注
- 准实时估值（10分钟）
- 当日分时走势
- 基于规则的辅助决策
- 定投分析

### V2.0（未来）
- 规则回测引擎
- 最优参数推荐
- 多策略对比
- 历史验证报告

