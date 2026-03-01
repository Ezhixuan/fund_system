package com.fund.entity.watchlist;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("fund_estimate_intraday")
public class FundEstimateIntraday {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private LocalDateTime estimateTime;
    private BigDecimal estimateNav;
    private BigDecimal estimateChangePct;
    private BigDecimal estimateChangeAmt;
    private BigDecimal preCloseNav;
    private LocalDate tradeDate;
    private String dataSource;
    private Integer isTradingTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    public LocalDateTime getEstimateTime() { return estimateTime; }
    public void setEstimateTime(LocalDateTime estimateTime) { this.estimateTime = estimateTime; }
    public BigDecimal getEstimateNav() { return estimateNav; }
    public void setEstimateNav(BigDecimal estimateNav) { this.estimateNav = estimateNav; }
    public BigDecimal getEstimateChangePct() { return estimateChangePct; }
    public void setEstimateChangePct(BigDecimal estimateChangePct) { this.estimateChangePct = estimateChangePct; }
    public BigDecimal getEstimateChangeAmt() { return estimateChangeAmt; }
    public void setEstimateChangeAmt(BigDecimal estimateChangeAmt) { this.estimateChangeAmt = estimateChangeAmt; }
    public BigDecimal getPreCloseNav() { return preCloseNav; }
    public void setPreCloseNav(BigDecimal preCloseNav) { this.preCloseNav = preCloseNav; }
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public Integer getIsTradingTime() { return isTradingTime; }
    public void setIsTradingTime(Integer isTradingTime) { this.isTradingTime = isTradingTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
