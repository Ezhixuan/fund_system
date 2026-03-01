package com.ezhixuan.fund.domain.entity.watchlist;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户关注列表
 */
@Data
@TableName("user_watchlist")
public class UserWatchlist {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 基金代码
     */
    private String fundCode;
    
    /**
     * 基金名称（缓存）
     */
    private String fundName;
    
    /**
     * 添加日期
     */
    private LocalDate addDate;
    
    /**
     * 关注类型: 1-持有, 2-关注
     */
    private Integer watchType;
    
    /**
     * 目标收益率%
     */
    private BigDecimal targetReturn;
    
    /**
     * 止损线%
     */
    private BigDecimal stopLoss;
    
    /**
     * 备注
     */
    private String notes;
    
    /**
     * 排序权重
     */
    private Integer sortOrder;
    
    /**
     * 是否启用: 0-停用, 1-启用
     */
    private Integer isActive;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
