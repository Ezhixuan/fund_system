package com.fund.service;

import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.mapper.FundInfoMapper;
import com.fund.mapper.FundMetricsMapper;
import com.fund.mapper.FundNavMapper;
import com.fund.service.collect.CollectClient;
import com.fund.service.collect.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基金数据获取服务
 * 负责从本地数据库或Python采集服务获取基金数据
 * 支持实时数据补全机制、空值缓存、并发控制
 */
@Service
public class FundDataFetchService {

    private static final Logger log = LoggerFactory.getLogger(FundDataFetchService.class);

    @Autowired
    private CollectClient collectClient;

    @Autowired
    private FundInfoMapper fundInfoMapper;

    @Autowired
    private FundMetricsMapper fundMetricsMapper;

    @Autowired
    private FundNavMapper fundNavMapper;

    @Autowired
    private EmptyCacheService emptyCacheService;

    @Autowired
    private CollectTaskManager collectTaskManager;

    // 空值缓存TTL
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 检查基金信息是否完整
     */
    private boolean isInfoComplete(FundInfo info) {
        if (info == null) return false;
        return StringUtils.hasText(info.getManagerName())
            && StringUtils.hasText(info.getCompanyName())
            && info.getRiskLevel() != null
            && info.getEstablishDate() != null;
    }

    /**
     * 检查基金指标是否完整
     */
    private boolean isMetricsComplete(FundMetrics metrics) {
        if (metrics == null) return false;
        return metrics.getReturn1y() != null
            && metrics.getSharpeRatio1y() != null;
    }

    /**
     * 检查NAV历史是否完整（至少有10条记录）
     */
    private boolean isNavComplete(List<FundNav> navList) {
        return navList != null && navList.size() >= 10;
    }

    /**
     * 获取基金基本信息（自动补全版）
     * 本地数据库不存在或数据不完整时，自动触发Python服务采集
     *
     * @param fundCode 基金代码
     * @return 基金信息，不存在时返回null
     */
    public FundInfo getFundInfo(String fundCode) {
        log.debug("获取基金[{}]基本信息", fundCode);

        // 1. 检查空值缓存（近期已确认数据不存在）
        if (emptyCacheService.isEmptyCached(fundCode, EmptyCacheService.DataType.INFO)) {
            log.debug("基金[{}]基本信息已确认不存在（缓存命中）", fundCode);
            return null;
        }

        // 2. 查询本地数据库
        FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
        
        // 3. 【自动补全】数据不存在或不完整，触发采集
        if (!isInfoComplete(fundInfo)) {
            if (fundInfo == null) {
                log.info("基金[{}]基本信息不存在，触发自动采集", fundCode);
            } else {
                log.info("基金[{}]基本信息不完整（经理:{} 公司:{}），触发自动补全", 
                    fundCode, 
                    StringUtils.hasText(fundInfo.getManagerName()) ? "✓" : "✗",
                    StringUtils.hasText(fundInfo.getCompanyName()) ? "✓" : "✗");
            }
            
            // 触发采集（带并发控制）
            String taskKey = CollectTaskManager.buildTaskKey("info", fundCode);
            CollectResult<FundInfo> result = collectTaskManager.execute(taskKey, () -> {
                log.info("自动采集基金[{}]基本信息", fundCode);
                return collectClient.collectFundInfo(fundCode);
            });

            // 处理采集结果
            if (result != null && result.isSuccess()) {
                FundInfo newInfo = result.getData();
                try {
                    if (fundInfo == null) {
                        fundInfoMapper.insert(newInfo);
                        fundInfo = newInfo;
                        log.info("基金[{}]基本信息自动采集完成并保存", fundCode);
                    } else {
                        // 更新现有记录，保留已有数据
                        newInfo.setFundCode(fundCode);
                        fundInfoMapper.updateById(newInfo);
                        // 重新查询获取更新后的数据
                        fundInfo = fundInfoMapper.selectById(fundCode);
                        log.info("基金[{}]基本信息自动补全完成", fundCode);
                    }
                } catch (Exception e) {
                    log.error("基金[{}]基本信息自动保存失败: {}", fundCode, e.getMessage());
                }
            } else {
                // 采集失败，只有原数据不存在时才设置空值缓存
                if (fundInfo == null) {
                    if (result != null && CollectResult.ERR_FUND_NOT_FOUND.equals(result.getErrorCode())) {
                        log.warn("基金[{}]在Python端也不存在", fundCode);
                    }
                    emptyCacheService.setEmptyCache(fundCode, EmptyCacheService.DataType.INFO, EMPTY_CACHE_TTL);
                    return null;
                }
            }
        } else {
            log.debug("基金[{}]基本信息完整，从本地数据库获取", fundCode);
        }

        return fundInfo;
    }

