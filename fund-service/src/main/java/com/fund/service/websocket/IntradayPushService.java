package com.fund.service.websocket;

import com.fund.entity.watchlist.FundEstimateIntraday;

import java.util.List;

/**
 * 实时估值推送服务接口
 */
public interface IntradayPushService {
    
    /**
     * 推送到基金详情页
     * @param estimate 估值数据
     */
    void pushToFundDetail(FundEstimateIntraday estimate);
    
    /**
     * 推送到持仓页面（只推送给持有该基金的用户）
     * @param estimate 估值数据
     */
    void pushToPortfolio(FundEstimateIntraday estimate);
    
    /**
     * 批量推送更新
     * @param estimates 估值数据列表
     */
    void batchPush(List<FundEstimateIntraday> estimates);
    
    /**
     * 广播更新（推送给所有在线用户）
     * @param estimate 估值数据
     */
    void broadcastUpdate(FundEstimateIntraday estimate);
}
