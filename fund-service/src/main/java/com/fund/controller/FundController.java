package com.fund.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.dto.ApiResponse;
import com.fund.dto.FundInfoVO;
import com.fund.dto.FundMetricsVO;
import com.fund.dto.FundNavVO;
import com.fund.service.CollectClient;
import com.fund.service.FundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 基金控制器
 */
@Tag(name = "基金接口", description = "基金查询相关接口")
@RestController
@RequestMapping("/api/funds")
public class FundController {
    private static final Logger log = LoggerFactory.getLogger(FundController.class);
    
    private final FundService fundService;
    private final CollectClient collectClient;
    
    public FundController(FundService fundService, CollectClient collectClient) {
        this.fundService = fundService;
        this.collectClient = collectClient;
    }
    
    /**
     * 分页查询基金列表
     */
    @Operation(summary = "分页查询基金列表", description = "支持分页、筛选和搜索")
    @GetMapping
    public ApiResponse<IPage<FundInfoVO>> listFunds(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "基金类型") @RequestParam(required = false) String fundType,
            @Parameter(description = "风险等级") @RequestParam(required = false) Integer riskLevel,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(fundService.listFunds(page, size, fundType, riskLevel, keyword));
    }
    
    /**
     * 获取基金详情（自动触发采集）
     */
    @Operation(summary = "获取基金详情", description = "根据基金代码获取详细信息，如不存在则自动触发采集")
    @GetMapping("/{fundCode}")
    public ApiResponse<FundInfoVO> getFundDetail(
            @Parameter(description = "基金代码", example = "000001") @PathVariable String fundCode) {
        FundInfoVO detail = fundService.getFundDetail(fundCode);
        if (detail == null) {
            // 自动触发采集
            log.info("基金 {} 不存在，触发自动采集", fundCode);
            boolean collected = collectClient.collectFundData(fundCode);
            if (collected) {
                // 重新查询
                detail = fundService.getFundDetail(fundCode);
                if (detail != null) {
                    return ApiResponse.success(detail);
                }
            }
            return ApiResponse.notFound("基金不存在，自动采集失败");
        }
        return ApiResponse.success(detail);
    }
    
    /**
     * 搜索建议
     */
    @Operation(summary = "搜索建议", description = "根据关键词返回搜索建议（支持拼音）")
    @GetMapping("/search/suggest")
    public ApiResponse<List<FundInfoVO>> searchSuggest(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "返回数量限制", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(fundService.searchSuggest(keyword, limit));
    }
    
    /**
     * 获取基金最新指标
     */
    @Operation(summary = "获取基金最新指标", description = "获取指定基金的最新指标数据")
    @GetMapping("/{fundCode}/metrics")
    public ApiResponse<FundMetricsVO> getMetrics(
            @Parameter(description = "基金代码", example = "000001") @PathVariable String fundCode) {
        FundMetricsVO metrics = fundService.getLatestMetrics(fundCode);
        if (metrics == null) {
            return ApiResponse.notFound("指标数据不存在");
        }
        return ApiResponse.success(metrics);
    }
    
    /**
     * 获取净值历史
     */
    @Operation(summary = "获取净值历史", description = "获取指定日期范围内的净值数据")
    @GetMapping("/{fundCode}/nav")
    public ApiResponse<List<FundNavVO>> getNavHistory(
            @Parameter(description = "基金代码", example = "000001") @PathVariable String fundCode,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(fundService.getNavHistory(fundCode, startDate, endDate));
    }
    
    /**
     * 获取近期净值
     */
    @Operation(summary = "获取近期净值", description = "获取最近N天的净值数据")
    @GetMapping("/{fundCode}/nav/recent")
    public ApiResponse<List<FundNavVO>> getRecentNav(
            @Parameter(description = "基金代码", example = "000001") @PathVariable String fundCode,
            @Parameter(description = "天数", example = "30") @RequestParam(defaultValue = "30") Integer days) {
        return ApiResponse.success(fundService.getRecentNav(fundCode, days));
    }

    /**
     * TOP基金排名
     */
    @Operation(summary = "TOP基金排名", description = "按指定指标获取排名靠前的基金")
    @GetMapping("/top")
    public ApiResponse<List<FundMetricsVO>> getTopFunds(
            @Parameter(description = "排序字段", example = "sharpe") @RequestParam(defaultValue = "sharpe") String sortBy,
            @Parameter(description = "基金类型") @RequestParam(required = false) String fundType,
            @Parameter(description = "数量限制", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(fundService.getTopFunds(sortBy, fundType, limit));
    }

    /**
     * 基金指标对比
     */
    @Operation(summary = "基金指标对比", description = "对比多只基金的指标数据（最多5只）")
    @GetMapping("/compare")
    public ApiResponse<List<FundMetricsVO>> compareFunds(
            @Parameter(description = "基金代码列表，逗号分隔", example = "000001,000002") @RequestParam String codes) {
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
    @Operation(summary = "按指标筛选基金", description = "根据夏普比率、回撤等指标筛选基金")
    @GetMapping("/filter")
    public ApiResponse<IPage<FundInfoVO>> filterFunds(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "基金类型") @RequestParam(required = false) String fundType,
            @Parameter(description = "最小夏普比率") @RequestParam(required = false) Double minSharpe,
            @Parameter(description = "最大回撤") @RequestParam(required = false) Double maxDrawdown) {
        Page<FundInfoVO> pageParam = new Page<>(page, size);
        return ApiResponse.success(fundService.filterFundsByMetrics(pageParam, fundType, minSharpe, maxDrawdown));
    }
}
