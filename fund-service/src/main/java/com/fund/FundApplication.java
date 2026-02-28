package com.fund;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基金服务启动类
 */
@SpringBootApplication
@MapperScan("com.fund.mapper")
public class FundApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(FundApplication.class, args);
    }
}
