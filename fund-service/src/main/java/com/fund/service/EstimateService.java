package com.fund.service;

import com.fund.dto.FundEstimateDTO;
import com.fund.dto.FundEstimateVO;
import com.fund.entity.FundInfo;
import com.fund.mapper.FundInfoMapper;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

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

    @Autowired(required = false)
    private RedissonClient redissonClient;

    public EstimateService(FundInfoMapper fundInfoMapper, CollectClient collectClient) {
        this.fundInfoMapper = fundInfoMapper;
        this.collectClient = collectClient;
    }

    /**
     * 获取基金实时估值（带缓存异常降级）
     * 从Python采集服务获取真实实时数据
     * 缓存2分钟
     */
    public FundEstimateVO getEstimate(String fundCode) {
        log.debug("获取基金实时估值: {}", fundCode);

        // 先尝试从缓存获取（带异常处理）
        try {
            FundEstimateVO cached = getEstimateFromCache(fundCode);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("从缓存获取估值失败，降级到直接查询: {}", e.getMessage());
        }

        // 缓存未命中或异常，直接查询并写入缓存
        return getEstimateFromSource(fundCode);
    }

    /**
     * 从缓存获取估值
     */
    private FundEstimateVO getEstimateFromCache(String fundCode) {
        if (redissonClient == null) {
            return null;
        }
        try {
            RMap<String, FundEstimateVO> map = redissonClient.getMap("fund:estimate");
            return map.get(fundCode);
        } catch (RedisException e) {
            log.error("缓存格式错误，清除缓存后重试: {}", e.getMessage());
            clearCorruptedCache("fund:estimate");
            return null;
        }
    }

    /**
     * 从数据源获取估值并写入缓存
     */
    private FundEstimateVO getEstimateFromSource(String fundCode) {
        FundEstimateVO estimate = doGetEstimate(fundCode);
        if (estimate != null && redissonClient != null) {
            try {
                RMap<String, FundEstimateVO> map = redissonClient.getMap("fund:estimate");
                map.put(fundCode, estimate);
            } catch (Exception e) {
                log.warn("写入缓存失败: {}", e.getMessage());
            }
        }
        return estimate;
    }

    /**
     * 清除损坏的缓存
     */
    private void clearCorruptedCache(String cacheKey) {
        try {
            redissonClient.getKeys().delete(cacheKey);
            redissonClient.getKeys().delete("redisson__timeout__set:{" + cacheKey + "}");
            log.info("已清除损坏的缓存: {}", cacheKey);
        } catch (Exception ex) {
            log.error("清除缓存失败: {}", ex.getMessage());
        }
    }

    /**
     * 实际获取估值逻辑
     */
    private FundEstimateVO doGetEstimate(String fundCode) {
        log.debug("从数据源获取基金实时估值: {}", fundCode);

        // 获取基金信息
        FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
        if (fundInfo == null) {
            /*
             todo Ezhixuan : 如果通过基金代码没有获取到该基金的信息,需要调用 dataCore 服务再核实一遍该基金是否存在
             */
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
