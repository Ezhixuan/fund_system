package com.fund.service.watchlist;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fund.entity.watchlist.UserWatchlist;
import java.util.List;

public interface WatchlistService extends IService<UserWatchlist> {
    UserWatchlist addWatchlist(String fundCode, String fundName, Integer watchType);
    UserWatchlist addWatchlist(UserWatchlist watchlist);
    List<UserWatchlist> getAllWatchlist();
    List<UserWatchlist> getWatchlistByType(Integer watchType);
    UserWatchlist updateWatchlist(UserWatchlist watchlist);
    boolean removeWatchlist(String fundCode);
    boolean isFundWatched(String fundCode);
    int importFromPortfolio();
    List<String> getWatchedFundCodes();
}
