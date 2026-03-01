package com.fund.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步任务和定时任务配置
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // 配置在application.yml中
}
