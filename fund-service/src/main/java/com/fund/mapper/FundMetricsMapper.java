package com.fund.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 基金指标Mapper
 */
@Mapper
public interface FundMetricsMapper extends BaseMapper<FundMetrics> {
    
    /**
     * 查询基金最新指标 - 使用XML配置
     */
    FundMetrics selectLatestByFundCode(@Param("fundCode") String fundCode);
    
    /**
     * 查询TOP N基金（按夏普比率，优化：只查询必要字段）
     */
    @Select("SELECT fund_code, calc_date, sharpe_ratio_1y, return_1y, max_drawdown_1y, volatility_1y, beta_1y, alpha_1y " +
            "FROM fund_metrics WHERE calc_date = #{date} " +
            "ORDER BY sharpe_ratio_1y DESC LIMIT #{limit}")
    List<FundMetrics> selectTopBySharpe(String date, int limit);
    
    /**
     * 按指定字段排序查询TOP基金
     */
    @Select("<script>" +
            "SELECT m.* FROM fund_metrics m " +
            "INNER JOIN fund_info f ON m.fund_code = f.fund_code " +
            "WHERE m.calc_date = (SELECT MAX(calc_date) FROM fund_metrics) " +
            "<if test='fundType != null'> AND f.fund_type = #{fundType} </if> " +
            "ORDER BY ${orderBy} DESC LIMIT #{limit} " +
            "</script>")
    List<FundMetrics> selectTopByOrder(@Param("orderBy") String orderBy, 
                                       @Param("fundType") String fundType, 
                                       @Param("limit") int limit);
}
