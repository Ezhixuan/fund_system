package com.fund.service.watchlist;

import com.fund.entity.watchlist.TradingCalendar;
import java.time.LocalDate;

public interface TradingCalendarService {
    boolean isTradingDay(LocalDate date);
    boolean isTradingDay();
    boolean isTradingTime();
    LocalDate getPrevTradingDay(LocalDate date);
    LocalDate getPrevTradingDay();
    LocalDate getNextTradingDay(LocalDate date);
    LocalDate getNextTradingDay();
    LocalDate getNearestTradingDay(LocalDate date);
    LocalDate getCurrentTradeDate();
    TradingCalendar getTradingCalendar(LocalDate date);
}
