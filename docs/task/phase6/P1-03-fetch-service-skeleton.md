# Task P1-03: FundDataFetchService 骨架

## 任务定义

创建 FundDataFetchService 服务类，实现基础数据获取骨架。

## 执行内容 Checklist

- [ ] 创建 `FundDataFetchService.java`
- [ ] 注入 CollectClient
- [ ] 实现 `getFundInfo(String fundCode)` 骨架
- [ ] 实现 `getFundMetrics(String fundCode)` 骨架
- [ ] 实现 `getNavHistory(String fundCode)` 骨架
- [ ] 添加日志记录
- [ ] 编写单元测试
- [ ] 运行测试并记录结果

## 类结构

```java
@Service
@Slf4j
public class FundDataFetchService {
    
    @Autowired
    private CollectClient collectClient;
    
    @Autowired
    private FundInfoMapper fundInfoMapper;
    
    public FundInfo getFundInfo(String fundCode) {
        // TODO: 实现数据获取逻辑
    }
    
    public FundMetrics getFundMetrics(String fundCode) {
        // TODO: 实现数据获取逻辑
    }
    
    public List<NavData> getNavHistory(String fundCode) {
        // TODO: 实现数据获取逻辑
    }
}
```

## Git 提交规范

```bash
git add fund-service/src/main/java/com/fund/service/FundDataFetchService.java
git add fund-service/src/test/java/com/fund/service/FundDataFetchServiceTest.java
git commit -m "feat(service): create FundDataFetchService skeleton"
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
feat(service): create FundDataFetchService skeleton
```
