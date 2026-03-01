package com.fund.controller.watchlist;

import com.fund.dto.ApiResponse;
import com.fund.service.watchlist.TradingCalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trading-calendar")
public class TradingCalendarController {
    
    private final TradingCalendarService calendarService;
    
    public TradingCalendarController(TradingCalendarService calendarService) {
        this.calendarService = calendarService;
    }
    
    @GetMapping("/today/is-trading-day")
    public ApiResponse<Boolean> isTradingDay() {
        return ApiResponse.success(calendarService.isTradingDay());
    }
    
    @GetMapping("/is-trading-day")
    public ApiResponse<Boolean> isTradingDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(calendarService.isTradingDay(date));
    }
    
    @GetMapping("/is-trading-time")
    public ApiResponse<Boolean> isTradingTime() {
        return ApiResponse.success(calendarService.isTradingTime());
    }
    
    @GetMapping("/prev-trading-day")
    public ApiResponse<LocalDate> getPrevTradingDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate result = date != null 
            ? calendarService.getPrevTradingDay(date)
            : calendarService.getPrevTradingDay();
        return ApiResponse.success(result);
    }
    
    @GetMapping("/next-trading-day")
    public ApiResponse<LocalDate> getNextTradingDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate result = date != null
            ? calendarService.getNextTradingDay(date)
            : calendarService.getNextTradingDay();
        return ApiResponse.success(result);
    }
    
    @GetMapping("/current-trade-date")
    public ApiResponse<LocalDate> getCurrentTradeDate() {
        return ApiResponse.success(calendarService.getCurrentTradeDate());
    }
    
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
