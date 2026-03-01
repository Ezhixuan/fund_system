package com.ezhixuan.fund.application.service.watchlist.impl;

import com.ezhixuan.fund.application.service.watchlist.TradingCalendarService;
import com.ezhixuan.fund.domain.entity.watchlist.TradingCalendar;
import com.ezhixuan.fund.infrastructure.persistence.mapper.watchlist.TradingCalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 交易日历服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingCalendarServiceImpl implements TradingCalendarService {
    
    private final TradingCalendarMapper calendarMapper;
    
    @Override
    public boolean isTradingDay(LocalDate date) {
        Integer result = calendarMapper.isTradingDay(date);
        return result != null && result == 1;
    }
    
    @Override
    public boolean isTradingDay() {
        return isTradingDay(LocalDate.now());
    }
    
    @Override
    public boolean isTradingTime() {
        // 首先判断是否为交易日
        if (!isTradingDay()) {
            return false;
        }
        
        // 判断是否在交易时段
        LocalTime now = LocalTime.now();
        LocalTime morningStart = LocalTime.of(9, 30);
        LocalTime morningEnd = LocalTime.of(11, 30);
        LocalTime afternoonStart = LocalTime.of(13, 0);
        LocalTime afternoonEnd = LocalTime.of(15, 0);
        
        boolean morningSession = !now.isBefore(morningStart) && !now.isAfter(morningEnd);
        boolean afternoonSession = !now.isBefore(afternoonStart) && !now.isAfter(afternoonEnd);
        
        return morningSession || afternoonSession;
    }
    
    @Override
    public LocalDate getPrevTradingDay(LocalDate date) {
        return calendarMapper.getPrevTradingDay(date);
    }
    
    @Override
    public LocalDate getPrevTradingDay() {
        return getPrevTradingDay(LocalDate.now());
    }
    
    @Override
    public LocalDate getNextTradingDay(LocalDate date) {
        return calendarMapper.getNextTradingDay(date);
    }
    
    @Override
    public LocalDate getNextTradingDay() {
        return getNextTradingDay(LocalDate.now());
    }
    
    @Override
    public LocalDate getNearestTradingDay(LocalDate date) {
        return calendarMapper.getNearestTradingDay(date);
    }
    
    @Override
    public LocalDate getCurrentTradeDate() {
        LocalDate today = LocalDate.now();
        if (isTradingDay(today)) {
            return today;
        }
        // 如果今天不是交易日，返回上一交易日
        return getPrevTradingDay(today);
    }
    
    @Override
    public TradingCalendar getTradingCalendar(LocalDate date) {
        return calendarMapper.selectById(date);
    }
}
