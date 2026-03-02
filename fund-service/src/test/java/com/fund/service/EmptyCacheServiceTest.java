package com.fund.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmptyCacheService 单元测试
 */
@SpringBootTest
class EmptyCacheServiceTest {

    @Autowired
    private EmptyCacheService emptyCacheService;

    @Test
    @DisplayName("测试服务注入成功")
    void testServiceInjection() {
        assertNotNull(emptyCacheService);
    }

    @Test
    @DisplayName("测试空值缓存完整流程")
    void testEmptyCacheFlow() throws InterruptedException {
        String testFundCode = "TEST999999";
        EmptyCacheService.DataType dataType = EmptyCacheService.DataType.INFO;

        try {
            // 1. 初始状态：缓存不存在
            assertFalse(emptyCacheService.isEmptyCached(testFundCode, dataType));

            // 2. 设置空值缓存（1秒TTL用于测试）
            emptyCacheService.setEmptyCache(testFundCode, dataType, Duration.ofSeconds(1));

            // 3. 验证缓存存在
            assertTrue(emptyCacheService.isEmptyCached(testFundCode, dataType));

            // 4. 验证TTL
            long ttl = emptyCacheService.getEmptyCacheTtl(testFundCode, dataType);
            assertTrue(ttl > 0 && ttl <= 1);

            // 5. 等待缓存过期
            Thread.sleep(1100);

            // 6. 验证缓存已过期
            assertFalse(emptyCacheService.isEmptyCached(testFundCode, dataType));
        } finally {
            // 清理
            emptyCacheService.clearEmptyCache(testFundCode, dataType);
        }
    }

    @Test
    @DisplayName("测试批量清除缓存")
    void testClearAllEmptyCache() {
        String testFundCode = "TEST888888";

        try {
            // 设置所有类型的空值缓存
            for (EmptyCacheService.DataType dataType : EmptyCacheService.DataType.values()) {
                emptyCacheService.setEmptyCache(testFundCode, dataType, Duration.ofMinutes(1));
                assertTrue(emptyCacheService.isEmptyCached(testFundCode, dataType));
            }

            // 批量清除
            emptyCacheService.clearAllEmptyCache(testFundCode);

            // 验证全部清除
            for (EmptyCacheService.DataType dataType : EmptyCacheService.DataType.values()) {
                assertFalse(emptyCacheService.isEmptyCached(testFundCode, dataType));
            }
        } finally {
            emptyCacheService.clearAllEmptyCache(testFundCode);
        }
    }

    @Test
    @DisplayName("测试三种数据类型")
    void testAllDataTypes() {
        String testFundCode = "TEST777777";

        try {
            // INFO类型
            emptyCacheService.setEmptyCache(testFundCode, EmptyCacheService.DataType.INFO, Duration.ofSeconds(1));
            assertTrue(emptyCacheService.isEmptyCached(testFundCode, EmptyCacheService.DataType.INFO));

            // METRICS类型
            emptyCacheService.setEmptyCache(testFundCode, EmptyCacheService.DataType.METRICS, Duration.ofSeconds(1));
            assertTrue(emptyCacheService.isEmptyCached(testFundCode, EmptyCacheService.DataType.METRICS));

            // NAV类型
            emptyCacheService.setEmptyCache(testFundCode, EmptyCacheService.DataType.NAV, Duration.ofSeconds(1));
            assertTrue(emptyCacheService.isEmptyCached(testFundCode, EmptyCacheService.DataType.NAV));

        } finally {
            emptyCacheService.clearAllEmptyCache(testFundCode);
        }
    }

    @Test
    @DisplayName("测试清除不存在的缓存不报错")
    void testClearNonExistentCache() {
        String testFundCode = "TEST666666";
        // 不应该抛出异常
        assertDoesNotThrow(() -> {
            emptyCacheService.clearEmptyCache(testFundCode, EmptyCacheService.DataType.INFO);
        });
    }
}
