package com.fund.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.dto.ApiResponse;
import com.fund.dto.FundInfoVO;
import com.fund.dto.FundMetricsVO;
import com.fund.dto.FundNavVO;
import com.fund.service.FundService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 基金控制器
 */
@RestController
@RequestMapping("/api/funds")
public class FundController {
    
    private final FundService fundService;
    
    public FundController(FundService fundService) {
        this.fundService = fundService;
    }
    
    /**
     * 分页查询基金列表
     */
    @GetMapping
    public ApiResponse<IPage<FundInfoVO>> listFunds(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String fundType,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(fundService.listFunds(page, size, fundType, riskLevel, keyword));
    }
    
    /**
     * 获取基金详情
     */
    @GetMapping("/{fundCode}")
    public ApiResponse<FundInfoVO> getFundDetail(@PathVariable String fundCode) {
        FundInfoVO detail = fundService.getFundDetail(fundCode);
        if (detail == null) {
            return ApiResponse.notFound("基金不存在");
        }
        return ApiResponse.success(detail);
    }
    
    /**
     * 搜索建议
     */
    @GetMapping("/search/suggest")
    public ApiResponse<List<FundInfoVO>> searchSuggest(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(fundService.searchSuggest(keyword, limit));
    }
    
    /**
     * 获取基金最新指标
     */
    @GetMapping("/{fundCode}/metrics")
    public ApiResponse<FundMetricsVO> getMetrics(@PathVariable String fundCode) {
        FundMetricsVO metrics = fundService.getLatestMetrics(fundCode);
        if (metrics == null) {
            return ApiResponse.notFound("指标数据不存在");
        }
        return ApiResponse.success(metrics);
    }
    
    /**
     * 获取净值历史
     */
    @GetMapping("/{fundCode}/nav")
    public ApiResponse<List<FundNavVO>> getNavHistory(
            @PathVariable String fundCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(fundService.getNavHistory(fundCode, startDate, endDate));
    }
    
    /**
     * 获取近期净值
     */
    @GetMapping("/{fundCode}/nav/recent")
    public ApiResponse<List<FundNavVO>> getRecentNav(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "30") Integer days) {
        return ApiResponse.success(fundService.getRecentNav(fundCode, days));
    }

    /**
     * TOP基金排名
     */
    @GetMapping("/top")
    public ApiResponse<List<FundMetricsVO>> getTopFunds(
            @RequestParam(defaultValue = "sharpe") String sortBy,
            @RequestParam(required = false) String fundType,
            @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(fundService.getTopFunds(sortBy, fundType, limit));
    }

    /**
     * 基金指标对比
     */
    @GetMapping("/compare")
    public ApiResponse<List<FundMetricsVO>> compareFunds(
            @RequestParam String codes) {
        if (codes == null || codes.isEmpty()) {
            return ApiResponse.badRequest("codes参数不能为空");
        }
        List<String> codeList = List.of(codes.split(","));
        if (codeList.size() > 5) {
            return ApiResponse.badRequest("最多对比5只基金");
        }
        return ApiResponse.success(fundService.compareFunds(codeList));
    }

    /**
     * 按指标筛选基金
     */
    @GetMapping("/filter")
    public ApiResponse<IPage<FundInfoVO>> filterFunds(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String fundType,
            @RequestParam(required = false) Double minSharpe,
            @RequestParam(required = false) Double maxDrawdown) {
        Page<FundInfoVO> pageParam = new Page<>(page, size);
        return ApiResponse.success(fundService.filterFundsByMetrics(pageParam, fundType, minSharpe, maxDrawdown));
    }
}
