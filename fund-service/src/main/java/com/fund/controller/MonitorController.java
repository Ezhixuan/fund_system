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
}
