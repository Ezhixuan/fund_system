package com.fund.dto;

import java.util.List;
import java.util.Map;

/**
 * 监控状态响应DTO
 */
public class MonitorStatusDTO {
    
    private String status;  // healthy / warning / error
    private String timestamp;
    
    // 数据表状态
    private List<TableStatusDTO> tables;
    
    // 采集统计
    private CollectionStatsDTO collection;
    
    // 数据质量
    private Map<String, Object> quality;
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<TableStatusDTO> getTables() {
        return tables;
    }
    
    public void setTables(List<TableStatusDTO> tables) {
        this.tables = tables;
    }
    
    public CollectionStatsDTO getCollection() {
        return collection;
    }
    
    public void setCollection(CollectionStatsDTO collection) {
        this.collection = collection;
    }
    
    public Map<String, Object> getQuality() {
        return quality;
    }
    
    public void setQuality(Map<String, Object> quality) {
        this.quality = quality;
    }
}
