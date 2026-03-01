package com.fund.service.watchlist.impl;

import com.fund.entity.watchlist.TradingCalendar;
import com.fund.mapper.watchlist.TradingCalendarMapper;
import com.fund.service.watchlist.TradingCalendarService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class TradingCalendarServiceImpl implements TradingCalendarService {
    
    private final TradingCalendarMapper calendarMapper;
    
    public TradingCalendarServiceImpl(TradingCalendarMapper calendarMapper) {
        this.calendarMapper = calendarMapper;
    }
    
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
        if (!isTradingDay()) {
            return false;
        }
        
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
        return getPrevTradingDay(today);
    }
    
    @Override
    public TradingCalendar getTradingCalendar(LocalDate date) {
        return calendarMapper.selectById(date);
    }
}
