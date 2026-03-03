# Task Executor Skill (task2)

## 简介

自定义 Skill，用于执行具体任务。根据文档或需求描述，创建任务文件、实际执行、测试验证，并生成测试报告。

## 使用方法

在 Claude Code 中输入：

```
/task2 [任务描述或附加文档]
```

## 工作流程

```
输入 → 创建任务文件 → 执行 → 测试 → 生成测试报告
```

## 文件结构

```
docs/task/
├── {category}/
│   ├── YYYY-MM-DD-task-name.md              # 任务定义
│   └── YYYY-MM-DD-task-name-test-report.md  # 测试报告
├── phase1/
├── phase2/
├── bugfix/
├── feature/
└── refactor/
```

## 任务分类

| 分类 | 用途 |
|------|------|
| `phase1/phase2/phase3` | 分阶段任务 |
| `bugfix` | Bug 修复 |
| `feature` | 新功能开发 |
| `refactor` | 代码重构 |
| `backend/frontend` | 前后端任务 |
| `database` | 数据库任务 |
| `devops` | 部署运维 |
| `config` | 配置变更 |
| `docs` | 文档任务 |

## 任务文档包含

- 任务描述和目标
- 详细的执行步骤（可勾选）
- 文件修改清单
- 关键代码变更
- 测试验证结果
- 执行结果和耗时

## 测试报告包含

- 测试范围和用例
- 测试环境信息
- 测试结果汇总
- 发现的问题
- 性能测试（如有）
- 测试结论

## 示例

### 示例 1: Bug 修复
```
/task2 修复 Redis 缓存格式错误，清除 fund:estimate 缓存
```

生成：
- `docs/task/bugfix/2026-03-04-redis-cache-fix.md`
- `docs/task/bugfix/2026-03-04-redis-cache-fix-test-report.md`

### 示例 2: 功能开发
```
/task2 添加基金数据初始化功能，启动时自动导入热门基金
```

生成：
- `docs/task/feature/2026-03-04-fund-data-initializer.md`
- `docs/task/feature/2026-03-04-fund-data-initializer-test-report.md`

### 示例 3: 配置升级
```
/task2 1. 升级 SQLAlchemy 到 2.0.36  2. 验证指标计算正常
```

生成：
- `docs/task/config/2026-03-04-sqlalchemy-upgrade.md`
- `docs/task/config/2026-03-04-sqlalchemy-upgrade-test-report.md`

## 安全规则

1. **执行前验证**: 检查当前状态，必要时备份
2. **分步执行**: 完成一步再进行下一步
3. **及时测试**: 每个主要变更后验证
4. **详细记录**: 记录所有修改和遇到的问题
5. **生成报告**: 必须创建测试报告

## 注意事项

- 任务文档和测试报告保存在同一目录
- 所有步骤使用中文记录
- 文件名使用 kebab-case
- 包含实际的测试命令和输出
- 记录失败和回滚方案
