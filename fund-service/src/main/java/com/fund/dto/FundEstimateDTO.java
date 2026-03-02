package com.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基金实时估值DTO（从Python采集服务获取）
 */
public class FundEstimateDTO {
    private String fundCode;
    private String fundName;
    private BigDecimal estimateNav;
    private BigDecimal estimateChangePct;
    private BigDecimal preCloseNav;
    private String dataSource;
    private LocalDateTime estimateTime;
    
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public BigDecimal getEstimateNav() { return estimateNav; }
    public void setEstimateNav(BigDecimal estimateNav) { this.estimateNav = estimateNav; }
    
    public BigDecimal getEstimateChangePct() { return estimateChangePct; }
    public void setEstimateChangePct(BigDecimal estimateChangePct) { this.estimateChangePct = estimateChangePct; }
    
    public BigDecimal getPreCloseNav() { return preCloseNav; }
    public void setPreCloseNav(BigDecimal preCloseNav) { this.preCloseNav = preCloseNav; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    
    public LocalDateTime getEstimateTime() { return estimateTime; }
    public void setEstimateTime(LocalDateTime estimateTime) { this.estimateTime = estimateTime; }
}
