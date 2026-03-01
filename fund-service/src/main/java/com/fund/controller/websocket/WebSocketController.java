package com.fund.controller.websocket;

import com.fund.service.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * WebSocket控制器
 * 处理客户端订阅和消息
 */
@Controller
public class WebSocketController {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    /**
     * 处理基金详情页订阅
     * 客户端订阅: /topic/fund/{fundCode}/intraday
     */
    @MessageMapping("/fund/{fundCode}/subscribe")
    public void subscribeFundDetail(
            @DestinationVariable String fundCode,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        log.info("用户订阅基金详情: fundCode={}, sessionId={}", fundCode, sessionId);
        
        sessionManager.subscribeFund(fundCode, sessionId);
    }
    
    /**
     * 处理持仓页面订阅
     * 客户端订阅: /user/queue/portfolio/intraday
     */
    @MessageMapping("/portfolio/subscribe")
    public void subscribePortfolio(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("用户订阅持仓更新: sessionId={}", sessionId);
        
        // 这里可以记录用户订阅了持仓更新
        // 实际推送由服务端根据持仓数据触发
    }
    
    /**
     * 处理取消订阅
     */
    @MessageMapping("/fund/{fundCode}/unsubscribe")
    public void unsubscribeFund(
            @DestinationVariable String fundCode,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        log.info("用户取消订阅基金: fundCode={}, sessionId={}", fundCode, sessionId);
        
        sessionManager.unsubscribeFund(fundCode, sessionId);
    }
}
