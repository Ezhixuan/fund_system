package com.fund.vo.websocket;

import java.math.BigDecimal;

/**
 * 实时估值消息VO
 * 用于WebSocket传输
 */
public class IntradayEstimateVO {
    
    private String fundCode;
    private String fundName;
    private String estimateTime;
    private BigDecimal estimateNav;
    private BigDecimal estimateChangePct;
    private BigDecimal estimateChangeAmt;
    private BigDecimal preCloseNav;
    private String tradeDate;
    private String dataSource;
    
    // Getters and Setters
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public String getEstimateTime() { return estimateTime; }
    public void setEstimateTime(String estimateTime) { this.estimateTime = estimateTime; }
    
    public BigDecimal getEstimateNav() { return estimateNav; }
    public void setEstimateNav(BigDecimal estimateNav) { this.estimateNav = estimateNav; }
    
    public BigDecimal getEstimateChangePct() { return estimateChangePct; }
    public void setEstimateChangePct(BigDecimal estimateChangePct) { this.estimateChangePct = estimateChangePct; }
    
    public BigDecimal getEstimateChangeAmt() { return estimateChangeAmt; }
    public void setEstimateChangeAmt(BigDecimal estimateChangeAmt) { this.estimateChangeAmt = estimateChangeAmt; }
    
    public BigDecimal getPreCloseNav() { return preCloseNav; }
    public void setPreCloseNav(BigDecimal preCloseNav) { this.preCloseNav = preCloseNav; }
    
    public String getTradeDate() { return tradeDate; }
    public void setTradeDate(String tradeDate) { this.tradeDate = tradeDate; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
