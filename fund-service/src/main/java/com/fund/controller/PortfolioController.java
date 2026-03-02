package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.dto.HoldingUpdateRequest;
import com.fund.dto.HoldingVO;
import com.fund.dto.PortfolioAnalysis;
import com.fund.dto.PortfolioSummaryVO;
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
        try {
            portfolioService.recordTrade(request);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
    
    /**
     * 获取当前持仓
     */
    @GetMapping("/holdings")
    public ApiResponse<List<HoldingVO>> getHoldings() {
        return ApiResponse.success(portfolioService.getHoldings());
    }
    
    /**
     * 更新持仓
     */
    @PutMapping("/holdings/{fundCode}")
    public ApiResponse<Void> updateHolding(
            @PathVariable String fundCode,
            @RequestBody HoldingUpdateRequest request) {
        try {
            portfolioService.updateHolding(fundCode, request);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
    
    /**
     * 删除持仓（清仓）
     */
    @DeleteMapping("/holdings/{fundCode}")
    public ApiResponse<Void> deleteHolding(@PathVariable String fundCode) {
        try {
            portfolioService.deleteHolding(fundCode);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
    
    /**
     * 组合分析
     */
    @GetMapping("/analysis")
    public ApiResponse<PortfolioAnalysis> analyze() {
        return ApiResponse.success(portfolioService.analyze());
    }
    /**
     * 获取持仓带实时估值
     */
    @GetMapping("/holdings-with-estimate")
    public ApiResponse<PortfolioSummaryVO> getHoldingsWithEstimate() {
        return ApiResponse.success(portfolioService.getHoldingsWithEstimate());
    }
}
