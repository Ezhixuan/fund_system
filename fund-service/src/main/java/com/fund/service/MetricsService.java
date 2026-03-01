package com.fund.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 性能指标服务
 * 用于记录和追踪系统性能指标
 */
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * 记录API响应时间
     */
    public void recordApiResponseTime(String api, long timeMs) {
        Timer.builder("fund.api.response.time")
                .tag("api", api)
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String cacheName) {
        meterRegistry.counter("fund.cache.hit", "cache", cacheName).increment();
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String cacheName) {
        meterRegistry.counter("fund.cache.miss", "cache", cacheName).increment();
    }
    
    /**
     * 记录数据库查询
     */
    public void recordDbQuery(String operation, long timeMs) {
        Timer.builder("fund.db.query.time")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录外部API调用
     */
    public void recordExternalApiCall(String api, long timeMs, boolean success) {
        Timer.builder("fund.external.api.time")
                .tag("api", api)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }
}
