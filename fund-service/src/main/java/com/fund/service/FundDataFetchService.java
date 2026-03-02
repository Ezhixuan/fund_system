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

import java.time.Duration;
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
     * 获取基金基本信息
     * 本地数据库不存在时，调用Python服务采集
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
        if (fundInfo != null) {
            log.debug("基金[{}]基本信息从本地数据库获取", fundCode);
            return fundInfo;
        }

        // 3. 本地不存在，触发采集（带并发控制）
        String taskKey = CollectTaskManager.buildTaskKey("info", fundCode);
        CollectResult<FundInfo> result = collectTaskManager.execute(taskKey, () -> {
            log.info("触发采集基金[{}]基本信息", fundCode);
            return collectClient.collectFundInfo(fundCode);
        });

        // 4. 处理采集结果
        if (result != null && result.isSuccess()) {
            fundInfo = result.getData();
            // 保存到本地数据库
            try {
                fundInfoMapper.insert(fundInfo);
                log.info("基金[{}]基本信息采集完成并保存", fundCode);
            } catch (Exception e) {
                log.warn("基金[{}]基本信息保存失败（可能已存在）: {}", fundCode, e.getMessage());
            }
            return fundInfo;
        } else {
            // 采集失败，设置空值缓存
            if (result != null && CollectResult.ERR_FUND_NOT_FOUND.equals(result.getErrorCode())) {
                log.warn("基金[{}]在Python端也不存在", fundCode);
            }
            emptyCacheService.setEmptyCache(fundCode, EmptyCacheService.DataType.INFO, EMPTY_CACHE_TTL);
            return null;
        }
    }

    /**
     * 获取基金指标数据
     * 本地数据库不存在时，调用Python服务采集
     *
     * @param fundCode 基金代码
     * @return 基金指标，不存在时返回null
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
        if (metrics != null) {
            log.debug("基金[{}]指标数据从本地数据库获取", fundCode);
            return metrics;
        }

        // 3. 本地不存在，触发采集
        String taskKey = CollectTaskManager.buildTaskKey("metrics", fundCode);
        CollectResult<FundMetrics> result = collectTaskManager.execute(taskKey, () -> {
            log.info("触发采集基金[{}]指标数据", fundCode);
            return collectClient.collectFundMetrics(fundCode);
        });

        // 4. 处理结果
        if (result != null && result.isSuccess()) {
            metrics = result.getData();
            try {
                fundMetricsMapper.insert(metrics);
                log.info("基金[{}]指标数据采集完成并保存", fundCode);
            } catch (Exception e) {
                log.warn("基金[{}]指标数据保存失败: {}", fundCode, e.getMessage());
            }
            return metrics;
        } else {
            emptyCacheService.setEmptyCache(fundCode, EmptyCacheService.DataType.METRICS, EMPTY_CACHE_TTL);
            return null;
        }
    }

    /**
     * 获取基金NAV历史数据
     * 本地数据库不存在时，调用Python服务采集
     *
     * @param fundCode 基金代码
     * @return NAV历史列表，不存在时返回空列表
     */
    public List<FundNav> getNavHistory(String fundCode) {
        log.debug("获取基金[{}]NAV历史数据", fundCode);

        // 1. 检查空值缓存
        if (emptyCacheService.isEmptyCached(fundCode, EmptyCacheService.DataType.NAV)) {
            log.debug("基金[{}]NAV历史已确认不存在（缓存命中）", fundCode);
            return List.of();
        }

        // 2. 查询本地数据库（取最近30条）
        List<FundNav> navList = fundNavMapper.selectRecentByCode(fundCode, 30);
        if (navList != null && !navList.isEmpty()) {
            log.debug("基金[{}]NAV历史从本地数据库获取，共{}条", fundCode, navList.size());
            return navList;
        }

        // 3. 本地不存在，触发采集
        String taskKey = CollectTaskManager.buildTaskKey("nav", fundCode);
        CollectResult<List<FundNav>> result = collectTaskManager.execute(taskKey, () -> {
            log.info("触发采集基金[{}]NAV历史", fundCode);
            return collectClient.collectNavHistory(fundCode);
        });

        // 4. 处理结果
        if (result != null && result.isSuccess()) {
            navList = result.getData();
            if (navList != null && !navList.isEmpty()) {
                // 批量保存
                try {
                    for (FundNav nav : navList) {
                        fundNavMapper.insert(nav);
                    }
                    log.info("基金[{}]NAV历史采集完成并保存，共{}条", fundCode, navList.size());
                } catch (Exception e) {
                    log.warn("基金[{}]NAV历史保存失败: {}", fundCode, e.getMessage());
                }
                return navList;
            }
        }

        // 采集失败或数据为空
        emptyCacheService.setEmptyCache(fundCode, EmptyCacheService.DataType.NAV, EMPTY_CACHE_TTL);
        return List.of();
    }

    /**
     * 强制刷新基金基本信息
     *
     * @param fundCode 基金代码
     * @return 采集结果
     */
    public CollectResult<FundInfo> refreshFundInfo(String fundCode) {
        log.info("强制刷新基金[{}]基本信息", fundCode);

        // 清除空值缓存
        emptyCacheService.clearEmptyCache(fundCode, EmptyCacheService.DataType.INFO);

        // 直接调用采集
        CollectResult<FundInfo> result = collectClient.collectFundInfo(fundCode);

        if (result.isSuccess()) {
            FundInfo fundInfo = result.getData();
            try {
                // 尝试更新或插入
                FundInfo existing = fundInfoMapper.selectById(fundCode);
                if (existing != null) {
                    fundInfoMapper.updateById(fundInfo);
                } else {
                    fundInfoMapper.insert(fundInfo);
                }
                log.info("基金[{}]基本信息刷新完成", fundCode);
            } catch (Exception e) {
                log.error("基金[{}]基本信息刷新保存失败: {}", fundCode, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 强制刷新基金指标数据
     *
     * @param fundCode 基金代码
     * @return 采集结果
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
                log.info("基金[{}]指标数据刷新完成", fundCode);
            } catch (Exception e) {
                log.error("基金[{}]指标数据刷新保存失败: {}", fundCode, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 强制刷新基金NAV历史
     *
     * @param fundCode 基金代码
     * @return 采集结果
     */
    public CollectResult<List<FundNav>> refreshNavHistory(String fundCode) {
        log.info("强制刷新基金[{}]NAV历史", fundCode);

        emptyCacheService.clearEmptyCache(fundCode, EmptyCacheService.DataType.NAV);
        CollectResult<List<FundNav>> result = collectClient.collectNavHistory(fundCode);

        if (result.isSuccess()) {
            List<FundNav> navList = result.getData();
            if (navList != null && !navList.isEmpty()) {
                try {
                    // 删除旧数据
                    fundNavMapper.deleteByFundCode(fundCode);
                    // 插入新数据
                    for (FundNav nav : navList) {
                        fundNavMapper.insert(nav);
                    }
                    log.info("基金[{}]NAV历史刷新完成，共{}条", fundCode, navList.size());
                } catch (Exception e) {
                    log.error("基金[{}]NAV历史刷新保存失败: {}", fundCode, e.getMessage());
                }
            }
        }

        return result;
    }
}
