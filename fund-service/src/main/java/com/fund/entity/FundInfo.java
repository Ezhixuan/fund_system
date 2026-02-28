package com.fund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金基础信息实体
 */
@TableName("fund_info")
public class FundInfo {
    
    @TableId(type = IdType.INPUT)
    private String fundCode;
    private String fundName;
    private String namePinyin;
    private String fundType;
    private String investStyle;
    private String managerCode;
    private String managerName;
    private String companyCode;
    private String companyName;
    private LocalDate establishDate;
    private String benchmark;
    private BigDecimal managementFee;
    private BigDecimal custodyFee;
    private Integer riskLevel;
    private BigDecimal currentScale;
    private Integer status;
    private LocalDateTime updateTime;
    
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
    
    public String getManagerCode() { return managerCode; }
    public void setManagerCode(String managerCode) { this.managerCode = managerCode; }
    
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public LocalDate getEstablishDate() { return establishDate; }
    public void setEstablishDate(LocalDate establishDate) { this.establishDate = establishDate; }
    
    public String getBenchmark() { return benchmark; }
    public void setBenchmark(String benchmark) { this.benchmark = benchmark; }
    
    public BigDecimal getManagementFee() { return managementFee; }
    public void setManagementFee(BigDecimal managementFee) { this.managementFee = managementFee; }
    
    public BigDecimal getCustodyFee() { return custodyFee; }
    public void setCustodyFee(BigDecimal custodyFee) { this.custodyFee = custodyFee; }
    
    public Integer getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Integer riskLevel) { this.riskLevel = riskLevel; }
    
    public BigDecimal getCurrentScale() { return currentScale; }
    public void setCurrentScale(BigDecimal currentScale) { this.currentScale = currentScale; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
