package com.fund.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollectProperties 配置测试
 */
@SpringBootTest
class CollectPropertiesTest {

    @Autowired
    private CollectProperties collectProperties;

    @Test
    @DisplayName("测试配置注入成功")
    void testConfigurationInjection() {
        assertNotNull(collectProperties);
    }

    @Test
    @DisplayName("测试轮询配置默认值")
    void testPollConfiguration() {
        assertNotNull(collectProperties.getPoll());
        assertEquals(500, collectProperties.getPoll().getInterval());
        assertEquals(30, collectProperties.getPoll().getMaxAttempts());
        assertEquals(15, collectProperties.getPoll().getTimeoutSeconds());
    }

    @Test
    @DisplayName("测试缓存配置默认值")
    void testCacheConfiguration() {
        assertNotNull(collectProperties.getCache());
        assertEquals(30, collectProperties.getCache().getEmptyTtlMinutes());
    }

    @Test
    @DisplayName("测试配置字符串输出")
    void testToString() {
        String str = collectProperties.toString();
        assertTrue(str.contains("interval=500"));
        assertTrue(str.contains("maxAttempts=30"));
        assertTrue(str.contains("timeout=15"));
        assertTrue(str.contains("emptyTtl=30"));
    }
}
