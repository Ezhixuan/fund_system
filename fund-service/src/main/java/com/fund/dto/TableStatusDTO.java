package com.fund.dto;

/**
 * 数据表状态DTO
 */
public class TableStatusDTO {
    
    private String tableName;
    private String latestDate;
    private Integer recordCount;
    private Boolean isFresh;
    private Integer delayDays;
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getLatestDate() {
        return latestDate;
    }
    
    public void setLatestDate(String latestDate) {
        this.latestDate = latestDate;
    }
    
    public Integer getRecordCount() {
        return recordCount;
    }
    
    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }
    
    public Boolean getIsFresh() {
        return isFresh;
    }
    
    public void setIsFresh(Boolean isFresh) {
        this.isFresh = isFresh;
    }
    
    public Integer getDelayDays() {
        return delayDays;
    }
    
    public void setDelayDays(Integer delayDays) {
        this.delayDays = delayDays;
    }
}
