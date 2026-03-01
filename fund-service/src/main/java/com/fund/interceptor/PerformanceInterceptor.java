package com.fund.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API性能监控拦截器
 * 用于统计API响应时间和请求次数
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PerformanceInterceptor.class);

    // 存储每个API的统计信息
    private final Map<String, ApiStats> statsMap = new ConcurrentHashMap<>();

    // 线程本地变量，存储请求开始时间
    private static final ThreadLocal<Long> startTimeHolder = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTimeHolder.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        Long startTime = startTimeHolder.get();
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String api = request.getRequestURI();
            String method = request.getMethod();
            String key = method + " " + api;

            statsMap.computeIfAbsent(key, k -> new ApiStats())
                    .record(duration, response.getStatus() >= 400);

            // 慢查询日志
            if (duration > 500) {
                log.warn("慢查询: {} 耗时 {}ms", key, duration);
            }

            startTimeHolder.remove();
        }
    }

    /**
     * 获取API统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> result = new ConcurrentHashMap<>();

        statsMap.forEach((api, stats) -> {
            Map<String, Object> apiInfo = new ConcurrentHashMap<>();
            apiInfo.put("totalRequests", stats.getTotalRequests());
            apiInfo.put("errorRequests", stats.getErrorRequests());
            apiInfo.put("avgResponseTime", stats.getAvgResponseTime());
            apiInfo.put("maxResponseTime", stats.getMaxResponseTime());
            apiInfo.put("minResponseTime", stats.getMinResponseTime());
            apiInfo.put("p95", stats.getP95());
            apiInfo.put("p99", stats.getP99());
            apiInfo.put("errorRate", stats.getErrorRate());

            result.put(api, apiInfo);
        });

        return result;
    }

    /**
     * 获取总体统计
     */
    public Map<String, Object> getOverallStats() {
        long totalRequests = 0;
        long errorRequests = 0;
        long totalDuration = 0;
        long maxDuration = 0;

        for (ApiStats stats : statsMap.values()) {
            totalRequests += stats.getTotalRequests();
            errorRequests += stats.getErrorRequests();
            totalDuration += stats.getTotalDuration();
            maxDuration = Math.max(maxDuration, stats.getMaxResponseTime());
        }

        Map<String, Object> overall = new ConcurrentHashMap<>();
        overall.put("totalRequests", totalRequests);
        overall.put("errorRequests", errorRequests);
        overall.put("avgResponseTime", totalRequests > 0 ? totalDuration / totalRequests : 0);
        overall.put("maxResponseTime", maxDuration);
        overall.put("errorRate", totalRequests > 0 ? (double) errorRequests / totalRequests * 100 : 0);

        return overall;
    }

    /**
     * 清除统计
     */
    public void clearStats() {
        statsMap.clear();
    }

    /**
     * API统计信息内部类
     */
    private static class ApiStats {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong errorRequests = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private volatile long maxResponseTime = 0;
        private volatile long minResponseTime = Long.MAX_VALUE;

        // 用于计算P95/P99的响应时间列表（简化实现，实际生产环境使用滑动窗口）
        private final java.util.concurrent.CopyOnWriteArrayList<Long> durations = 
            new java.util.concurrent.CopyOnWriteArrayList<>();

        void record(long duration, boolean isError) {
            totalRequests.incrementAndGet();
            totalDuration.addAndGet(duration);

            if (isError) {
                errorRequests.incrementAndGet();
            }

            // 更新最大/最小响应时间
            synchronized (this) {
                if (duration > maxResponseTime) {
                    maxResponseTime = duration;
                }
                if (duration < minResponseTime) {
                    minResponseTime = duration;
                }
            }

            // 保存响应时间用于计算P95/P99
            durations.add(duration);
            // 限制列表大小，防止内存溢出
            if (durations.size() > 10000) {
                durations.subList(0, 1000).clear();
            }
        }

        long getTotalRequests() {
            return totalRequests.get();
        }

        long getErrorRequests() {
            return errorRequests.get();
        }

        long getTotalDuration() {
            return totalDuration.get();
        }

        long getAvgResponseTime() {
            long total = totalRequests.get();
            return total > 0 ? totalDuration.get() / total : 0;
        }

        long getMaxResponseTime() {
            return maxResponseTime;
        }

        long getMinResponseTime() {
            return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime;
        }

        double getErrorRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) errorRequests.get() / total * 100 : 0;
        }

        long getP95() {
            return getPercentile(0.95);
        }

        long getP99() {
            return getPercentile(0.99);
        }

        private long getPercentile(double percentile) {
            if (durations.isEmpty()) {
                return 0;
            }
            java.util.List<Long> sorted = new java.util.ArrayList<>(durations);
            java.util.Collections.sort(sorted);
            int index = (int) Math.ceil(percentile * sorted.size()) - 1;
            return sorted.get(Math.max(0, index));
        }
    }
}
