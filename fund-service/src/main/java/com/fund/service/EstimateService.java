package com.fund.service;

import com.fund.dto.FundEstimateVO;
import com.fund.entity.FundInfo;
import com.fund.entity.FundNav;
import com.fund.mapper.FundInfoMapper;
import com.fund.mapper.FundNavMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 基金估值服务
 */
@Service
public class EstimateService {
    
    private static final Logger log = LoggerFactory.getLogger(EstimateService.class);
    
    private final FundInfoMapper fundInfoMapper;
    private final FundNavMapper fundNavMapper;
    
    public EstimateService(FundInfoMapper fundInfoMapper, FundNavMapper fundNavMapper) {
        this.fundInfoMapper = fundInfoMapper;
        this.fundNavMapper = fundNavMapper;
    }
    
    /**
     * 获取基金实时估值
     * 缓存5分钟
     */
    @Cacheable(value = "fund:estimate", key = "#fundCode", unless = "#result == null")
    public FundEstimateVO getEstimate(String fundCode) {
        log.debug("获取基金估值: {}", fundCode);
        
        // 获取基金信息
        FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
        if (fundInfo == null) {
            return null;
        }
        
        // 获取最新净值（作为昨日净值参考）
        FundNav latestNav = fundNavMapper.selectLatestNav(fundCode);
        if (latestNav == null) {
            return null;
        }
        
        // 获取前一交易日净值
        FundNav previousNav = fundNavMapper.selectPreviousNav(fundCode, latestNav.getNavDate());
        
        FundEstimateVO estimate = new FundEstimateVO();
        estimate.setFundCode(fundCode);
        estimate.setFundName(fundInfo.getFundName());
        estimate.setPreviousNav(previousNav != null ? previousNav.getUnitNav() : latestNav.getUnitNav());
        estimate.setEstimateNav(latestNav.getUnitNav()); // 简化处理，使用最新净值
        estimate.setUpdateTime(LocalDateTime.now());
        estimate.setMarketOpen(isMarketOpen());
        
        // 计算涨跌幅
        if (previousNav != null && previousNav.getUnitNav().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = latestNav.getUnitNav()
                    .subtract(previousNav.getUnitNav())
                    .divide(previousNav.getUnitNav(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            estimate.setDailyChange(change);
        } else {
            estimate.setDailyChange(BigDecimal.ZERO);
        }
        
        estimate.setEstimateTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        
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
