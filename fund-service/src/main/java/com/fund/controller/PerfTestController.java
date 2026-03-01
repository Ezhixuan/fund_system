package com.fund.controller;

import com.fund.dto.ApiResponse;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 性能测试报告控制器
 */
@RestController
@RequestMapping("/admin/perf")
public class PerfTestController {
    
    private final MetricsEndpoint metricsEndpoint;
    
    public PerfTestController(MetricsEndpoint metricsEndpoint) {
        this.metricsEndpoint = metricsEndpoint;
    }
    
    /**
     * 获取性能概览
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> overview = new HashMap<>();
        
        // JVM内存
        MetricsEndpoint.MetricDescriptor jvmMemoryUsed = metricsEndpoint.metric("jvm.memory.used", null);
        if (jvmMemoryUsed != null && !jvmMemoryUsed.getMeasurements().isEmpty()) {
            double value = jvmMemoryUsed.getMeasurements().get(0).getValue();
            overview.put("jvmMemoryUsedMB", String.format("%.2f", value / 1024 / 1024));
        }
        
        // JVM最大内存
        MetricsEndpoint.MetricDescriptor jvmMemoryMax = metricsEndpoint.metric("jvm.memory.max", null);
        if (jvmMemoryMax != null && !jvmMemoryMax.getMeasurements().isEmpty()) {
            double value = jvmMemoryMax.getMeasurements().get(0).getValue();
            overview.put("jvmMemoryMaxMB", String.format("%.2f", value / 1024 / 1024));
        }
        
        // CPU使用
        MetricsEndpoint.MetricDescriptor processCpu = metricsEndpoint.metric("process.cpu.usage", null);
        if (processCpu != null && !processCpu.getMeasurements().isEmpty()) {
            double value = processCpu.getMeasurements().get(0).getValue();
            overview.put("cpuUsage", String.format("%.2f%%", value * 100));
        }
        
        // 线程数
        MetricsEndpoint.MetricDescriptor threads = metricsEndpoint.metric("jvm.threads.live", null);
        if (threads != null && !threads.getMeasurements().isEmpty()) {
            overview.put("threadCount", threads.getMeasurements().get(0).getValue().intValue());
        }
        
        // GC次数
        MetricsEndpoint.MetricDescriptor gcCount = metricsEndpoint.metric("jvm.gc.pause.count", null);
        if (gcCount != null && !gcCount.getMeasurements().isEmpty()) {
            overview.put("gcCount", gcCount.getMeasurements().get(0).getValue().intValue());
        }
        
        // HTTP请求统计
        MetricsEndpoint.MetricDescriptor httpRequests = metricsEndpoint.metric("http.server.requests.count", null);
        if (httpRequests != null && !httpRequests.getMeasurements().isEmpty()) {
            overview.put("totalRequests", httpRequests.getMeasurements().get(0).getValue().intValue());
        }
        
        return ApiResponse.success(overview);
    }
    
    /**
     * 获取HTTP接口性能指标
     */
    @GetMapping("/http")
    public ApiResponse<List<Map<String, Object>>> httpMetrics() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取所有HTTP指标
        Set<String> metricNames = metricsEndpoint.listNames().getNames();
        
        List<String> httpMetrics = metricNames.stream()
                .filter(name -> name.startsWith("http.server.requests"))
                .filter(name -> name.contains("fund"))
                .collect(Collectors.toList());
        
        for (String metricName : httpMetrics) {
            MetricsEndpoint.MetricDescriptor metric = metricsEndpoint.metric(metricName, null);
            if (metric != null) {
                Map<String, Object> metricData = new HashMap<>();
                metricData.put("name", metricName);
                metricData.put("description", metric.getDescription());
                metricData.put("measurements", metric.getMeasurements());
                result.add(metricData);
            }
        }
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取数据库连接池指标
     */
    @GetMapping("/datasource")
    public ApiResponse<Map<String, Object>> datasourceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 活跃连接数
        MetricsEndpoint.MetricDescriptor active = metricsEndpoint.metric("jdbc.connections.active", null);
        if (active != null && !active.getMeasurements().isEmpty()) {
            metrics.put("activeConnections", active.getMeasurements().get(0).getValue().intValue());
        }
        
        // 空闲连接数
        MetricsEndpoint.MetricDescriptor idle = metricsEndpoint.metric("jdbc.connections.idle", null);
        if (idle != null && !idle.getMeasurements().isEmpty()) {
            metrics.put("idleConnections", idle.getMeasurements().get(0).getValue().intValue());
        }
        
        // 最大连接数
        MetricsEndpoint.MetricDescriptor max = metricsEndpoint.metric("jdbc.connections.max", null);
        if (max != null && !max.getMeasurements().isEmpty()) {
            metrics.put("maxConnections", max.getMeasurements().get(0).getValue().intValue());
        }
        