    /**
     * 获取基金指标数据（自动补全版）
     */
    public FundMetrics getFundMetrics(String fundCode) {
        log.debug("获取基金[{}]指标数据", fundCode);

        // 1. 检查空值缓存
        if (emptyCacheService.isEmptyCached(fundCode, EmptyCacheService.DataType.METRICS)) {
            log.debug("基金[{}]指标数据已确认不存在（缓存命中）", fundCode);
            return null;
        }

        // 2. 查询本地数据库
        FundMetrics metrics = fundMetricsMapper.selectById(fundCode);
        
        // 3. 【自动补全】数据不完整时触发采集
        if (!isMetricsComplete(metrics)) {
            if (metrics == null) {
                log.info("基金[{}]指标数据不存在，触发自动采集", fundCode);
            } else {
                log.info("基金[{}]指标数据不完整，触发自动补全", fundCode);
            }
            
            String taskKey = CollectTaskManager.buildTaskKey("metrics", fundCode);
            CollectResult<FundMetrics> result = collectTaskManager.execute(taskKey, () -> {
                log.info("自动采集基金[{}]指标数据", fundCode);
                return collectClient.collectFundMetrics(fundCode);
            });

            if (result != null && result.isSuccess()) {
                FundMetrics newMetrics = result.getData();
                try {
                    if (metrics == null) {
                        fundMetricsMapper.insert(newMetrics);
                        metrics = newMetrics;
                    } else {
                        fundMetricsMapper.updateById(newMetrics);
                        metrics = fundMetricsMapper.selectById(fundCode);
                    }
                    log.info("基金[{}]指标数据自动采集完成", fundCode);
                } catch (Exception e) {
                    log.error("基金[{}]指标数据自动保存失败: {}", fundCode, e.getMessage());
                }
            } else {
                if (metrics == null) {
                    emptyCacheService.setEmptyCache(fundCode, EmptyCacheService.DataType.METRICS, EMPTY_CACHE_TTL);
                    return null;
                }
            }
        }

        return metrics;
    }

    /**
     * 获取基金NAV历史数据（自动补全版）
     */
    public List<FundNav> getNavHistory(String fundCode) {
        log.debug("获取基金[{}]NAV历史数据", fundCode);

        // 1. 检查空值缓存
        if (emptyCacheService.isEmptyCached(fundCode, EmptyCacheService.DataType.NAV)) {
            log.debug("基金[{}]NAV历史已确认不存在（缓存命中）", fundCode);
            return List.of();
        }

        // 2. 查询本地数据库
        List<FundNav> navList = fundNavMapper.selectRecentByCode(fundCode, 30);
        
        // 3. 【自动补全】数据不足时触发采集
        if (!isNavComplete(navList)) {
            log.info("基金[{}]NAV历史不足{}条（当前{}条），触发自动采集", 
                fundCode, 10, navList != null ? navList.size() : 0);
            
            String taskKey = CollectTaskManager.buildTaskKey("nav", fundCode);
            CollectResult<List<FundNav>> result = collectTaskManager.execute(taskKey, () -> {
                log.info("自动采集基金[{}]NAV历史", fundCode);
                return collectClient.collectNavHistory(fundCode);
            });

            if (result != null && result.isSuccess()) {
                List<FundNav> newNavList = result.getData();
                if (newNavList != null && !newNavList.isEmpty()) {
                    try {
                        // 合并新旧数据，避免重复
                        for (FundNav nav : newNavList) {
                            try {
                                fundNavMapper.insert(nav);
                            } catch (Exception e) {
                                // 忽略重复插入错误
                            }
                        }
                        // 重新查询
                        navList = fundNavMapper.selectRecentByCode(fundCode, 30);
                        log.info("基金[{}]NAV历史自动采集完成，共{}条", fundCode, navList.size());
                    } catch (Exception e) {
                        log.error("基金[{}]NAV历史自动保存失败: {}", fundCode, e.getMessage());
                    }
                }
            } else {
                if (navList == null || navList.isEmpty()) {
                    emptyCacheService.setEmptyCache(fundCode, EmptyCacheService.DataType.NAV, EMPTY_CACHE_TTL);
                    return List.of();
                }
            }
        }

        return navList != null ? navList : List.of();
    }

