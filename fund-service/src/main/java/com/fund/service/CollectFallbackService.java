package com.fund.service;

import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.service.collect.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 采集降级服务
 * 当 Python 服务不可用时的降级处理
 */
@Service
public class CollectFallbackService {

    private static final Logger log = LoggerFactory.getLogger(CollectFallbackService.class);

    /**
     * 获取基金信息降级处理
     * 当采集服务不可用时返回部分数据或null
     *
     * @param fundCode 基金代码
     * @param ex 异常信息
     * @return 降级后的基金信息（可能为null）
     */
    public FundInfo fallbackFundInfo(String fundCode, Exception ex) {
        log.error("Python采集服务异常，基金[{}]获取基本信息降级处理", fundCode, ex);
        
        // 降级策略：返回null，前端显示"数据暂不可用"
        // 可扩展：从备用数据源获取、返回缓存数据等
        return null;
    }

    /**
     * 获取基金指标降级处理
     *
     * @param fundCode 基金代码
     * @param ex 异常信息
     * @return 降级后的指标数据（可能为null）
     */
    public FundMetrics fallbackFundMetrics(String fundCode, Exception ex) {
        log.error("Python采集服务异常，基金[{}]获取指标数据降级处理", fundCode, ex);
        return null;
    }

    /**
     * 获取NAV历史降级处理
     *
     * @param fundCode 基金代码
     * @param ex 异常信息
     * @return 降级后的NAV列表（返回空列表）
     */
    public List<FundNav> fallbackNavHistory(String fundCode, Exception ex) {
        log.error("Python采集服务异常，基金[{}]获取NAV历史降级处理", fundCode, ex);
        return List.of();
    }

    /**
     * 构建降级响应结果
     *
     * @param fundCode 基金代码
     * @param errorMessage 错误信息
     * @return 包含降级信息的CollectResult
     */
    public <T> CollectResult<T> buildFallbackResult(String fundCode, String errorMessage) {
        log.warn("构建基金[{}]降级响应: {}", fundCode, errorMessage);
        return CollectResult.fail(
            CollectResult.ERR_SERVICE_ERROR,
            "服务暂时不可用，请稍后重试: " + errorMessage
        );
    }

    /**
     * 判断是否可降级
     * 某些关键业务不允许降级
     *
     * @param fundCode 基金代码
     * @return true - 可以降级
     */
    public boolean canFallback(String fundCode) {
        // 所有基金详情查询都允许降级
        // 返回null或空列表，前端展示友好提示
        return true;
    }
}
