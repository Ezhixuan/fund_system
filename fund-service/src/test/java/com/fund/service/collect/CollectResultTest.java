package com.fund.service.collect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CollectResult 单元测试
 */
class CollectResultTest {
    
    @Test
    @DisplayName("测试成功结果构造")
    void testSuccessResult() {
        // given
        String data = "test data";
        
        // when
        CollectResult<String> result = CollectResult.success(data);
        
        // then
        assertTrue(result.isSuccess());
        assertEquals(data, result.getData());
        assertNull(result.getErrorCode());
        assertNull(result.getMessage());
    }
    
    @Test
    @DisplayName("测试失败结果构造")
    void testFailResult() {
        // given
        String errorCode = "TEST_ERROR";
        String message = "测试错误信息";
        
        // when
        CollectResult<String> result = CollectResult.fail(errorCode, message);
        
        // then
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals(errorCode, result.getErrorCode());
        assertEquals(message, result.getMessage());
    }
    
    @Test
    @DisplayName("测试基金不存在错误")
    void testNotFoundResult() {
        // given
        String fundCode = "000001";
        
        // when
        CollectResult<Object> result = CollectResult.notFound(fundCode);
        
        // then
        assertFalse(result.isSuccess());
        assertEquals(CollectResult.ERR_FUND_NOT_FOUND, result.getErrorCode());
        assertTrue(result.getMessage().contains(fundCode));
    }
    
    @Test
    @DisplayName("测试超时错误")
    void testTimeoutResult() {
        // given
        String message = "连接超时";
        
        // when
        CollectResult<Object> result = CollectResult.timeout(message);
        
        // then
        assertFalse(result.isSuccess());
        assertEquals(CollectResult.ERR_TIMEOUT, result.getErrorCode());
        assertTrue(result.getMessage().contains(message));
    }
    
    @Test
    @DisplayName("测试服务异常错误")
    void testServiceErrorResult() {
        // given
        String message = "Python服务不可用";
        
        // when
        CollectResult<Object> result = CollectResult.serviceError(message);
        
        // then
        assertFalse(result.isSuccess());
        assertEquals(CollectResult.ERR_SERVICE_ERROR, result.getErrorCode());
        assertTrue(result.getMessage().contains(message));
    }
    
    @Test
    @DisplayName("测试 Builder 模式构造")
    void testBuilder() {
        // given
        String data = "builder data";
        
        // when
        CollectResult<String> result = CollectResult.<String>builder()
                .success(true)
                .data(data)
                .errorCode(null)
                .message(null)
                .build();
        
        // then
        assertTrue(result.isSuccess());
        assertEquals(data, result.getData());
    }
    
    @Test
    @DisplayName("测试泛型类型")
    void testGenericTypes() {
        // Integer 类型
        CollectResult<Integer> intResult = CollectResult.success(42);
        assertEquals(42, intResult.getData());
        
        // 自定义对象类型
        class TestData {
            String name;
            TestData(String name) { this.name = name; }
        }
        TestData testData = new TestData("test");
        CollectResult<TestData> objResult = CollectResult.success(testData);
        assertEquals("test", objResult.getData().name);
    }
    
    @Test
    @DisplayName("测试错误码常量值")
    void testErrorCodeConstants() {
        assertEquals("FUND_NOT_FOUND", CollectResult.ERR_FUND_NOT_FOUND);
        assertEquals("TIMEOUT", CollectResult.ERR_TIMEOUT);
        assertEquals("SERVICE_ERROR", CollectResult.ERR_SERVICE_ERROR);
        assertEquals("INVALID_PARAM", CollectResult.ERR_INVALID_PARAM);
    }
}
