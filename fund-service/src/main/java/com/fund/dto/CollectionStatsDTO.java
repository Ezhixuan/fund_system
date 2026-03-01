package com.fund.dto;

/**
 * 采集统计DTO
 */
public class CollectionStatsDTO {
    
    private String date;
    private Integer totalFunds;
    private Integer collectedFunds;
    private Double successRate;
    private Integer failedCount;
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public Integer getTotalFunds() {
        return totalFunds;
    }
    
    public void setTotalFunds(Integer totalFunds) {
        this.totalFunds = totalFunds;
    }
    
    public Integer getCollectedFunds() {
        return collectedFunds;
    }
    
    public void setCollectedFunds(Integer collectedFunds) {
        this.collectedFunds = collectedFunds;
    }
    
    public Double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }
    
    public Integer getFailedCount() {
        return failedCount;
    }
    
    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }
}
