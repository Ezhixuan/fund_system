package com.fund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金全维指标实体
 */
@TableName("fund_metrics")
public class FundMetrics {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private LocalDate calcDate;
    private BigDecimal return1m;
    private BigDecimal return3m;
    private BigDecimal return1y;
    private BigDecimal return3y;
    private BigDecimal return5y;
    private BigDecimal sharpeRatio1y;
    private BigDecimal sharpeRatio3y;
    private BigDecimal sortinoRatio1y;
    private BigDecimal calmarRatio3y;
    private BigDecimal informationRatio1y;
    private BigDecimal maxDrawdown1y;
    private BigDecimal maxDrawdown3y;
    private BigDecimal volatility1y;
    private BigDecimal volatility3y;
    private BigDecimal alpha1y;
    private BigDecimal beta1y;
    private BigDecimal trackingError1y;
    private Integer pePercentile;
    private Integer pbPercentile;
    private LocalDateTime updateTime;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public LocalDate getCalcDate() { return calcDate; }
    public void setCalcDate(LocalDate calcDate) { this.calcDate = calcDate; }
    
    public BigDecimal getReturn1m() { return return1m; }
    public void setReturn1m(BigDecimal return1m) { this.return1m = return1m; }
    
    public BigDecimal getReturn3m() { return return3m; }
    public void setReturn3m(BigDecimal return3m) { this.return3m = return3m; }
    
    public BigDecimal getReturn1y() { return return1y; }
    public void setReturn1y(BigDecimal return1y) { this.return1y = return1y; }
    
    public BigDecimal getReturn3y() { return return3y; }
    public void setReturn3y(BigDecimal return3y) { this.return3y = return3y; }
    
    public BigDecimal getReturn5y() { return return5y; }
    public void setReturn5y(BigDecimal return5y) { this.return5y = return5y; }
    
    public BigDecimal getSharpeRatio1y() { return sharpeRatio1y; }
    public void setSharpeRatio1y(BigDecimal sharpeRatio1y) { this.sharpeRatio1y = sharpeRatio1y; }
    
    public BigDecimal getSharpeRatio3y() { return sharpeRatio3y; }
    public void setSharpeRatio3y(BigDecimal sharpeRatio3y) { this.sharpeRatio3y = sharpeRatio3y; }
    
    public BigDecimal getSortinoRatio1y() { return sortinoRatio1y; }
    public void setSortinoRatio1y(BigDecimal sortinoRatio1y) { this.sortinoRatio1y = sortinoRatio1y; }
    
    public BigDecimal getCalmarRatio3y() { return calmarRatio3y; }
    public void setCalmarRatio3y(BigDecimal calmarRatio3y) { this.calmarRatio3y = calmarRatio3y; }
    
    public BigDecimal getInformationRatio1y() { return informationRatio1y; }
    public void setInformationRatio1y(BigDecimal informationRatio1y) { this.informationRatio1y = informationRatio1y; }
    
    public BigDecimal getMaxDrawdown1y() { return maxDrawdown1y; }
    public void setMaxDrawdown1y(BigDecimal maxDrawdown1y) { this.maxDrawdown1y = maxDrawdown1y; }
    
    public BigDecimal getMaxDrawdown3y() { return maxDrawdown3y; }
    public void setMaxDrawdown3y(BigDecimal maxDrawdown3y) { this.maxDrawdown3y = maxDrawdown3y; }
    
    public BigDecimal getVolatility1y() { return volatility1y; }
    public void setVolatility1y(BigDecimal volatility1y) { this.volatility1y = volatility1y; }
    
    public BigDecimal getVolatility3y() { return volatility3y; }
    public void setVolatility3y(BigDecimal volatility3y) { this.volatility3y = volatility3y; }
    
    public BigDecimal getAlpha1y() { return alpha1y; }
    public void setAlpha1y(BigDecimal alpha1y) { this.alpha1y = alpha1y; }
    
    public BigDecimal getBeta1y() { return beta1y; }
    public void setBeta1y(BigDecimal beta1y) { this.beta1y = beta1y; }
    
    public BigDecimal getTrackingError1y() { return trackingError1y; }
    public void setTrackingError1y(BigDecimal trackingError1y) { this.trackingError1y = trackingError1y; }
    
    public Integer getPePercentile() { return pePercentile; }
    public void setPePercentile(Integer pePercentile) { this.pePercentile = pePercentile; }
    
    public Integer getPbPercentile() { return pbPercentile; }
    public void setPbPercentile(Integer pbPercentile) { this.pbPercentile = pbPercentile; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
