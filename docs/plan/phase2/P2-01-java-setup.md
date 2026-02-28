# P2-01: Java项目搭建与配置 - 执行计划

> 工期：3天 | 依赖：P1-01

---

## Day 1: 项目初始化

### 1.1 Spring Boot项目创建
```bash
# 使用Spring Initializr或IDE创建
spring init \
  --boot-version=3.2.0 \
  --java-version=17 \
  --dependencies=web,mysql,data-redis,mybatis,lombok \
  --groupId=com.fund \
  --artifactId=fund-service \
  fund-service
```

### 1.2 pom.xml依赖
```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.5</version>
    </dependency>
    
    <!-- Redisson -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.25.0</version>
    </dependency>
    
    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- 其他 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <dependency>
        <groupId>pinyin4j</groupId>
        <artifactId>pinyin4j</artifactId>
        <version>2.5.1</version>
    </dependency>
</dependencies>
```

---

## Day 2: 配置与基础类

### 2.1 application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fund_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: fund
    password: fund123
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:/mapper/**/*.xml
  type-aliases-package: com.fund.entity

logging:
  level:
    com.fund: debug
```

### 2.2 统一响应类
```java
// dto/ApiResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
```

### 2.3 全局异常处理
```java
// config/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error("系统繁忙，请稍后重试");
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }
}
```

---

## Day 3: 实体类与Mapper

### 3.1 实体类
```java
// entity/FundInfo.java
@Data
@TableName("fund_info")
public class FundInfo {
    @TableId
    private String fundCode;
    private String fundName;
    private String namePinyin;
    private String fundType;
    private String investStyle;
    private String managerCode;
    private String managerName;
    private String companyCode;
    private String companyName;
    private LocalDate establishDate;
    private String benchmark;
    private BigDecimal managementFee;
    private BigDecimal custodyFee;
    private Integer riskLevel;
    private BigDecimal currentScale;
    private Integer status;
    private LocalDateTime updateTime;
}
```

### 3.2 Mapper接口
```java
// mapper/FundInfoMapper.java
@Mapper
public interface FundInfoMapper extends BaseMapper<FundInfo> {
}
```

### 3.3 启动类
```java
@SpringBootApplication
@MapperScan("com.fund.mapper")
public class FundApplication {
    public static void main(String[] args) {
        SpringApplication.run(FundApplication.class, args);
    }
}
```

---

## 验收清单
- [ ] 项目可正常启动
- [ ] 数据库连接成功
- [ ] GET /health 返回healthy
