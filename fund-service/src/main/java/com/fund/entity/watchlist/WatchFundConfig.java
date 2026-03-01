package com.fund.entity.watchlist;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("watch_fund_config")
public class WatchFundConfig {
    @TableId(type = IdType.INPUT)
    private String fundCode;
    private Integer needDetail;
    private Integer needNav;
    private Integer needIntraday;
    private Integer needPortfolio;
    private LocalDate lastCollectDate;
    private LocalDateTime lastIntradayTime;
    private Integer collectIntervalMinutes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    public Integer getNeedDetail() { return needDetail; }
    public void setNeedDetail(Integer needDetail) { this.needDetail = needDetail; }
    public Integer getNeedNav() { return needNav; }
    public void setNeedNav(Integer needNav) { this.needNav = needNav; }
    public Integer getNeedIntraday() { return needIntraday; }
    public void setNeedIntraday(Integer needIntraday) { this.needIntraday = needIntraday; }
    public Integer getNeedPortfolio() { return needPortfolio; }
    public void setNeedPortfolio(Integer needPortfolio) { this.needPortfolio = needPortfolio; }
    public LocalDate getLastCollectDate() { return lastCollectDate; }
    public void setLastCollectDate(LocalDate lastCollectDate) { this.lastCollectDate = lastCollectDate; }
    public LocalDateTime getLastIntradayTime() { return lastIntradayTime; }
    public void setLastIntradayTime(LocalDateTime lastIntradayTime) { this.lastIntradayTime = lastIntradayTime; }
    public Integer getCollectIntervalMinutes() { return collectIntervalMinutes; }
    public void setCollectIntervalMinutes(Integer collectIntervalMinutes) { this.collectIntervalMinutes = collectIntervalMinutes; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
