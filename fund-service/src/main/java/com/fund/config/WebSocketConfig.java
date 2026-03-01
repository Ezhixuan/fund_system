package com.fund.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 配置STOMP协议和消息代理
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单内存消息代理
        // /topic 用于广播消息
        // /queue 用于点对点消息
        config.enableSimpleBroker("/topic", "/queue");
        
        // 设置应用前缀，客户端发送消息时需要加上这个前缀
        config.setApplicationDestinationPrefixes("/app");
        
        // 设置用户前缀，用于点对点消息
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点，客户端连接使用的URL
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 允许跨域，生产环境应限制具体域名
                .withSockJS();  // 启用SockJS支持，兼容不支持WebSocket的浏览器
    }
}
