package com.fund.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 采集服务配置属性
 */
@Component
@ConfigurationProperties(prefix = "collect")
public class CollectProperties {

    /**
     * 轮询配置
     */
    private Poll poll = new Poll();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    /**
     * 轮询配置
     */
    public static class Poll {
        /**
         * 轮询间隔（毫秒）
         */
        private long interval = 500;

        /**
         * 最大轮询次数
         */
        private int maxAttempts = 30;

        /**
         * 总超时时间（秒）
         */
        private long timeoutSeconds = 15;

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    /**
     * 缓存配置
     */
    public static class Cache {
        /**
         * 空值缓存TTL（分钟）
         */
        private long emptyTtlMinutes = 30;

        public long getEmptyTtlMinutes() {
            return emptyTtlMinutes;
        }

        public void setEmptyTtlMinutes(long emptyTtlMinutes) {
            this.emptyTtlMinutes = emptyTtlMinutes;
        }
    }

    @Override
    public String toString() {
        return "CollectProperties{" +
                "poll={interval=" + poll.getInterval() +
                "ms, maxAttempts=" + poll.getMaxAttempts() +
                ", timeout=" + poll.getTimeoutSeconds() +
                "s}, cache={emptyTtl=" + cache.getEmptyTtlMinutes() +
                "min}}";
    }
}
