package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.dto.FundInfoVO;
import com.fund.dto.FundMetricsVO;
import com.fund.dto.FundNavVO;
import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.mapper.FundInfoMapper;
import com.fund.mapper.FundMetricsMapper;
import com.fund.mapper.FundNavMapper;
import com.fund.service.FundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基金服务实现
 */
@Service
public class FundServiceImpl implements FundService {
    
    private static final Logger log = LoggerFactory.getLogger(FundServiceImpl.class);
    
    private final FundInfoMapper fundInfoMapper;
    private final FundMetricsMapper fundMetricsMapper;
    private final FundNavMapper fundNavMapper;
    
    public FundServiceImpl(FundInfoMapper fundInfoMapper, 
                          FundMetricsMapper fundMetricsMapper,
                          FundNavMapper fundNavMapper) {
        this.fundInfoMapper = fundInfoMapper;
        this.fundMetricsMapper = fundMetricsMapper;
        this.fundNavMapper = fundNavMapper;
    }
    
    @Override
    public IPage<FundInfoVO> listFunds(Integer page, Integer size, 
                                     String fundType, Integer riskLevel, String keyword) {
        Page<FundInfo> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FundInfo> wrapper = new LambdaQueryWrapper<>();
        
        // 筛选条件 - 使用模糊匹配
        if (StringUtils.hasText(fundType)) {
            wrapper.like(FundInfo::getFundType, fundType);
        }
        if (riskLevel != null) {
            wrapper.eq(FundInfo::getRiskLevel, riskLevel);
        }
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            String kw = "%" + keyword + "%";
            wrapper.and(w -> w.like(FundInfo::getFundName, kw)
                              .or()
                              .like(FundInfo::getFundCode, kw)
                              .or()
                              .like(FundInfo::getNamePinyin, kw));
        }
        
        // 只查询状态正常的基金
        wrapper.eq(FundInfo::getStatus, 1);
        wrapper.orderByDesc(FundInfo::getCurrentScale);
        
        IPage<FundInfo> entityPage = fundInfoMapper.selectPage(pageParam, wrapper);
        
