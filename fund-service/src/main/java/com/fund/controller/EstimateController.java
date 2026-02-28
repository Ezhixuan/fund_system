package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.dto.FundEstimateVO;
import com.fund.service.EstimateService;
import org.springframework.web.bind.annotation.*;

/**
 * 基金估值控制器
 */
@RestController
@RequestMapping("/api/funds")
public class EstimateController {
    
    private final EstimateService estimateService;
    
    public EstimateController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }
    
    /**
     * 获取基金实时估值
     */
    @GetMapping("/{fundCode}/estimate")
    public ApiResponse<FundEstimateVO> getEstimate(@PathVariable String fundCode) {
        FundEstimateVO estimate = estimateService.getEstimate(fundCode);
        if (estimate == null) {
            return ApiResponse.notFound("估值数据不存在");
        }
        return ApiResponse.success(estimate);
    }
    
    /**
     * 刷新估值（清除缓存）
     * 限流：每分钟1次
     */
    @PostMapping("/{fundCode}/estimate/refresh")
    public ApiResponse<FundEstimateVO> refreshEstimate(@PathVariable String fundCode) {
        // TODO: 添加限流控制
        estimateService.refreshEstimate(fundCode);
        FundEstimateVO estimate = estimateService.getEstimate(fundCode);
        return ApiResponse.success(estimate);
    }
}
