package com.fund.dto;

import java.math.BigDecimal;

/**
 * 持仓VO
 */
public class HoldingVO {
    
    private String fundCode;
    private String fundName;
    private BigDecimal totalShares;      // 总持有份额
    private BigDecimal avgCost;          // 平均成本
    private BigDecimal totalCost;        // 总成本
    private BigDecimal currentNav;       // 当前净值
    private BigDecimal currentValue;     // 当前市值
    private BigDecimal totalReturn;      // 累计收益
    private BigDecimal returnRate;       // 收益率(%)
    private String qualityLevel;         // 质量等级
    
    // Getters and Setters
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public BigDecimal getTotalShares() { return totalShares; }
    public void setTotalShares(BigDecimal totalShares) { this.totalShares = totalShares; }
    
    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public BigDecimal getCurrentNav() { return currentNav; }
    public void setCurrentNav(BigDecimal currentNav) { this.currentNav = currentNav; }
    
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    
    public BigDecimal getTotalReturn() { return totalReturn; }
    public void setTotalReturn(BigDecimal totalReturn) { this.totalReturn = totalReturn; }
    
    public BigDecimal getReturnRate() { return returnRate; }
    public void setReturnRate(BigDecimal returnRate) { this.returnRate = returnRate; }
    
    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }
}
