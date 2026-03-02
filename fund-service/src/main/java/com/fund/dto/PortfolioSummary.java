package com.fund.dto;

import java.math.BigDecimal;

public class PortfolioSummary {
    private BigDecimal totalCost;
    private BigDecimal totalMarketValue;
    private BigDecimal totalDailyReturn;
    private BigDecimal totalReturn;
    private BigDecimal totalReturnPct;
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public BigDecimal getTotalMarketValue() { return totalMarketValue; }
    public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }
    
    public BigDecimal getTotalDailyReturn() { return totalDailyReturn; }
    public void setTotalDailyReturn(BigDecimal totalDailyReturn) { this.totalDailyReturn = totalDailyReturn; }
    
    public BigDecimal getTotalReturn() { return totalReturn; }
    public void setTotalReturn(BigDecimal totalReturn) { this.totalReturn = totalReturn; }
    
    public BigDecimal getTotalReturnPct() { return totalReturnPct; }
    public void setTotalReturnPct(BigDecimal totalReturnPct) { this.totalReturnPct = totalReturnPct; }
}
