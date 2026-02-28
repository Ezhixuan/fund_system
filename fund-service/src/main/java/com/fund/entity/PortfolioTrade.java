package com.fund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易记录实体
 */
@TableName("portfolio_trade")
public class PortfolioTrade {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private LocalDate tradeDate;
    private Integer tradeType;  // 1买入, 2卖出
    private BigDecimal tradeShare;
    private BigDecimal tradePrice;
    private BigDecimal tradeAmount;
    private BigDecimal tradeFee;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public BigDecimal getTradeAmount() { return tradeAmount; }
    public void setTradeAmount(BigDecimal tradeAmount) { this.tradeAmount = tradeAmount; }
    
    public BigDecimal getTradeFee() { return tradeFee; }
    public void setTradeFee(BigDecimal tradeFee) { this.tradeFee = tradeFee; }
    
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
