package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.PortfolioTrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 交易记录Mapper
 */
@Mapper
public interface PortfolioTradeMapper extends BaseMapper<PortfolioTrade> {
    
    /**
     * 查询基金的所有交易记录
     */
    @Select("SELECT * FROM portfolio_trade WHERE fund_code = #{fundCode} ORDER BY trade_date ASC")
    List<PortfolioTrade> selectByFundCode(String fundCode);
}
