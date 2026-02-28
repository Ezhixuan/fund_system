package com.fund.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 组合分析VO
 */
public class PortfolioAnalysis {
    
    private Integer holdingCount;           // 持仓基金数
    private BigDecimal totalCost;           // 总成本
    private BigDecimal totalValue;          // 总市值
    private BigDecimal totalReturn;         // 总收益
    private BigDecimal totalReturnRate;     // 总收益率(%)
    
    // 风险分布
    private Map<String, Integer> riskDistribution;  // 高中低风险分布
    
    // 类型分布
    private Map<String, Integer> typeDistribution;   // 基金类型分布
    
    // 质量分布
    private Map<String, Integer> qualityDistribution; // S/A/B/C/D分布
    
    // Getters and Setters
    public Integer getHoldingCount() { return holdingCount; }
    public void setHoldingCount(Integer holdingCount) { this.holdingCount = holdingCount; }
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public BigDecimal getTotalReturn() { return totalReturn; }
    public void setTotalReturn(BigDecimal totalReturn) { this.totalReturn = totalReturn; }
    
    public BigDecimal getTotalReturnRate() { return totalReturnRate; }
    public void setTotalReturnRate(BigDecimal totalReturnRate) { this.totalReturnRate = totalReturnRate; }
    
    public Map<String, Integer> getRiskDistribution() { return riskDistribution; }
    public void setRiskDistribution(Map<String, Integer> riskDistribution) { this.riskDistribution = riskDistribution; }
    
    public Map<String, Integer> getTypeDistribution() { return typeDistribution; }
    public void setTypeDistribution(Map<String, Integer> typeDistribution) { this.typeDistribution = typeDistribution; }
    
    public Map<String, Integer> getQualityDistribution() { return qualityDistribution; }
    public void setQualityDistribution(Map<String, Integer> qualityDistribution) { this.qualityDistribution = qualityDistribution; }
}
