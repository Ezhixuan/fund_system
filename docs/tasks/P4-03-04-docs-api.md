# P4-03-04: 文档-API接口文档

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P4-03-04 |
| 名称 | 文档-API接口文档 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 0.5天 |
| 依赖 | 所有 |

---

## 需求描述
使用Swagger生成API接口文档。

---

## 实现步骤

### 1. 添加Swagger依赖
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 2. 配置Swagger
**文件**：`fund-service/src/main/java/com/fund/config/OpenApiConfig.java`

### 3. 添加注解
为Controller添加Swagger注解：
```java
@Tag(name = "基金接口", description = "基金查询相关接口")
@RestController
public class FundController {
    
    @Operation(summary = "获取基金列表")
    @GetMapping("/api/funds")
    public ApiResponse<PageResult<FundInfoVO>> listFunds(...) {}
}
```

### 4. 访问地址
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

---

## 验收标准
- [ ] Swagger UI可正常访问
- [ ] 所有接口都有文档说明
- [ ] 参数和返回值有描述

---

## 测试计划
测试日志将记录在：[P4-03-04-test-log.md](./P4-03-04-test-log.md)
