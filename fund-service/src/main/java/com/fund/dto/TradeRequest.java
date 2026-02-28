package com.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 交易请求DTO
 */
public class TradeRequest {
    
    private String fundCode;
    private LocalDate tradeDate;
    private Integer tradeType;  // 1买入, 2卖出
    private BigDecimal tradeShare;
    private BigDecimal tradePrice;
    private BigDecimal tradeFee;
    private String remark;
    
    // Getters and Setters
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    
    public Integer getTradeType() { return tradeType; }
    public void setTradeType(Integer tradeType) { this.tradeType = tradeType; }
    
    public BigDecimal getTradeShare() { return tradeShare; }
    public void setTradeShare(BigDecimal tradeShare) { this.tradeShare = tradeShare; }
    
    public BigDecimal getTradePrice() { return tradePrice; }
    public void setTradePrice(BigDecimal tradePrice) { this.tradePrice = tradePrice; }
    
    public BigDecimal getTradeFee() { return tradeFee; }
    public void setTradeFee(BigDecimal tradeFee) { this.tradeFee = tradeFee; }
    
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
