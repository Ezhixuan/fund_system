package com.fund.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fund.dto.TradeSignal;
import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.mapper.FundInfoMapper;
import com.fund.mapper.FundMetricsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 决策信号引擎
 * 双轨决策：规则引擎 + 评分模型
 */
@Service
public class SignalEngine {
    
    private static final Logger log = LoggerFactory.getLogger(SignalEngine.class);
    
    private final FundMetricsMapper metricsMapper;
    private final FundInfoMapper fundInfoMapper;
    
    public SignalEngine(FundMetricsMapper metricsMapper, FundInfoMapper fundInfoMapper) {
        this.metricsMapper = metricsMapper;
        this.fundInfoMapper = fundInfoMapper;
    }
    
    /**
     * 生成交易信号
     */
    @Cacheable(value = "fund:signal", key = "#fundCode + '_v2'", unless = "#result == null")
    public TradeSignal generateSignal(String fundCode) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取指标数据 - 直接查询数据库
            FundMetrics metrics = metricsMapper.selectLatestByFundCode(fundCode);
            
            System.out.println("[DEBUG] Signal查询: fundCode=" + fundCode + ", found=" + (metrics != null));
            
            if (metrics == null) {
                System.out.println("[DEBUG] 未找到指标数据");
                return TradeSignal.hold("暂无指标数据");
            }
            
            System.out.println("[DEBUG] 指标数据: sharpe=" + metrics.getSharpeRatio1y() + 
                ", return1y=" + metrics.getReturn1y());
            
            // 获取基金信息
            FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
            
            // 执行双轨决策
            SignalResult result = evaluateRules(metrics);
            
            // 评分模型干预
            String qualityLevel = calculateQualityLevel(metrics);
            result.qualityLevel = qualityLevel;
            
            // 综合决策
            TradeSignal signal = makeDecision(result, qualityLevel);
            
            // 补充信息
            signal.setFundCode(fundCode);
            if (fundInfo != null) {
                signal.setFundName(fundInfo.getFundName());
            }
            signal.setQualityLevel(qualityLevel);
            signal.setBuyScore(result.buyScore);
            signal.setSellScore(result.sellScore);
            signal.setConfidence(calculateConfidence(result, qualityLevel));
            
            long cost = System.currentTimeMillis() - startTime;
            log.debug("生成信号耗时: {}ms, fundCode={}", cost, fundCode);
            
