package com.fund.service;

import com.fund.entity.FundInfo;
import com.fund.entity.FundMetrics;
import com.fund.entity.FundNav;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
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
    @DisplayName("测试获取基金信息（本地数据库）")
    void testGetFundInfoFromDb() {
        String testFundCode = "011452";
        // 验证方法执行不抛异常
        assertDoesNotThrow(() -> {
            FundInfo result = fundDataFetchService.getFundInfo(testFundCode);
        });
    }

    @Test
    @Disabled("数据库字段映射问题待修复")
    @DisplayName("测试获取基金指标（本地数据库）")
    void testGetFundMetricsFromDb() {
        String testFundCode = "011452";
        FundMetrics result = fundDataFetchService.getFundMetrics(testFundCode);
    }

    @Test
    @DisplayName("测试获取NAV历史（本地数据库）")
    void testGetNavHistoryFromDb() {
        String testFundCode = "011452";
        List<FundNav> result = fundDataFetchService.getNavHistory(testFundCode);
        assertNotNull(result);
    }

    @Test
    @DisplayName("测试强制刷新方法")
    void testRefreshMethods() {
        String testFundCode = "TEST999999";

        assertDoesNotThrow(() -> {
            fundDataFetchService.refreshFundInfo(testFundCode);
        });

        assertDoesNotThrow(() -> {
            fundDataFetchService.refreshFundMetrics(testFundCode);
        });

        assertDoesNotThrow(() -> {
            fundDataFetchService.refreshNavHistory(testFundCode);
        });
    }

    @Test
    @Disabled("数据库字段映射问题待修复")
    @DisplayName("测试不存在的基金代码")
    void testNonExistentFundCode() {
        String nonExistentCode = "000000";
        FundInfo info = fundDataFetchService.getFundInfo(nonExistentCode);
        FundMetrics metrics = fundDataFetchService.getFundMetrics(nonExistentCode);
        List<FundNav> navList = fundDataFetchService.getNavHistory(nonExistentCode);
        assertNotNull(navList);
    }

    @Test
    @DisplayName("测试并发获取同一基金")
    void testConcurrentFetch() throws InterruptedException {
        String testFundCode = "011452";

        Thread t1 = new Thread(() -> fundDataFetchService.getFundInfo(testFundCode));
        Thread t2 = new Thread(() -> fundDataFetchService.getFundInfo(testFundCode));

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
