package com.fund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金净值历史实体
 */
@TableName("fund_nav")
public class FundNav {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private LocalDate navDate;
    private BigDecimal unitNav;
    private BigDecimal accumNav;
    private BigDecimal adjustNav;
    private BigDecimal dailyReturn;
    private String source;
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public LocalDate getNavDate() { return navDate; }
    public void setNavDate(LocalDate navDate) { this.navDate = navDate; }
    
    public BigDecimal getUnitNav() { return unitNav; }
    public void setUnitNav(BigDecimal unitNav) { this.unitNav = unitNav; }
    
    public BigDecimal getAccumNav() { return accumNav; }
    public void setAccumNav(BigDecimal accumNav) { this.accumNav = accumNav; }
    
    public BigDecimal getAdjustNav() { return adjustNav; }
    public void setAdjustNav(BigDecimal adjustNav) { this.adjustNav = adjustNav; }
    
    public BigDecimal getDailyReturn() { return dailyReturn; }
    public void setDailyReturn(BigDecimal dailyReturn) { this.dailyReturn = dailyReturn; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
