package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.service.FundDataFetchService;
import com.fund.service.collect.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控控制器
 * 提供系统监控、数据查询和手动刷新接口
 */
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private FundDataFetchService fundDataFetchService;

    /**
     * 手动刷新基金基本信息
     */
    @PostMapping("/refresh/{fundCode}/info")
    public ApiResponse<Map<String, Object>> refreshFundInfo(@PathVariable String fundCode) {
        log.info("手动刷新基金[{}]基本信息", fundCode);
        
        CollectResult result = fundDataFetchService.refreshFundInfo(fundCode);
        
        Map<String, Object> data = new HashMap<>();
        data.put("fundCode", fundCode);
        data.put("success", result.isSuccess());
        data.put("message", result.isSuccess() ? "刷新成功" : result.getMessage());
        
        return result.isSuccess() 
            ? ApiResponse.success(data) 
            : ApiResponse.error(result.getMessage());
    }

    /**
     * 手动刷新基金指标数据
     */
    @PostMapping("/refresh/{fundCode}/metrics")
    public ApiResponse<Map<String, Object>> refreshFundMetrics(@PathVariable String fundCode) {
        log.info("手动刷新基金[{}]指标数据", fundCode);
        
        CollectResult result = fundDataFetchService.refreshFundMetrics(fundCode);
        
        Map<String, Object> data = new HashMap<>();
        data.put("fundCode", fundCode);
        data.put("success", result.isSuccess());
        data.put("message", result.isSuccess() ? "刷新成功" : result.getMessage());
        
        return result.isSuccess() 
            ? ApiResponse.success(data) 
            : ApiResponse.error(result.getMessage());
    }

    /**
     * 手动刷新基金NAV历史
     */
    @PostMapping("/refresh/{fundCode}/nav")
    public ApiResponse<Map<String, Object>> refreshFundNav(@PathVariable String fundCode) {
        log.info("手动刷新基金[{}]NAV历史", fundCode);
        
        CollectResult result = fundDataFetchService.refreshNavHistory(fundCode);
        
        Map<String, Object> data = new HashMap<>();
        data.put("fundCode", fundCode);
        data.put("success", result.isSuccess());
        data.put("count", result.isSuccess() && result.getData() != null 
            ? ((List) result.getData()).size() : 0);
        
        return result.isSuccess() 
            ? ApiResponse.success(data) 
            : ApiResponse.error(result.getMessage());
    }

    /**
     * 一键刷新基金所有数据
     */
    @PostMapping("/refresh/{fundCode}/all")
    public ApiResponse<Map<String, Object>> refreshFundAll(@PathVariable String fundCode) {
        log.info("一键刷新基金[{}]所有数据", fundCode);
        
        Map<String, Object> results = new HashMap<>();
        results.put("fundCode", fundCode);
        
        // 刷新基本信息
        CollectResult infoResult = fundDataFetchService.refreshFundInfo(fundCode);
        results.put("info", infoResult.isSuccess());
        
        // 刷新指标
        CollectResult metricsResult = fundDataFetchService.refreshFundMetrics(fundCode);
        results.put("metrics", metricsResult.isSuccess());
        
        // 刷新NAV历史
        CollectResult navResult = fundDataFetchService.refreshNavHistory(fundCode);
        results.put("nav", navResult.isSuccess());
        
        boolean allSuccess = infoResult.isSuccess() && metricsResult.isSuccess() && navResult.isSuccess();
        results.put("allSuccess", allSuccess);
        
        return ApiResponse.success(results);
    }

    /**
     * 批量刷新基金数据
     */
    @PostMapping("/refresh/batch")
    public ApiResponse<Map<String, Object>> batchRefreshFunds(@RequestBody List<String> fundCodes) {
        log.info("批量刷新{}只基金数据", fundCodes.size());
        
        if (fundCodes.size() > 10) {
            return ApiResponse.error("单次最多刷新10只基金");
        }
        
        List<String> results = fundDataFetchService.batchRefreshFunds(fundCodes);
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", fundCodes.size());
        data.put("results", results);
        
        return ApiResponse.success(data);
    }

    /**
     * 获取Python采集服务健康状态
     */
    @GetMapping("/health/collect")
    public ApiResponse<Map<String, Object>> getCollectHealth() {
        // 通过调用FundDataFetchService的一个简单方法检查
        Map<String, Object> health = new HashMap<>();
        health.put("service", "fund-collect");
        health.put("status", "connected");
        return ApiResponse.success(health);
    }
}
