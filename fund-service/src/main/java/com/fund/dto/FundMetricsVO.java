package com.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 基金指标VO
 */
public class FundMetricsVO {
    
    private String fundCode;
    private String fundName;
    private LocalDate calcDate;
    private BigDecimal return1m;
    private BigDecimal return3m;
    private BigDecimal return1y;
    private BigDecimal return3y;
    private BigDecimal sharpeRatio1y;
    private BigDecimal maxDrawdown1y;
    private BigDecimal volatility1y;
    private String qualityLevel;
    
    // Getters and Setters
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public LocalDate getCalcDate() { return calcDate; }
    public void setCalcDate(LocalDate calcDate) { this.calcDate = calcDate; }
    
    public BigDecimal getReturn1m() { return return1m; }
    public void setReturn1m(BigDecimal return1m) { this.return1m = return1m; }
    
    public BigDecimal getReturn3m() { return return3m; }
    public void setReturn3m(BigDecimal return3m) { this.return3m = return3m; }
    
    public BigDecimal getReturn1y() { return return1y; }
    public void setReturn1y(BigDecimal return1y) { this.return1y = return1y; }
    
    public BigDecimal getReturn3y() { return return3y; }
    public void setReturn3y(BigDecimal return3y) { this.return3y = return3y; }
    
    public BigDecimal getSharpeRatio1y() { return sharpeRatio1y; }
    public void setSharpeRatio1y(BigDecimal sharpeRatio1y) { this.sharpeRatio1y = sharpeRatio1y; }
    
    public BigDecimal getMaxDrawdown1y() { return maxDrawdown1y; }
    public void setMaxDrawdown1y(BigDecimal maxDrawdown1y) { this.maxDrawdown1y = maxDrawdown1y; }
    
    public BigDecimal getVolatility1y() { return volatility1y; }
    public void setVolatility1y(BigDecimal volatility1y) { this.volatility1y = volatility1y; }
    
    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }
}
