package com.fund.service.collect.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.entity.ApiCallLog;
import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.service.ApiTraceService;
import com.fund.service.collect.CollectClient;
import com.fund.service.collect.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * CollectClient实现（V2 - 带链路追踪）
 */
@Service
public class CollectClientImplV2 implements CollectClient {

    private static final Logger log = LoggerFactory.getLogger(CollectClientImplV2.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ApiTraceService traceService;

    @Value("${collector.url:http://localhost:5000}")
    private String collectorUrl;

    @Override
    public CollectResult<FundInfo> collectFundInfo(String fundCode) {
        String url = collectorUrl + "/api/collect/fund/" + fundCode;
        
        // 开始链路追踪
        ApiCallLog trace = traceService.startTrace(fundCode, "info", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, Map.class);
            
            // 记录Python响应
            String bodyStr = objectMapper.writeValueAsString(response.getBody());
            traceService.recordPythonSuccess(trace, response.getStatusCodeValue(), bodyStr);
            
            // 处理结果
            Map<String, Object> result = response.getBody();
            Boolean success = (Boolean) result.get("success");
            
            if (Boolean.TRUE.equals(success)) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                FundInfo fundInfo = convertToFundInfo(data);
                
                traceService.recordJavaResponse(trace, fundInfo);
                traceService.endTrace(trace, true, null);
                
                return CollectResult.success(fundInfo);
            } else {
                String error = (String) result.get("error");
                traceService.endTrace(trace, false, error);
                return CollectResult.fail("COLLECT_ERROR", error);
            }
            
        } catch (Exception e) {
            traceService.recordPythonError(trace, e.getMessage());
            traceService.endTrace(trace, false, e.getMessage());
            return CollectResult.serviceError(e.getMessage());
        }
    }

    @Override
    public CollectResult<FundMetrics> collectFundMetrics(String fundCode) {
        String url = collectorUrl + "/api/collect/metrics/" + fundCode;
        
        ApiCallLog trace = traceService.startTrace(fundCode, "metrics", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, Map.class);
            
            String bodyStr = objectMapper.writeValueAsString(response.getBody());
            traceService.recordPythonSuccess(trace, response.getStatusCodeValue(), bodyStr);
            
            Map<String, Object> result = response.getBody();
            Boolean success = (Boolean) result.get("success");
            
            if (Boolean.TRUE.equals(success)) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                FundMetrics metrics = convertToFundMetrics(data);
                
                traceService.recordJavaResponse(trace, metrics);
                traceService.endTrace(trace, true, null);
                
                return CollectResult.success(metrics);
            } else {
                String error = (String) result.get("error");
                traceService.endTrace(trace, false, error);
                return CollectResult.fail("COLLECT_ERROR", error);
            }
            
        } catch (Exception e) {
            traceService.recordPythonError(trace, e.getMessage());
            traceService.endTrace(trace, false, e.getMessage());
            return CollectResult.serviceError(e.getMessage());
        }
    }

    @Override
    public CollectResult<List<FundNav>> collectNavHistory(String fundCode) {
        String url = collectorUrl + "/api/collect/nav/" + fundCode;
        
        ApiCallLog trace = traceService.startTrace(fundCode, "nav", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, Map.class);
            
            String bodyStr = objectMapper.writeValueAsString(response.getBody());
            traceService.recordPythonSuccess(trace, response.getStatusCodeValue(), bodyStr);
            
            Map<String, Object> result = response.getBody();
            Boolean success = (Boolean) result.get("success");
            
            if (Boolean.TRUE.equals(success)) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.get("data");
                List<FundNav> navList = convertToFundNavList(fundCode, dataList);
                
                traceService.recordJavaResponse(trace, navList);
                traceService.endTrace(trace, true, null);
                
                return CollectResult.success(navList);
            } else {
                String error = (String) result.get("error");
                traceService.endTrace(trace, false, error);
                return CollectResult.fail("COLLECT_ERROR", error);
            }
            
        } catch (Exception e) {
            traceService.recordPythonError(trace, e.getMessage());
            traceService.endTrace(trace, false, e.getMessage());
            return CollectResult.serviceError(e.getMessage());
        }
    }

    // 其他方法保持不变...
    private FundInfo convertToFundInfo(Map<String, Object> data) {
        FundInfo info = new FundInfo();
        info.setFundCode((String) data.get("fundCode"));
        info.setFundName((String) data.get("fundName"));
        return info;
    }

    private FundMetrics convertToFundMetrics(Map<String, Object> data) {
        FundMetrics metrics = new FundMetrics();
        metrics.setFundCode((String) data.get("fundCode"));
        return metrics;
    }

    private List<FundNav> convertToFundNavList(String fundCode, List<Map<String, Object>> dataList) {
        return List.of();
    }
}
