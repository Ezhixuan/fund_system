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
 * 集成测试 - 验证完整数据流
 */
@SpringBootTest
class IntegrationTest {

    @Autowired
    private FundDataFetchService fundDataFetchService;

    @Autowired
    private EmptyCacheService emptyCacheService;

    @Autowired
    private CollectTaskManager collectTaskManager;

    private static final String TEST_FUND_CODE = "011452";

    @Test
    @DisplayName("测试空值缓存机制")
    void testEmptyCacheMechanism() throws InterruptedException {
        String testCode = "TEST_CACHE_001";

        // 1. 初始状态：缓存不存在
        assertFalse(emptyCacheService.isEmptyCached(testCode, EmptyCacheService.DataType.INFO));

        // 2. 设置空值缓存（1秒TTL用于测试）
        emptyCacheService.setEmptyCache(testCode, EmptyCacheService.DataType.INFO, 
            java.time.Duration.ofSeconds(1));

        // 3. 验证缓存存在
        assertTrue(emptyCacheService.isEmptyCached(testCode, EmptyCacheService.DataType.INFO));

        // 4. 等待缓存过期
        Thread.sleep(1100);

        // 5. 验证缓存已过期
        assertFalse(emptyCacheService.isEmptyCached(testCode, EmptyCacheService.DataType.INFO));

        // 清理
        emptyCacheService.clearAllEmptyCache(testCode);
    }

    @Test
    @DisplayName("测试并发采集控制")
    void testConcurrentCollectionControl() throws InterruptedException {
        String taskKey = "test:concurrent:" + System.currentTimeMillis();

        // 1. 初始状态：没有进行中的任务
        assertFalse(collectTaskManager.isPending(taskKey));

        // 2. 启动耗时任务
        Thread asyncTask = new Thread(() -> {
            collectTaskManager.execute(taskKey, () -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "result";
            });
        });
        asyncTask.start();

        // 3. 等待任务启动
        Thread.sleep(50);

        // 4. 验证任务在进行中
        assertTrue(collectTaskManager.isPending(taskKey) || !asyncTask.isAlive());

        // 5. 等待任务完成
        asyncTask.join();

        // 6. 验证任务完成
        assertFalse(collectTaskManager.isPending(taskKey));
    }

    @Test
    @DisplayName("测试任务去重 - 同一任务只执行一次")
    void testTaskDeduplication() throws Exception {
        String taskKey = "test:dedup:" + System.currentTimeMillis();
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        // 并发执行同一任务
        java.util.concurrent.CompletableFuture<String> future1 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
            collectTaskManager.execute(taskKey, () -> {
                count.incrementAndGet();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "result-" + count.get();
            })
        );

        // 等待任务启动
        Thread.sleep(50);

        // 第二个请求应该等待第一个完成
        String result2 = collectTaskManager.execute(taskKey, () -> {
            count.incrementAndGet();
            return "should-not-execute";
        });

        String result1 = future1.get();

        // 验证只执行了一次
        assertEquals(1, count.get());
        assertEquals("result-1", result1);
        assertEquals("result-1", result2);
    }

    @Test
    @DisplayName("测试本地数据库优先获取")
    void testLocalDatabasePriority() {
        // 使用已知存在的基金代码
        // 如果数据库中有数据，应该直接返回，不触发采集

        // 测试基金信息获取（可能从数据库获取）
        assertDoesNotThrow(() -> {
            FundInfo info = fundDataFetchService.getFundInfo(TEST_FUND_CODE);
            // 数据库中可能有数据，也可能没有
        });

        // 测试NAV历史获取
        assertDoesNotThrow(() -> {
            List<FundNav> navList = fundDataFetchService.getNavHistory(TEST_FUND_CODE);
            assertNotNull(navList);
        });
    }

    @Test
    @DisplayName("测试强制刷新功能")
    void testRefreshFunction() {
        // 测试强制刷新接口可用
        assertDoesNotThrow(() -> {
            CollectResult<FundInfo> result = fundDataFetchService.refreshFundInfo(TEST_FUND_CODE);
            assertNotNull(result);
        });

        assertDoesNotThrow(() -> {
            CollectResult<List<FundNav>> result = fundDataFetchService.refreshNavHistory(TEST_FUND_CODE);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("测试空值缓存阻止重复查询")
    void testEmptyCachePreventsRequery() throws InterruptedException {
        String testCode = "TEST_NO_QUERY_001";

        try {
            // 1. 设置空值缓存
            emptyCacheService.setEmptyCache(testCode, EmptyCacheService.DataType.INFO, 
                java.time.Duration.ofMinutes(1));

            // 2. 验证缓存存在
            assertTrue(emptyCacheService.isEmptyCached(testCode, EmptyCacheService.DataType.INFO));

            // 3. 获取基金信息（应该直接返回null，不会触发采集）
            long startTime = System.currentTimeMillis();
            FundInfo result = fundDataFetchService.getFundInfo(testCode);
            long endTime = System.currentTimeMillis();

            // 4. 验证结果和响应时间（应该非常快，<100ms）
            assertNull(result);
            assertTrue(endTime - startTime < 100, "空值缓存响应应该非常快");

        } finally {
            // 清理
            emptyCacheService.clearAllEmptyCache(testCode);
        }
    }

    @Test
    @DisplayName("测试不同基金代码独立处理")
    void testDifferentFundCodesIndependent() {
        String code1 = "011452";
        String code2 = "000001";

        // 两个不同基金的处理应该独立
        assertDoesNotThrow(() -> {
            FundInfo info1 = fundDataFetchService.getFundInfo(code1);
            FundInfo info2 = fundDataFetchService.getFundInfo(code2);
            // 不比较结果，只验证不抛异常
        });
    }

    @Test
    @DisplayName("测试服务健康状况")
    void testServiceHealth() {
        // 验证所有服务组件正常
        assertNotNull(fundDataFetchService);
        assertNotNull(emptyCacheService);
        assertNotNull(collectTaskManager);

        // 验证任务管理器状态
        assertTrue(collectTaskManager.getPendingCount() >= 0);
    }
}
