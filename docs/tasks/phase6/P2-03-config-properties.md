# Task P2-03: 配置参数

## 任务定义

添加轮询和缓存配置参数到配置文件。

## 执行内容 Checklist

- [ ] 创建 `CollectProperties.java` 配置类
- [ ] 添加轮询配置（interval, max-attempts, timeout）
- [ ] 添加缓存配置（empty-ttl）
- [ ] 更新 application.yml
- [ ] 编写配置测试
- [ ] 运行测试并记录结果

## 配置项

```yaml
collect:
  poll:
    interval: 500ms
    max-attempts: 30
    timeout: 15s
  cache:
    empty-ttl: 30m
```

## Git 提交规范

```bash
git commit -m "feat(config): 添加采集服务轮询和缓存配置参数"
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
feat(config): 添加采集服务轮询和缓存配置参数
```
