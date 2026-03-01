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
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存配置 - 优化版
 * 包含：热点数据缓存、TTL策略、防穿透/雪崩配置
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
    
    // 缓存名称常量
    public static final String CACHE_FUND_DETAIL = "fund:detail";
    public static final String CACHE_FUND_METRICS = "fund:metrics";
    public static final String CACHE_FUND_RANKING = "fund:ranking";
    public static final String CACHE_FUND_SEARCH = "fund:search";
    public static final String CACHE_FUND_TOP = "fund:top";
    public static final String CACHE_SIGNAL = "fund:signal";
    public static final String CACHE_ESTIMATE = "fund:estimate";
    public static final String BLOOM_FILTER_FUND = "fund:bloom";
    
    // TTL配置（毫秒）
    private static final long TTL_FUND_DETAIL = 5 * 60 * 1000;        // 5分钟
    private static final long TTL_FUND_METRICS = 5 * 60 * 1000;       // 5分钟
    private static final long TTL_FUND_RANKING = 60 * 60 * 1000;      // 1小时
    private static final long TTL_FUND_SEARCH = 2 * 60 * 1000;        // 2分钟
    private static final long TTL_FUND_TOP = 30 * 60 * 1000;          // 30分钟（热点数据）
    private static final long TTL_SIGNAL = 10 * 60 * 1000;            // 10分钟
    private static final long TTL_ESTIMATE = 5 * 60 * 1000;           // 5分钟（当日估值变化快）
    
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
              .setTimeout(3000)
              .setRetryAttempts(3)
              .setRetryInterval(1500);
        return Redisson.create(config);
    }
    
    /**
     * 缓存管理器 - 优化TTL策略
     */
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();
        
        // 基金详情缓存 - 5分钟
        config.put(CACHE_FUND_DETAIL, createCacheConfig(TTL_FUND_DETAIL, 0));
        
        // 基金指标缓存 - 5分钟
        config.put(CACHE_FUND_METRICS, createCacheConfig(TTL_FUND_METRICS, 0));
        
        // TOP排名缓存 - 1小时
        config.put(CACHE_FUND_RANKING, createCacheConfig(TTL_FUND_RANKING, 0));
        
        // 搜索结果缓存 - 2分钟
        config.put(CACHE_FUND_SEARCH, createCacheConfig(TTL_FUND_SEARCH, 0));
        
        // 热点基金缓存 - 30分钟
        config.put(CACHE_FUND_TOP, createCacheConfig(TTL_FUND_TOP, 0));
        
        // 信号缓存 - 10分钟
        config.put(CACHE_SIGNAL, createCacheConfig(TTL_SIGNAL, 0));
        
        // 估值缓存 - 5分钟
        config.put(CACHE_ESTIMATE, createCacheConfig(TTL_ESTIMATE, 0));
        
        return new RedissonSpringCacheManager(redissonClient, config);
    }
    
    /**
     * 创建缓存配置
     */
    private CacheConfig createCacheConfig(long ttl, long maxIdleTime) {
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setTTL(ttl);
        if (maxIdleTime > 0) {
            cacheConfig.setMaxIdleTime(maxIdleTime);
        }
        return cacheConfig;
    }
}
