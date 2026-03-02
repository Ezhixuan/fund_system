package com.fund.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollectTaskManager 单元测试
 */
@SpringBootTest
class CollectTaskManagerTest {

    @Autowired
    private CollectTaskManager collectTaskManager;

    @Test
    @DisplayName("测试服务注入成功")
    void testServiceInjection() {
        assertNotNull(collectTaskManager);
    }

    @Test
    @DisplayName("测试单次任务执行")
    void testSingleTaskExecution() {
        String taskKey = "test:single:" + System.currentTimeMillis();

        String result = collectTaskManager.execute(taskKey, () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "success";
        });

        assertEquals("success", result);
    }

    @Test
    @DisplayName("测试任务复用")
    void testTaskReuse() throws Exception {
        String taskKey = "test:reuse:" + System.currentTimeMillis();
        AtomicInteger executionCount = new AtomicInteger(0);

        // 第一个任务
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() ->
            collectTaskManager.execute(taskKey, () -> {
                executionCount.incrementAndGet();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "result-" + executionCount.get();
            })
        );

        // 等待任务启动
        Thread.sleep(50);

        // 第二个请求应该等待第一个完成
        String result2 = collectTaskManager.execute(taskKey, () -> {
            executionCount.incrementAndGet();
            return "should-not-execute";
        });

        String result1 = future1.get();

        // 验证只执行了一次
        assertEquals(1, executionCount.get());
        assertEquals("result-1", result1);
        assertEquals("result-1", result2);
    }

    @Test
    @DisplayName("测试任务进行中检测")
    void testPendingDetection() throws InterruptedException {
        String taskKey = "test:pending:" + System.currentTimeMillis();

        assertFalse(collectTaskManager.isPending(taskKey));

        // 启动一个耗时任务
        Thread asyncTask = new Thread(() -> {
            collectTaskManager.execute(taskKey, () -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "done";
            });
        });
        asyncTask.start();

        // 等待任务启动
        Thread.sleep(50);

        // 验证任务在进行中
        assertTrue(collectTaskManager.isPending(taskKey) || !asyncTask.isAlive());

        asyncTask.join();
    }

    @Test
    @DisplayName("测试任务超时")
    void testTaskTimeout() {
        String taskKey = "test:timeout:" + System.currentTimeMillis();

        // 超时时间为1秒，任务执行2秒
        String result = collectTaskManager.execute(taskKey, () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "should-not-return";
        }, 1);

        // 超时后返回null
        assertNull(result);
    }

    @Test
    @DisplayName("测试构建任务key")
    void testBuildTaskKey() {
        String key = CollectTaskManager.buildTaskKey("info", "000001");
        assertEquals("collect:info:000001", key);
    }

    @Test
    @DisplayName("测试不同key的任务独立执行")
    void testDifferentKeysIndependent() {
        String key1 = "test:independent:1:" + System.currentTimeMillis();
        String key2 = "test:independent:2:" + System.currentTimeMillis();

        AtomicInteger count = new AtomicInteger(0);

        String result1 = collectTaskManager.execute(key1, () -> {
            count.incrementAndGet();
            return "result1";
        });

        String result2 = collectTaskManager.execute(key2, () -> {
            count.incrementAndGet();
            return "result2";
        });

        assertEquals(2, count.get());
        assertEquals("result1", result1);
        assertEquals("result2", result2);
    }

    @Test
    @DisplayName("测试任务完成后可以重新执行")
    void testTaskCanReExecuteAfterComplete() {
        String taskKey = "test:reexecute:" + System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);

        // 第一次执行
        String result1 = collectTaskManager.execute(taskKey, () -> {
            count.incrementAndGet();
            return "first-" + count.get();
        });

        // 等待确保任务完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 第二次执行（应该重新执行）
        String result2 = collectTaskManager.execute(taskKey, () -> {
            count.incrementAndGet();
            return "second-" + count.get();
        });

        assertEquals(2, count.get());
        assertEquals("first-1", result1);
        assertEquals("second-2", result2);
    }
}
