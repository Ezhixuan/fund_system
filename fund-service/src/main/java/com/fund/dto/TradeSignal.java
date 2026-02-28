package com.fund.dto;

import java.time.LocalDateTime;

/**
 * 交易信号VO
 */
public class TradeSignal {
    
    /**
     * 信号类型: BUY/HOLD/SELL
     */
    public enum SignalType {
        BUY("买入"),
        HOLD("持有"),
        SELL("卖出");
        
        private final String label;
        
        SignalType(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    private String fundCode;
    private String fundName;
    private SignalType signal;
    private String reason;
    private Integer confidence; // 置信度 0-100
    private LocalDateTime generateTime;
    
    // 规则得分
    private Integer buyScore;
    private Integer sellScore;
    private String qualityLevel;
    
    public static TradeSignal buy(String reason) {
        TradeSignal ts = new TradeSignal();
        ts.setSignal(SignalType.BUY);
        ts.setReason(reason);
        ts.setGenerateTime(LocalDateTime.now());
        return ts;
    }
    
    public static TradeSignal hold(String reason) {
        TradeSignal ts = new TradeSignal();
        ts.setSignal(SignalType.HOLD);
        ts.setReason(reason);
        ts.setGenerateTime(LocalDateTime.now());
        return ts;
    }
    
    public static TradeSignal sell(String reason) {
        TradeSignal ts = new TradeSignal();
        ts.setSignal(SignalType.SELL);
        ts.setReason(reason);
        ts.setGenerateTime(LocalDateTime.now());
        return ts;
    }
    
    // Getters and Setters
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    
    public SignalType getSignal() { return signal; }
    public void setSignal(SignalType signal) { this.signal = signal; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }
    
    public LocalDateTime getGenerateTime() { return generateTime; }
    public void setGenerateTime(LocalDateTime generateTime) { this.generateTime = generateTime; }
    
    public Integer getBuyScore() { return buyScore; }
    public void setBuyScore(Integer buyScore) { this.buyScore = buyScore; }
    
    public Integer getSellScore() { return sellScore; }
    public void setSellScore(Integer sellScore) { this.sellScore = sellScore; }
    
    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }
}
