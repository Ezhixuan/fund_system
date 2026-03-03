# Plan Generator Skill (plan2)

## 简介

自定义 Skill，用于根据文档或需求描述生成结构化的执行计划。支持将复杂需求拆解为多个分类计划。

## 使用方法

在 Claude Code 中输入：

```
/plan2 [需求描述或附加文档]
```

## 功能

1. **解析输入**: 读取用户提供的文档或直接描述
2. **分析需求**: 理解目标、任务和依赖关系
3. **分类归档**: 将计划归类到适当的目录
4. **多计划生成**: 复杂需求拆分为多个专项计划

## 计划分类

| 分类 | 用途 |
|------|------|
| `phase1/phase2/phase3` | 分阶段开发 |
| `bugfix` | Bug 修复 |
| `feature` | 新功能 |
| `refactor` | 代码重构 |
| `backend/frontend` | 前后端专项 |
| `database` | 数据库变更 |
| `devops` | 部署运维 |
| `testing` | 测试计划 |
| `docs` | 文档编写 |
| `research` | 技术研究 |

## 文档格式

生成的计划文档包含：
- 背景说明
- 明确目标
- 分步骤执行清单（可勾选）
- 依赖关系图
- 风险评估
- 相关文档链接

## 文件名格式

```
docs/plan/{category}/YYYY-MM-DD-brief-description.md
```

## 示例

### 示例 1: 直接描述需求
```
/plan2 需要重构用户认证模块，使用JWT替换Session，包含登录、注册、密码重置功能
```

生成: `docs/plan/refactor/2026-03-04-auth-jwt-migration.md`

### 示例 2: 附加多份设计文档
```
/plan2 [附加: 数据库设计.md, API设计.md, UI设计.md]
```

生成多份计划：
- `docs/plan/database/2026-03-04-schema-design.md`
- `docs/plan/backend/2026-03-04-api-implementation.md`
- `docs/plan/frontend/2026-03-04-ui-components.md`

### 示例 3: 复杂功能开发
```
/plan2 [附加: 基金实时监控功能需求.md]
```

生成：
- `docs/plan/phase1/2026-03-04-data-model.md`
- `docs/plan/phase1/2026-03-04-core-collector.md`
- `docs/plan/phase2/2026-03-04-websocket-integration.md`
- `docs/plan/testing/2026-03-04-test-strategy.md`

## 计划示例

```markdown
# 计划: JWT 认证重构

**Created**: 2026-03-04 10:00
**Category**: refactor
**Status**: Draft
**Priority**: P1

## 背景

当前使用 Session 认证，不利于微服务扩展和移动端支持。

## 目标

1. 使用 JWT 替换 Session 认证
2. 支持 Token 自动刷新
3. 实现无感知登录体验

## 执行步骤

### Step 1: 基础 JWT 实现
- [ ] 添加 jjwt 依赖
- [ ] 创建 JwtUtil 工具类
- [ ] 实现 Token 生成和验证
- **预期产出**: JWT 基础功能可用
- **截止时间**: 2026-03-06

### Step 2: 认证接口改造
- [ ] 修改登录接口返回 Token
- [ ] 添加 Token 刷新接口
- [ ] 修改登录拦截器
- **预期产出**: 登录/刷新接口正常工作
- **截止时间**: 2026-03-08

## 依赖关系

```
Step 1 → Step 2 → Step 3
```

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Token 泄露 | High | 设置短有效期，支持吊销 |
| 旧接口兼容 | Med | 双轨运行，逐步迁移 |

## 相关文档

- 原认证实现: `docs/design/auth-session.md`
- JWT 规范: https://jwt.io/

## 备注

注意保持与现有权限系统的兼容性。
```

## 注意事项

- 计划文档使用中文编写
- 每个计划聚焦单一目标
- 步骤需要可执行、可验证
- 包含明确的时间节点
- 识别依赖关系和阻塞点
