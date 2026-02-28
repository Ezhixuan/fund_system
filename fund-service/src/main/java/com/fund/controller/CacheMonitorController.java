package com.fund.controller;

import com.fund.dto.ApiResponse;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控控制器
 */
@RestController
@RequestMapping("/admin/cache")
public class CacheMonitorController {
    
    private final RedissonClient redissonClient;
    
    public CacheMonitorController(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
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
        
        for (String key : allKeys) {
            if (key.startsWith("fund:detail")) detailCount++;
            else if (key.startsWith("fund:metrics")) metricsCount++;
            else if (key.startsWith("fund:ranking")) rankingCount++;
            else if (key.startsWith("fund:search")) searchCount++;
        }
        
        stats.put("fund:detail", detailCount);
        stats.put("fund:metrics", metricsCount);
        stats.put("fund:ranking", rankingCount);
        stats.put("fund:search", searchCount);
        stats.put("total_keys", totalKeys);
        
        return ApiResponse.success(stats);
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
}
