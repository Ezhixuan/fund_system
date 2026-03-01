package com.ezhixuan.fund.infrastructure.persistence.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ezhixuan.fund.domain.entity.watchlist.UserWatchlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户关注列表 Mapper
 */
@Mapper
public interface UserWatchlistMapper extends BaseMapper<UserWatchlist> {
    
    /**
     * 查询所有启用的关注基金
     */
    @Select("SELECT * FROM user_watchlist WHERE is_active = 1 ORDER BY sort_order, id")
    List<UserWatchlist> selectActiveWatchlist();
    
    /**
     * 根据类型查询
     */
    @Select("SELECT * FROM user_watchlist WHERE is_active = 1 AND watch_type = #{watchType} ORDER BY sort_order, id")
    List<UserWatchlist> selectByWatchType(@Param("watchType") Integer watchType);
    
    /**
     * 检查基金是否已关注
     */
    @Select("SELECT COUNT(*) FROM user_watchlist WHERE fund_code = #{fundCode} AND is_active = 1")
    int existsByFundCode(@Param("fundCode") String fundCode);
}
