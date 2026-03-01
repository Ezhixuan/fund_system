package com.fund.service.websocket.impl;

import com.fund.service.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket会话管理器实现
 */
@Component
public class WebSocketSessionManagerImpl implements WebSocketSessionManager {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManagerImpl.class);
    
    // fundCode -> Set<sessionId> 映射
    private final Map<String, Set<String>> fundSubscribers = new ConcurrentHashMap<>();
    
    // sessionId -> Set<fundCode> 映射
    private final Map<String, Set<String>> sessionToFunds = new ConcurrentHashMap<>();
    
    @Override
    public void subscribeFund(String fundCode, String sessionId) {
        // 添加到基金订阅者列表
        fundSubscribers.computeIfAbsent(fundCode, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        
        // 添加到会话订阅列表
        sessionToFunds.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(fundCode);
        
        log.debug("用户 {} 订阅基金 {}，当前订阅者数: {}", 
                sessionId, fundCode, fundSubscribers.get(fundCode).size());
    }
    
    @Override
    public void unsubscribeFund(String fundCode, String sessionId) {
        // 从基金订阅者列表移除
        Set<String> subscribers = fundSubscribers.get(fundCode);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                fundSubscribers.remove(fundCode);
            }
        }
        
        // 从会话订阅列表移除
        Set<String> funds = sessionToFunds.get(sessionId);
        if (funds != null) {
            funds.remove(fundCode);
            if (funds.isEmpty()) {
                sessionToFunds.remove(sessionId);
            }
        }
        
        log.debug("用户 {} 取消订阅基金 {}", sessionId, fundCode);
    }
    
    @Override
    public void unsubscribeAll(String sessionId) {
        Set<String> funds = sessionToFunds.get(sessionId);
        if (funds != null) {
            // 复制一份避免ConcurrentModificationException
            List<String> fundsCopy = new ArrayList<>(funds);
            for (String fundCode : fundsCopy) {
                unsubscribeFund(fundCode, sessionId);
            }
        }
        sessionToFunds.remove(sessionId);
        log.debug("用户 {} 取消所有订阅", sessionId);
    }
    
    @Override
    public boolean hasSubscribers(String fundCode) {
        Set<String> subscribers = fundSubscribers.get(fundCode);
        return subscribers != null && !subscribers.isEmpty();
    }
    
    @Override
    public Set<String> getSubscribers(String fundCode) {
        return fundSubscribers.getOrDefault(fundCode, Collections.emptySet());
    }
    
    @Override
    public List<String> getSubscribedFunds(String sessionId) {
        Set<String> funds = sessionToFunds.get(sessionId);
        return funds != null ? new ArrayList<>(funds) : Collections.emptyList();
    }
    
    @Override
    public Set<String> getAllSubscribedFunds() {
        return new HashSet<>(fundSubscribers.keySet());
    }
}
