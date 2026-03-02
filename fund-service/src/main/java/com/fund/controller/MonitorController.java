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
     * 直接查询Python原始数据 - 使用实际存在的接口
     */
    @GetMapping("/raw/python/{fundCode}")
    public ApiResponse<Map<String, Object>> getPythonRawData(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "estimate") String type) {
        
        log.info("直接查询Python原始数据: {} type={}", fundCode, type);
        
        try {
            String url;
            Map<String, Object> result;
            
            if ("estimate".equals(type)) {
                // 实时估值接口 - POST
                url = "http://fund-collect:5000/api/collect/estimate";
                Map<String, String> body = new HashMap<>();
                body.put("fundCode", fundCode);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
                
                result = restTemplate.postForObject(url, request, Map.class);
            } else if ("batch".equals(type)) {
                // 批量接口 - POST
                url = "http://fund-collect:5000/api/collect/batch";
                Map<String, List<String>> body = new HashMap<>();
                body.put("fundCodes", List.of(fundCode));
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);
                
                result = restTemplate.postForObject(url, request, Map.class);
            } else {
                // 其他类型使用健康检查接口返回服务状态
                url = "http://fund-collect:5000/health";
                Map<String, Object> health = restTemplate.getForObject(url, Map.class);
                
                result = new HashMap<>();
                result.put("success", true);
                result.put("service", health);
                result.put("note", "该类型接口正在开发中，当前仅支持 estimate 类型");
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
