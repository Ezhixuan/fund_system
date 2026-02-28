package com.fund.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置
 */
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Value("${spring.redis.host:127.0.0.1}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.redis.database:0}")
    private int database;
    
    /**
     * Redisson客户端
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redisHost + ":" + redisPort)
              .setDatabase(database)
              .setConnectionMinimumIdleSize(5)
              .setConnectionPoolSize(10)
              .setTimeout(3000);
        return Redisson.create(config);
    }
    
    /**
     * 缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();
        
        // 基金详情缓存 - 5分钟
        config.put("fund:detail", new CacheConfig(5 * 60 * 1000, 0));
        
        // 基金指标缓存 - 5分钟
        config.put("fund:metrics", new CacheConfig(5 * 60 * 1000, 0));
        
        // TOP排名缓存 - 1小时
        config.put("fund:ranking", new CacheConfig(60 * 60 * 1000, 0));
        
        // 搜索结果缓存 - 2分钟
        config.put("fund:search", new CacheConfig(2 * 60 * 1000, 0));
        
        return new RedissonSpringCacheManager(redissonClient, config);
    }
}
