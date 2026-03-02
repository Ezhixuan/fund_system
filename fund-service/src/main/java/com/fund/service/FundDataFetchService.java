package com.fund.service;

import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.service.collect.CollectClient;
import com.fund.service.collect.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基金数据获取服务
 * 负责从本地数据库或Python采集服务获取基金数据
 * 支持实时数据补全机制
 */
@Service
public class FundDataFetchService {

    private static final Logger log = LoggerFactory.getLogger(FundDataFetchService.class);

    @Autowired
    private CollectClient collectClient;

    /**
     * 获取基金基本信息
     * 本地数据库不存在时，调用Python服务采集
     *
     * @param fundCode 基金代码
     * @return 基金信息，不存在时返回null
     */
    public FundInfo getFundInfo(String fundCode) {
        log.debug("获取基金[{}]基本信息", fundCode);
        // TODO: 实现数据获取逻辑
        return null;
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
        // TODO: 实现数据获取逻辑
        return null;
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
        // TODO: 实现数据获取逻辑
        return List.of();
    }

    /**
     * 强制刷新基金基本信息
     * 直接从Python服务采集，覆盖本地数据
     *
     * @param fundCode 基金代码
     * @return 采集结果
     */
    public CollectResult<FundInfo> refreshFundInfo(String fundCode) {
        log.info("强制刷新基金[{}]基本信息", fundCode);
        return collectClient.collectFundInfo(fundCode);
    }

    /**
     * 强制刷新基金指标数据
     *
     * @param fundCode 基金代码
     * @return 采集结果
     */
    public CollectResult<FundMetrics> refreshFundMetrics(String fundCode) {
        log.info("强制刷新基金[{}]指标数据", fundCode);
        return collectClient.collectFundMetrics(fundCode);
    }

    /**
     * 强制刷新基金NAV历史
     *
     * @param fundCode 基金代码
     * @return 采集结果
     */
    public CollectResult<List<FundNav>> refreshNavHistory(String fundCode) {
        log.info("强制刷新基金[{}]NAV历史", fundCode);
        return collectClient.collectNavHistory(fundCode);
    }
}
