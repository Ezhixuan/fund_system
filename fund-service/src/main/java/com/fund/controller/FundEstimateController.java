package com.fund.controller;

import com.fund.dto.ApiResponse;
import com.fund.entity.watchlist.FundEstimateIntraday;
import com.fund.mapper.watchlist.FundEstimateIntradayMapper;
import com.fund.service.collect.CollectClient;
import com.fund.service.watchlist.TradingCalendarService;
import com.fund.service.websocket.IntradayPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/fund")
public class FundEstimateController {
    
    private static final Logger log = LoggerFactory.getLogger(FundEstimateController.class);
    
    @Autowired
    private FundEstimateIntradayMapper estimateMapper;
    
    @Autowired
    private TradingCalendarService calendarService;
    
    @Autowired
    private CollectClient collectClient;
    
    @Autowired
    private IntradayPushService pushService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 获取基金当日分时数据
     */
    @GetMapping("/{fundCode}/intraday")
    public ApiResponse<Map<String, Object>> getIntradayData(
            @PathVariable String fundCode) {
        
        // 获取当前交易日
        LocalDate tradeDate = calendarService.getCurrentTradeDate();
        
        // 查询该交易日的所有点位数据
        List<FundEstimateIntraday> points = estimateMapper.selectByFundAndDate(fundCode, tradeDate);
        
        // 如果没有数据，可能是交易刚开始，返回空列表
        Map<String, Object> result = new HashMap<>();
        result.put("fundCode", fundCode);
        result.put("tradeDate", tradeDate.toString());
        result.put("points", points);
        result.put("isToday", tradeDate.equals(LocalDate.now()));
        
        return ApiResponse.success(result);
    }
    
    /**
     * 手动刷新估值
     */
    @PostMapping("/{fundCode}/estimate/refresh")
    public ApiResponse<String> refreshEstimate(@PathVariable String fundCode) {
        // 1. 检查是否在交易时间
        if (!calendarService.isTradingTime()) {
            return ApiResponse.error("当前非交易时间，无法刷新");
        }
        
        // 2. 检查冷却时间（30秒）
        String cooldownKey = "refresh:cooldown:" + fundCode;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long ttl = redisTemplate.getExpire(cooldownKey);
            return ApiResponse.error("请" + ttl + "秒后再刷新");
        }
        
        // 3. 设置冷却时间
        redisTemplate.opsForValue().set(cooldownKey, "1", 30, TimeUnit.SECONDS);
        
        // 4. 异步调用Python采集服务
        CompletableFuture.runAsync(() -> {
            try {
                // 调用Python采集
                var response = collectClient.collectEstimate(fundCode);
                
                if (response.getCode() == 200 && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    
                    // 转换为实体
                    FundEstimateIntraday estimate = new FundEstimateIntraday();
                    estimate.setFundCode(fundCode);
                    estimate.setEstimateTime(LocalDateTime.now());
                    estimate.setEstimateNav(new BigDecimal(data.get("estimateNav").toString()));
                    estimate.setEstimateChangePct(new BigDecimal(data.get("estimateChangePct").toString()));
                    estimate.setTradeDate(LocalDate.now());
                    estimate.setDataSource(data.get("dataSource").toString());
                    
                    // 保存到数据库
                    estimateMapper.insert(estimate);
                    
                    // WebSocket推送更新
                    pushService.pushToFundDetail(estimate);
                    
                    log.info("手动刷新估值成功: {} - {}", fundCode, estimate.getEstimateNav());
                }
            } catch (Exception e) {
                log.error("手动刷新估值失败: {}", fundCode, e);
            }
        });
        
        return ApiResponse.success("刷新请求已提交，请稍后查看");
    }
    
    /**
     * 获取最新估值
     */
    @GetMapping("/{fundCode}/estimate/latest")
    public ApiResponse<FundEstimateIntraday> getLatestEstimate(@PathVariable String fundCode) {
        FundEstimateIntraday estimate = estimateMapper.selectLatest(fundCode);
        return ApiResponse.success(estimate);
    }
}
