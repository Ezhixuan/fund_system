# Task P2-02: 并发采集控制

## 任务定义

实现并发采集控制，防止对同一基金的并发请求触发多次 Python 采集。

## 执行内容 Checklist

- [ ] 创建采集任务管理器
- [ ] 实现任务去重逻辑（ConcurrentHashMap）
- [ ] 实现 CompletableFuture 异步等待
- [ ] 集成到 FundDataFetchService
- [ ] 编写单元测试
- [ ] 运行测试并记录结果

## Git 提交规范

```bash
git commit -m "feat(service): 实现并发采集控制，避免重复触发同一基金的采集任务"
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
feat(service): 实现并发采集控制，避免重复触发同一基金的采集任务
```
