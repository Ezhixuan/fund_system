# Phase 5: 精准关注与智能调度 - 最终版

## 版本信息
- **版本**: v1.2 (最终版)
- **更新日期**: 2026-03-02
- **更新说明**: 根据产品确认，明确数据定义、推送策略、保留策略

---

## 关键概念定义

### 当日点位（Intraday Point）
**定义**: 每10分钟采集的**预估涨跌幅**（不是最终净值）

**示例**:
```
时间        预估净值    预估涨跌幅（当日点位）
09:30       1.5234      +0.15%
09:40       1.5256      +0.29%
09:50       1.5210      -0.01%
...
15:00       1.5289      +0.78%  ← 收盘预估

T+1日公布:
实际净值    1.5291      +0.79%  ← 实际涨跌幅（存入fund_nav表）
```

**区别**:
- **当日点位**（intraday）: 每10分钟的预估涨跌幅，用于实时显示和分时图
- **当日净值**（nav）: 收盘后公布的实际净值，用于精确计算和历史记录

### 数据保留策略

| 数据类型 | 保留时长 | 用途 |
|---------|---------|------|
| 当日点位（预估涨跌幅） | **1个月** | 分时图显示、短期回测 |
| 当日净值（实际涨跌幅） | **长期保留** | 精确收益计算、长期回测 |
| 实时估值（最新一条） | **Redis 15分钟** | 快速读取 |

---

## 准实时估值系统详细设计

### 1. 数据采集

#### 数据源配置
```python
# config/data_source.py
DATA_SOURCES = {
    'primary': {
        'name': 'akshare',
        'module': 'akshare.fund_em_value_estimation',
        'rate_limit': '10/minute',
        'priority': 1
    },
    'backup1': {
        'name': 'eastmoney_api',
        'url': 'https://fundmobapi.eastmoney.com/FundMNewApi/FundMNFInfo',
        'rate_limit': '30/minute',
        'priority': 2
    },
    'backup2': {
        'name': 'danjuan_api',
        'url': 'https://danjuanfunds.com/djapi/fund/{fund_code}',
        'rate_limit': '20/minute',
        'priority': 3
    }
}
```

#### 采集策略
```python
class IntradayCollector:
    def collect_single_fund(self, fund_code):
        """采集单只基金估值，带备用源切换"""
        for source in sorted(DATA_SOURCES.values(), key=lambda x: x['priority']):
            try:
                data = self.collect_from_source(fund_code, source)
                if data and self.validate_data(data):
                    return data
            except Exception as e:
                logger.warning(f"{source['name']} 采集失败 {fund_code}: {e}")
                continue
        
        logger.error(f"所有数据源均失败 {fund_code}")
        return None
    
    def validate_data(self, data):
        """数据校验"""
        # 涨跌幅范围校验
        if abs(data['change_pct']) > 15:  # 超过15%视为异常
            return False
        # 净值范围校验
        if data['nav'] < 0.1 or data['nav'] > 100:
            return False
        return True
```

### 2. 节假日/非交易时间处理

#### 判断逻辑
```python
class TradingCalendar:
    def is_trading_time(self, dt=None):
        """判断是否为交易时间"""
        if dt is None:
            dt = datetime.now()
        
        # 1. 判断是否为交易日（需要交易日历表）
        if not self.is_trading_day(dt.date()):
            return False
        
        # 2. 判断是否在交易时段
        time = dt.time()
        morning = time(9, 30) <= time <= time(11, 30)
        afternoon = time(13, 0) <= time <= time(15, 0)
        
        return morning or afternoon
    
    def get_last_trading_day(self, date=None):
        """获取上一交易日"""
        if date is None:
            date = datetime.now().date()
        
        # 查询交易日历表
        sql = """
        SELECT trade_date FROM trading_calendar 
        WHERE trade_date < %s AND is_trading_day = 1
        ORDER BY trade_date DESC LIMIT 1
        """
        return db.query_one(sql, (date,))
```

#### 节假日显示策略
```
场景1: 用户在周末打开持仓页面
↓
系统检测: 当前非交易时间
↓
显示策略: 
  - 基金列表: 显示上一交易日结算涨跌幅
  - 详情页: 显示上一交易日分时图（历史数据）
  - 提示文案: "今日为非交易日，显示上一交易日（2024-03-01）数据"

场景2: 用户在交易日9:00打开（开盘前）
↓
系统检测: 当前非交易时间（9:00 < 9:30）
↓
显示策略:
  - 同上，显示上一交易日数据
  - 倒计时提示: "距离开盘还有 30 分钟"

场景3: 用户在交易日15:30打开（收盘后）
↓
系统检测: 当前非交易时间（15:30 > 15:00）
↓
显示策略:
  - 基金列表: 显示当日预估涨跌幅（最后一次采集）
  - 提示文案: "已收盘，估值仅供参考，实际净值明日公布"
```

