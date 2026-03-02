package com.fund.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Python采集服务客户端
 */
@Service
public class CollectClient {
    
    private static final Logger log = LoggerFactory.getLogger(CollectClient.class);
    
    @Value("${collector.url:http://localhost:5002}")
    private String collectorUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public CollectClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 触发基金数据采集
     */
    public boolean collectFundData(String fundCode) {
        try {
            String url = collectorUrl + "/api/collect/fund";
            
            Map<String, String> request = new HashMap<>();
            request.put("fundCode", fundCode);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode json = objectMapper.readTree(response.getBody());
                boolean success = json.path("success").asBoolean();
                if (success) {
                    log.info("基金数据采集成功: {}", fundCode);
                    return true;
                } else {
                    log.warn("基金数据采集失败: {}", fundCode);
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("调用采集服务失败: {}", e.getMessage());
            return false;
        }
    }
}
