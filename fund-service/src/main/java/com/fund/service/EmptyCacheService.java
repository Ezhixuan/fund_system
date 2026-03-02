package com.fund.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 空值缓存服务
 * 用于缓存"数据不存在"的查询结果，防止缓存穿透和重复查询
 */
@Service
public class EmptyCacheService {

    private static final Logger log = LoggerFactory.getLogger(EmptyCacheService.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 缓存Key前缀
    private static final String EMPTY_PREFIX = "fund:empty:";

    // 空值缓存默认TTL（30分钟）
    private static final Duration DEFAULT_EMPTY_TTL = Duration.ofMinutes(30);

    /**
     * 数据类型枚举
     */
    public enum DataType {
        INFO("info"),      // 基金基本信息
        METRICS("metrics"), // 基金指标
        NAV("nav");        // NAV历史

        private final String suffix;

        DataType(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    /**
     * 生成空值缓存Key
     */
    private String buildKey(String fundCode, DataType dataType) {
        return EMPTY_PREFIX + dataType.getSuffix() + ":" + fundCode;
    }

    /**
     * 检查是否存在空值缓存（近期已确认数据不存在）
     *
     * @param fundCode 基金代码
     * @param dataType 数据类型
     * @return true表示已确认数据不存在
     */
    public boolean isEmptyCached(String fundCode, DataType dataType) {
        String key = buildKey(fundCode, dataType);
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            log.debug("基金[{}]的[{}]数据已确认不存在（缓存命中）", fundCode, dataType);
        }
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 设置空值缓存（数据不存在时调用）
     *
     * @param fundCode 基金代码
     * @param dataType 数据类型
     */
    public void setEmptyCache(String fundCode, DataType dataType) {
        setEmptyCache(fundCode, dataType, DEFAULT_EMPTY_TTL);
    }

    /**
     * 设置空值缓存（指定TTL）
     *
     * @param fundCode 基金代码
     * @param dataType 数据类型
     * @param ttl 缓存有效期
     */
    public void setEmptyCache(String fundCode, DataType dataType, Duration ttl) {
        String key = buildKey(fundCode, dataType);
        redisTemplate.opsForValue().set(key, "1", ttl);
        log.info("设置空值缓存: 基金[{}]的[{}]数据不存在，TTL={}分钟", 
                fundCode, dataType, ttl.toMinutes());
    }

    /**
     * 清除空值缓存（数据被补充后调用）
     *
     * @param fundCode 基金代码
     * @param dataType 数据类型
     */
    public void clearEmptyCache(String fundCode, DataType dataType) {
        String key = buildKey(fundCode, dataType);
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("清除空值缓存: 基金[{}]的[{}]数据", fundCode, dataType);
        }
    }

    /**
     * 批量清除空值缓存
     *
     * @param fundCode 基金代码
     */
    public void clearAllEmptyCache(String fundCode) {
        for (DataType dataType : DataType.values()) {
            clearEmptyCache(fundCode, dataType);
        }
    }

    /**
     * 获取空值缓存剩余时间
     *
     * @param fundCode 基金代码
     * @param dataType 数据类型
     * @return 剩余秒数，-1表示不存在
     */
    public long getEmptyCacheTtl(String fundCode, DataType dataType) {
        String key = buildKey(fundCode, dataType);
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null ? ttl : -1;
    }
}
