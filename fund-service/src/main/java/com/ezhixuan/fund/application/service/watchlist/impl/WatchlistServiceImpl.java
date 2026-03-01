package com.ezhixuan.fund.application.service.watchlist.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezhixuan.fund.application.service.watchlist.WatchlistService;
import com.ezhixuan.fund.domain.entity.watchlist.UserWatchlist;
import com.ezhixuan.fund.domain.entity.watchlist.WatchFundConfig;
import com.ezhixuan.fund.infrastructure.persistence.mapper.watchlist.UserWatchlistMapper;
import com.ezhixuan.fund.infrastructure.persistence.mapper.watchlist.WatchFundConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 关注列表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl extends ServiceImpl<UserWatchlistMapper, UserWatchlist> 
        implements WatchlistService {
    
    private final UserWatchlistMapper watchlistMapper;
    private final WatchFundConfigMapper configMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserWatchlist addWatchlist(String fundCode, String fundName, Integer watchType) {
        // 检查是否已关注
        if (isFundWatched(fundCode)) {
            throw new RuntimeException("该基金已在关注列表中");
        }
        
        UserWatchlist watchlist = new UserWatchlist();
        watchlist.setFundCode(fundCode);
        watchlist.setFundName(fundName);
        watchlist.setAddDate(LocalDate.now());
        watchlist.setWatchType(watchType);
        watchlist.setSortOrder(0);
        watchlist.setIsActive(1);
        
        // 保存关注记录
        save(watchlist);
        
        // 初始化采集配置
        initFundConfig(fundCode);
        
        log.info("添加关注基金成功: {}", fundCode);
        return watchlist;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserWatchlist addWatchlist(UserWatchlist watchlist) {
        // 检查是否已关注
        if (isFundWatched(watchlist.getFundCode())) {
            throw new RuntimeException("该基金已在关注列表中");
        }
        
        // 设置默认值
        if (watchlist.getAddDate() == null) {
            watchlist.setAddDate(LocalDate.now());
        }
        if (watchlist.getWatchType() == null) {
            watchlist.setWatchType(2); // 默认关注
        }
        if (watchlist.getSortOrder() == null) {
            watchlist.setSortOrder(0);
        }
        if (watchlist.getIsActive() == null) {
            watchlist.setIsActive(1);
        }
        
        save(watchlist);
        
        // 初始化采集配置
        initFundConfig(watchlist.getFundCode());
        
        log.info("添加关注基金成功: {}", watchlist.getFundCode());
        return watchlist;
    }
    
    @Override
    public List<UserWatchlist> getAllWatchlist() {
        return watchlistMapper.selectActiveWatchlist();
    }
    
    @Override
    public List<UserWatchlist> getWatchlistByType(Integer watchType) {
        return watchlistMapper.selectByWatchType(watchType);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserWatchlist updateWatchlist(UserWatchlist watchlist) {
        updateById(watchlist);
        return getById(watchlist.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeWatchlist(String fundCode) {
        // 软删除，标记为停用
        UserWatchlist watchlist = lambdaQuery()
                .eq(UserWatchlist::getFundCode, fundCode)
                .eq(UserWatchlist::getIsActive, 1)
                .one();
        
        if (watchlist == null) {
            return false;
        }
        
        watchlist.setIsActive(0);
        return updateById(watchlist);
    }
    
    @Override
    public boolean isFundWatched(String fundCode) {
        return watchlistMapper.existsByFundCode(fundCode) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importFromPortfolio() {
        // TODO: 从持仓表导入
        // 查询所有持仓中的基金
        // 过滤掉已关注的
        // 批量添加
        log.info("从持仓导入关注基金...");
        return 0;
    }
    
    @Override
    public List<String> getWatchedFundCodes() {
        return getAllWatchlist().stream()
                .map(UserWatchlist::getFundCode)
                .collect(Collectors.toList());
    }
    
    /**
     * 初始化基金采集配置
     */
    private void initFundConfig(String fundCode) {
        WatchFundConfig config = configMapper.selectById(fundCode);
        if (config == null) {
            config = new WatchFundConfig();
            config.setFundCode(fundCode);
            config.setNeedDetail(1);
            config.setNeedNav(1);
            config.setNeedIntraday(1);
            config.setNeedPortfolio(0);
            config.setCollectIntervalMinutes(10);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
        }
    }
}