        // 最小空闲连接
        MetricsEndpoint.MetricDescriptor minIdle = metricsEndpoint.metric("jdbc.connections.min", null);
        if (minIdle != null && !minIdle.getMeasurements().isEmpty()) {
            metrics.put("minIdleConnections", minIdle.getMeasurements().get(0).getValue().intValue());
        }
        
        return ApiResponse.success(metrics);
    }
    
    /**
     * 获取JVM详细指标
     */
    @GetMapping("/jvm")
    public ApiResponse<Map<String, Object>> jvmMetrics() {
        Map<String, Object> jvm = new HashMap<>();
        
        // 堆内存
        Map<String, Object> heap = new HashMap<>();
        
        // 获取所有内存指标
        MetricsEndpoint.MetricDescriptor memoryUsed = metricsEndpoint.metric("jvm.memory.used", null);
        if (memoryUsed != null && !memoryUsed.getMeasurements().isEmpty()) {
            for (MetricsEndpoint.Sample sample : memoryUsed.getMeasurements()) {
                String area = getTagValue(sample.getTags(), "area");
                if ("heap".equals(area)) {
                    heap.put("used", String.format("%.2f MB", sample.getValue() / 1024 / 1024));
                }
            }
        }
        
        MetricsEndpoint.MetricDescriptor memoryMax = metricsEndpoint.metric("jvm.memory.max", null);
        if (memoryMax != null && !memoryMax.getMeasurements().isEmpty()) {
            for (MetricsEndpoint.Sample sample : memoryMax.getMeasurements()) {
                String area = getTagValue(sample.getTags(), "area");
                if ("heap".equals(area)) {
                    heap.put("max", String.format("%.2f MB", sample.getValue() / 1024 / 1024));
                }
            }
        }
        jvm.put("heap", heap);
        
        // 非堆内存
        Map<String, Object> nonHeap = new HashMap<>();
        if (memoryUsed != null && !memoryUsed.getMeasurements().isEmpty()) {
            for (MetricsEndpoint.Sample sample : memoryUsed.getMeasurements()) {
                String area = getTagValue(sample.getTags(), "area");
                if ("nonheap".equals(area)) {
                    nonHeap.put("used", String.format("%.2f MB", sample.getValue() / 1024 / 1024));
                }
            }
        }
        jvm.put("nonHeap", nonHeap);
        
        // GC信息
        Map<String, Object> gc = new HashMap<>();
        MetricsEndpoint.MetricDescriptor gcPause = metricsEndpoint.metric("jvm.gc.pause.count", null);
        if (gcPause != null && !gcPause.getMeasurements().isEmpty()) {
            gc.put("pauseCount", gcPause.getMeasurements().get(0).getValue().intValue());
        }
        MetricsEndpoint.MetricDescriptor gcTime = metricsEndpoint.metric("jvm.gc.pause.sum", null);
        if (gcTime != null && !gcTime.getMeasurements().isEmpty()) {
            gc.put("pauseTime", String.format("%.2f ms", gcTime.getMeasurements().get(0).getValue() * 1000));
        }
        jvm.put("gc", gc);
        
        // 线程信息
        Map<String, Object> threads = new HashMap<>();
        MetricsEndpoint.MetricDescriptor threadCount = metricsEndpoint.metric("jvm.threads.live", null);
        if (threadCount != null && !threadCount.getMeasurements().isEmpty()) {
            threads.put("live", threadCount.getMeasurements().get(0).getValue().intValue());
        }
        MetricsEndpoint.MetricDescriptor peakThreadCount = metricsEndpoint.metric("jvm.threads.peak", null);
        if (peakThreadCount != null && !peakThreadCount.getMeasurements().isEmpty()) {
            threads.put("peak", peakThreadCount.getMeasurements().get(0).getValue().intValue());
        }
        jvm.put("threads", threads);
        
        return ApiResponse.success(jvm);
    }
    
    /**
     * 从标签列表中获取指定key的值
     */
    private String getTagValue(List<MetricsEndpoint.Sample.Tag> tags, String key) {
        if (tags == null) return null;
        for (MetricsEndpoint.Sample.Tag tag : tags) {
            if (tag.getTag().equals(key)) {
                return tag.getValue();
            }
        }
        return null;
    }
    
    /**
     * 获取所有可用的指标名称
     */
    @GetMapping("/metrics")
    public ApiResponse<List<String>> listMetrics() {
        Set<String> names = metricsEndpoint.listNames().getNames();
        return ApiResponse.success(new ArrayList<>(names));
    }
}
