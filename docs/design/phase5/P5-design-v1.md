# Phase 5: 精准关注与智能调度

## 设计原则

### 核心理念
**"我只关心我想关心的，我只看我需要看的"**

### 与其他软件差异化
| 功能 | 其他软件 | 本系统定位 |
|------|---------|-----------|
| 全市场基金 | ✅ 大而全 | ❌ 不做，只关注用户自选 |
| 板块分析 | ✅ 专业图表 | ❌ 不做，提供跳转链接 |
| 实时行情 | ✅ 秒级更新 | ❌ 不做，日终数据足够 |
| 持仓管理 | ⚠️ 基础功能 | ✅ 核心功能，重点优化 |
| 交易记录 | ⚠️ 简单记录 | ✅ 精细化成本计算 |
| 定投分析 | ❌ 很少支持 | ✅ 重点功能 |
| 买卖信号 | ❌ 主观判断 | ✅ 基于规则的辅助决策 |

---

## 核心功能设计

### 1. 我的关注列表（Watchlist）

#### 功能描述
- 用户只添加自己**真实持有**或**正在关注**的基金
- 系统**只采集**这些关注基金的详细数据
- 避免全市场 26000+ 基金的数据噪音

#### 数据模型
```sql
-- 关注列表表
CREATE TABLE user_watchlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    fund_name VARCHAR(100),
    add_date DATE COMMENT '添加日期',
    watch_type TINYINT COMMENT '1-持有, 2-关注',
    target_return DECIMAL(5,2) COMMENT '目标收益率%',
    stop_loss DECIMAL(5,2) COMMENT '止损线%',
    notes VARCHAR(500) COMMENT '备注',
    is_active TINYINT DEFAULT 1,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund (fund_code)
);

-- 关注基金采集配置
CREATE TABLE watch_fund_config (
    fund_code VARCHAR(10) PRIMARY KEY,
    need_detail TINYINT DEFAULT 1 COMMENT '是否需要详细信息',
    need_nav TINYINT DEFAULT 1 COMMENT '是否需要净值',
    need_portfolio TINYINT DEFAULT 0 COMMENT '是否需要持仓',
    last_collect_date DATE COMMENT '最后采集日期',
    collect_interval_days INT DEFAULT 1 COMMENT '采集间隔天数'
);
```

#### 接口设计
```java
// 添加关注
POST /api/watchlist/add
{
    "fundCode": "005827",
    "watchType": 1,  // 1-持有, 2-关注
    "targetReturn": 20.0,
    "stopLoss": -10.0,
    "notes": "张坤管理，长期看好"
}

// 获取我的关注列表
GET /api/watchlist/list?type=1

// 移除关注
DELETE /api/watchlist/{fundCode}

// 批量导入（从持仓自动导入）
POST /api/watchlist/import-from-portfolio
```

---

### 2. 智能调度系统（三层架构）

#### 架构设计
```
用户操作（前端）
    ↓ HTTP
Java 后端（调度器）
    ↓ HTTP/消息队列
Python 采集服务
    ↓ akshare
外部数据源
```

#### 触发机制

##### 2.1 主动触发（用户驱动）
| 场景 | 触发方式 | 采集范围 |
|------|---------|---------|
| 添加新关注基金 | 用户点击"添加关注" | 该基金基础信息+历史净值 |
| 查看基金详情 | 用户进入详情页 | 如数据过期，触发更新 |
| 刷新数据 | 用户点击刷新按钮 | 所有关注基金最新净值 |
| 查看持仓分析 | 用户进入持仓页 | 确保今日数据已更新 |

##### 2.2 定时触发（系统驱动）
```
交易日 15:30: 采集关注基金当日净值
交易日 20:00: 计算关注基金指标和评分
周末 02:00:  更新关注基金详细信息
```

##### 2.3 事件触发（数据驱动）
- 持仓基金净值更新 → 触发成本价重算
- 信号条件满足 → 触发提醒通知

#### Java 调度服务设计

```java
@Service
public class FundCollectScheduler {
    
    @Autowired
    private WatchlistService watchlistService;
    
    @Autowired
    private PythonCollectClient pythonClient;
    
    /**
     * 用户添加关注时触发
     */
    public void triggerCollectOnAdd(String fundCode) {
        // 1. 检查是否需要采集
        if (needCollect(fundCode)) {
            // 2. 调用 Python 采集服务
            pythonClient.collectFundBasic(fundCode);
            pythonClient.collectFundNav(fundCode, 90); // 最近90天
            
            // 3. 更新采集状态
            updateCollectStatus(fundCode);
        }
    }
    
    /**
     * 定时任务：每日采集关注基金净值
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void scheduledDailyCollect() {
        // 1. 获取所有关注基金
        List<String> watchlist = watchlistService.getActiveFundCodes();
        
        // 2. 批量调用 Python 采集
        pythonClient.batchCollectNav(watchlist, LocalDate.now());
        
        // 3. 数据管道处理
        pythonClient.runPipeline();
    }
    
    /**
     * 用户手动刷新
     */
    public void triggerManualRefresh(String fundCode) {
        // 异步采集，不阻塞用户
        CompletableFuture.runAsync(() -> {
            pythonClient.collectFundNav(fundCode, LocalDate.now());
            // 发送 WebSocket 通知前端更新
            websocketService.notifyUpdate(fundCode);
        });
    }
}
```

#### Python 采集服务改造

