package com.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓带实时估值VO
 */
public class HoldingWithEstimateVO {
    
    private String fundCode;
    private String fundName;
    private BigDecimal holdShares;
    private BigDecimal avgCost;
    private BigDecimal totalCost;
    private BigDecimal estimateNav;
    private BigDecimal estimateChangePct;
    private BigDecimal estimateMarketValue;
    private BigDecimal estimateDailyReturn;
    private BigDecimal totalReturn;
    private BigDecimal totalReturnPct;
    private LocalDateTime lastUpdateTime;
    
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public BigDecimal getHoldShares() { return holdShares; }
    public void setHoldShares(BigDecimal holdShares) { this.holdShares = holdShares; }
    
    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public BigDecimal getEstimateNav() { return estimateNav; }
    public void setEstimateNav(BigDecimal estimateNav) { this.estimateNav = estimateNav; }
    
    public BigDecimal getEstimateChangePct() { return estimateChangePct; }
    public void setEstimateChangePct(BigDecimal estimateChangePct) { this.estimateChangePct = estimateChangePct; }
    
    public BigDecimal getEstimateMarketValue() { return estimateMarketValue; }
    public void setEstimateMarketValue(BigDecimal estimateMarketValue) { this.estimateMarketValue = estimateMarketValue; }
    
    public BigDecimal getEstimateDailyReturn() { return estimateDailyReturn; }
    public void setEstimateDailyReturn(BigDecimal estimateDailyReturn) { this.estimateDailyReturn = estimateDailyReturn; }
    
    public BigDecimal getTotalReturn() { return totalReturn; }
    public void setTotalReturn(BigDecimal totalReturn) { this.totalReturn = totalReturn; }
    
    public BigDecimal getTotalReturnPct() { return totalReturnPct; }
    public void setTotalReturnPct(BigDecimal totalReturnPct) { this.totalReturnPct = totalReturnPct; }
    
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}
