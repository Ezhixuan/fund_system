package com.fund.service;

import com.fund.dto.FundInfoVO;
import com.fund.dto.FundMetricsVO;
import com.fund.dto.FundNavVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDate;
import java.util.List;

/**
 * 基金服务接口
 */
public interface FundService {
    
    /**
     * 分页查询基金列表
     */
    IPage<FundInfoVO> listFunds(Integer page, Integer size, 
                              String fundType, Integer riskLevel, String keyword);
    
    /**
     * 获取基金详情
     */
    FundInfoVO getFundDetail(String fundCode);
    
    /**
     * 搜索基金建议
     */
    List<FundInfoVO> searchSuggest(String keyword, Integer limit);
    
    /**
     * 获取最新指标
     */
    FundMetricsVO getLatestMetrics(String fundCode);
    
    /**
     * 获取净值历史
     */
    List<FundNavVO> getNavHistory(String fundCode, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取近期净值
     */
    List<FundNavVO> getRecentNav(String fundCode, Integer days);
}
