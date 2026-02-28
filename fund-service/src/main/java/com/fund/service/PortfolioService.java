package com.fund.service;

import com.fund.dto.HoldingVO;
import com.fund.dto.PortfolioAnalysis;
import com.fund.dto.TradeRequest;
import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.entity.PortfolioTrade;
import com.fund.mapper.FundInfoMapper;
import com.fund.mapper.FundMetricsMapper;
import com.fund.mapper.FundNavMapper;
import com.fund.mapper.PortfolioTradeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 持仓服务
 */
@Service
public class PortfolioService {
    
    private final PortfolioTradeMapper tradeMapper;
    private final FundInfoMapper fundInfoMapper;
    private final FundNavMapper fundNavMapper;
    private final FundMetricsMapper fundMetricsMapper;
    
    public PortfolioService(PortfolioTradeMapper tradeMapper,
                          FundInfoMapper fundInfoMapper,
                          FundNavMapper fundNavMapper,
                          FundMetricsMapper fundMetricsMapper) {
        this.tradeMapper = tradeMapper;
        this.fundInfoMapper = fundInfoMapper;
        this.fundNavMapper = fundNavMapper;
        this.fundMetricsMapper = fundMetricsMapper;
    }
    
    /**
     * 记录交易
     */
    @Transactional
    public void recordTrade(TradeRequest request) {
        // 验证基金代码是否存在
        FundInfo fundInfo = fundInfoMapper.selectById(request.getFundCode());
        if (fundInfo == null) {
            throw new IllegalArgumentException("基金代码不存在: " + request.getFundCode());
        }
        
        PortfolioTrade trade = new PortfolioTrade();
        trade.setFundCode(request.getFundCode());
        trade.setTradeDate(request.getTradeDate());
        trade.setTradeType(request.getTradeType());
        trade.setTradeShare(request.getTradeShare());
        trade.setTradePrice(request.getTradePrice());
        trade.setTradeFee(request.getTradeFee() != null ? request.getTradeFee() : BigDecimal.ZERO);
        
        // 计算交易金额
        BigDecimal amount = request.getTradeShare().multiply(request.getTradePrice());
        trade.setTradeAmount(amount);
        trade.setRemark(request.getRemark());
        
        tradeMapper.insert(trade);
    }
    
    /**
     * 获取所有持仓
     */
    public List<HoldingVO> getHoldings() {
        // 查询所有交易过的基金
        List<String> fundCodes = tradeMapper.selectList(null).stream()
                .map(PortfolioTrade::getFundCode)
                .distinct()
                .toList();
        
        List<HoldingVO> holdings = new ArrayList<>();
        
        for (String fundCode : fundCodes) {
            HoldingVO holding = calculateHolding(fundCode);
            if (holding != null && holding.getTotalShares().compareTo(BigDecimal.ZERO) > 0) {
                holdings.add(holding);
            }
        }
        
        return holdings;
    }
    
    /**
     * 计算单个基金持仓
     */
    public HoldingVO calculateHolding(String fundCode) {
        List<PortfolioTrade> trades = tradeMapper.selectByFundCode(fundCode);
        if (trades.isEmpty()) {
            return null;
        }
        
        BigDecimal totalShares = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (PortfolioTrade trade : trades) {
            if (trade.getTradeType() == 1) { // 买入
                totalShares = totalShares.add(trade.getTradeShare());
                BigDecimal cost = trade.getTradeAmount().add(trade.getTradeFee() != null ? trade.getTradeFee() : BigDecimal.ZERO);
                totalCost = totalCost.add(cost);
            } else if (trade.getTradeType() == 2) { // 卖出
                totalShares = totalShares.subtract(trade.getTradeShare());
            }
        }
        
        if (totalShares.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        
        // 平均成本
        BigDecimal avgCost = totalCost.divide(totalShares, 4, RoundingMode.HALF_UP);
        
        // 当前净值
        FundNav latestNav = fundNavMapper.selectLatestNav(fundCode);
        BigDecimal currentNav = latestNav != null ? latestNav.getUnitNav() : avgCost;
        
        // 当前市值
        BigDecimal currentValue = currentNav.multiply(totalShares);
        
        // 收益
        BigDecimal totalReturn = currentValue.subtract(totalCost);
        BigDecimal returnRate = totalCost.compareTo(BigDecimal.ZERO) > 0 
                ? totalReturn.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        HoldingVO holding = new HoldingVO();
        holding.setFundCode(fundCode);
        
        FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
        if (fundInfo != null) {
            holding.setFundName(fundInfo.getFundName());
        }
        
        holding.setTotalShares(totalShares);
        holding.setAvgCost(avgCost);
        holding.setTotalCost(totalCost);
        holding.setCurrentNav(currentNav);
        holding.setCurrentValue(currentValue);
        holding.setTotalReturn(totalReturn);
        holding.setReturnRate(returnRate);
        
        // 质量等级
        FundMetrics metrics = fundMetricsMapper.selectLatestByFundCode(fundCode);
        if (metrics != null && metrics.getSharpeRatio1y() != null) {
            BigDecimal sharpe = metrics.getSharpeRatio1y();
            if (sharpe.compareTo(new BigDecimal("2")) >= 0) holding.setQualityLevel("S");
            else if (sharpe.compareTo(new BigDecimal("1.5")) >= 0) holding.setQualityLevel("A");
            else if (sharpe.compareTo(BigDecimal.ONE) >= 0) holding.setQualityLevel("B");
            else if (sharpe.compareTo(new BigDecimal("0.5")) >= 0) holding.setQualityLevel("C");
            else holding.setQualityLevel("D");
        }
        
        return holding;
    }
    
    /**
     * 组合分析
     */
    public PortfolioAnalysis analyze() {
        List<HoldingVO> holdings = getHoldings();
        
        PortfolioAnalysis analysis = new PortfolioAnalysis();
        analysis.setHoldingCount(holdings.size());
        
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        
        Map<String, Integer> riskDist = new HashMap<>();
        Map<String, Integer> typeDist = new HashMap<>();
        Map<String, Integer> qualityDist = new HashMap<>();
        
        for (HoldingVO holding : holdings) {
            totalCost = totalCost.add(holding.getTotalCost());
            totalValue = totalValue.add(holding.getCurrentValue());
            
            // 质量分布
            String quality = holding.getQualityLevel() != null ? holding.getQualityLevel() : "-";
            qualityDist.merge(quality, 1, Integer::sum);
            
            // 类型和风险分布（简化）
            FundInfo info = fundInfoMapper.selectById(holding.getFundCode());
            if (info != null) {
                String type = info.getFundType() != null ? info.getFundType() : "未知";
                typeDist.merge(type, 1, Integer::sum);
                
                String risk = info.getRiskLevel() != null ? "R" + info.getRiskLevel() : "未知";
                riskDist.merge(risk, 1, Integer::sum);
            }
        }
        
        analysis.setTotalCost(totalCost);
        analysis.setTotalValue(totalValue);
        
        BigDecimal totalReturn = totalValue.subtract(totalCost);
        BigDecimal returnRate = totalCost.compareTo(BigDecimal.ZERO) > 0 
                ? totalReturn.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        analysis.setTotalReturn(totalReturn);
        analysis.setTotalReturnRate(returnRate);
        analysis.setRiskDistribution(riskDist);
        analysis.setTypeDistribution(typeDist);
        analysis.setQualityDistribution(qualityDist);
        
        return analysis;
    }
}
