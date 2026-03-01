package com.ezhixuan.fund.application.service.watchlist;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ezhixuan.fund.domain.entity.watchlist.UserWatchlist;

import java.util.List;

/**
 * 关注列表服务接口
 */
public interface WatchlistService extends IService<UserWatchlist> {
    
    /**
     * 添加关注基金
     */
    UserWatchlist addWatchlist(String fundCode, String fundName, Integer watchType);
    
    /**
     * 添加关注基金（完整版）
     */
    UserWatchlist addWatchlist(UserWatchlist watchlist);
    
    /**
     * 获取所有关注的基金
     */
    List<UserWatchlist> getAllWatchlist();
    
    /**
     * 获取指定类型的关注基金
     */
    List<UserWatchlist> getWatchlistByType(Integer watchType);
    
    /**
     * 更新关注信息
     */
    UserWatchlist updateWatchlist(UserWatchlist watchlist);
    
    /**
     * 移除关注
     */
    boolean removeWatchlist(String fundCode);
    
    /**
     * 检查基金是否已关注
     */
    boolean isFundWatched(String fundCode);
    
    /**
     * 从持仓自动导入
     */
    int importFromPortfolio();
    
    /**
     * 获取关注基金代码列表
     */
    List<String> getWatchedFundCodes();
}
