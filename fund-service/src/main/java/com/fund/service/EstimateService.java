package com.fund.service;

import com.fund.dto.FundEstimateDTO;
import com.fund.dto.FundEstimateVO;
import com.fund.entity.FundInfo;
import com.fund.mapper.FundInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 基金估值服务
 * 从Python采集服务获取实时估值
 */
@Service
public class EstimateService {
    
    private static final Logger log = LoggerFactory.getLogger(EstimateService.class);
    
    private final FundInfoMapper fundInfoMapper;
    private final CollectClient collectClient;
    
    public EstimateService(FundInfoMapper fundInfoMapper, CollectClient collectClient) {
        this.fundInfoMapper = fundInfoMapper;
        this.collectClient = collectClient;
    }
    
    /**
     * 获取基金实时估值
     * 从Python采集服务获取真实实时数据
     * 缓存2分钟
     */
    @Cacheable(value = "fund:estimate", key = "#fundCode", unless = "#result == null")
    public FundEstimateVO getEstimate(String fundCode) {
        log.debug("获取基金实时估值: {}", fundCode);
        
        // 获取基金信息
        FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
        if (fundInfo == null) {
            log.warn("基金不存在: {}", fundCode);
            return null;
        }
        
        // 调用Python采集服务获取实时估值
        FundEstimateDTO dto = collectClient.collectEstimate(fundCode);
        
        if (dto == null) {
            log.warn("无法获取实时估值: {}", fundCode);
            return null;
        }
        
        // 转换为VO
        FundEstimateVO estimate = new FundEstimateVO();
        estimate.setFundCode(fundCode);
        estimate.setFundName(fundInfo.getFundName());
        estimate.setPreviousNav(dto.getPreCloseNav());
        estimate.setEstimateNav(dto.getEstimateNav());
        estimate.setDailyChange(dto.getEstimateChangePct());
        estimate.setUpdateTime(LocalDateTime.now());
        estimate.setMarketOpen(isMarketOpen());
        estimate.setEstimateTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        
        log.info("获取实时估值成功: {} - {} - {}%", fundCode, dto.getDataSource(), dto.getEstimateChangePct());
        
        return estimate;
    }
    
    /**
     * 刷新估值（清除缓存）
     */
    @CacheEvict(value = "fund:estimate", key = "#fundCode")
    public void refreshEstimate(String fundCode) {
        log.info("刷新基金估值: {}", fundCode);
    }
    
    /**
     * 判断当前是否交易中
     * 工作日 9:30-15:00
     */
    public boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now();
        int dayOfWeek = now.getDayOfWeek().getValue();
        
        // 周六日休市
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return false;
        }
        
        LocalTime time = now.toLocalTime();
        // 9:30-15:00 开盘
        return !time.isBefore(LocalTime.of(9, 30)) && !time.isAfter(LocalTime.of(15, 0));
    }
}
