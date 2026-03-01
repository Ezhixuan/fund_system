package com.fund.controller;

import com.fund.config.RedisConfig;
import com.fund.dto.ApiResponse;
import com.fund.service.CacheWarmupService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控控制器 - 优化版
 * 功能：缓存统计、布隆过滤器监控、手动预热
 */
@RestController
@RequestMapping("/admin/cache")
public class CacheMonitorController {
    
    private final RedissonClient redissonClient;
    private final CacheWarmupService cacheWarmupService;
    
    public CacheMonitorController(RedissonClient redissonClient, 
                                  CacheWarmupService cacheWarmupService) {
        this.redissonClient = redissonClient;
        this.cacheWarmupService = cacheWarmupService;
    }
    
    /**
     * 获取缓存统计
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        
        RKeys keys = redissonClient.getKeys();
        
        // 统计各类缓存数量
        long totalKeys = keys.count();
        Iterable<String> allKeys = keys.getKeys();
        
        long detailCount = 0;
        long metricsCount = 0;
        long rankingCount = 0;
        long searchCount = 0;
        long topCount = 0;
        long signalCount = 0;
        long estimateCount = 0;
        long bloomFilterCount = 0;
        
        for (String key : allKeys) {
            if (key.startsWith("fund:detail")) detailCount++;
            else if (key.startsWith("fund:metrics")) metricsCount++;
            else if (key.startsWith("fund:ranking")) rankingCount++;
            else if (key.startsWith("fund:search")) searchCount++;
            else if (key.startsWith("fund:top")) topCount++;
            else if (key.startsWith("fund:signal")) signalCount++;
            else if (key.startsWith("fund:estimate")) estimateCount++;
            else if (key.startsWith("fund:bloom")) bloomFilterCount++;
        }
        
        stats.put("fund:detail", detailCount);
        stats.put("fund:metrics", metricsCount);
        stats.put("fund:ranking", rankingCount);
        stats.put("fund:search", searchCount);
        stats.put("fund:top", topCount);
        stats.put("fund:signal", signalCount);
        stats.put("fund:estimate", estimateCount);
        stats.put("fund:bloom", bloomFilterCount);
        stats.put("total_keys", totalKeys);
        
        // 布隆过滤器统计
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(RedisConfig.BLOOM_FILTER_FUND);
        if (bloomFilter.isExists()) {
            Map<String, Object> bloomStats = new HashMap<>();
            bloomStats.put("count", bloomFilter.count());
            bloomStats.put("expectedInsertions", bloomFilter.getExpectedInsertions());
            bloomStats.put("falseProbability", bloomFilter.getFalseProbability());
            stats.put("bloomFilter", bloomStats);
        }
        
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取布隆过滤器信息
     */
    @GetMapping("/bloom")
    public ApiResponse<Map<String, Object>> bloomFilterInfo() {
        Map<String, Object> info = new HashMap<>();
        
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(RedisConfig.BLOOM_FILTER_FUND);
        
        if (!bloomFilter.isExists()) {
            return ApiResponse.error("布隆过滤器未初始化");
        }
        
        info.put("name", RedisConfig.BLOOM_FILTER_FUND);
        info.put("count", bloomFilter.count());
        info.put("expectedInsertions", bloomFilter.getExpectedInsertions());
        info.put("falseProbability", bloomFilter.getFalseProbability());
        info.put("hashIterations", bloomFilter.getHashIterations());
        
        return ApiResponse.success(info);
    }
    
    /**
     * 检查基金代码是否在布隆过滤器中
     */
    @GetMapping("/bloom/check")
    public ApiResponse<Map<String, Object>> checkBloomFilter(@RequestParam String fundCode) {
        Map<String, Object> result = new HashMap<>();
        
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(RedisConfig.BLOOM_FILTER_FUND);
        boolean mightContain = bloomFilter.contains(fundCode);
        
        result.put("fundCode", fundCode);
        result.put("mightContain", mightContain);
        result.put("note", mightContain ? "可能在集合中" : "一定不在集合中");
        
        return ApiResponse.success(result);
    }
    
    /**
     * 手动触发缓存预热
     */
    @PostMapping("/warmup")
    public ApiResponse<String> warmup() {
        cacheWarmupService.manualWarmup();
        return ApiResponse.success("缓存预热已触发");
    }
    
    /**
     * 清除指定基金的所有缓存
     */
    @PostMapping("/clear/fund")
    public ApiResponse<String> clearFundCache(@RequestParam String fundCode) {
        cacheWarmupService.clearFundCache(fundCode);
        return ApiResponse.success("已清除基金 " + fundCode + " 的所有缓存");
    }
    
    /**
     * 清空指定缓存
     */
    @PostMapping("/clear")
    public ApiResponse<String> clear(@RequestParam String name) {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keyList = keys.getKeysByPattern(name + "*");
        long deleted = 0;
        for (String key : keyList) {
            redissonClient.getBucket(key).delete();
            deleted++;
        }
        return ApiResponse.success("已清除 " + deleted + " 个缓存键");
    }
    
    /**
     * 清空所有业务缓存
     */
    @PostMapping("/clear/all")
    public ApiResponse<String> clearAll() {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keyList = keys.getKeysByPattern("fund:*");
        long deleted = 0;
        for (String key : keyList) {
            redissonClient.getBucket(key).delete();
            deleted++;
        }
        return ApiResponse.success("已清除 " + deleted + " 个缓存键");
    }
    
    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查Redis连接
            redissonClient.getKeys().count();
            health.put("redis", "UP");
            
            // 检查布隆过滤器
            RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(RedisConfig.BLOOM_FILTER_FUND);
            health.put("bloomFilter", bloomFilter.isExists() ? "UP" : "DOWN");
            
            // 缓存统计
            CacheWarmupService.CacheStats stats = cacheWarmupService.getCacheStats();
            health.put("stats", stats);
            
            health.put("status", "HEALTHY");
        } catch (Exception e) {
            health.put("status", "UNHEALTHY");
            health.put("error", e.getMessage());
        }
        
        return ApiResponse.success(health);
    }
}
