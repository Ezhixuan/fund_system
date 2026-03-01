package com.fund.service.websocket;

import java.util.List;
import java.util.Set;

/**
 * WebSocket会话管理器接口
 * 管理基金订阅关系
 */
public interface WebSocketSessionManager {
    
    /**
     * 订阅基金
     * @param fundCode 基金代码
     * @param sessionId 会话ID
     */
    void subscribeFund(String fundCode, String sessionId);
    
    /**
     * 取消订阅基金
     * @param fundCode 基金代码
     * @param sessionId 会话ID
     */
    void unsubscribeFund(String fundCode, String sessionId);
    
    /**
     * 取消所有订阅
     * @param sessionId 会话ID
     */
    void unsubscribeAll(String sessionId);
    
    /**
     * 检查基金是否有订阅者
     * @param fundCode 基金代码
     * @return 是否有订阅者
     */
    boolean hasSubscribers(String fundCode);
    
    /**
     * 获取基金的订阅者列表
     * @param fundCode 基金代码
     * @return 会话ID集合
     */
    Set<String> getSubscribers(String fundCode);
    
    /**
     * 获取订阅的基金代码列表
     * @param sessionId 会话ID
     * @return 基金代码列表
     */
    List<String> getSubscribedFunds(String sessionId);
    
    /**
     * 获取所有被订阅的基金
     * @return 基金代码集合
     */
    Set<String> getAllSubscribedFunds();
}
