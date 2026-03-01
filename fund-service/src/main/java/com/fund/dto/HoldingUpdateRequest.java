package com.fund.dto;

import java.math.BigDecimal;

/**
 * 持仓更新请求
 */
public class HoldingUpdateRequest {
    
    private BigDecimal totalShares;  // 新的份额，不修改可为null
    private BigDecimal avgCost;      // 新的成本价，不修改可为null
    private String remark;           // 备注
    
    // Getters and Setters
    public BigDecimal getTotalShares() {
        return totalShares;
    }
    
    public void setTotalShares(BigDecimal totalShares) {
        this.totalShares = totalShares;
    }
    
    public BigDecimal getAvgCost() {
        return avgCost;
    }
    
    public void setAvgCost(BigDecimal avgCost) {
        this.avgCost = avgCost;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
