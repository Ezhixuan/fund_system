package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.dto.HoldingVO;
import com.fund.dto.PortfolioAnalysis;
import com.fund.dto.TradeRequest;
import com.fund.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 持仓控制器
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    
    private final PortfolioService portfolioService;
    
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }
    
    /**
     * 记录交易
     */
    @PostMapping("/trade")
    public ApiResponse<Void> recordTrade(@RequestBody TradeRequest request) {
        portfolioService.recordTrade(request);
        return ApiResponse.success(null);
    }
    
    /**
     * 获取当前持仓
     */
    @GetMapping("/holdings")
    public ApiResponse<List<HoldingVO>> getHoldings() {
        return ApiResponse.success(portfolioService.getHoldings());
    }
    
    /**
     * 获取单个基金持仓
     */
    @GetMapping("/holding/{fundCode}")
    public ApiResponse<HoldingVO> getHolding(@PathVariable String fundCode) {
        return ApiResponse.success(portfolioService.calculateHolding(fundCode));
    }
    
    /**
     * 组合分析
     */
    @GetMapping("/analysis")
    public ApiResponse<PortfolioAnalysis> analyze() {
        return ApiResponse.success(portfolioService.analyze());
    }
}
