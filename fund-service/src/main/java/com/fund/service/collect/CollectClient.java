package com.fund.service.collect;

import com.fund.dto.ApiResponse;
import java.util.List;
import java.util.Map;

/**
 * 采集服务客户端接口
 * 调用Python采集服务
 */
public interface CollectClient {
    
    /**
     * 采集单只基金估值
     */
    ApiResponse<Map<String, Object>> collectEstimate(String fundCode);
    
    /**
     * 批量采集基金估值
     */
    ApiResponse<Map<String, Object>> collectBatch(List<String> fundCodes);
    
    /**
     * 健康检查
     */
    boolean healthCheck();
}
