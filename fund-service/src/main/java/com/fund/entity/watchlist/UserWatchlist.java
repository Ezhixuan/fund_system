package com.fund.entity.watchlist;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("user_watchlist")
public class UserWatchlist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private String fundName;
    private LocalDate addDate;
    private Integer watchType;
    private BigDecimal targetReturn;
    private BigDecimal stopLoss;
    private String notes;
    private Integer sortOrder;
    private Integer isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    public LocalDate getAddDate() { return addDate; }
    public void setAddDate(LocalDate addDate) { this.addDate = addDate; }
    public Integer getWatchType() { return watchType; }
    public void setWatchType(Integer watchType) { this.watchType = watchType; }
    public BigDecimal getTargetReturn() { return targetReturn; }
    public void setTargetReturn(BigDecimal targetReturn) { this.targetReturn = targetReturn; }
    public BigDecimal getStopLoss() { return stopLoss; }
    public void setStopLoss(BigDecimal stopLoss) { this.stopLoss = stopLoss; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
