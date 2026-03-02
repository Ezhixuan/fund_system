package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.entity.ApiCallLog;
import com.fund.service.ApiTraceService;
import com.fund.service.collect.CollectClient;
import com.fund.service.collect.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private ApiTraceService traceService;

    @Autowired
    private CollectClient collectClient;
    
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/traces")
    public ApiResponse<Map<String, Object>> getTraces(
            @RequestParam(required = false) String apiType,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<ApiCallLog> traces = traceService.getRecentTraces(apiType, limit);
        ApiTraceService.TraceStats stats = traceService.getTraceStats(apiType, 60);
        
        Map<String, Object> data = new HashMap<>();
        data.put("traces", traces);
        data.put("stats", stats);
        
        return ApiResponse.success(data);
    }

    @GetMapping("/raw/python/{fundCode}")
    public ApiResponse<Map<String, Object>> getPythonRawData(
            @PathVariable String fundCode,
            @RequestParam String type) {
        
        log.info("直接查询Python原始数据: {} type={}", fundCode, type);
        
        try {
            String url = "http://fund-collect:5000/api/collect/" + type + "/" + fundCode;
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询Python原始数据失败: {}", e.getMessage());
            return ApiResponse.error("Python查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare/{fundCode}")
    public ApiResponse<Map<String, Object>> compareData(@PathVariable String fundCode) {
        Map<String, Object> comparison = new HashMap<>();
        
        try {
            String pythonUrl = "http://fund-collect:5000/api/collect/info/" + fundCode;
            Map<String, Object> pythonRaw = restTemplate.getForObject(pythonUrl, Map.class);
            comparison.put("pythonRaw", pythonRaw);
        } catch (Exception e) {
            comparison.put("pythonRaw", Map.of("error", e.getMessage()));
        }
        
        CollectResult result = collectClient.collectFundInfo(fundCode);
        comparison.put("javaProcessed", Map.of(
            "success", result.isSuccess(),
            "errorCode", result.getErrorCode() != null ? result.getErrorCode() : "",
            "message", result.getMessage() != null ? result.getMessage() : "",
            "data", result.getData() != null ? result.getData().toString() : "null"
        ));
        
        return ApiResponse.success(comparison);
    }

    @GetMapping("/trace-stats")
    public ApiResponse<ApiTraceService.TraceStats> getTraceStats(
            @RequestParam(required = false) String apiType) {
        return ApiResponse.success(traceService.getTraceStats(apiType, 60));
    }
}
