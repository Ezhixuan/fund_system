# Git 提交规范 Skill

> 版本：v1.0  
> 适用范围：fund-system 项目  
> 强制要求：每次系列改动都必须进行 Git 提交

---

## 一、核心原则

### 1.1 提交纪律（强制执行）

```
┌─────────────────────────────────────────────────────────────┐
│                     提交黄金法则                               │
├─────────────────────────────────────────────────────────────┤
│  1. 【单次任务单提交】每个独立任务/功能完成后必须立即提交        │
│  2. 【禁止批量提交】多个不相关改动不得合并为一次提交            │
│  3. 【提交即可用】每次提交后代码应处于可运行状态                │
│  4. 【提交必推送】重要修改提交后必须推送到远程仓库              │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 何时必须提交

| 场景 | 必须提交 | 说明 |
|------|----------|------|
| 完成一个功能模块 | ✅ | 如：完成基金检索 API |
| 修复一个 Bug | ✅ | 即使只有一行代码 |
| 重构代码 | ✅ | 完成后立即提交 |
| 更新配置 | ✅ | 如：修改数据库连接 |
| 添加文档 | ✅ | 如：更新 API 文档 |
| 阶段性完成 | ✅ | 任务拆解后的每个阶段 |
| 临时切换任务 | ✅ | 保存当前进度 |

---

## 二、提交规范（Conventional Commits）

### 2.1 格式规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 2.2 Type 类型（必选）

| Type | 含义 | 使用场景 |
|------|------|----------|
| `feat` | 新功能 | 新增 API、新页面、新模块 |
| `fix` | 修复 | Bug 修复、问题修复 |
| `docs` | 文档 | 文档更新、注释添加 |
| `style` | 格式 | 代码格式化、分号补全（不影响代码逻辑）|
| `refactor` | 重构 | 代码重构（既不修复 bug 也不添加功能）|
| `perf` | 性能 | 性能优化 |
| `test` | 测试 | 添加或修改测试代码 |
| `chore` | 构建 | 构建过程、辅助工具、依赖更新 |
| `ci` | CI/CD | 持续集成配置修改 |

### 2.3 Scope 范围（可选但推荐）

| Scope | 说明 |
|-------|------|
| `api` | 后端接口 |
| `ui` | 前端界面 |
| `db` | 数据库 |
| `collector` | 数据采集 |
| `docs` | 文档 |
| `config` | 配置 |

### 2.4 Subject 规范

- 使用**祈使语气**（做了什么，不是做了什么了）
- 首字母**小写**
- 结尾**不加句号**
- 长度不超过 **50 个字符**
- 清晰描述改动内容

✅ **正确示例**：
```
feat(api): add fund search with pinyin support
fix(db): correct nav date index
refactor(collector): extract data validator
docs(api): update swagger annotations
```

❌ **错误示例**：
```
feat: Added new feature          # 过去时，首字母大写
fix: bug fixed.                  # 有句号
chore: update                    # 过于模糊
```

### 2.5 Body 规范（可选）

- 详细描述改动的背景和原因
- 使用列表说明多个改动点
- 每行不超过 72 个字符

示例：
```
feat(api): add fund search with pinyin support

- implement pinyin conversion using pinyin4j
- add name_pinyin column to fund_info table
- support keyword search by code/name/pinyin
- add index for pinyin column

Closes #123
```

---

## 三、提交流程（强制执行）

### 3.1 标准提交流程

```bash
# Step 1: 检查当前状态
git status

# Step 2: 查看具体改动
git diff

# Step 3: 添加相关文件（不要 git add .）
git add <specific-files>

# Step 4: 提交（遵循规范）
git commit -m "type(scope): subject"

# Step 5: 推送到远程（重要修改）
git push
```

### 3.2 提交前检查清单

- [ ] 改动是否属于单一任务？（如果不是，拆分提交）
- [ ] 代码是否可以正常运行？
- [ ] 是否添加了必要的注释？
- [ ] 是否更新了相关文档？
- [ ] 提交信息是否符合规范？

### 3.3 提交信息检查脚本

```bash
#!/bin/bash
# commit-msg-hook.sh
# 放在 .git/hooks/commit-msg

