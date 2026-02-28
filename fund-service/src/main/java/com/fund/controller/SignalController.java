package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.dto.TradeSignal;
import com.fund.service.SignalEngine;
import org.springframework.web.bind.annotation.*;

/**
 * 交易信号控制器
 */
@RestController
@RequestMapping("/api/funds")
public class SignalController {
    
    private final SignalEngine signalEngine;
    
    public SignalController(SignalEngine signalEngine) {
        this.signalEngine = signalEngine;
    }
    
    /**
     * 获取基金交易信号
     */
    @GetMapping("/{fundCode}/signal")
    public ApiResponse<TradeSignal> getSignal(@PathVariable String fundCode) {
        TradeSignal signal = signalEngine.generateSignal(fundCode);
        return ApiResponse.success(signal);
    }
}