        // 转换VO
        return entityPage.convert(this::convertToFundInfoVO);
    }
    
    @Override
    @Cacheable(value = "fund:detail", key = "#fundCode + '_v2'", unless = "#result == null")
    public FundInfoVO getFundDetail(String fundCode) {
        FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
        if (fundInfo == null) {
            return null;
        }
        return convertToFundInfoVO(fundInfo);
    }
    
    @Override
    @Cacheable(value = "fund:search", key = "#keyword + '-' + #limit", unless = "#result == null or #result.isEmpty()")
    public List<FundInfoVO> searchSuggest(String keyword, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        
        String kw = "%" + keyword + "%";
        LambdaQueryWrapper<FundInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(FundInfo::getFundName, kw)
                          .or()
                          .like(FundInfo::getFundCode, kw)
                          .or()
                          .like(FundInfo::getNamePinyin, kw))
               .eq(FundInfo::getStatus, 1)
               .orderByDesc(FundInfo::getCurrentScale)
               .last("LIMIT " + limit);
        
        List<FundInfo> list = fundInfoMapper.selectList(wrapper);
        return list.stream().map(this::convertToFundInfoVO).collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "fund:metrics", key = "#fundCode", unless = "#result == null")
    public FundMetricsVO getLatestMetrics(String fundCode) {
        FundMetrics metrics = fundMetricsMapper.selectLatestByFundCode(fundCode);
        if (metrics == null) {
            return null;
        }
        return convertToFundMetricsVO(metrics);
    }
    
    @Override
    public List<FundNavVO> getNavHistory(String fundCode, LocalDate startDate, LocalDate endDate) {
        List<FundNav> list = fundNavMapper.selectByDateRange(fundCode, startDate, endDate);
        return list.stream().map(this::convertToFundNavVO).collect(Collectors.toList());
    }
    
    @Override
    public List<FundNavVO> getRecentNav(String fundCode, Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return getNavHistory(fundCode, startDate, endDate);
    }
    
    private FundInfoVO convertToFundInfoVO(FundInfo entity) {
        FundInfoVO vo = new FundInfoVO();
        vo.setFundCode(entity.getFundCode());
        vo.setFundName(entity.getFundName());
        vo.setNamePinyin(entity.getNamePinyin());
        vo.setFundType(entity.getFundType());
        vo.setInvestStyle(entity.getInvestStyle());
        vo.setManagerName(entity.getManagerName());
        vo.setCompanyName(entity.getCompanyName());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setEstablishDate(entity.getEstablishDate());
        vo.setCurrentScale(entity.getCurrentScale());
        vo.setBenchmark(entity.getBenchmark());
        return vo;
    }
    
    private FundNavVO convertToFundNavVO(FundNav entity) {
        FundNavVO vo = new FundNavVO();
        vo.setNavDate(entity.getNavDate());
        vo.setUnitNav(entity.getUnitNav());
        vo.setAccumNav(entity.getAccumNav());
        vo.setDailyReturn(entity.getDailyReturn());
        return vo;
    }
    
    private String calculateQualityLevel(FundMetrics metrics) {
        if (metrics.getSharpeRatio1y() == null) {
            return "-";
        }
        BigDecimal sharpe = metrics.getSharpeRatio1y();
        if (sharpe.compareTo(new BigDecimal("2")) >= 0) return "S";
        if (sharpe.compareTo(new BigDecimal("1.5")) >= 0) return "A";
        if (sharpe.compareTo(new BigDecimal("1")) >= 0) return "B";
        if (sharpe.compareTo(new BigDecimal("0.5")) >= 0) return "C";
        return "D";
    }
    
    @Override
    @Cacheable(value = "fund:ranking", key = "#sortBy + '-' + #fundType + '-' + #limit")
    public List<FundMetricsVO> getTopFunds(String sortBy, String fundType, Integer limit) {
        // 限制最大数量
        if (limit > 50) limit = 50;
        if (limit < 1) limit = 10;
        
        // 验证排序字段
        String orderBy = switch (sortBy) {
            case "return1y" -> "return_1y";
            case "return3y" -> "return_3y";
            case "return1m" -> "return_1m";
            case "return3m" -> "return_3m";
            case "maxDrawdown" -> "max_drawdown_1y";
            case "sharpe" -> "sharpe_ratio_1y";
            default -> "sharpe_ratio_1y";
        };
        
        // 查询TOP基金
        List<FundMetrics> metricsList = fundMetricsMapper.selectTopByOrder(orderBy, fundType, limit);
        
        return metricsList.stream()
                .map(this::convertToFundMetricsVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<FundMetricsVO> compareFunds(List<String> fundCodes) {
        if (fundCodes == null || fundCodes.isEmpty()) {
            return List.of();
        }
        
        return fundCodes.stream()
                .map(this::getLatestMetrics)
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
    }
    
    @Override
    public IPage<FundInfoVO> filterFundsByMetrics(IPage<FundInfoVO> page, String fundType,
                                                   Double minSharpe, Double maxDrawdown) {
        // 先获取基金列表
        LambdaQueryWrapper<FundInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(fundType)) {
            wrapper.eq(FundInfo::getFundType, fundType);
        }
        wrapper.eq(FundInfo::getStatus, 1);
        
        Page<FundInfo> pageParam = new Page<>(page.getCurrent(), page.getSize());
        IPage<FundInfo> entityPage = fundInfoMapper.selectPage(pageParam, wrapper);
        
        // 过滤并转换
        List<FundInfoVO> filteredList = entityPage.getRecords().stream()
                .map(this::convertToFundInfoVO)
                .filter(vo -> {
                    FundMetricsVO metrics = getLatestMetrics(vo.getFundCode());
                    if (metrics == null) return false;
                    
                    if (minSharpe != null) {
                        if (metrics.getSharpeRatio1y() == null || 
                            metrics.getSharpeRatio1y().doubleValue() < minSharpe) {
                            return false;
                        }
                    }
                    
                    if (maxDrawdown != null) {
                        if (metrics.getMaxDrawdown1y() == null || 
                            metrics.getMaxDrawdown1y().doubleValue() > -maxDrawdown) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        page.setRecords(filteredList);
        page.setTotal(filteredList.size());
        return page;
    }
    
    private FundMetricsVO convertToFundMetricsVO(FundMetrics metrics) {
        FundMetricsVO vo = new FundMetricsVO();
        vo.setFundCode(metrics.getFundCode());
        vo.setCalcDate(metrics.getCalcDate());
        vo.setReturn1m(metrics.getReturn1m());
        vo.setReturn3m(metrics.getReturn3m());
        vo.setReturn1y(metrics.getReturn1y());
        vo.setReturn3y(metrics.getReturn3y());
        vo.setSharpeRatio1y(metrics.getSharpeRatio1y());
        vo.setMaxDrawdown1y(metrics.getMaxDrawdown1y());
        vo.setVolatility1y(metrics.getVolatility1y());
        
        // 获取基金名称
        FundInfo fundInfo = fundInfoMapper.selectById(metrics.getFundCode());
        if (fundInfo != null) {
            vo.setFundName(fundInfo.getFundName());
        }
        
        vo.setQualityLevel(calculateQualityLevel(metrics));
        return vo;
    }
}
