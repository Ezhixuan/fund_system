# Task P2-01: 空值缓存实现

## 任务定义

实现 Redis 空值缓存机制，防止对已确认不存在的基金重复查询 Python 服务。

## 执行内容 Checklist

- [ ] 创建 `EmptyCacheService.java` 空值缓存服务
- [ ] 实现缓存key生成方法
- [ ] 实现缓存检查方法
- [ ] 实现缓存写入方法（30分钟TTL）
- [ ] 集成到 FundDataFetchService
- [ ] 编写单元测试
- [ ] 运行测试并记录结果

## 缓存Key设计

```
fund:empty:info:{code}     - 基金信息缺失标记 (TTL: 30min)
fund:empty:metrics:{code}  - 指标缺失标记 (TTL: 30min)
fund:empty:nav:{code}      - NAV缺失标记 (TTL: 30min)
```

## Git 提交规范

```bash
git add fund-service/src/main/java/com/fund/service/EmptyCacheService.java
git add fund-service/src/test/java/com/fund/service/EmptyCacheServiceTest.java
git commit -m "feat(service): 实现空值缓存机制，防止重复查询不存在的基金数据"
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
feat(service): 实现空值缓存机制，防止重复查询不存在的基金数据
```
