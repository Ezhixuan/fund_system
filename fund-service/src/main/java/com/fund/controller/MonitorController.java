package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.entity.ApiCallLog;
import com.fund.service.ApiTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private ApiTraceService traceService;
    
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

    /**
     * 直接查询Python原始数据 - 支持所有类型
     */
    @GetMapping("/raw/python/{fundCode}")
    public ApiResponse<Map<String, Object>> getPythonRawData(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "estimate") String type) {
        
        log.info("直接查询Python原始数据: {} type={}", fundCode, type);
        
        try {
            String url;
            Map<String, Object> result;
            
            switch (type) {
                case "estimate":
                    // 实时估值接口 - POST
                    url = "http://fund-collect:5000/api/collect/estimate";
                    Map<String, String> estimateBody = new HashMap<>();
                    estimateBody.put("fundCode", fundCode);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, String>> request = new HttpEntity<>(estimateBody, headers);
                    
                    result = restTemplate.postForObject(url, request, Map.class);
                    break;
                    
                case "info":
                    // 基金基本信息 - GET (新接口)
                    url = "http://fund-collect:5000/api/collect/fund/" + fundCode;
                    result = restTemplate.getForObject(url, Map.class);
                    break;
                    
                case "metrics":
                    // 基金指标 - GET (新接口)
                    url = "http://fund-collect:5000/api/collect/metrics/" + fundCode;
                    result = restTemplate.getForObject(url, Map.class);
                    break;
                    
                case "nav":
                    // NAV历史 - GET (新接口)
                    url = "http://fund-collect:5000/api/collect/nav/" + fundCode;
                    result = restTemplate.getForObject(url, Map.class);
                    break;
                    
                case "batch":
                    // 批量接口 - POST
                    url = "http://fund-collect:5000/api/collect/batch";
                    Map<String, List<String>> batchBody = new HashMap<>();
                    batchBody.put("fundCodes", List.of(fundCode));
                    
                    HttpHeaders batchHeaders = new HttpHeaders();
                    batchHeaders.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, List<String>>> batchRequest = new HttpEntity<>(batchBody, batchHeaders);
                    
                    result = restTemplate.postForObject(url, batchRequest, Map.class);
                    break;
                    
                default:
                    // 默认使用健康检查
                    url = "http://fund-collect:5000/health";
                    Map<String, Object> health = restTemplate.getForObject(url, Map.class);
                    
                    result = new HashMap<>();
                    result.put("success", true);
                    result.put("service", health);
                    result.put("note", "未知类型: " + type + "，支持: estimate, info, metrics, nav, batch");
            }
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询Python原始数据失败: {}", e.getMessage());
            return ApiResponse.error("Python查询失败: " + e.getMessage());
        }
    }

    /**
     * 对比链路追踪中的原始数据 vs 处理后的数据
     */
    @GetMapping("/compare/{fundCode}")
    public ApiResponse<Map<String, Object>> compareData(@PathVariable String fundCode) {
        Map<String, Object> comparison = new HashMap<>();
        
        // 从链路追踪中获取最近的一次调用记录
        List<ApiCallLog> traces = traceService.getRecentTraces(null, 100);
        ApiCallLog matchedTrace = traces.stream()
            .filter(t -> fundCode.equals(t.getFundCode()))
            .findFirst()
            .orElse(null);
        
        if (matchedTrace != null) {
            comparison.put("hasTrace", true);
            comparison.put("pythonRaw", matchedTrace.getPythonResponse());
            comparison.put("javaProcessed", matchedTrace.getJavaResponse());
            comparison.put("traceInfo", Map.of(
                "requestId", matchedTrace.getRequestId(),
                "apiType", matchedTrace.getApiType(),
                "status", matchedTrace.getStatus(),
                "duration", matchedTrace.getDuration() + "ms",
                "pythonUrl", matchedTrace.getPythonUrl()
            ));
        } else {
            comparison.put("hasTrace", false);
            comparison.put("message", "暂无该基金的链路追踪记录，请先访问基金详情页触发采集");
        }
        
        return ApiResponse.success(comparison);
    }

    @GetMapping("/trace-stats")
    public ApiResponse<ApiTraceService.TraceStats> getTraceStats(
            @RequestParam(required = false) String apiType) {
        return ApiResponse.success(traceService.getTraceStats(apiType, 60));
    }
}
