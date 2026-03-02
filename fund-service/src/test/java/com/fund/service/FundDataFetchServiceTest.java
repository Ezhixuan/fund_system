package com.fund.service;

import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FundDataFetchService 单元测试
 */
@SpringBootTest
class FundDataFetchServiceTest {

    @Autowired
    private FundDataFetchService fundDataFetchService;

    @Test
    @DisplayName("测试服务注入成功")
    void testServiceInjection() {
        assertNotNull(fundDataFetchService);
    }

    @Test
    @DisplayName("测试获取基金信息方法存在")
    void testGetFundInfoMethod() {
        FundInfo result = fundDataFetchService.getFundInfo("000001");
        // 当前骨架实现返回null
        assertNull(result);
    }

    @Test
    @DisplayName("测试获取基金指标方法存在")
    void testGetFundMetricsMethod() {
        FundMetrics result = fundDataFetchService.getFundMetrics("000001");
        // 当前骨架实现返回null
        assertNull(result);
    }

    @Test
    @DisplayName("测试获取NAV历史方法存在")
    void testGetNavHistoryMethod() {
        List<FundNav> result = fundDataFetchService.getNavHistory("000001");
        // 当前骨架实现返回空列表
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试强制刷新方法存在")
    void testRefreshMethods() {
        // 验证方法存在且可调用
        assertDoesNotThrow(() -> {
            fundDataFetchService.refreshFundInfo("000001");
        });
        assertDoesNotThrow(() -> {
            fundDataFetchService.refreshFundMetrics("000001");
        });
        assertDoesNotThrow(() -> {
            fundDataFetchService.refreshNavHistory("000001");
        });
    }
}
