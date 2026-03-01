package com.ezhixuan.fund.infrastructure.persistence.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ezhixuan.fund.domain.entity.watchlist.FundEstimateIntraday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时估值点位 Mapper
 */
@Mapper
public interface FundEstimateIntradayMapper extends BaseMapper<FundEstimateIntraday> {
    
    /**
     * 查询指定基金当日的所有点位
     */
    @Select("SELECT * FROM fund_estimate_intraday WHERE fund_code = #{fundCode} AND trade_date = #{tradeDate} ORDER BY estimate_time")
    List<FundEstimateIntraday> selectByFundAndDate(@Param("fundCode") String fundCode, @Param("tradeDate") LocalDate tradeDate);
    
    /**
     * 查询最新的一条估值
     */
    @Select("SELECT * FROM fund_estimate_intraday WHERE fund_code = #{fundCode} ORDER BY estimate_time DESC LIMIT 1")
    FundEstimateIntraday selectLatest(@Param("fundCode") String fundCode);
    
    /**
     * 查询指定时间之后的估值（用于WebSocket推送后获取）
     */
    @Select("SELECT * FROM fund_estimate_intraday WHERE fund_code = #{fundCode} AND estimate_time \u003e #{time} ORDER BY estimate_time")
    List<FundEstimateIntraday> selectAfterTime(@Param("fundCode") String fundCode, @Param("time") LocalDateTime time);
}
