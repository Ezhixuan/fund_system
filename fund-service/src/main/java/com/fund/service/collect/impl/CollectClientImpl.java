package com.fund.service.collect.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.dto.ApiResponse;
import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.service.collect.CollectClient;
import com.fund.service.collect.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
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

    // ============ 新增：基金详情页数据实时采集接口实现 ============

    @Override
    public CollectResult<FundInfo> collectFundInfo(String fundCode) {
        try {
            log.info("调用Python服务采集基金[{}]基本信息", fundCode);
            String url = collectorUrl + "/api/collect/fund/" + fundCode;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("采集基金[{}]基本信息失败: HTTP {}", fundCode, response.getStatusCode());
                return CollectResult.serviceError("HTTP " + response.getStatusCode());
            }

            Map<String, Object> result = response.getBody();
            if (result == null) {
                return CollectResult.serviceError("返回结果为空");
            }

            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                FundInfo fundInfo = convertToFundInfo(data);
                return CollectResult.success(fundInfo);
            } else {
                String errorCode = (String) result.get("errorCode");
                String message = (String) result.get("message");
                if ("FUND_NOT_FOUND".equals(errorCode)) {
                    return CollectResult.notFound(fundCode);
                }
                return CollectResult.fail(errorCode, message);
            }
        } catch (RestClientException e) {
            log.error("采集基金[{}]基本信息异常: {}", fundCode, e.getMessage());
            return CollectResult.serviceError(e.getMessage());
        }
    }

    @Override
    public CollectResult<FundMetrics> collectFundMetrics(String fundCode) {
        try {
            log.info("调用Python服务采集基金[{}]指标数据", fundCode);
            String url = collectorUrl + "/api/collect/metrics/" + fundCode;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("采集基金[{}]指标数据失败: HTTP {}", fundCode, response.getStatusCode());
                return CollectResult.serviceError("HTTP " + response.getStatusCode());
            }

            Map<String, Object> result = response.getBody();
            if (result == null) {
                return CollectResult.serviceError("返回结果为空");
            }

            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                FundMetrics metrics = convertToFundMetrics(data);
                return CollectResult.success(metrics);
            } else {
                String errorCode = (String) result.get("errorCode");
                String message = (String) result.get("message");
                if ("FUND_NOT_FOUND".equals(errorCode)) {
                    return CollectResult.notFound(fundCode);
                }
                return CollectResult.fail(errorCode, message);
            }
        } catch (RestClientException e) {
            log.error("采集基金[{}]指标数据异常: {}", fundCode, e.getMessage());
            return CollectResult.serviceError(e.getMessage());
        }
    }

    @Override
    public CollectResult<List<FundNav>> collectNavHistory(String fundCode) {
        try {
            log.info("调用Python服务采集基金[{}]NAV历史数据", fundCode);
            String url = collectorUrl + "/api/collect/nav/" + fundCode;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("采集基金[{}]NAV历史失败: HTTP {}", fundCode, response.getStatusCode());
                return CollectResult.serviceError("HTTP " + response.getStatusCode());
            }

            Map<String, Object> result = response.getBody();
            if (result == null) {
                return CollectResult.serviceError("返回结果为空");
            }

            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.get("data");
                List<FundNav> navList = convertToFundNavList(fundCode, dataList);
                return CollectResult.success(navList);
            } else {
                String errorCode = (String) result.get("errorCode");
                String message = (String) result.get("message");
                if ("FUND_NOT_FOUND".equals(errorCode)) {
                    return CollectResult.notFound(fundCode);
                }
                return CollectResult.fail(errorCode, message);
            }
        } catch (RestClientException e) {
            log.error("采集基金[{}]NAV历史异常: {}", fundCode, e.getMessage());
            return CollectResult.serviceError(e.getMessage());
        }
    }

    // ============ 数据转换方法 ============

    private FundInfo convertToFundInfo(Map<String, Object> data) {
        FundInfo info = new FundInfo();
        info.setFundCode((String) data.get("fundCode"));
        info.setFundName((String) data.get("fundName"));
        info.setFundType((String) data.get("fundType"));
        info.setManagerName((String) data.get("managerName"));
        info.setCompanyName((String) data.get("companyName"));
        // TODO: 完善其他字段映射
        return info;
    }

    private FundMetrics convertToFundMetrics(Map<String, Object> data) {
        FundMetrics metrics = new FundMetrics();
        metrics.setFundCode((String) data.get("fundCode"));
        // TODO: 完善字段映射
        return metrics;
    }

    private List<FundNav> convertToFundNavList(String fundCode, List<Map<String, Object>> dataList) {
        // TODO: 实现转换逻辑
        return List.of();
    }
}
