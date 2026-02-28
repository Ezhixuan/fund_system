package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 基金指标Mapper
 */
@Mapper
public interface FundMetricsMapper extends BaseMapper<FundMetrics> {
    
    /**
     * 查询基金最新指标
     */
    @Select("SELECT * FROM fund_metrics WHERE fund_code = #{fundCode} " +
            "ORDER BY calc_date DESC LIMIT 1")
    FundMetrics selectLatestByFundCode(String fundCode);
    
    /**
     * 查询TOP N基金（按夏普比率）
     */
    @Select("SELECT * FROM fund_metrics WHERE calc_date = #{date} " +
            "ORDER BY sharpe_ratio_1y DESC LIMIT #{limit}")
    List<FundMetrics> selectTopBySharpe(String date, int limit);
}
