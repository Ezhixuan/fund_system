package com.fund.controller.watchlist;

import com.fund.dto.ApiResponse;
import com.fund.entity.watchlist.UserWatchlist;
import com.fund.service.watchlist.WatchlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {
    
    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);
    
    private final WatchlistService watchlistService;
    
    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }
    
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
    
    @PutMapping("/{fundCode}")
    public ApiResponse<UserWatchlist> updateWatchlist(
            @PathVariable String fundCode,
            @RequestBody WatchlistUpdateRequest request) {
        log.info("更新关注基金: {}", fundCode);
        UserWatchlist existing = watchlistService.lambdaQuery()
                .eq(UserWatchlist::getFundCode, fundCode)
                .eq(UserWatchlist::getIsActive, 1)
                .one();
        
        if (existing == null) {
            return ApiResponse.error("基金不在关注列表中");
        }
        
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
    
    @GetMapping("/{fundCode}/check")
    public ApiResponse<Boolean> isWatched(@PathVariable String fundCode) {
        boolean watched = watchlistService.isFundWatched(fundCode);
        return ApiResponse.success(watched);
    }
    
    @PostMapping("/import-from-portfolio")
    public ApiResponse<ImportResult> importFromPortfolio() {
        log.info("从持仓导入关注基金...");
        int count = watchlistService.importFromPortfolio();
        return ApiResponse.success(new ImportResult(count));
    }
    
    @GetMapping("/codes")
    public ApiResponse<List<String>> getWatchedFundCodes() {
        List<String> codes = watchlistService.getWatchedFundCodes();
        return ApiResponse.success(codes);
    }
    
    public static class WatchlistAddRequest {
        private String fundCode;
        private String fundName;
        private Integer watchType;
        public String getFundCode() { return fundCode; }
        public void setFundCode(String fundCode) { this.fundCode = fundCode; }
        public String getFundName() { return fundName; }
        public void setFundName(String fundName) { this.fundName = fundName; }
        public Integer getWatchType() { return watchType; }
        public void setWatchType(Integer watchType) { this.watchType = watchType; }
    }
    
    public static class WatchlistUpdateRequest {
        private Integer watchType;
        private java.math.BigDecimal targetReturn;
        private java.math.BigDecimal stopLoss;
        private String notes;
        private Integer sortOrder;
        public Integer getWatchType() { return watchType; }
        public void setWatchType(Integer watchType) { this.watchType = watchType; }
        public java.math.BigDecimal getTargetReturn() { return targetReturn; }
        public void setTargetReturn(java.math.BigDecimal targetReturn) { this.targetReturn = targetReturn; }
        public java.math.BigDecimal getStopLoss() { return stopLoss; }
        public void setStopLoss(java.math.BigDecimal stopLoss) { this.stopLoss = stopLoss; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
    
    public static class ImportResult {
        private int importedCount;
        public ImportResult(int importedCount) { this.importedCount = importedCount; }
        public int getImportedCount() { return importedCount; }
        public void setImportedCount(int importedCount) { this.importedCount = importedCount; }
    }
}
