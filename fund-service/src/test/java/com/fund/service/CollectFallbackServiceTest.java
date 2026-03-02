package com.fund.service;

import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import com.fund.service.collect.CollectResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollectFallbackService 单元测试
 */
@SpringBootTest
class CollectFallbackServiceTest {

    @Autowired
    private CollectFallbackService collectFallbackService;

    @Test
    @DisplayName("测试服务注入成功")
    void testServiceInjection() {
        assertNotNull(collectFallbackService);
    }

    @Test
    @DisplayName("测试基金信息降级返回null")
    void testFallbackFundInfoReturnsNull() {
        String fundCode = "000001";
        Exception ex = new RuntimeException("Service unavailable");
        
        FundInfo result = collectFallbackService.fallbackFundInfo(fundCode, ex);
        
        assertNull(result);
    }

    @Test
    @DisplayName("测试基金指标降级返回null")
    void testFallbackFundMetricsReturnsNull() {
        String fundCode = "000001";
        Exception ex = new RuntimeException("Service unavailable");
        
        FundMetrics result = collectFallbackService.fallbackFundMetrics(fundCode, ex);
        
        assertNull(result);
    }

    @Test
    @DisplayName("测试NAV历史降级返回空列表")
    void testFallbackNavHistoryReturnsEmptyList() {
        String fundCode = "000001";
        Exception ex = new RuntimeException("Service unavailable");
        
        List<FundNav> result = collectFallbackService.fallbackNavHistory(fundCode, ex);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试构建降级结果")
    void testBuildFallbackResult() {
        String fundCode = "000001";
        String errorMessage = "Connection timeout";
        
        CollectResult<Object> result = collectFallbackService.buildFallbackResult(fundCode, errorMessage);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(CollectResult.ERR_SERVICE_ERROR, result.getErrorCode());
        assertTrue(result.getMessage().contains(errorMessage));
    }

    @Test
    @DisplayName("测试判断是否可降级")
    void testCanFallback() {
        assertTrue(collectFallbackService.canFallback("000001"));
        assertTrue(collectFallbackService.canFallback("999999"));
    }
}
