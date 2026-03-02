package com.fund.service.collect;

import com.fund.dto.ApiResponse;
import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;

import java.util.List;
import java.util.Map;

/**
 * 采集服务客户端接口
 * 调用Python采集服务
 */
public interface CollectClient {

    /**
     * 采集单只基金估值
     */
    ApiResponse<Map<String, Object>> collectEstimate(String fundCode);

    /**
     * 批量采集基金估值
     */
    ApiResponse<Map<String, Object>> collectBatch(List<String> fundCodes);

    /**
     * 健康检查
     */
    boolean healthCheck();

    // ============ 新增：基金详情页数据实时采集接口 ============

    /**
     * 采集基金基本信息
     * 当本地数据库不存在时，调用Python服务采集
     *
     * @param fundCode 基金代码
     * @return 采集结果，包含FundInfo或错误信息
     */
    CollectResult<FundInfo> collectFundInfo(String fundCode);

    /**
     * 采集基金指标数据
     * 当本地数据库不存在时，调用Python服务采集并计算
     *
     * @param fundCode 基金代码
     * @return 采集结果，包含FundMetrics或错误信息
     */
    CollectResult<FundMetrics> collectFundMetrics(String fundCode);

    /**
     * 采集基金NAV历史数据
     * 当本地数据库不存在时，调用Python服务采集
     *
     * @param fundCode 基金代码
     * @return 采集结果，包含NavData列表或错误信息
     */
    CollectResult<List<FundNav>> collectNavHistory(String fundCode);
}
