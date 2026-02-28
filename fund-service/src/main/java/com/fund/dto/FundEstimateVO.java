package com.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基金实时估值VO
 */
public class FundEstimateVO {
    
    private String fundCode;
    private String fundName;
    private BigDecimal estimateNav;        // 预估净值
    private BigDecimal previousNav;        // 昨日净值
    private BigDecimal dailyChange;        // 日涨跌幅%
    private String estimateTime;           // 估值时间
    private LocalDateTime updateTime;      // 更新时间
    private boolean isMarketOpen;          // 是否交易中
    
    // Getters and Setters
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public BigDecimal getEstimateNav() { return estimateNav; }
    public void setEstimateNav(BigDecimal estimateNav) { this.estimateNav = estimateNav; }
    
    public BigDecimal getPreviousNav() { return previousNav; }
    public void setPreviousNav(BigDecimal previousNav) { this.previousNav = previousNav; }
    
    public BigDecimal getDailyChange() { return dailyChange; }
    public void setDailyChange(BigDecimal dailyChange) { this.dailyChange = dailyChange; }
    
    public String getEstimateTime() { return estimateTime; }
    public void setEstimateTime(String estimateTime) { this.estimateTime = estimateTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public boolean isMarketOpen() { return isMarketOpen; }
    public void setMarketOpen(boolean marketOpen) { isMarketOpen = marketOpen; }
}
