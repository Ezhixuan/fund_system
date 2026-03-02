package com.fund.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 采集任务管理器
 * 防止对同一基金的并发请求触发多次 Python 采集
 */
@Component
public class CollectTaskManager {

    private static final Logger log = LoggerFactory.getLogger(CollectTaskManager.class);

    // 正在进行的采集任务
    private final ConcurrentHashMap<String, CompletableFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    // 默认超时时间：15秒
    private static final long DEFAULT_TIMEOUT_SECONDS = 15;

    /**
     * 执行任务（带并发控制）
     * 如果同一key的任务正在进行，则等待该任务完成并返回结果
     * 如果没有进行中的任务，则创建新任务执行
     *
     * @param taskKey    任务唯一标识
     * @param task       任务执行逻辑
     * @param timeoutSec 超时时间（秒）
     * @param <T>        返回类型
     * @return 任务结果
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(String taskKey, Supplier<T> task, long timeoutSec) {
        // 检查是否已有进行中的任务
        CompletableFuture<T> future = (CompletableFuture<T>) pendingTasks.get(taskKey);

        if (future != null) {
            log.debug("任务[{}]已在进行中，等待结果", taskKey);
            try {
                return future.get(timeoutSec, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("等待任务[{}]结果异常", taskKey, e);
                return null;
            }
        }

        // 创建新任务
        CompletableFuture<T> newFuture = CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始执行任务[{}]", taskKey);
                return task.get();
            } catch (Exception e) {
                log.error("任务[{}]执行异常", taskKey, e);
                throw new RuntimeException(e);
            }
        });

        // 放入任务池（仅当不存在时才放入）
        CompletableFuture<?> existing = pendingTasks.putIfAbsent(taskKey, newFuture);

        if (existing != null) {
            // 有其他线程抢先创建了任务，等待那个任务
            log.debug("任务[{}]被其他线程创建，等待其结果", taskKey);
            try {
                return ((CompletableFuture<T>) existing).get(timeoutSec, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("等待任务[{}]结果异常", taskKey, e);
                return null;
            }
        }

        // 任务完成后清理
        newFuture.whenComplete((result, ex) -> {
            pendingTasks.remove(taskKey);
            if (ex != null) {
                log.error("任务[{}]完成但发生异常", taskKey, ex);
            } else {
                log.debug("任务[{}]完成", taskKey);
            }
        });

        // 等待结果
        try {
            return newFuture.get(timeoutSec, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("等待任务[{}]结果超时或异常", taskKey, e);
            pendingTasks.remove(taskKey);
            return null;
        }
    }

    /**
     * 执行任务（使用默认超时时间）
     *
     * @param taskKey 任务唯一标识
     * @param task    任务执行逻辑
     * @param <T>     返回类型
     * @return 任务结果
     */
    public <T> T execute(String taskKey, Supplier<T> task) {
        return execute(taskKey, task, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 检查任务是否在进行中
     *
     * @param taskKey 任务唯一标识
     * @return true - 进行中
     */
    public boolean isPending(String taskKey) {
        return pendingTasks.containsKey(taskKey);
    }

    /**
     * 获取进行中任务数量
     *
     * @return 任务数
     */
    public int getPendingCount() {
        return pendingTasks.size();
    }

    /**
     * 取消任务
     *
     * @param taskKey 任务唯一标识
     * @return true - 取消成功
     */
    public boolean cancel(String taskKey) {
        CompletableFuture<?> future = pendingTasks.remove(taskKey);
        if (future != null) {
            future.cancel(true);
            log.info("任务[{}]已取消", taskKey);
            return true;
        }
        return false;
    }

    /**
     * 构建采集任务key
     *
     * @param dataType 数据类型
     * @param fundCode 基金代码
     * @return 任务key
     */
    public static String buildTaskKey(String dataType, String fundCode) {
        return "collect:" + dataType + ":" + fundCode;
    }
}