            return signal;
            
        } catch (Exception e) {
            log.error("生成信号失败: {}", fundCode, e);
            return TradeSignal.hold("系统错误");
        }
    }
    
    /**
     * 规则引擎评估
     */
    private SignalResult evaluateRules(FundMetrics m) {
        SignalResult result = new SignalResult();
        List<String> buyReasons = new ArrayList<>();
        List<String> sellReasons = new ArrayList<>();
        
        // ========== 买入规则 ==========
        
        // 1. 估值低位 (PE分位 < 30)
        if (m.getPePercentile() != null && m.getPePercentile() < 30) {
            result.buyScore += 3;
            buyReasons.add("PE估值低位(" + m.getPePercentile() + "%)");
        }
        
        // 2. 夏普比率优秀 (>1.5)
        if (m.getSharpeRatio1y() != null && m.getSharpeRatio1y().compareTo(new BigDecimal("1.5")) > 0) {
            result.buyScore += 3;
            buyReasons.add("夏普比率优秀(" + m.getSharpeRatio1y() + ")");
        } else if (m.getSharpeRatio1y() != null && m.getSharpeRatio1y().compareTo(BigDecimal.ONE) > 0) {
            result.buyScore += 2;
            buyReasons.add("夏普比率良好");
        }
        
        // 3. 回撤控制良好 (<20%)
        if (m.getMaxDrawdown1y() != null && m.getMaxDrawdown1y().compareTo(new BigDecimal("-20")) > 0) {
            result.buyScore += 2;
            buyReasons.add("回撤控制良好(<20%)");
        }
        
        // 4. 近1年收益为正
        if (m.getReturn1y() != null && m.getReturn1y().compareTo(BigDecimal.ZERO) > 0) {
            result.buyScore += 1;
            buyReasons.add("近1年收益为正");
        }
        
        // ========== 卖出规则 ==========
        
        // 1. 估值高位 (PE分位 > 80)
        if (m.getPePercentile() != null && m.getPePercentile() > 80) {
            result.sellScore += 3;
            sellReasons.add("PE估值高位(" + m.getPePercentile() + "%)");
        }
        
        // 2. 回撤过大 (>30%)
        if (m.getMaxDrawdown1y() != null && m.getMaxDrawdown1y().compareTo(new BigDecimal("-30")) < 0) {
            result.sellScore += 3;
            sellReasons.add("回撤过大(>30%)");
        }
        
        // 3. 夏普比率差 (<0)
        if (m.getSharpeRatio1y() != null && m.getSharpeRatio1y().compareTo(BigDecimal.ZERO) < 0) {
            result.sellScore += 2;
            sellReasons.add("夏普比率负值");
        }
        
        // 4. 近1年收益过高 (止盈)
        if (m.getReturn1y() != null && m.getReturn1y().compareTo(new BigDecimal("25")) > 0) {
            result.sellScore += 2;
            sellReasons.add("收益超25%，考虑止盈");
        }
        
        // 5. 近1年亏损过大
        if (m.getReturn1y() != null && m.getReturn1y().compareTo(new BigDecimal("-20")) < 0) {
            result.sellScore += 2;
            sellReasons.add("亏损超20%，考虑止损");
        }
        
        result.buyReasons = buyReasons;
        result.sellReasons = sellReasons;
        return result;
    }
    
    /**
     * 综合决策
     */
    private TradeSignal makeDecision(SignalResult result, String qualityLevel) {
        // 评分模型干预
        if ("D".equals(qualityLevel) && result.buyScore > 0) {
            return TradeSignal.hold("综合评级D，暂不建议买入");
        }
        
        // 强烈买入信号 (买入分高 + 评级优良)
        if (result.buyScore >= 5 && ("S".equals(qualityLevel) || "A".equals(qualityLevel))) {
            return TradeSignal.buy(String.join("; ", result.buyReasons));
        }
        
        // 一般买入信号
        if (result.buyScore >= 6) {
            return TradeSignal.buy(String.join("; ", result.buyReasons));
        }
        
        // 强烈卖出信号
        if (result.sellScore >= 5) {
            return TradeSignal.sell(String.join("; ", result.sellReasons));
        }
        
        // 默认持有
        if (result.buyScore > result.sellScore) {
            return TradeSignal.hold("偏向积极，可继续持有");
        } else if (result.sellScore > result.buyScore) {
            return TradeSignal.hold("偏向谨慎，注意风险");
        }
        
        return TradeSignal.hold("估值合理，建议持有观望");
    }
    
    /**
     * 计算质量等级 (简化版，基于夏普)
     */
    private String calculateQualityLevel(FundMetrics m) {
        if (m.getSharpeRatio1y() == null) {
            return "-";
        }
        BigDecimal sharpe = m.getSharpeRatio1y();
        if (sharpe.compareTo(new BigDecimal("2")) >= 0) return "S";
        if (sharpe.compareTo(new BigDecimal("1.5")) >= 0) return "A";
        if (sharpe.compareTo(BigDecimal.ONE) >= 0) return "B";
        if (sharpe.compareTo(new BigDecimal("0.5")) >= 0) return "C";
        return "D";
    }
    
    /**
     * 计算置信度
     */
    private Integer calculateConfidence(SignalResult result, String qualityLevel) {
        int score = 50;
        
        // 规则得分差
        score += Math.abs(result.buyScore - result.sellScore) * 5;
        
        // 评级加成
        if ("S".equals(qualityLevel)) score += 20;
        else if ("A".equals(qualityLevel)) score += 15;
        else if ("B".equals(qualityLevel)) score += 5;
        else if ("D".equals(qualityLevel)) score -= 10;
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * 信号结果内部类
     */
    private static class SignalResult {
        int buyScore = 0;
        int sellScore = 0;
        List<String> buyReasons = new ArrayList<>();
        List<String> sellReasons = new ArrayList<>();
        String qualityLevel;
    }
}
