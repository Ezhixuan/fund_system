package com.fund.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.watchlist.WatchFundConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WatchFundConfigMapper extends BaseMapper<WatchFundConfig> {
    @Select("SELECT * FROM watch_fund_config WHERE need_intraday = 1")
    List<WatchFundConfig> selectNeedIntraday();
    
    @Update("UPDATE watch_fund_config SET last_intraday_time = #{time} WHERE fund_code = #{fundCode}")
    int updateLastIntradayTime(@Param("fundCode") String fundCode, @Param("time") LocalDateTime time);
    
    @Update("UPDATE watch_fund_config SET last_collect_date = #{date} WHERE fund_code = #{fundCode}")
    int updateLastCollectDate(@Param("fundCode") String fundCode, @Param("date") LocalDate date);
}
