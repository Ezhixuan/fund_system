package com.fund.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.entity.ApiCallLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API调用链路追踪服务
 * 记录Java -> Python的完整调用过程
 */
@Service
public class ApiTraceService {
    
    private static final Logger log = LoggerFactory.getLogger(ApiTraceService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String TRACE_KEY_PREFIX = "api:trace:";
    private static final int MAX_LOGS = 1000;
    
    /**
     * 开始记录调用链路
     */
    public ApiCallLog startTrace(String fundCode, String apiType, String pythonUrl) {
        ApiCallLog trace = new ApiCallLog();
        trace.setRequestId(UUID.randomUUID().toString().substring(0, 8));
        trace.setFundCode(fundCode);
        trace.setApiType(apiType);
        trace.setPythonUrl(pythonUrl);
        trace.setStartTime(LocalDateTime.now());
        trace.setStatus("pending");
        
        return trace;
    }
    
    /**
     * 记录Python调用成功
     */
    public void recordPythonSuccess(ApiCallLog trace, int httpStatus, String pythonResponse) {
        trace.setPythonStatus(httpStatus);
        trace.setPythonResponse(pythonResponse.length() > 2000 
            ? pythonResponse.substring(0, 2000) + "..." 
            : pythonResponse);
    }
    
    /**
     * 记录Python调用失败
     */
    public void recordPythonError(ApiCallLog trace, String errorMessage) {
        trace.setStatus("python_error");
        trace.setErrorMessage(errorMessage);
    }
    
    /**
     * 记录Java处理结果
     */
    public void recordJavaResponse(ApiCallLog trace, Object javaResponse) {
        try {
            String json = objectMapper.writeValueAsString(javaResponse);
            trace.setJavaResponse(json.length() > 500 
                ? json.substring(0, 500) + "..." 
                : json);
        } catch (JsonProcessingException e) {
            trace.setJavaResponse("无法序列化: " + e.getMessage());
        }
    }
    
    /**
     * 完成链路记录
     */
    public void endTrace(ApiCallLog trace, boolean success, String errorMsg) {
        trace.setEndTime(LocalDateTime.now());
        trace.setDuration(
            java.time.Duration.between(trace.getStartTime(), trace.getEndTime()).toMillis()
        );
        trace.setStatus(success ? "success" : "error");
        if (errorMsg != null) {
            trace.setErrorMessage(errorMsg);
        }
        
        saveTrace(trace);
        
        if (success) {
            log.info("[链路] {} {} -> {} 耗时{}ms", 
                trace.getFundCode(), trace.getApiType(), trace.getStatus(), trace.getDuration());
        } else {
            log.warn("[链路] {} {} -> {} 错误: {}", 
                trace.getFundCode(), trace.getApiType(), trace.getStatus(), errorMsg);
        }
    }
    
    /**
     * 保存链路日志到Redis
     */
    private void saveTrace(ApiCallLog trace) {
        try {
            String key = TRACE_KEY_PREFIX + trace.getApiType();
            String value = objectMapper.writeValueAsString(trace);
            
            redisTemplate.opsForList().leftPush(key, value);
            redisTemplate.opsForList().trim(key, 0, MAX_LOGS - 1);
            
            redisTemplate.opsForList().leftPush(TRACE_KEY_PREFIX + "all", value);
            redisTemplate.opsForList().trim(TRACE_KEY_PREFIX + "all", 0, MAX_LOGS - 1);
            
        } catch (Exception e) {
            log.error("保存链路日志失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取最近链路日志
     */
    public List<ApiCallLog> getRecentTraces(String apiType, int limit) {
        String key = TRACE_KEY_PREFIX + (apiType != null ? apiType : "all");
        
        List<String> logs = redisTemplate.opsForList().range(key, 0, limit - 1);
        if (logs == null) return List.of();
        
        return logs.stream()
            .map(json -> {
                try {
                    return objectMapper.readValue(json, ApiCallLog.class);
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(log -> log != null)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取链路统计
     */
    public TraceStats getTraceStats(String apiType, int minutes) {
        List<ApiCallLog> traces = getRecentTraces(apiType, 1000);
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        
        long total = traces.stream()
            .filter(t -> t.getStartTime().isAfter(cutoff))
            .count();
        
        long success = traces.stream()
            .filter(t -> t.getStartTime().isAfter(cutoff))
            .filter(t -> "success".equals(t.getStatus()))
            .count();
        
        double avgDuration = traces.stream()
            .filter(t -> t.getStartTime().isAfter(cutoff))
            .filter(t -> t.getDuration() != null)
            .mapToLong(ApiCallLog::getDuration)
            .average()
            .orElse(0);
        
        TraceStats stats = new TraceStats();
        stats.setTotal((int) total);
        stats.setSuccess((int) success);
        stats.setFailed((int) (total - success));
        stats.setSuccessRate(total > 0 ? (int) (success * 100 / total) : 0);
        stats.setAvgDuration((long) avgDuration);
        
        return stats;
    }
    
    /**
     * 链路统计内部类
     */
    public static class TraceStats {
        private int total;
        private int success;
        private int failed;
        private int successRate;
        private Long avgDuration;
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getSuccess() { return success; }
        public void setSuccess(int success) { this.success = success; }
        
        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
        
        public int getSuccessRate() { return successRate; }
        public void setSuccessRate(int successRate) { this.successRate = successRate; }
        
        public Long getAvgDuration() { return avgDuration; }
        public void setAvgDuration(Long avgDuration) { this.avgDuration = avgDuration; }
    }
}
