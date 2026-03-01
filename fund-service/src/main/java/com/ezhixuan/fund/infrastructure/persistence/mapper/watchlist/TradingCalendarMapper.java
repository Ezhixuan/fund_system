package com.ezhixuan.fund.infrastructure.persistence.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ezhixuan.fund.domain.entity.watchlist.TradingCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 交易日历 Mapper
 */
@Mapper
public interface TradingCalendarMapper extends BaseMapper<TradingCalendar> {
    
    /**
     * 查询指定日期是否为交易日
     */
    @Select("SELECT is_trading_day FROM trading_calendar WHERE trade_date = #{date}")
    Integer isTradingDay(@Param("date") LocalDate date);
    
    /**
     * 获取上一交易日
     */
    @Select("SELECT prev_trading_day FROM trading_calendar WHERE trade_date = #{date}")
    LocalDate getPrevTradingDay(@Param("date") LocalDate date);
    
    /**
     * 获取下一交易日
     */
    @Select("SELECT next_trading_day FROM trading_calendar WHERE trade_date = #{date}")
    LocalDate getNextTradingDay(@Param("date") LocalDate date);
    
    /**
     * 获取最近一个交易日
     */
    @Select("SELECT MAX(trade_date) FROM trading_calendar WHERE trade_date \u003c= #{date} AND is_trading_day = 1")
    LocalDate getNearestTradingDay(@Param("date") LocalDate date);
}
