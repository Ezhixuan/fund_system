package com.fund.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.watchlist.TradingCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;

@Mapper
public interface TradingCalendarMapper extends BaseMapper<TradingCalendar> {
    @Select("SELECT is_trading_day FROM trading_calendar WHERE trade_date = #{date}")
    Integer isTradingDay(@Param("date") LocalDate date);
    
    @Select("SELECT prev_trading_day FROM trading_calendar WHERE trade_date = #{date}")
    LocalDate getPrevTradingDay(@Param("date") LocalDate date);
    
    @Select("SELECT next_trading_day FROM trading_calendar WHERE trade_date = #{date}")
    LocalDate getNextTradingDay(@Param("date") LocalDate date);
    
    @Select("SELECT MAX(trade_date) FROM trading_calendar WHERE trade_date <= #{date} AND is_trading_day = 1")
    LocalDate getNearestTradingDay(@Param("date") LocalDate date);
}
