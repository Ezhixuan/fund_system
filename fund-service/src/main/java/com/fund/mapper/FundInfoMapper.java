package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 基金基础信息Mapper
 */
@Mapper
public interface FundInfoMapper extends BaseMapper<FundInfo> {
    
    /**
     * 根据拼音首字母搜索
     */
    @Select("SELECT * FROM fund_info WHERE name_pinyin LIKE CONCAT(#{pinyin}, '%') AND status = 1")
    List<FundInfo> selectByPinyin(String pinyin);
    
    /**
     * 模糊搜索基金名称或代码
     */
    @Select("SELECT * FROM fund_info WHERE (fund_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR fund_code LIKE CONCAT('%', #{keyword}, '%')) AND status = 1")
    List<FundInfo> searchByKeyword(String keyword);
}
