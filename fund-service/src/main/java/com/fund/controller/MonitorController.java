package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.dto.CollectionStatsDTO;
import com.fund.dto.TableStatusDTO;
import com.fund.interceptor.PerformanceInterceptor;
import com.fund.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控控制器
 */
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    private final MonitorService monitorService;
    private final PerformanceInterceptor performanceInterceptor;

    public MonitorController(MonitorService monitorService, 
                            PerformanceInterceptor performanceInterceptor) {
        this.monitorService = monitorService;
        this.performanceInterceptor = performanceInterceptor;
    }

    /**
     * 获取数据表状态
     */
    @GetMapping("/tables/status")
    public ApiResponse<List<TableStatusDTO>> getTableStatus() {
        try {
            List<TableStatusDTO> status = monitorService.getTableStatus();
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取数据表状态失败", e);
            return ApiResponse.error("获取数据表状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取采集统计
     */
    @GetMapping("/collection/stats")
    public ApiResponse<CollectionStatsDTO> getCollectionStats(
            @RequestParam(required = false) String date) {
        try {
            CollectionStatsDTO stats = monitorService.getCollectionStats(date);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取采集统计失败", e);
            return ApiResponse.error("获取采集统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据质量报告
     */
    @GetMapping("/quality/report")
    public ApiResponse<Map<String, Object>> getQualityReport() {
        try {
            Map<String, Object> report = monitorService.getDataQualityReport();
            return ApiResponse.success(report);
        } catch (Exception e) {
            log.error("获取数据质量报告失败", e);
            return ApiResponse.error("获取数据质量报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> status = monitorService.getHealthStatus();
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取健康状态失败", e);
            return ApiResponse.error("获取健康状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取API性能统计
     */
    @GetMapping("/api/performance")
    public ApiResponse<Map<String, Object>> getApiPerformance() {
        try {
            Map<String, Object> stats = performanceInterceptor.getStats();
            Map<String, Object> overall = performanceInterceptor.getOverallStats();

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("apis", stats);
            result.put("overall", overall);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取API性能统计失败", e);
            return ApiResponse.error("获取API性能统计失败: " + e.getMessage());
        }
    }

    /**
     * 清除API性能统计
     */
    @GetMapping("/api/performance/clear")
    public ApiResponse<Void> clearApiPerformance() {
        try {
            performanceInterceptor.clearStats();
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("清除API性能统计失败", e);
            return ApiResponse.error("清除API性能统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/alerts/rules")
    public ApiResponse<List<Map<String, Object>>> getAlertRules() {
        try {
            List<Map<String, Object>> rules = new ArrayList<>();
            
            // 采集失败率告警
            Map<String, Object> rule1 = new HashMap<>();
            rule1.put("name", "collection_failure_rate");
            rule1.put("level", "critical");
            rule1.put("condition", "success_rate < 95%");
            rule1.put("description", "采集成功率低于95%");
            rules.add(rule1);
            
            // 数据延迟告警
            Map<String, Object> rule2 = new HashMap<>();
            rule2.put("name", "data_delay");
            rule2.put("level", "critical");
            rule2.put("condition", "delay_days > 1");
            rule2.put("description", "数据延迟超过1天");
            rules.add(rule2);
            
            // API响应慢告警
            Map<String, Object> rule3 = new HashMap<>();
            rule3.put("name", "api_slow");
            rule3.put("level", "warning");
            rule3.put("condition", "p99 > 500ms");
            rule3.put("description", "API响应时间超过500ms");
            rules.add(rule3);
            
            // API错误率告警
            Map<String, Object> rule4 = new HashMap<>();
            rule4.put("name", "api_error_rate");
            rule4.put("level", "warning");
            rule4.put("condition", "error_rate > 5%");
            rule4.put("description", "API错误率超过5%");
            rules.add(rule4);
            
            return ApiResponse.success(rules);
        } catch (Exception e) {
            log.error("获取告警规则失败", e);
            return ApiResponse.error("获取告警规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前告警
     */
    @GetMapping("/alerts/current")
    public ApiResponse<List<Map<String, Object>>> getCurrentAlerts() {
        try {
            List<Map<String, Object>> alerts = new ArrayList<>();
            
            // 检查采集状态
            CollectionStatsDTO stats = monitorService.getCollectionStats(null);
            if (stats.getSuccessRate() < 95) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("ruleName", "collection_failure_rate");
                alert.put("level", "critical");
                alert.put("message", "采集成功率过低: " + stats.getSuccessRate() + "% (< 95%)");
                alert.put("timestamp", java.time.Instant.now().toString());
                alert.put("data", Map.of(
                    "date", stats.getDate(),
                    "totalFunds", stats.getTotalFunds(),
                    "collectedFunds", stats.getCollectedFunds(),
                    "successRate", stats.getSuccessRate()
                ));
                alerts.add(alert);
            }
            
            // 检查数据新鲜度
            List<TableStatusDTO> tableStatus = monitorService.getTableStatus();
            for (TableStatusDTO status : tableStatus) {
                if (!status.getIsFresh() && status.getDelayDays() > 1) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("ruleName", "data_delay");
                    alert.put("level", "critical");
                    alert.put("message", "数据延迟: " + status.getTableName() + " 延迟 " + status.getDelayDays() + " 天");
                    alert.put("timestamp", java.time.Instant.now().toString());
                    alert.put("data", Map.of(
                        "table", status.getTableName(),
                        "latestDate", status.getLatestDate(),
                        "delayDays", status.getDelayDays()
                    ));
                    alerts.add(alert);
                }
            }
            
            // 检查API性能
            Map<String, Object> apiStats = performanceInterceptor.getStats();
            for (Map.Entry<String, Object> entry : apiStats.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> apiInfo = (Map<String, Object>) entry.getValue();
                Long p99 = ((Number) apiInfo.get("p99")).longValue();
                Double errorRate = ((Number) apiInfo.get("errorRate")).doubleValue();
                
                if (p99 > 500) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("ruleName", "api_slow");
                    alert.put("level", "warning");
                    alert.put("message", "API响应慢: " + entry.getKey() + " P99=" + p99 + "ms (> 500ms)");
                    alert.put("timestamp", java.time.Instant.now().toString());
                    alert.put("data", Map.of(
                        "api", entry.getKey(),
                        "p99", p99
                    ));
                    alerts.add(alert);
                }
                
                if (errorRate > 5) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("ruleName", "api_error_rate");
                    alert.put("level", "warning");
                    alert.put("message", "API错误率高: " + entry.getKey() + " 错误率=" + errorRate + "% (> 5%)");
                    alert.put("timestamp", java.time.Instant.now().toString());
                    alert.put("data", Map.of(
                        "api", entry.getKey(),
                        "errorRate", errorRate
                    ));
                    alerts.add(alert);
                }
            }
            
            return ApiResponse.success(alerts);
        } catch (Exception e) {
            log.error("获取当前告警失败", e);
            return ApiResponse.error("获取当前告警失败: " + e.getMessage());
        }
    }
}
