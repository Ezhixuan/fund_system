package com.fund.service.collect.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.dto.ApiResponse;
import com.fund.service.collect.CollectClient;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectClientImpl implements CollectClient {
    
    private static final Logger log = LoggerFactory.getLogger(CollectClientImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    @Value("${collector.url:http://localhost:5000}")
    private String collectorUrl;
    
    @Override
    public ApiResponse<Map<String, Object>> collectEstimate(String fundCode) {
        try {
            String url = collectorUrl + "/api/collect/estimate";
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("fundCode", fundCode);
            
            RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestBody), 
                JSON
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("采集服务调用失败: {}", response.code());
                    return ApiResponse.error("采集服务调用失败");
                }
                
                String responseBody = response.body().string();
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                
                if (Boolean.TRUE.equals(result.get("success"))) {
                    return ApiResponse.success((Map<String, Object>) result.get("data"));
                } else {
                    return ApiResponse.error((String) result.get("error"));
                }
            }
        } catch (IOException e) {
            log.error("采集服务调用异常: {}", e.getMessage());
            return ApiResponse.error("采集服务不可用");
        }
    }
    
    @Override
    public ApiResponse<Map<String, Object>> collectBatch(List<String> fundCodes) {
        try {
            String url = collectorUrl + "/api/collect/batch";
            
            Map<String, List<String>> requestBody = new HashMap<>();
            requestBody.put("fundCodes", fundCodes);
            
            RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestBody), 
                JSON
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("批量采集服务调用失败: {}", response.code());
                    return ApiResponse.error("批量采集服务调用失败");
                }
                
                String responseBody = response.body().string();
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                
                if (Boolean.TRUE.equals(result.get("success"))) {
                    return ApiResponse.success((Map<String, Object>) result.get("data"));
                } else {
                    return ApiResponse.error((String) result.get("error"));
                }
            }
        } catch (IOException e) {
            log.error("批量采集服务调用异常: {}", e.getMessage());
            return ApiResponse.error("采集服务不可用");
        }
    }
    
    @Override
    public boolean healthCheck() {
        try {
            String url = collectorUrl + "/health";
            
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            log.warn("采集服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