commit_msg=$(cat "$1")
pattern="^(feat|fix|docs|style|refactor|perf|test|chore|ci)(\([a-z]+\))?: .+"

if ! echo "$commit_msg" | grep -qE "$pattern"; then
    echo "❌ 提交信息不符合规范！"
    echo ""
    echo "正确格式: type(scope): subject"
    echo "示例: feat(api): add fund search"
    exit 1
fi
```

---

## 四、示例场景

### 场景 1：完成基金检索 API

```bash
# 完成代码编写后
git status
git diff src/main/java/com/fund/controller/FundController.java

git add src/main/java/com/fund/controller/FundController.java
git add src/main/java/com/fund/service/FundService.java
git add src/main/java/com/fund/mapper/FundInfoMapper.java

git commit -m "feat(api): implement fund search with pinyin support

- add search by code/name/pinyin
- implement pagination and sorting
- add pinyin4j dependency

Related to #5"

git push
```

### 场景 2：修复数据库连接 Bug

```bash
# 修复后
git diff src/main/resources/application.yml

git add src/main/resources/application.yml

git commit -m "fix(config): correct database connection pool size

- change max pool size from 100 to 10
- add connection timeout settings

Fixes #8"

git push
```

### 场景 3：重构指标计算

```bash
# 重构完成后
git status

git add fund_collector/core/metrics_calculator.py
git add fund_collector/core/data_pipeline.py

git commit -m "refactor(collector): extract metrics calculation to separate module

- create MetricsCalculator class
- move calculation logic from collector.py
- add unit tests for metrics calculation

Closes #12"

git push
```

---

## 五、禁止行为

### 5.1 绝对禁止

```
❌ git commit -m "update"                    # 过于模糊
❌ git commit -m "fix"                       # 无类型无范围
❌ git commit -m "WIP"                       # 工作进行中
❌ git add . && git commit -m "update"       # 批量提交
❌ git commit -m "fix bug & add feature"     # 混合多个改动
```

### 5.2 避免行为

```
⚠️ 长时间不提交（超过半天）
⚠️ 提交后忘记推送
⚠️ 提交信息使用中文（项目统一使用英文）
⚠️ 提交包含未完成的代码
```

---

## 六、Skill 使用方式

### 6.1 对于 AI 助手

当用户要求执行任何代码改动时：

1. **评估改动范围**：确定是否构成一个完整任务
2. **执行改动**：完成代码修改
3. **强制提交**：
   ```bash
   git status                    # 查看改动
   git diff --stat              # 确认改动范围
   git add <相关文件>           # 添加文件
   git commit -m "type: subject" # 规范提交
   git push                     # 推送到远程
   ```
4. **报告结果**：告知用户提交信息

### 6.2 提交信息模板

```
<type>(<scope>): <subject>

- <detail 1>
- <detail 2>

<issue reference>
```

---

## 七、检查命令

```bash
# 查看提交历史
git log --oneline -20

# 查看提交统计
git shortlog -sn

# 查看某文件的提交历史
git log --follow --oneline -- path/to/file

# 检查提交信息格式
git log --pretty=format:"%s" | head -20
```

---

## 八、附录

### 8.1 常用提交示例

```bash
# 功能开发
feat(api): add user authentication
feat(ui): implement fund detail page
feat(collector): add multi-source data validation

# Bug 修复
fix(api): correct pagination total count
fix(db): resolve connection leak
fix(ui): fix chart rendering issue

# 文档更新
docs(api): update swagger annotations
docs(readme): add deployment guide

# 重构
refactor(service): extract common query logic
refactor(model): rename FundMetrics to FundIndicator

# 性能优化
perf(api): add redis cache for fund list
perf(db): optimize nav history query

# 构建相关
chore(deps): upgrade spring boot to 3.2.1
chore(config): update docker compose
```

---

**生效日期**：2026-02-28  
**维护者**：OpenClaw  
**更新频率**：随项目发展调整