```python
# 新增 API 模式（而非命令行模式）
from flask import Flask, request, jsonify

app = Flask(__name__)

collector = FundCollector()

@app.route('/api/collect/basic', methods=['POST'])
def collect_basic():
    """采集基金基础信息"""
    fund_code = request.json.get('fundCode')
    result = collector.collect_fund_basic(fund_code)
    return jsonify({'success': True, 'data': result})

@app.route('/api/collect/nav', methods=['POST'])
def collect_nav():
    """采集基金净值"""
    fund_code = request.json.get('fundCode')
    date = request.json.get('date')
    result = collector.collect_fund_nav(fund_code, date)
    return jsonify({'success': True, 'data': result})

@app.route('/api/collect/batch', methods=['POST'])
def collect_batch():
    """批量采集"""
    fund_codes = request.json.get('fundCodes')
    date = request.json.get('date')
    results = []
    for code in fund_codes:
        result = collector.collect_fund_nav(code, date)
        results.append(result)
    return jsonify({'success': True, 'data': results})

@app.route('/api/collect/pipeline', methods=['POST'])
def run_pipeline():
    """执行数据管道"""
    pipeline = DataPipeline()
    result = pipeline.run()
    return jsonify({'success': result.is_success})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

---

### 3. 板块分析简化设计

#### 定位
**不做板块分析，只做跳转引导**

#### 实现方式
```vue
<!-- 基金详情页 -->
<template>
  <div class="fund-detail">
    <!-- 基金基本信息 -->
    <FundBasicInfo :fund="fund" />
    
    <!-- 外部链接区 -->
    <div class="external-links">
      <h3>更多分析</h3>
      <a :href="`https://danjuanfunds.com/djapi/fund/${fund.code}`" target="_blank">
        蛋卷基金分析
      </a>
      <a :href="`https://fund.eastmoney.com/${fund.code}.html`" target="_blank">
        东方财富详情
      </a>
      <a :href="`https://www.howbuy.com/fund/${fund.code}/`" target="_blank">
        好买基金评级
      </a>
    </div>
  </div>
</template>
```

---

### 4. 重点关注功能强化

#### 4.1 智能提醒
```sql
-- 提醒规则表
CREATE TABLE alert_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10),
    alert_type VARCHAR(20),  -- 'target', 'stop_loss', 'signal'
    threshold DECIMAL(8,4),
    is_active TINYINT DEFAULT 1
);
```

提醒场景：
- 达到目标收益率
- 跌破止损线
- 产生买入/卖出信号
- 基金经理变更

#### 4.2 定投分析
```sql
-- 定投计划表
CREATE TABLE定投_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10),
    amount DECIMAL(10,2),      -- 每期金额
    frequency VARCHAR(10),      -- 'weekly', 'monthly'
    deduction_day INT,          -- 扣款日
    start_date DATE,
    end_date DATE,
    status TINYINT
);

-- 定投收益计算
CREATE TABLE定投_analysis (
    plan_id BIGINT,
    total_invest DECIMAL(15,2),     -- 总投入
    total_value DECIMAL(15,2),      -- 当前市值
    total_return DECIMAL(8,4),      -- 总收益率
    annualized_return DECIMAL(8,4), -- 年化收益率
    xirr DECIMAL(8,4)               -- 内部收益率
);
```

---

## 技术实现方案

### 前端改造
1. **添加"我的关注"页面**
   - 搜索添加基金
   - 关注列表管理
   - 拖拽排序

2. **首页改为"我的仪表盘"**
   - 关注基金概览
   - 今日收益汇总
   - 待处理提醒

3. **添加"手动刷新"按钮**
   - 单个基金刷新
   - 全部刷新
   - 显示最后更新时间

### 后端改造
1. **新增 watchlist 模块**
   - WatchlistController
   - WatchlistService
   - WatchlistMapper

2. **改造采集调度**
   - PythonCollectClient（HTTP 客户端）
   - CollectScheduler（调度器）
   - 从定时全量 → 触发式精准

3. **WebSocket 实时推送**
   - 采集完成通知
   - 数据更新推送

### Python 改造
1. **命令行模式 → API 服务模式**
   - Flask/FastAPI 封装
   - 提供 RESTful 接口
   - 支持 Docker 部署

2. **精准采集**
   - 只采集指定基金
   - 支持日期范围
   - 支持增量更新

---

## 实施计划

### Phase 5-1: 关注列表功能 (3天)
- [ ] 数据库表设计
- [ ] 后端 API 开发
- [ ] 前端页面开发
- [ ] 从持仓自动导入

### Phase 5-2: Python API 改造 (2天)
- [ ] Flask 服务搭建
- [ ] 接口封装
- [ ] Docker 配置

### Phase 5-3: Java 调度服务 (3天)
- [ ] Python 客户端
- [ ] 调度逻辑
- [ ] WebSocket 推送

### Phase 5-4: 前端调度集成 (2天)
- [ ] 手动刷新功能
- [ ] 进度显示
- [ ] 实时更新

### Phase 5-5: 定投分析 (3天)
- [ ] 定投计划管理
- [ ] XIRR 计算
- [ ] 收益分析图表

---

## 数据流图

```
用户添加关注基金
    ↓
前端调用 POST /api/watchlist/add
    ↓
Java 写入 watchlist 表
    ↓
触发异步采集任务
    ↓
Java 调用 Python API
    ↓
Python 采集该基金数据
    ↓
数据写入数据库
    ↓
WebSocket 通知前端
    ↓
前端自动刷新显示
```

---

## 价值点

1. **数据精准**: 只关心自己关注的基金，减少噪音
2. **资源节省**: 从全量 26000+ 基金 → 只采集关注的几十只
3. **实时可控**: 用户决定何时更新数据
4. **体验提升**: 页面加载更快，数据更相关
