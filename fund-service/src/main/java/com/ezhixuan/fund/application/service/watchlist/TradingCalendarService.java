package com.ezhixuan.fund.application.service.watchlist;

import com.ezhixuan.fund.domain.entity.watchlist.TradingCalendar;

import java.time.LocalDate;

/**
 * 交易日历服务
 */
public interface TradingCalendarService {
    
    /**
     * 判断指定日期是否为交易日
     */
    boolean isTradingDay(LocalDate date);
    
    /**
     * 判断今天是否为交易日
     */
    boolean isTradingDay();
    
    /**
     * 判断当前是否为交易时间
     * 交易时间: 9:30-11:30, 13:00-15:00
     */
    boolean isTradingTime();
    
    /**
     * 获取上一交易日
     */
    LocalDate getPrevTradingDay(LocalDate date);
    
    /**
     * 获取上一交易日（基于今天）
     */
    LocalDate getPrevTradingDay();
    
    /**
     * 获取下一交易日
     */
    LocalDate getNextTradingDay(LocalDate date);
    
    /**
     * 获取下一交易日（基于今天）
     */
    LocalDate getNextTradingDay();
    
    /**
     * 获取最近的交易日（如果今天是交易日则返回今天，否则返回上一交易日）
     */
    LocalDate getNearestTradingDay(LocalDate date);
    
    /**
     * 获取当前交易日
     */
    LocalDate getCurrentTradeDate();
    
    /**
     * 获取交易日历信息
     */
    TradingCalendar getTradingCalendar(LocalDate date);
}
