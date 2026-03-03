package com.fund.config;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 缓存清理组件
 * 应用启动时自动清理可能存在格式问题的缓存
 * 防止 Redisson 缓存格式错误导致系统异常
 */
@Component
@Order(1) // 确保尽早执行
public class CacheCleaner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheCleaner.class);

    @Autowired
    private RedissonClient redissonClient;

    // 需要清理的缓存 key 列表
    private static final String[] CACHE_KEYS_TO_CLEAN = {
        RedisConfig.CACHE_ESTIMATE,    // fund:estimate
        RedisConfig.CACHE_FUND_DETAIL, // fund:detail
        RedisConfig.CACHE_FUND_METRICS,// fund:metrics
        RedisConfig.CACHE_SIGNAL       // fund:signal
    };

    @Override
    public void run(String... args) {
        log.info("开始执行启动时缓存清理...");

        int cleanedCount = 0;
        int errorCount = 0;

        for (String cacheKey : CACHE_KEYS_TO_CLEAN) {
            try {
                cleanCache(cacheKey);
                cleanedCount++;
            } catch (Exception e) {
                errorCount++;
                log.warn("清理缓存 {} 失败: {}", cacheKey, e.getMessage());
            }
        }

        // 清理 Redisson 内部的 timeout set
        cleanRedissonTimeoutSets();

        log.info("缓存清理完成: 成功={}, 失败={}", cleanedCount, errorCount);
    }

    /**
     * 清理单个缓存
     */
    private void cleanCache(String cacheKey) {
        try {
            RMap<?, ?> map = redissonClient.getMap(cacheKey);
            if (map != null && map.size() > 0) {
                map.delete();
                log.info("已清理缓存: {} (包含 {} 条数据)", cacheKey, map.size());
            } else {
                log.debug("缓存 {} 为空或不存在，无需清理", cacheKey);
            }
        } catch (Exception e) {
            // 如果是格式错误，尝试直接删除 key
            log.warn("缓存 {} 格式异常，尝试直接删除: {}", cacheKey, e.getMessage());
            try {
                redissonClient.getKeys().delete(cacheKey);
                log.info("已强制删除缓存 key: {}", cacheKey);
            } catch (Exception ex) {
                log.error("强制删除缓存 {} 失败: {}", cacheKey, ex.getMessage());
                throw ex;
            }
        }
    }

    /**
     * 清理 Redisson 内部的 timeout set
     * 这些 set 用于管理缓存过期时间，格式错误时也需要清理
     */
    private void cleanRedissonTimeoutSets() {
        try {
            for (String cacheKey : CACHE_KEYS_TO_CLEAN) {
                String timeoutSetKey = "redisson__timeout__set:{" + cacheKey + "}";
                String idleSetKey = "redisson__idle__set:{" + cacheKey + "}";

                try {
                    redissonClient.getKeys().delete(timeoutSetKey, idleSetKey);
                    log.debug("已清理 Redisson timeout set: {}", cacheKey);
                } catch (Exception e) {
                    // 这些 key 可能不存在，忽略错误
                    log.debug("清理 timeout set {} 时出错（可能不存在）: {}", cacheKey, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("清理 Redisson timeout sets 时出错: {}", e.getMessage());
        }
    }
}
