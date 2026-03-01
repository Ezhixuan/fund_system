package com.ezhixuan.fund.domain.entity.watchlist;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易日历
 */
@Data
@TableName("trading_calendar")
public class TradingCalendar {
    
    /**
     * 日期
     */
    @TableId(type = IdType.INPUT)
    private LocalDate tradeDate;
    
    /**
     * 是否为交易日: 0-否, 1-是
     */
    private Integer isTradingDay;
    
    /**
     * 是否为节假日: 0-否, 1-是
     */
    private Integer isHoliday;
    
    /**
     * 节假日名称
     */
    private String holidayName;
    
    /**
     * 上一交易日
     */
    private LocalDate prevTradingDay;
    
    /**
     * 下一交易日
     */
    private LocalDate nextTradingDay;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
