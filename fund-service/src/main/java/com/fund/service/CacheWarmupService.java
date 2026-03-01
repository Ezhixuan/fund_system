package com.fund.service;

import com.fund.config.RedisConfig;
import com.fund.dto.FundInfoVO;
import com.fund.dto.FundMetricsVO;
import com.fund.entity.FundInfo;
import com.fund.mapper.FundInfoMapper;
import com.fund.mapper.FundMetricsMapper;
import com.fund.util.CacheUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存预热服务
 * 功能：系统启动时预热热点数据、定时刷新、布隆过滤器维护
 */
@Service
public class CacheWarmupService implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(CacheWarmupService.class);
    
    // 热点基金数量
    private static final int HOT_FUND_COUNT = 100;
    // 布隆过滤器预期元素数（基金总数）
    private static final long BLOOM_EXPECTED_ELEMENTS = 20000;
    
    private final FundInfoMapper fundInfoMapper;
    private final FundMetricsMapper fundMetricsMapper;
    private final FundService fundService;
    private final CacheUtil cacheUtil;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    
    public CacheWarmupService(FundInfoMapper fundInfoMapper,
                              FundMetricsMapper fundMetricsMapper,
                              FundService fundService,
                              CacheUtil cacheUtil,
                              RedissonClient redissonClient,
                              StringRedisTemplate redisTemplate) {
        this.fundInfoMapper = fundInfoMapper;
        this.fundMetricsMapper = fundMetricsMapper;
        this.fundService = fundService;
        this.cacheUtil = cacheUtil;
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 系统启动时执行缓存预热
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始缓存预热 ==========");
        
        try {
            // 1. 初始化布隆过滤器
            initBloomFilter();
            
            // 2. 预热热点基金数据
            warmupHotFunds();
            
            // 3. 预热TOP排名数据
            warmupTopRankings();
            
            log.info("========== 缓存预热完成 ==========");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
    }
    
    /**
     * 初始化布隆过滤器
     */
    private void initBloomFilter() {
        log.info("初始化布隆过滤器...");
        
        // 获取所有基金代码
        List<String> allFundCodes = fundInfoMapper.selectAllFundCodes();
        
        if (allFundCodes.isEmpty()) {
            log.warn("数据库中没有基金数据，布隆过滤器初始化跳过");
            return;
        }
        
        // 创建布隆过滤器
        RBloomFilter<String> bloomFilter = cacheUtil.getBloomFilter(
            RedisConfig.BLOOM_FILTER_FUND, 
            BLOOM_EXPECTED_ELEMENTS
        );
        
        // 清空并重新加载
        bloomFilter.delete();
        bloomFilter.tryInit(BLOOM_EXPECTED_ELEMENTS, 0.01);
        
        // 批量添加基金代码
        cacheUtil.addAllToBloomFilter(RedisConfig.BLOOM_FILTER_FUND, allFundCodes);
        
        log.info("布隆过滤器初始化完成，加载 {} 个基金代码", allFundCodes.size());
    }
    
    /**
     * 预热热点基金数据
     * 策略：规模TOP100的基金
     */
    private void warmupHotFunds() {
        log.info("预热热点基金数据...");
        
        // 获取规模TOP100的基金
        List<FundInfo> hotFunds = fundInfoMapper.selectTopByScale(HOT_FUND_COUNT);
        
        if (hotFunds.isEmpty()) {
            log.warn("没有找到热点基金数据");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (FundInfo fund : hotFunds) {
            try {
                String fundCode = fund.getFundCode();
                
                // 预热基金详情（使用随机TTL防雪崩）
                warmupFundDetail(fundCode);
                
                // 预热基金指标
                warmupFundMetrics(fundCode);
                
                successCount++;
            } catch (Exception e) {
                log.error("预热基金失败: {}", fund.getFundCode(), e);
                failCount++;
            }
        }
        
        log.info("热点基金预热完成: 成功 {}, 失败 {}", successCount, failCount);
    }
    
    /**
     * 预热基金详情缓存
     */
    @CachePut(value = RedisConfig.CACHE_FUND_TOP, key = "#fundCode")
    public FundInfoVO warmupFundDetail(String fundCode) {
        FundInfoVO detail = fundService.getFundDetail(fundCode);
        if (detail != null) {
            // 同时写入fund:detail缓存
            cacheFundDetailWithRandomTtl(fundCode, detail);
        }
        return detail;
    }
    
    /**
     * 预热基金指标缓存
     */
    @CachePut(value = RedisConfig.CACHE_FUND_TOP, key = "'metrics:' + #fundCode")
    public FundMetricsVO warmupFundMetrics(String fundCode) {
        return fundService.getLatestMetrics(fundCode);
    }
    
    /**
     * 使用随机TTL缓存基金详情（防雪崩）
     */
    private void cacheFundDetailWithRandomTtl(String fundCode, FundInfoVO detail) {
        String cacheKey = RedisConfig.CACHE_FUND_DETAIL + "::" + fundCode + "_v2";
        long ttl = cacheUtil.getRandomTtlMinutes(5); // 基础5分钟 + 随机偏移
        redisTemplate.opsForValue().set(cacheKey, serialize(detail), Duration.ofSeconds(ttl));
    }
    
    /**
     * 预热TOP排名数据
     */
    private void warmupTopRankings() {
        log.info("预热TOP排名数据...");
        
        try {
            // 预热各类型基金的TOP排名
            String[] sortTypes = {"sharpe", "return1y", "return3y"};
            String[] fundTypes = {null, "股票型", "混合型", "债券型"};
            
            for (String sortBy : sortTypes) {
                for (String fundType : fundTypes) {
                    try {
                        List<FundMetricsVO> topFunds = fundService.getTopFunds(sortBy, fundType, 20);
                        
                        // 写入缓存（带随机TTL）
                        String cacheKey = RedisConfig.CACHE_FUND_RANKING + "::" + sortBy + "-" + fundType + "-20";
                        long ttl = cacheUtil.getRandomTtlMinutes(60); // 基础1小时 + 随机偏移
                        redisTemplate.opsForValue().set(cacheKey, serialize(topFunds), Duration.ofSeconds(ttl));
                        
                        log.debug("预热排名缓存: sortBy={}, fundType={}, count={}", sortBy, fundType, topFunds.size());
                    } catch (Exception e) {
                        log.error("预热排名缓存失败: sortBy={}, fundType={}", sortBy, fundType, e);
                    }
                }
            }
            
            log.info("TOP排名预热完成");
        } catch (Exception e) {
            log.error("预热TOP排名失败", e);
        }
    }
    
    /**
     * 定时刷新热点数据（每15分钟）
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Async
    public void scheduledWarmup() {
        log.debug("定时刷新热点基金数据...");
        warmupHotFunds();
    }
    
    /**
     * 定时刷新布隆过滤器（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Async
    public void scheduledBloomFilterRefresh() {
        log.info("定时刷新布隆过滤器...");
        initBloomFilter();
    }
    
    /**
     * 手动触发缓存预热（管理接口用）
     */
    public void manualWarmup() {
        warmupHotFunds();
        warmupTopRankings();
    }
    
    /**
     * 添加新基金到布隆过滤器
     */
    public void addFundToBloomFilter(String fundCode) {
        cacheUtil.addToBloomFilter(RedisConfig.BLOOM_FILTER_FUND, fundCode);
    }
    
    /**
     * 清除指定基金的所有缓存
     */
    public void clearFundCache(String fundCode) {
        // 清除各个缓存
        redisTemplate.delete(RedisConfig.CACHE_FUND_DETAIL + "::" + fundCode + "_v2");
        redisTemplate.delete(RedisConfig.CACHE_FUND_METRICS + "::" + fundCode);
        redisTemplate.delete(RedisConfig.CACHE_FUND_TOP + "::" + fundCode);
        redisTemplate.delete(RedisConfig.CACHE_FUND_TOP + "::metrics:" + fundCode);
        
        log.info("已清除基金缓存: {}", fundCode);
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        
        // 获取Redis key数量（近似值）
        stats.setTotalKeys(redisTemplate.keys("*").size());
        
        // 布隆过滤器信息
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(RedisConfig.BLOOM_FILTER_FUND);
        if (bloomFilter.isExists()) {
            stats.setBloomFilterSize(bloomFilter.count());
            stats.setBloomFilterExpectedSize(bloomFilter.getExpectedInsertions());
        }
        
        return stats;
    }
    
    // 简单的序列化（实际生产可以使用JSON）
    private String serialize(Object obj) {
        if (obj == null) return "null";
        return obj.toString(); // 简化处理
    }
    
    /**
     * 缓存统计信息类
     */
    public static class CacheStats {
        private int totalKeys;
        private long bloomFilterSize;
        private long bloomFilterExpectedSize;
        
        public int getTotalKeys() { return totalKeys; }
        public void setTotalKeys(int totalKeys) { this.totalKeys = totalKeys; }
        public long getBloomFilterSize() { return bloomFilterSize; }
        public void setBloomFilterSize(long bloomFilterSize) { this.bloomFilterSize = bloomFilterSize; }
        public long getBloomFilterExpectedSize() { return bloomFilterExpectedSize; }
        public void setBloomFilterExpectedSize(long bloomFilterExpectedSize) { this.bloomFilterExpectedSize = bloomFilterExpectedSize; }
        
        @Override
        public String toString() {
            return "CacheStats{" +
                    "totalKeys=" + totalKeys +
                    ", bloomFilterSize=" + bloomFilterSize +
                    ", bloomFilterExpectedSize=" + bloomFilterExpectedSize +
                    '}';
        }
    }
}