#### 数据库表（交易日历）
```sql
CREATE TABLE trading_calendar (
    trade_date DATE PRIMARY KEY,
    is_trading_day TINYINT DEFAULT 1 COMMENT '是否为交易日',
    is_holiday TINYINT DEFAULT 0 COMMENT '是否为节假日',
    holiday_name VARCHAR(50) COMMENT '节假日名称',
    prev_trading_day DATE COMMENT '上一交易日',
    next_trading_day DATE COMMENT '下一交易日',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 初始化2024-2025年交易日历数据
```

### 3. WebSocket 推送策略

#### 推送范围
```java
@Service
public class IntradayPushService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 批量推送估值更新
     */
    public void pushIntradayUpdate(List<IntradayEstimate> estimates) {
        for (IntradayEstimate estimate : estimates) {
            String fundCode = estimate.getFundCode();
            
            // 1. 推送给该基金详情页的订阅者
            messagingTemplate.convertAndSend(
                "/topic/fund/" + fundCode + "/intraday", 
                estimate
            );
            
            // 2. 推送给持仓页面（该用户在持仓中持有此基金）
            List<Long> userIds = portfolioService.getUsersByFund(fundCode);
            for (Long userId : userIds) {
                messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/portfolio/intraday",
                    estimate
                );
            }
        }
    }
}
```

#### 订阅规则
```javascript
// 前端 WebSocket 订阅
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    // 场景1: 用户在基金详情页
    // 订阅该基金的实时估值
    stompClient.subscribe('/topic/fund/005827/intraday', (message) => {
        const estimate = JSON.parse(message.body);
        updateIntradayChart(estimate);
        updateRealtimeInfo(estimate);
    });
    
    // 场景2: 用户在持仓页面
    // 订阅用户持仓的所有基金的实时估值
    stompClient.subscribe('/user/queue/portfolio/intraday', (message) => {
        const estimate = JSON.parse(message.body);
        updatePortfolioItem(estimate.fundCode, estimate);
    });
});
```

#### 连接管理
```java
@Component
public class WebSocketSessionManager {
    
    // 记录哪些基金正在被用户查看
    private Map<String, Set<String>> fundSubscribers = new ConcurrentHashMap<>();
    
    // 用户进入基金详情页
    public void subscribeFund(String fundCode, String sessionId) {
        fundSubscribers.computeIfAbsent(fundCode, k -> ConcurrentHashMap.newKeySet())
                      .add(sessionId);
    }
    
    // 检查基金是否有订阅者（用于优化推送）
    public boolean hasSubscribers(String fundCode) {
        Set<String> subscribers = fundSubscribers.get(fundCode);
        return subscribers != null && !subscribers.isEmpty();
    }
}
```

### 4. 数据存储策略（明确）

#### 表结构设计
```sql
-- 实时估值原始数据（保留1个月）
CREATE TABLE fund_estimate_intraday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL,
    estimate_time DATETIME NOT NULL COMMENT '采集时间（每10分钟）',
    estimate_nav DECIMAL(10,4) COMMENT '预估净值',
    estimate_change_pct DECIMAL(8,4) COMMENT '预估涨跌幅%（当日点位）',
    pre_close_nav DECIMAL(10,4) COMMENT '昨日收盘净值',
    data_source VARCHAR(20) DEFAULT 'akshare' COMMENT '数据来源',
    is_trading_time TINYINT DEFAULT 1 COMMENT '是否为交易时间采集',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fund_time (fund_code, estimate_time),
    INDEX idx_fund_date (fund_code, trade_date),
    INDEX idx_time (estimate_time)
) PARTITION BY RANGE COLUMNS(estimate_time) (
    PARTITION p202403 VALUES LESS THAN ('2024-04-01'),
    PARTITION p202404 VALUES LESS THAN ('2024-05-01'),
    PARTITION p_max VALUES LESS THAN (MAXVALUE)
);

-- 自动清理1个月前的数据
-- 通过分区表删除旧分区，或定时任务清理
```

#### 清理策略
```sql
-- 方式1: 分区表（推荐）
-- 每月初创建新分区，删除3个月前的分区

-- 方式2: 定时清理（备选）
DELETE FROM fund_estimate_intraday 
WHERE estimate_time < DATE_SUB(NOW(), INTERVAL 1 MONTH);
```

### 5. 前端展示逻辑

