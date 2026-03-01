package com.ezhixuan.fund.domain.entity.watchlist;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实时估值点位
 */
@Data
@TableName("fund_estimate_intraday")
public class FundEstimateIntraday {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 基金代码
     */
    private String fundCode;
    
    /**
     * 采集时间（每10分钟）
     */
    private LocalDateTime estimateTime;
    
    /**
     * 预估净值
     */
    private BigDecimal estimateNav;
    
    /**
     * 预估涨跌幅%（当日点位）
     */
    private BigDecimal estimateChangePct;
    
    /**
     * 预估涨跌额
     */
    private BigDecimal estimateChangeAmt;
    
    /**
     * 昨日收盘净值
     */
    private BigDecimal preCloseNav;
    
    /**
     * 交易日期
     */
    private LocalDate tradeDate;
    
    /**
     * 数据来源: akshare/eastmoney/danjuan
     */
    private String dataSource;
    
    /**
     * 是否为交易时间采集: 0-否, 1-是
     */
    private Integer isTradingTime;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
