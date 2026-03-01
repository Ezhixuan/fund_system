package com.fund.service.collect.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.dto.ApiResponse;
import com.fund.service.collect.CollectClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectClientImpl implements CollectClient {
    
    private static final Logger log = LoggerFactory.getLogger(CollectClientImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${collector.url:http://localhost:5000}")
    private String collectorUrl;
    
    @Override
    public ApiResponse<Map<String, Object>> collectEstimate(String fundCode) {
        try {
            String url = collectorUrl + "/api/collect/estimate";
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("fundCode", fundCode);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("采集服务调用失败: {}", response.getStatusCode());
                return ApiResponse.error("采集服务调用失败");
            }
            
            Map<String, Object> result = response.getBody();
            
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                return ApiResponse.success((Map<String, Object>) result.get("data"));
            } else {
                return ApiResponse.error((String) (result != null ? result.get("error") : "未知错误"));
            }
        } catch (Exception e) {
            log.error("采集服务调用异常: {}", e.getMessage());
            return ApiResponse.error("采集服务不可用: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<Map<String, Object>> collectBatch(List<String> fundCodes) {
        try {
            String url = collectorUrl + "/api/collect/batch";
            
            Map<String, List<String>> requestBody = new HashMap<>();
            requestBody.put("fundCodes", fundCodes);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("批量采集服务调用失败: {}", response.getStatusCode());
                return ApiResponse.error("批量采集服务调用失败");
            }
            
            Map<String, Object> result = response.getBody();
            
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                return ApiResponse.success((Map<String, Object>) result.get("data"));
            } else {
                return ApiResponse.error((String) (result != null ? result.get("error") : "未知错误"));
            }
        } catch (Exception e) {
            log.error("批量采集服务调用异常: {}", e.getMessage());
            return ApiResponse.error("采集服务不可用: " + e.getMessage());
        }
    }
    
    @Override
    public boolean healthCheck() {
        try {
            String url = collectorUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("采集服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
