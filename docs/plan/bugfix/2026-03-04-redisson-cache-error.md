# 计划: 修复 Redisson 缓存格式错误

**Created**: 2026-03-04 03:38
**Category**: bugfix
**Status**: Draft
**Priority**: P0

## 背景

系统运行时抛出 RedisException 异常：
```
org.redisson.client.RedisException: ERR user_script:1: bad argument #2 to 'unpack' (data string too short)
```

异常发生在访问 `fund:estimate` 缓存时，导致估值相关接口无法正常响应。

根本原因：Redis 中存储的缓存数据格式与 Redisson 期望的格式不一致，可能是应用升级后缓存格式变化或数据损坏。

## 目标

1. **治标**: 立即清除问题缓存，恢复服务
2. **治本**: 实现启动时自动清理机制，防止问题再次发生
3. **加固**: 添加异常处理和降级机制，提高系统鲁棒性

## 执行步骤

### Step 1: 立即清除 Redis 问题缓存
- [ ] 连接 Redis (端口 16379)
- [ ] 删除 `fund:estimate` key
- [ ] 删除 `redisson__timeout__set:{fund:estimate}`
- [ ] 验证清除成功
- **预期产出**: Redis 缓存已清除，服务可正常启动
- **截止时间**: 2026-03-04 03:40

### Step 2: 实现 CacheCleaner 启动清理组件
- [ ] 在 `fund-service` 创建 `CacheCleaner.java`
- [ ] 实现 `CommandLineRunner` 接口
- [ ] 清理 `fund:estimate`, `fund:detail`, `fund:metrics` 等缓存
- [ ] 添加日志记录
- **预期产出**: 应用启动时自动清理可能存在格式问题的缓存
- **截止时间**: 2026-03-04 03:45

### Step 3: 添加缓存异常处理
- [ ] 在 `EstimateService` 中添加 try-catch
- [ ] 缓存读取失败时自动清除并降级
- [ ] 添加监控日志
- **预期产出**: 即使缓存格式错误也能优雅降级，不影响服务
- **截止时间**: 2026-03-04 03:50

### Step 4: 配置优化
- [ ] 检查 Redisson 配置
- [ ] 考虑使用独立 Redis DB (如 DB 1)
- [ ] 配置缓存过期时间
- **预期产出**: 配置优化，减少缓存冲突风险
- **截止时间**: 2026-03-04 03:55

### Step 5: 测试验证
- [ ] 重启 Java 应用
- [ ] 测试估值接口是否正常
- [ ] 验证缓存自动清理功能
- [ ] 测试异常降级场景
- **预期产出**: 所有测试通过，服务稳定运行
- **截止时间**: 2026-03-04 04:00

## 依赖关系

```
Step 1 (立即处理) → Step 2 (启动清理) → Step 3 (异常处理)
     ↓
Step 4 (配置优化) → Step 5 (测试验证)
```

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 清除缓存后数据丢失 | Low | 缓存数据可重新从数据库/采集服务获取 |
| 自动清理影响性能 | Low | 仅在启动时执行，数据量小 |
| 修复后仍出现问题 | Med | 添加异常降级，确保服务可用 |

## 相关文档

- Issue: [Redisson 缓存格式错误](../../issues/2026-03-04-0233-redisson-cache-format-error.md)
- 应用配置: `fund-service/src/main/resources/application.yml`
- Redis 配置: `fund-service/src/main/java/com/fund/config/RedisConfig.java`

## 备注

- Step 1 可立即执行，恢复服务
- Step 2-4 需要代码修改和部署
- 生产环境执行前建议备份 Redis 数据（如有重要缓存）
