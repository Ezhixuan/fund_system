# Issue Tracker Skill

## 简介

自定义 Skill，用于在项目中记录和分析问题。当遇到需要跟踪的问题时，使用 `/issue` 命令创建结构化的 issue 文档。

## 使用方法

在 Claude Code 中输入：

```
/issue 问题描述
```

例如：
```
/issue 数据库连接超时导致接口响应慢
```

## 功能

1. **自动创建目录**: 检查并创建 `docs/issues/` 文件夹
2. **生成文档**: 以 markdown 格式创建问题记录
3. **结构化分析**: 包含问题描述、环境、分析、根因、解决方案等

## 文档格式

创建的 issue 文档包含以下部分：

- **问题描述**: 用户提供的原始问题
- **环境**: 项目信息和相关文件路径
- **分析**: Claude 对问题的分析
- **根因**: 识别出的根本原因
- **解决方案**: 多个可选方案（含推荐方案）
- **行动项**: 具体的待办任务

## 文件名格式

```
docs/issues/YYYY-MM-DD-HHMM-issue-title.md
```

例如：
```
docs/issues/2026-03-04-1430-database-connection-timeout.md
```

## 示例

### 输入
```
/issue Python 采集服务启动失败
```

### 输出文件
文件: `docs/issues/2026-03-04-1435-python-collector-start-failed.md`

内容：
```markdown
# Issue: Python 采集服务启动失败

**Created**: 2026-03-04 14:35
**Status**: Open
**Priority**: High

## Problem Description

Python 采集服务启动失败，无法采集基金数据。

## Environment

- Project: fund-system
- Location: collector/app.py

## Analysis

依赖包 flask 未安装，导致导入错误。

## Root Cause

虚拟环境中的依赖未正确安装。

## Solution

### Option 1 (Recommended)
运行 `pip install -r requirements.txt` 安装依赖。

### Option 2
手动安装 flask: `pip install flask`

## Action Items

- [ ] 安装 Python 依赖
- [ ] 验证服务启动
- [ ] 更新文档说明

## Related

- 相关文档: collector/README.md
```

## 注意事项

- Issue 文档使用中文编写
- 文件名使用 kebab-case（小写+连字符）
- 自动包含时间戳确保唯一性
- 如问题描述不清晰，Claude 会询问细节
