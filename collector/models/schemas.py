"""
Pydantic数据模型
"""
from datetime import date, datetime
from typing import Optional
from pydantic import BaseModel, Field


class FundInfo(BaseModel):
    """基金基础信息模型"""
    fund_code: str = Field(..., max_length=10, description="基金代码")
    fund_name: str = Field(..., max_length=100, description="基金名称")
    name_pinyin: Optional[str] = Field(None, max_length=100, description="拼音首字母")
    fund_type: Optional[str] = Field(None, max_length=20, description="类型")
    invest_style: Optional[str] = Field(None, max_length=20, description="投资风格")
    manager_code: Optional[str] = Field(None, max_length=20, description="基金经理代码")
    manager_name: Optional[str] = Field(None, max_length=50, description="基金经理姓名")
    company_code: Optional[str] = Field(None, max_length=20, description="基金公司代码")
    company_name: Optional[str] = Field(None, max_length=100, description="基金公司名称")
    establish_date: Optional[date] = Field(None, description="成立日期")
    benchmark: Optional[str] = Field(None, max_length=200, description="业绩比较基准")
    management_fee: Optional[float] = Field(None, description="管理费率")
    custody_fee: Optional[float] = Field(None, description="托管费率")
    risk_level: Optional[int] = Field(None, ge=1, le=5, description="风险等级：1-5")
    current_scale: Optional[float] = Field(None, description="最新规模（亿元）")
    status: int = Field(default=1, description="状态")


class FundNav(BaseModel):
    """基金净值模型"""
    fund_code: str = Field(..., max_length=10, description="基金代码")
    nav_date: date = Field(..., description="净值日期")
    unit_nav: float = Field(..., description="单位净值")
    accum_nav: Optional[float] = Field(None, description="累计净值")
    adjust_nav: Optional[float] = Field(None, description="复权净值")
    daily_return: Optional[float] = Field(None, description="日增长率%")
    source: str = Field(default="akshare", max_length=20, description="数据来源")


class FundHolding(BaseModel):
    """基金持仓模型"""
    fund_code: str = Field(..., max_length=10, description="基金代码")
    report_date: date = Field(..., description="报告期")
    stock_code: Optional[str] = Field(None, max_length=10, description="股票代码")
    stock_name: Optional[str] = Field(None, max_length=100, description="股票名称")
    holding_amount: Optional[int] = Field(None, description="持股数量(股)")
    holding_value: Optional[float] = Field(None, description="持股市值(元)")
    holding_ratio: Optional[float] = Field(None, description="占净值比例%")
    holding_type: Optional[str] = Field(None, max_length=20, description="持仓类型")
    is_top10: int = Field(default=0, description="是否前十大重仓")


class DataUpdateLog(BaseModel):
    """数据更新日志模型"""
    table_name: str = Field(..., max_length=50, description="更新的表名")
    update_date: date = Field(..., description="数据日期")
    record_count: Optional[int] = Field(None, description="更新记录数")
    status: Optional[str] = Field(None, max_length=20, description="状态")
    error_msg: Optional[str] = Field(None, description="错误信息")
    start_time: Optional[datetime] = Field(None, description="开始时间")
    end_time: Optional[datetime] = Field(None, description="结束时间")
    duration_seconds: Optional[int] = Field(None, description="耗时(秒)")
