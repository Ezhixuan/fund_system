package com.ezhixuan.fund.infrastructure.persistence.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ezhixuan.fund.domain.entity.watchlist.WatchFundConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 关注基金配置 Mapper
 */
@Mapper
public interface WatchFundConfigMapper extends BaseMapper<WatchFundConfig> {
    
    /**
     * 查询需要实时估值的基金
     */
    @Select("SELECT * FROM watch_fund_config WHERE need_intraday = 1")
    List<WatchFundConfig> selectNeedIntraday();
    
    /**
     * 更新最后采集时间
     */
    @Update("UPDATE watch_fund_config SET last_intraday_time = #{time} WHERE fund_code = #{fundCode}")
    int updateLastIntradayTime(@Param("fundCode") String fundCode, @Param("time") LocalDateTime time);
    
    /**
     * 更新最后采集日期
     */
    @Update("UPDATE watch_fund_config SET last_collect_date = #{date} WHERE fund_code = #{fundCode}")
    int updateLastCollectDate(@Param("fundCode") String fundCode, @Param("date") LocalDate date);
}
