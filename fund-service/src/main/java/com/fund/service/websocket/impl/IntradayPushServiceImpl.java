package com.fund.service.websocket.impl;

import com.fund.entity.watchlist.FundEstimateIntraday;
import com.fund.service.websocket.IntradayPushService;
import com.fund.service.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实时估值推送服务实现
 */
@Service
public class IntradayPushServiceImpl implements IntradayPushService {
    
    private static final Logger log = LoggerFactory.getLogger(IntradayPushServiceImpl.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Override
    public void pushToFundDetail(FundEstimateIntraday estimate) {
        String fundCode = estimate.getFundCode();
        
        // 检查是否有订阅者（优化：无订阅者不推送）
        if (!sessionManager.hasSubscribers(fundCode)) {
            return;
        }
        
        Map<String, Object> message = convertToMessage(estimate);
        
        // 推送到主题 /topic/fund/{fundCode}/intraday
        String destination = "/topic/fund/" + fundCode + "/intraday";
        messagingTemplate.convertAndSend(destination, message);
        
        log.debug("推送估值更新到详情页 {}: {}", fundCode, estimate.getEstimateNav());
    }
    
    @Override
    public void pushToPortfolio(FundEstimateIntraday estimate) {
        String fundCode = estimate.getFundCode();
        
        // 获取订阅该基金的会话列表
        Set<String> subscribers = sessionManager.getSubscribers(fundCode);
        if (subscribers.isEmpty()) {
            return;
        }
        
        Map<String, Object> message = convertToMessage(estimate);
        
        // 推送给每个订阅用户
        for (String sessionId : subscribers) {
            // 使用convertAndSendToUser发送点对点消息
            // 客户端订阅地址: /user/queue/portfolio/intraday
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/portfolio/intraday",
                    message
            );
        }
        
        log.debug("推送估值更新到持仓页 {}: {} 个订阅者", fundCode, subscribers.size());
    }
    
    @Override
    public void batchPush(List<FundEstimateIntraday> estimates) {
        for (FundEstimateIntraday estimate : estimates) {
            pushToFundDetail(estimate);
            pushToPortfolio(estimate);
        }
    }
    
    @Override
    public void broadcastUpdate(FundEstimateIntraday estimate) {
        Map<String, Object> message = convertToMessage(estimate);
        
        // 广播到所有订阅者
        messagingTemplate.convertAndSend("/topic/intraday/updates", message);
        
        log.debug("广播估值更新 {}: {}", estimate.getFundCode(), estimate.getEstimateNav());
    }
    
    /**
     * 将估值实体转换为消息格式
     */
    private Map<String, Object> convertToMessage(FundEstimateIntraday estimate) {
        Map<String, Object> message = new HashMap<>();
        message.put("fundCode", estimate.getFundCode());
        message.put("estimateTime", estimate.getEstimateTime() != null 
                ? estimate.getEstimateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                : null);
        message.put("estimateNav", estimate.getEstimateNav());
        message.put("estimateChangePct", estimate.getEstimateChangePct());
        message.put("estimateChangeAmt", estimate.getEstimateChangeAmt());
        message.put("preCloseNav", estimate.getPreCloseNav());
        message.put("tradeDate", estimate.getTradeDate() != null 
                ? estimate.getTradeDate().toString() 
                : null);
        message.put("dataSource", estimate.getDataSource());
        return message;
    }
}
