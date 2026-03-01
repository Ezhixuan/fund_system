# P5-03: WebSocket 实时推送

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P5-03 |
| 名称 | WebSocket 实时推送 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 3天 |
| 依赖 | P5-02（准实时估值采集）|

---

## 需求描述

实现 WebSocket 实时推送系统：
1. 基金详情页实时接收估值更新
2. 持仓页面实时接收多只基金更新
3. 订阅管理（只推送用户正在查看的基金）
4. Redis 缓存最新估值

---

## 技术方案

```
Python采集完成
    ↓
写入数据库
    ↓
写入Redis缓存
    ↓
WebSocket推送
    ├─→ /topic/fund/{code}/intraday (详情页)
    └─→ /user/queue/portfolio/intraday (持仓页)
```

---

## 实现步骤

### Day 1: Spring Boot WebSocket 配置
- [ ] WebSocketConfig 配置类
  - [ ] STOMP协议配置
  - [ ] Endpoint注册
  - [ ] 消息代理配置
- [ ] WebSocketSecurity 安全配置
- [ ] 跨域配置

### Day 2: 推送服务 + 订阅管理
- [ ] IntradayPushService
  - [ ] pushToFundDetail() - 推送到详情页
  - [ ] pushToPortfolio() - 推送到持仓页
  - [ ] broadcastUpdate() - 广播更新
- [ ] WebSocketSessionManager
  - [ ] subscribeFund() - 订阅基金
  - [ ] unsubscribeFund() - 取消订阅
  - [ ] hasSubscribers() - 检查是否有订阅者
- [ ] Redis缓存
  - [ ] 最新估值缓存
  - [ ] 缓存更新逻辑

### Day 3: Python推送触发 + 前端接入
- [ ] Python采集完成后触发推送
  - [ ] HTTP回调Java API
  - [ ] 或直接Redis订阅
- [ ] 前端 WebSocket 客户端
  - [ ] 详情页订阅
  - [ ] 持仓页订阅
  - [ ] 重连机制

---

## 核心代码

### Spring Boot WebSocket 配置
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单内存消息代理
        config.enableSimpleBroker("/topic", "/queue");
        // 应用前缀
        config.setApplicationDestinationPrefixes("/app");
        // 用户前缀
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

### 推送服务
```java
@Service
@Slf4j
public class IntradayPushService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    /**
     * 推送到基金详情页
     */
    public void pushToFundDetail(IntradayEstimate estimate) {
        String fundCode = estimate.getFundCode();
        
        // 检查是否有订阅者（优化：无订阅者不推送）
        if (!sessionManager.hasFundSubscribers(fundCode)) {
            return;
        }
        
        IntradayEstimateVO vo = convertToVO(estimate);
        
        messagingTemplate.convertAndSend(
            "/topic/fund/" + fundCode + "/intraday",
            vo
        );
        
        log.debug("推送估值更新 {}: {}", fundCode, estimate.getEstimateNav());
    }
    
    /**
     * 推送到持仓页面（只推送给持有该基金的用户）
     */
    public void pushToPortfolio(IntradayEstimate estimate) {
        String fundCode = estimate.getFundCode();
        
        // 获取持有该基金的用户列表
        List<Long> userIds = portfolioService.getUserIdsByFund(fundCode);
        
        IntradayEstimateVO vo = convertToVO(estimate);
        
        for (Long userId : userIds) {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/portfolio/intraday",
                vo
            );
        }
    }
    
    /**
     * 批量推送
     */
    public void batchPush(List<IntradayEstimate> estimates) {
        for (IntradayEstimate estimate : estimates) {
            pushToFundDetail(estimate);
            pushToPortfolio(estimate);
        }
    }
}
```

### 订阅管理
```java
@Component
public class WebSocketSessionManager {
    
    // fundCode -> Set<sessionId>
    private Map<String, Set<String>> fundSubscribers = new ConcurrentHashMap<>();
    
    // sessionId -> fundCode
    private Map<String, String> sessionToFund = new ConcurrentHashMap<>();
    
    public void subscribeFund(String fundCode, String sessionId) {
        fundSubscribers.computeIfAbsent(fundCode, k -> ConcurrentHashMap.newKeySet())
                      .add(sessionId);
        sessionToFund.put(sessionId, fundCode);
    }
    
    public void unsubscribe(String sessionId) {
        String fundCode = sessionToFund.remove(sessionId);
        if (fundCode != null) {
            Set<String> subscribers = fundSubscribers.get(fundCode);
            if (subscribers != null) {
                subscribers.remove(sessionId);
                if (subscribers.isEmpty()) {
                    fundSubscribers.remove(fundCode);
                }
            }
        }
    }
    
    public boolean hasFundSubscribers(String fundCode) {
        Set<String> subscribers = fundSubscribers.get(fundCode);
        return subscribers != null && !subscribers.isEmpty();
    }
}
```

### 前端订阅
```typescript
// composables/useIntradayWebSocket.ts
import { useWebSocket } from '@vueuse/core'

export function useIntradayWebSocket(fundCode: string) {
  const { data, status, close } = useWebSocket(
    () => `ws://localhost:8080/ws`,
    {
      autoReconnect: true,
      heartbeat: true,
      onConnected: (ws) => {
        // 连接成功后订阅
        ws.send(JSON.stringify({
          type: 'subscribe',
          destination: `/topic/fund/${fundCode}/intraday`
        }))
      }
    }
  )
  
  const estimate = computed(() => {
    if (!data.value) return null
    return JSON.parse(data.value)
  })
  
  return { estimate, status, close }
}
```

---

## Redis 缓存设计

```java
@Service
public class IntradayCacheService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String KEY_PREFIX = "fund:estimate:";
    private static final long TTL_MINUTES = 15;
    
    public void cacheEstimate(IntradayEstimate estimate) {
        String key = KEY_PREFIX + estimate.getFundCode();
        String value = JSON.toJSONString(estimate);
        redisTemplate.opsForValue().set(key, value, TTL_MINUTES, TimeUnit.MINUTES);
    }
    
    public IntradayEstimate getCachedEstimate(String fundCode) {
        String key = KEY_PREFIX + fundCode;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return JSON.parseObject(value, IntradayEstimate.class);
        }
        return null;
    }
}
```

---

## 验收标准

- [ ] WebSocket服务可连接
- [ ] 详情页能收到估值推送
- [ ] 持仓页能收到多只基金推送
- [ ] 订阅管理正常（页面关闭取消订阅）
- [ ] Redis缓存正常
- [ ] 15分钟后缓存自动过期

---

## 测试计划

测试日志: P5-03-test-log.md

---

**制定日期**: 2026-03-02
