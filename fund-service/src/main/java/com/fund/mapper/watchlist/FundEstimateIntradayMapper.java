package com.fund.mapper.watchlist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.watchlist.FundEstimateIntraday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FundEstimateIntradayMapper extends BaseMapper<FundEstimateIntraday> {
    @Select("SELECT * FROM fund_estimate_intraday WHERE fund_code = #{fundCode} AND trade_date = #{tradeDate} ORDER BY estimate_time")
    List<FundEstimateIntraday> selectByFundAndDate(@Param("fundCode") String fundCode, @Param("tradeDate") LocalDate tradeDate);
    
    @Select("SELECT * FROM fund_estimate_intraday WHERE fund_code = #{fundCode} ORDER BY estimate_time DESC LIMIT 1")
    FundEstimateIntraday selectLatest(@Param("fundCode") String fundCode);
    
    @Select("SELECT * FROM fund_estimate_intraday WHERE fund_code = #{fundCode} AND estimate_time > #{time} ORDER BY estimate_time")
    List<FundEstimateIntraday> selectAfterTime(@Param("fundCode") String fundCode, @Param("time") LocalDateTime time);
}
