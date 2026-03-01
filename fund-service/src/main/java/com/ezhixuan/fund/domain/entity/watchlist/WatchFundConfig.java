package com.ezhixuan.fund.domain.entity.watchlist;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 关注基金采集配置
 */
@Data
@TableName("watch_fund_config")
public class WatchFundConfig {
    
    /**
     * 基金代码
     */
    @TableId(type = IdType.INPUT)
    private String fundCode;
    
    /**
     * 是否需要详细信息: 0-否, 1-是
     */
    private Integer needDetail;
    
    /**
     * 是否需要净值: 0-否, 1-是
     */
    private Integer needNav;
    
    /**
     * 是否需要实时估值: 0-否, 1-是
     */
    private Integer needIntraday;
    
    /**
     * 是否需要持仓: 0-否, 1-是
     */
    private Integer needPortfolio;
    
    /**
     * 最后采集日期
     */
    private LocalDate lastCollectDate;
    
    /**
     * 最后估值采集时间
     */
    private LocalDateTime lastIntradayTime;
    
    /**
     * 估值采集间隔(分钟)
     */
    private Integer collectIntervalMinutes;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
