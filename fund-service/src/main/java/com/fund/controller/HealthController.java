package com.fund.controller;

import com.fund.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    
    private final DataSource dataSource;
    
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 健康检查
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        
        // 检查数据库
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            status.put("database", valid ? "connected" : "disconnected");
        } catch (SQLException e) {
            log.error("数据库连接检查失败", e);
            status.put("database", "error: " + e.getMessage());
        }
        
        status.put("redis", "disabled");
        status.put("status", "healthy");
        status.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success(status);
    }
    
    /**
     * 就绪检查
     */
    @GetMapping("/ready")
    public ApiResponse<String> ready() {
        return ApiResponse.success("ready");
    }
    
    /**
     * 存活检查
     */
    @GetMapping("/live")
    public ApiResponse<String> live() {
        return ApiResponse.success("alive");
    }
}
