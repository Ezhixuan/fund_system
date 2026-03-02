# Task P1-02: CollectClient 扩展

## 任务定义

扩展 CollectClient 接口，新增基金信息采集方法。

## 执行内容 Checklist

- [ ] 分析现有 CollectClient 接口
- [ ] 新增 `collectFundInfo(String fundCode)` 方法
- [ ] 新增 `collectFundMetrics(String fundCode)` 方法
- [ ] 新增 `collectNavHistory(String fundCode)` 方法
- [ ] 实现 FeignClient 调用
- [ ] 配置超时参数
- [ ] 编写单元测试（Mock Python服务）
- [ ] 运行测试并记录结果

## 接口设计

```java
@FeignClient(name = "collect-service", url = "${collect.service.url}")
public interface CollectClient {
    
    @GetMapping("/api/collect/fund/{code}")
    CollectResult<FundInfo> collectFundInfo(@PathVariable("code") String fundCode);
    
    @GetMapping("/api/collect/metrics/{code}")
    CollectResult<FundMetrics> collectFundMetrics(@PathVariable("code") String fundCode);
    
    @GetMapping("/api/collect/nav/{code}")
    CollectResult<List<NavData>> collectNavHistory(@PathVariable("code") String fundCode);
}
```

## Git 提交规范

```bash
git add fund-service/src/main/java/com/fund/client/CollectClient.java
git add fund-service/src/test/java/com/fund/client/CollectClientTest.java
git commit -m "feat(client): extend CollectClient with fund data collection APIs"
```

---
## 执行记录

**执行时间**:  
**执行者**:  
**耗时**:  
**状态**:  

### 测试结果

| 测试项 | 状态 | 备注 |
|--------|------|------|
| | | |

### Git 提交

```
commit xxx
feat(client): extend CollectClient with fund data collection APIs
```
