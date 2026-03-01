package com.ezhixuan.fund.interfaces.rest.watchlist;

import com.ezhixuan.fund.application.service.watchlist.TradingCalendarService;
import com.ezhixuan.fund.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 交易日历控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/trading-calendar")
@RequiredArgsConstructor
public class TradingCalendarController {
    
    private final TradingCalendarService calendarService;
    
    /**
     * 检查今天是否为交易日
     */
    @GetMapping("/today/is-trading-day")
    public ApiResponse<Boolean> isTradingDay() {
        return ApiResponse.success(calendarService.isTradingDay());
    }
    
    /**
     * 检查指定日期是否为交易日
     */
    @GetMapping("/is-trading-day")
    public ApiResponse<Boolean> isTradingDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(calendarService.isTradingDay(date));
    }
    
    /**
     * 检查当前是否为交易时间
     */
    @GetMapping("/is-trading-time")
    public ApiResponse<Boolean> isTradingTime() {
        return ApiResponse.success(calendarService.isTradingTime());
    }
    
    /**
     * 获取上一交易日
     */
    @GetMapping("/prev-trading-day")
    public ApiResponse<LocalDate> getPrevTradingDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate result = date != null 
            ? calendarService.getPrevTradingDay(date)
            : calendarService.getPrevTradingDay();
        return ApiResponse.success(result);
    }
    
    /**
     * 获取下一交易日
     */
    @GetMapping("/next-trading-day")
    public ApiResponse<LocalDate> getNextTradingDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate result = date != null
            ? calendarService.getNextTradingDay(date)
            : calendarService.getNextTradingDay();
        return ApiResponse.success(result);
    }
    
    /**
     * 获取当前交易日
     */
    @GetMapping("/current-trade-date")
    public ApiResponse<LocalDate> getCurrentTradeDate() {
        return ApiResponse.success(calendarService.getCurrentTradeDate());
    }
    
    /**
     * 获取交易状态概览
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getTradingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isTradingDay", calendarService.isTradingDay());
        status.put("isTradingTime", calendarService.isTradingTime());
        status.put("currentTradeDate", calendarService.getCurrentTradeDate());
        status.put("prevTradingDay", calendarService.getPrevTradingDay());
        status.put("nextTradingDay", calendarService.getNextTradingDay());
        return ApiResponse.success(status);
    }
}
