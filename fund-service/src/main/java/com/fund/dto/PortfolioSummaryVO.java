package com.fund.dto;

import java.util.List;

public class PortfolioSummaryVO {
    private List<HoldingWithEstimateVO> holdings;
    private PortfolioSummary summary;
    private Boolean isTradingTime;
    
    public List<HoldingWithEstimateVO> getHoldings() { return holdings; }
    public void setHoldings(List<HoldingWithEstimateVO> holdings) { this.holdings = holdings; }
    
    public PortfolioSummary getSummary() { return summary; }
    public void setSummary(PortfolioSummary summary) { this.summary = summary; }
    
    public Boolean getIsTradingTime() { return isTradingTime; }
    public void setIsTradingTime(Boolean isTradingTime) { this.isTradingTime = isTradingTime; }
}
