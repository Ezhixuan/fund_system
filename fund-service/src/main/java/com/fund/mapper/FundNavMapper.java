package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundNav;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 基金净值Mapper
 */
@Mapper
public interface FundNavMapper extends BaseMapper<FundNav> {
    
    /**
     * 查询基金历史净值
     */
    @Select("SELECT * FROM fund_nav WHERE fund_code = #{fundCode} " +
            "AND nav_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY nav_date DESC")
    List<FundNav> selectByDateRange(@Param("fundCode") String fundCode,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
    
    /**
     * 查询最新净值
     */
    @Select("SELECT * FROM fund_nav WHERE fund_code = #{fundCode} " +
            "ORDER BY nav_date DESC LIMIT 1")
    FundNav selectLatestNav(String fundCode);
    
    /**
     * 查询前一交易日净值
     */
    @Select("SELECT * FROM fund_nav WHERE fund_code = #{fundCode} " +
            "AND nav_date < #{currentDate} " +
            "ORDER BY nav_date DESC LIMIT 1")
    FundNav selectPreviousNav(@Param("fundCode") String fundCode, @Param("currentDate") LocalDate currentDate);
}