#### 持仓列表页
```vue
<template>
  <div class="portfolio-list">
    <div v-for="item in portfolio" :key="item.fundCode" class="fund-item">
      <div class="fund-name">{{ item.fundName }}</div>
      
      <!-- 实时估值显示 -->
      <div class="realtime-data" v-if="isTradingTime">
        <span class="estimate-nav">{{ item.estimateNav }}</span>
        <span :class="['change-pct', item.estimateChangePct >= 0 ? 'up' : 'down']">
          {{ item.estimateChangePct >= 0 ? '+' : '' }}{{ item.estimateChangePct }}%
        </span>
        <span class="update-time">{{ item.lastUpdateTime }}</span>
      </div>
      
      <!-- 非交易时间显示 -->
      <div class="last-close-data" v-else>
        <span class="nav">{{ item.lastNav }}</span>
        <span :class="['change-pct', item.lastChangePct >= 0 ? 'up' : 'down']">
          {{ item.lastChangePct >= 0 ? '+' : '' }}{{ item.lastChangePct }}%
        </span>
        <span class="hint">上一交易日（{{ item.lastTradingDate }}）</span>
      </div>
      
      <div class="holdings-info">
        持仓: {{ item.holdShares }}份
        成本: {{ item.avgCost }}
        收益: {{ item.totalReturn }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useWebSocket } from '@vueuse/core'

const isTradingTime = computed(() => {
  const now = new Date()
  const hour = now.getHours()
  const minute = now.getMinutes()
  const time = hour * 100 + minute
  
  // 交易时间: 9:30-11:30, 13:00-15:00
  return (time >= 930 && time <= 1130) || (time >= 1300 && time <= 1500)
})

// WebSocket 接收实时推送
const { data } = useWebSocket('/ws/portfolio')
watch(data, (newData) => {
  const update = JSON.parse(newData)
  updatePortfolioItem(update.fundCode, update)
})
</script>
```

---

## 实施计划（更新）

### P5-1: 关注列表 + 交易日历（4天）
- [ ] trading_calendar 表创建和数据初始化
- [ ] user_watchlist 表和API
- [ ] 关注列表前端页面
- [ ] 节假日判断工具类

### P5-2: 准实时估值采集（4天）
- [ ] fund_estimate_intraday 表（分区表）
- [ ] Python 采集服务（多数据源+备用切换）
- [ ] APScheduler 定时调度
- [ ] 节假日/非交易时间处理

### P5-3: WebSocket 实时推送（3天）
- [ ] Spring Boot WebSocket 配置
- [ ] 订阅管理（详情页+持仓页）
- [ ] 估值更新推送逻辑
- [ ] 前端 WebSocket 接入

### P5-4: 当日分时图（3天）
- [ ] ECharts 分时图组件
- [ ] 历史数据查询API
- [ ] 实时数据更新
- [ ] 非交易时间显示处理

### P5-5: 持仓页面集成（2天）
- [ ] 持仓列表实时估值显示
- [ ] 交易时间/非交易时间切换
- [ ] 预估收益计算

**总计**: 16天（约3周）

---

## 数据流图（完整）

```
交易日 09:30-15:00
    ↓ 每10分钟
APScheduler 触发
    ↓
获取用户自选基金列表（50只）
    ↓
遍历基金:
  ├─ 尝试 akshare 获取估值
  ├─ 失败? → 尝试东方财富API
  ├─ 失败? → 尝试蛋卷API
  └─ 全部失败 → 记录错误，跳过
    ↓
数据校验（涨跌幅<15%，净值范围）
    ↓
写入 fund_estimate_intraday
    ↓
更新 Redis 缓存（最新估值）
    ↓
WebSocket 推送:
  ├─ /topic/fund/{code}/intraday → 详情页订阅者
  └─ /user/queue/portfolio/intraday → 持仓页用户
    ↓
前端接收更新:
  ├─ 详情页 → 更新分时图
  └─ 持仓页 → 更新基金卡片
```

非交易时间:
```
用户打开页面
    ↓
TradingCalendar.isTradingTime() = false
    ↓
查询上一交易日日期
    ↓
查询 fund_nav 表获取上一交易日净值和涨跌幅
    ↓
显示 "上一交易日（2024-03-01）数据"
    ↓
不建立 WebSocket 连接（节省资源）
```

---

## 风险点

1. **数据源稳定性**
   - 风险: akshare 接口可能变动或限流
   - 应对: 多数据源备用机制

2. **WebSocket 连接数**
   - 风险: 大量用户同时在线，连接数爆炸
   - 应对: 只推送用户正在查看的基金，页面关闭取消订阅

3. **数据准确性**
   - 风险: 预估涨跌幅与实际净值可能有偏差
   - 应对: 页面明确标注 "估值仅供参考，实际净值以基金公司公布为准"

---

## 待开发前确认

- [ ] akshare 实时估值接口调研（确认字段和稳定性）
- [ ] 东方财富/蛋卷备用接口调研
- [ ] 2024-2025年交易日历数据准备

