package com.fund.entity.watchlist;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("trading_calendar")
public class TradingCalendar {
    @TableId(type = IdType.INPUT)
    private LocalDate tradeDate;
    private Integer isTradingDay;
    private Integer isHoliday;
    private String holidayName;
    private LocalDate prevTradingDay;
    private LocalDate nextTradingDay;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    public Integer getIsTradingDay() { return isTradingDay; }
    public void setIsTradingDay(Integer isTradingDay) { this.isTradingDay = isTradingDay; }
    public Integer getIsHoliday() { return isHoliday; }
    public void setIsHoliday(Integer isHoliday) { this.isHoliday = isHoliday; }
    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }
    public LocalDate getPrevTradingDay() { return prevTradingDay; }
    public void setPrevTradingDay(LocalDate prevTradingDay) { this.prevTradingDay = prevTradingDay; }
    public LocalDate getNextTradingDay() { return nextTradingDay; }
    public void setNextTradingDay(LocalDate nextTradingDay) { this.nextTradingDay = nextTradingDay; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
