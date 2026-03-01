package com.fund.util;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存工具类
 * 功能：布隆过滤器防穿透、互斥锁防击穿、随机TTL防雪崩
 */
@Component
public class CacheUtil {
    
    private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);
    private static final Random random = new Random();
    
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    
    // 锁等待时间（毫秒）
    private static final long LOCK_WAIT_TIME = 100;
    private static final long LOCK_LEASE_TIME = 5000;
    
    // 空值缓存时间（分钟）
    private static final long NULL_CACHE_MINUTES = 5;
    
    // 随机TTL范围（用于防雪崩）
    private static final int RANDOM_TTL_MIN = 0;
    private static final int RANDOM_TTL_MAX = 300; // 最多加5分钟随机时间
    
    public CacheUtil(RedissonClient redissonClient, StringRedisTemplate redisTemplate) {
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 获取带随机偏移的TTL（防雪崩）
     * 在基础TTL上增加0-300秒的随机时间
     */
    public long getRandomTtl(long baseTtlSeconds) {
        long randomOffset = random.nextInt(RANDOM_TTL_MAX - RANDOM_TTL_MIN) + RANDOM_TTL_MIN;
        return baseTtlSeconds + randomOffset;
    }
    
    /**
     * 获取带随机偏移的TTL（分钟为单位）
     */
    public long getRandomTtlMinutes(int baseMinutes) {
        return getRandomTtl(baseMinutes * 60L);
    }
    
    /**
     * 获取布隆过滤器
     */
    public RBloomFilter<String> getBloomFilter(String filterName, long expectedElements) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        if (!bloomFilter.isExists()) {
            // 预期元素数量和误判率
            bloomFilter.tryInit(expectedElements, 0.01);
            log.info("布隆过滤器初始化: {}, 预期元素数: {}", filterName, expectedElements);
        }
        return bloomFilter;
    }
    
    /**
     * 检查元素是否可能在布隆过滤器中
     */
    public boolean mightContain(String filterName, String element) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        return bloomFilter.contains(element);
    }
    
    /**
     * 添加元素到布隆过滤器
     */
    public void addToBloomFilter(String filterName, String element) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        bloomFilter.add(element);
    }
    
    /**
     * 批量添加元素到布隆过滤器
     */
    public void addAllToBloomFilter(String filterName, Iterable<String> elements) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        for (String element : elements) {
            bloomFilter.add(element);
        }
    }
    
    /**
     * 互斥锁获取缓存（防击穿）
     * 使用Redisson分布式锁保证只有一个线程重建缓存
     * 
     * @param lockKey 锁的key
     * @param cacheGetter 缓存获取函数
     * @param dbLoader 数据库加载函数
     * @param cacheSetter 缓存设置函数（包含TTL）
     * @param <T> 数据类型
     * @return 数据
     */
    public <T> T getWithMutex(String lockKey, 
                              Supplier<T> cacheGetter, 
                              Supplier<T> dbLoader,
                              java.util.function.Consumer<T> cacheSetter) {
        // 1. 先查缓存
        T cached = cacheGetter.get();
        if (cached != null) {
            return cached;
        }
        
        // 2. 缓存未命中，尝试获取锁
        RLock lock = redissonClient.getLock("lock:" + lockKey);
        boolean locked = false;
        
        try {
            locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS);
            
            if (locked) {
                // 获取锁成功，再次检查缓存（双重检查）
                cached = cacheGetter.get();
                if (cached != null) {
                    return cached;
                }
                
                // 3. 查询数据库
                T data = dbLoader.get();
                
                // 4. 写入缓存（即使是null也缓存，防穿透）
                cacheSetter.accept(data);
                
                return data;
            } else {
                // 获取锁失败，等待后重试
                Thread.sleep(50);
                return cacheGetter.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: {}", lockKey, e);
            return null;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 带布隆过滤器的缓存获取（防穿透）
     * 
     * @param bloomFilterName 布隆过滤器名称
     * @param element 查询元素
     * @param cacheGetter 缓存获取函数
     * @param dbLoader 数据库加载函数
     * @param cacheSetter 缓存设置函数
     * @param <T> 数据类型
     * @return 数据
     */
    public <T> T getWithBloomFilter(String bloomFilterName,
                                    String element,
                                    Supplier<T> cacheGetter,
                                    Supplier<T> dbLoader,
                                    java.util.function.Consumer<T> cacheSetter) {
        // 1. 布隆过滤器检查
        if (!mightContain(bloomFilterName, element)) {
            log.debug("布隆过滤器拦截无效查询: {}", element);
            return null;
        }
        
        // 2. 正常缓存流程
        return getWithMutex(element, cacheGetter, dbLoader, cacheSetter);
    }
    
    /**
     * 设置空值缓存（防穿透）
     */
    public void setNullCache(String key, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, "null", getRandomTtl(ttlSeconds), TimeUnit.SECONDS);
    }
    
    /**
     * 判断是否为空值缓存
     */
    public boolean isNullCache(String value) {
        return "null".equals(value);
    }
    
    /**
     * 异步重建缓存
     * 用于缓存即将过期时的提前重建
     */
    public <T> void asyncRebuildCache(String lockKey,
                                     Supplier<T> dbLoader,
                                     java.util.function.Consumer<T> cacheSetter) {
        RLock lock = redissonClient.getLock("lock:async:" + lockKey);
        
        try {
            if (lock.tryLock(0, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                // 异步执行缓存重建
                new Thread(() -> {
                    try {
                        T data = dbLoader.get();
                        if (data != null) {
                            cacheSetter.accept(data);
                            log.debug("异步缓存重建完成: {}", lockKey);
                        }
                    } catch (Exception e) {
                        log.error("异步缓存重建失败: {}", lockKey, e);
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                }).start();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取缓存命中率
     */
    public double getHitRate(String cacheName) {
        // 这里可以通过Redisson或Redis统计
        // 简化实现，实际生产可以使用Micrometer监控
        return 0.0;
    }
}