    /**
     * 强制刷新基金基本信息（手动兜底）
     */
    public CollectResult<FundInfo> refreshFundInfo(String fundCode) {
        log.info("强制刷新基金[{}]基本信息", fundCode);
        emptyCacheService.clearEmptyCache(fundCode, EmptyCacheService.DataType.INFO);
        
        CollectResult<FundInfo> result = collectClient.collectFundInfo(fundCode);
        if (result.isSuccess()) {
            FundInfo fundInfo = result.getData();
            try {
                FundInfo existing = fundInfoMapper.selectById(fundCode);
                if (existing != null) {
                    fundInfoMapper.updateById(fundInfo);
                } else {
                    fundInfoMapper.insert(fundInfo);
                }
                log.info("基金[{}]基本信息手动刷新完成", fundCode);
            } catch (Exception e) {
                log.error("基金[{}]基本信息手动刷新保存失败: {}", fundCode, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 强制刷新基金指标数据（手动兜底）
     */
    public CollectResult<FundMetrics> refreshFundMetrics(String fundCode) {
        log.info("强制刷新基金[{}]指标数据", fundCode);
        emptyCacheService.clearEmptyCache(fundCode, EmptyCacheService.DataType.METRICS);
        
        CollectResult<FundMetrics> result = collectClient.collectFundMetrics(fundCode);
        if (result.isSuccess()) {
            FundMetrics metrics = result.getData();
            try {
                FundMetrics existing = fundMetricsMapper.selectById(fundCode);
                if (existing != null) {
                    fundMetricsMapper.updateById(metrics);
                } else {
                    fundMetricsMapper.insert(metrics);
                }
                log.info("基金[{}]指标数据手动刷新完成", fundCode);
            } catch (Exception e) {
                log.error("基金[{}]指标数据手动刷新保存失败: {}", fundCode, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 强制刷新基金NAV历史（手动兜底）
     */
    public CollectResult<List<FundNav>> refreshNavHistory(String fundCode) {
        log.info("强制刷新基金[{}]NAV历史", fundCode);
        emptyCacheService.clearEmptyCache(fundCode, EmptyCacheService.DataType.NAV);
        
        CollectResult<List<FundNav>> result = collectClient.collectNavHistory(fundCode);
        if (result.isSuccess()) {
            List<FundNav> navList = result.getData();
            if (navList != null && !navList.isEmpty()) {
                try {
                    fundNavMapper.deleteByFundCode(fundCode);
                    for (FundNav nav : navList) {
                        fundNavMapper.insert(nav);
                    }
                    log.info("基金[{}]NAV历史手动刷新完成，共{}条", fundCode, navList.size());
                } catch (Exception e) {
                    log.error("基金[{}]NAV历史手动刷新保存失败: {}", fundCode, e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 批量刷新基金数据（手动兜底 - 批量模式）
     */
    public List<String> batchRefreshFunds(List<String> fundCodes) {
        log.info("批量刷新{}只基金数据", fundCodes.size());
        List<String> results = new ArrayList<>();
        
        for (String fundCode : fundCodes) {
            try {
                // 刷新所有类型数据
                refreshFundInfo(fundCode);
                refreshFundMetrics(fundCode);
                refreshNavHistory(fundCode);
                results.add(fundCode + ": 成功");
            } catch (Exception e) {
                results.add(fundCode + ": 失败 - " + e.getMessage());
            }
        }
        
        return results;
    }
}
