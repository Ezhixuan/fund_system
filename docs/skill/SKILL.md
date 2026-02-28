# SKILL.md - Git Commit Convention

## 名称
git-commit-convention

## 描述
强制执行 Git 提交规范，确保每次代码改动都遵循 Conventional Commits 标准进行提交。适用于 fund-system 项目。

## 触发条件
- 当执行任何代码改动时
- 当完成一个功能模块时
- 当修复 Bug 时
- 当重构代码时

## 核心规则

### 1. 必须提交场景
每次完成以下任一场景都必须进行 Git 提交：
- 完成一个功能模块
- 修复一个 Bug
- 重构代码
- 更新配置
- 添加文档
- 阶段性完成任务

### 2. 提交格式
```
<type>(<scope>): <subject>
```

**Type 类型**：
- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式
- `refactor`: 重构
- `perf`: 性能
- `test`: 测试
- `chore`: 构建

**Scope 范围**：
- `api`: 后端接口
- `ui`: 前端界面
- `db`: 数据库
- `collector`: 数据采集
- `docs`: 文档

### 3. 提交流程
```bash
# 1. 检查状态
git status

# 2. 查看改动
git diff

# 3. 添加文件
git add <具体文件>

# 4. 提交（必须遵循规范）
git commit -m "type(scope): subject"

# 5. 推送
git push
```

### 4. 禁止行为
- ❌ `git commit -m "update"`（过于模糊）
- ❌ `git add . && git commit`（批量提交）
- ❌ 多个不相关改动合并提交
- ❌ 长时间不提交（超过半天）

## 示例

```bash
# 正确示例
feat(api): add fund search with pinyin support
fix(db): correct nav date index
refactor(collector): extract data validator
docs(api): update swagger annotations

# 错误示例
update                                      ❌ 无类型无范围
fix bug                                     ❌ 过于简单
feat: Added new feature                     ❌ 过去时，大写
```

## 详细文档
参见同目录下的 [git-commit-convention.md](./git-commit-convention.md)

## 生效日期
2026-02-28
