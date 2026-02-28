package com.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 基金净值VO
 */
public class FundNavVO {
    
    private LocalDate navDate;
    private BigDecimal unitNav;
    private BigDecimal accumNav;
    private BigDecimal dailyReturn;
    
    // Getters and Setters
    public LocalDate getNavDate() { return navDate; }
    public void setNavDate(LocalDate navDate) { this.navDate = navDate; }
    
    public BigDecimal getUnitNav() { return unitNav; }
    public void setUnitNav(BigDecimal unitNav) { this.unitNav = unitNav; }
    
    public BigDecimal getAccumNav() { return accumNav; }
    public void setAccumNav(BigDecimal accumNav) { this.accumNav = accumNav; }
    
    public BigDecimal getDailyReturn() { return dailyReturn; }
    public void setDailyReturn(BigDecimal dailyReturn) { this.dailyReturn = dailyReturn; }
}
