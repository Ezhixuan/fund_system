package com.ezhixuan.fund.interfaces.rest.watchlist;

import com.ezhixuan.fund.application.service.watchlist.WatchlistService;
import com.ezhixuan.fund.common.response.ApiResponse;
import com.ezhixuan.fund.domain.entity.watchlist.UserWatchlist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注列表控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {
    
    private final WatchlistService watchlistService;
    
    /**
     * 添加关注基金
     */
    @PostMapping("/add")
    public ApiResponse<UserWatchlist> addWatchlist(@RequestBody WatchlistAddRequest request) {
        log.info("添加关注基金: {}", request.getFundCode());
        
        UserWatchlist watchlist = watchlistService.addWatchlist(
            request.getFundCode(),
            request.getFundName(),
            request.getWatchType()
        );
        
        return ApiResponse.success(watchlist);
    }
    
    /**
     * 获取关注列表
     */
    @GetMapping("/list")
    public ApiResponse<List<UserWatchlist>> getWatchlist(
            @RequestParam(required = false) Integer type) {
        
        List<UserWatchlist> list;
        if (type != null) {
            list = watchlistService.getWatchlistByType(type);
        } else {
            list = watchlistService.getAllWatchlist();
        }
        
        return ApiResponse.success(list);
    }
    
    /**
     * 更新关注信息
     */
    @PutMapping("/{fundCode}")
    public ApiResponse<UserWatchlist> updateWatchlist(
            @PathVariable String fundCode,
            @RequestBody WatchlistUpdateRequest request) {
        
        log.info("更新关注基金: {}", fundCode);
        
        // 查询现有记录
        UserWatchlist existing = watchlistService.lambdaQuery()
                .eq(UserWatchlist::getFundCode, fundCode)
                .eq(UserWatchlist::getIsActive, 1)
                .one();
        
        if (existing == null) {
            return ApiResponse.error("基金不在关注列表中");
        }
        
        // 更新字段
        if (request.getWatchType() != null) {
            existing.setWatchType(request.getWatchType());
        }
        if (request.getTargetReturn() != null) {
            existing.setTargetReturn(request.getTargetReturn());
        }
        if (request.getStopLoss() != null) {
            existing.setStopLoss(request.getStopLoss());
        }
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        
        UserWatchlist updated = watchlistService.updateWatchlist(existing);
        return ApiResponse.success(updated);
    }
    
    /**
     * 移除关注
     */
    @DeleteMapping("/{fundCode}")
    public ApiResponse<String> removeWatchlist(@PathVariable String fundCode) {
        log.info("移除关注基金: {}", fundCode);
        
        boolean success = watchlistService.removeWatchlist(fundCode);
        if (success) {
            return ApiResponse.success("移除成功");
        } else {
            return ApiResponse.error("基金不在关注列表中");
        }
    }
    
    /**
     * 检查基金是否已关注
     */
    @GetMapping("/{fundCode}/check")
    public ApiResponse<Boolean> isWatched(@PathVariable String fundCode) {
        boolean watched = watchlistService.isFundWatched(fundCode);
        return ApiResponse.success(watched);
    }
    
    /**
     * 从持仓导入
     */
    @PostMapping("/import-from-portfolio")
    public ApiResponse<ImportResult> importFromPortfolio() {
        log.info("从持仓导入关注基金...");
        int count = watchlistService.importFromPortfolio();
        return ApiResponse.success(new ImportResult(count));
    }
    
    /**
     * 获取关注基金代码列表
     */
    @GetMapping("/codes")
    public ApiResponse<List<String>> getWatchedFundCodes() {
        List<String> codes = watchlistService.getWatchedFundCodes();
        return ApiResponse.success(codes);
    }
    
    // ==================== DTOs ====================
    
    @lombok.Data
    public static class WatchlistAddRequest {
        private String fundCode;
        private String fundName;
        private Integer watchType; // 1-持有, 2-关注
    }
    
    @lombok.Data
    public static class WatchlistUpdateRequest {
        private Integer watchType;
        private java.math.BigDecimal targetReturn;
        private java.math.BigDecimal stopLoss;
        private String notes;
        private Integer sortOrder;
    }
    
    @lombok.Data
    @AllArgsConstructor
    public static class ImportResult {
        private int importedCount;
    }
}
