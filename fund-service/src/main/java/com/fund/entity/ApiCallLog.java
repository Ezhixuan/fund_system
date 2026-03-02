package com.fund.entity;

import java.time.LocalDateTime;

/**
 * API调用链路日志
 * 记录Java -> Python的完整调用过程
 */
public class ApiCallLog {
    
    private String requestId;        // 请求唯一ID
    private String fundCode;         // 基金代码
    private String apiType;          // 调用类型: info/metrics/nav/estimate
    private LocalDateTime startTime; // 开始时间
    private LocalDateTime endTime;   // 结束时间
    private Long duration;           // 耗时(ms)
    private String status;           // 状态: success/timeout/error
    private String pythonUrl;        // Python接口URL
    private Integer pythonStatus;    // Python HTTP状态码
    private String pythonResponse;   // Python返回的原始数据(前1000字符)
    private String javaResponse;     // Java处理后返回的数据摘要
    private String errorMessage;     // 错误信息
    private String clientIp;         // 客户端IP
    
    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getApiType() { return apiType; }
    public void setApiType(String apiType) { this.apiType = apiType; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPythonUrl() { return pythonUrl; }
    public void setPythonUrl(String pythonUrl) { this.pythonUrl = pythonUrl; }
    
    public Integer getPythonStatus() { return pythonStatus; }
    public void setPythonStatus(Integer pythonStatus) { this.pythonStatus = pythonStatus; }
    
    public String getPythonResponse() { return pythonResponse; }
    public void setPythonResponse(String pythonResponse) { this.pythonResponse = pythonResponse; }
    
    public String getJavaResponse() { return javaResponse; }
    public void setJavaResponse(String javaResponse) { this.javaResponse = javaResponse; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}
