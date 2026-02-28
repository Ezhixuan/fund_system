package com.fund.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 基金信息VO
 */
public class FundInfoVO {
    
    private String fundCode;
    private String fundName;
    private String namePinyin;
    private String fundType;
    private String investStyle;
    private String managerName;
    private String companyName;
    private Integer riskLevel;
    private LocalDate establishDate;
    private BigDecimal currentScale;
    private String benchmark;
    
    // Getters and Setters
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public String getNamePinyin() { return namePinyin; }
    public void setNamePinyin(String namePinyin) { this.namePinyin = namePinyin; }
    
    public String getFundType() { return fundType; }
    public void setFundType(String fundType) { this.fundType = fundType; }
    
    public String getInvestStyle() { return investStyle; }
    public void setInvestStyle(String investStyle) { this.investStyle = investStyle; }
    
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public Integer getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Integer riskLevel) { this.riskLevel = riskLevel; }
    
    public LocalDate getEstablishDate() { return establishDate; }
    public void setEstablishDate(LocalDate establishDate) { this.establishDate = establishDate; }
    
    public BigDecimal getCurrentScale() { return currentScale; }
    public void setCurrentScale(BigDecimal currentScale) { this.currentScale = currentScale; }
    
    public String getBenchmark() { return benchmark; }
    public void setBenchmark(String benchmark) { this.benchmark = benchmark; }
}
