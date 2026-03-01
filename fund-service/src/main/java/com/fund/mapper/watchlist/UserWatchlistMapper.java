package com.fund.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.watchlist.UserWatchlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserWatchlistMapper extends BaseMapper<UserWatchlist> {
    @Select("SELECT * FROM user_watchlist WHERE is_active = 1 ORDER BY sort_order, id")
    List<UserWatchlist> selectActiveWatchlist();
    
    @Select("SELECT * FROM user_watchlist WHERE is_active = 1 AND watch_type = #{watchType} ORDER BY sort_order, id")
    List<UserWatchlist> selectByWatchType(@Param("watchType") Integer watchType);
    
    @Select("SELECT COUNT(*) FROM user_watchlist WHERE fund_code = #{fundCode} AND is_active = 1")
    int existsByFundCode(@Param("fundCode") String fundCode);
}
