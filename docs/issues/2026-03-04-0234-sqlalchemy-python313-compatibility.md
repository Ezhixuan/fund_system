# Issue: SQLAlchemy 与 Python 3.13 兼容性问题导致指标计算失败

**Created**: 2026-03-04 02:34
**Status**: Open
**Priority**: High

## Problem Description

Python 采集服务在计算基金指标时出现错误：

```
ERROR | services.fund_data_service:calculate_metrics:269 -
计算指标失败: Class <class 'sqlalchemy.sql.elements.SQLCoreOperations'>
directly inherits TypingOnly but has additional attributes
{'__firstlineno__', '__static_attributes__'}.
```

虽然基础信息和净值数据可以正常采集保存，但指标计算失败，导致 `fund_metrics` 表数据缺失。

## Environment

- Project: fund-system/collector
- Python: 3.13
- SQLAlchemy: 2.0.23
- Location: `collector/services/fund_data_service.py:269`
- Related Function: `calculate_metrics()`

## Error Log

```
2026-03-04 02:32:17.207 | INFO     | __main__:collect_fund_data:154 - 开始采集基金完整数据: 011452
2026-03-04 02:32:19.560 | INFO     | services.fund_data_service:collect_fund_info:87 - 基金基础信息已保存: 华泰柏瑞质量成长C
2026-03-04 02:32:19.672 | INFO     | services.fund_data_service:collect_nav_history:105 - 获取到 1222 条净值记录
2026-03-04 02:32:19.737 | INFO     | services.fund_data_service:collect_nav_history:143 - 已保存 1222 条净值记录
2026-03-04 02:32:19.737 | INFO     | services.fund_data_service:calculate_metrics:152 - 计算基金指标: 011452
2026-03-04 02:32:19.746 | ERROR    | services.fund_data_service:calculate_metrics:269 - 计算指标失败: Class <class 'sqlalchemy.sql.elements.SQLCoreOperations'> directly inherits TypingOnly but has additional attributes {'__firstlineno__', '__static_attributes__'}.
```

## Analysis

### 技术原因

这是一个 **Python 3.13 + SQLAlchemy 2.0.23** 的兼容性问题。

1. **TypingOnly 类变化**: Python 3.13 中的 `typing` 模块对 `TypingOnly` 基类的实现有所变更
2. **SQLAlchemy 版本过旧**: SQLAlchemy 2.0.23 是在 Python 3.13 发布之前推出的，未完全适配 Python 3.13 的类型系统变化
3. **属性冲突**: SQLAlchemy 的 `SQLCoreOperations` 类继承自 `TypingOnly`，但 Python 3.13 要求 `TypingOnly` 子类不能有额外属性，而 SQLAlchemy 定义了 `__firstlineno__` 和 `__static_attributes__`

### 影响范围

- ✅ 基金基础信息采集 - 正常
- ✅ 净值历史采集 - 正常
- ❌ 基金指标计算 - 失败
- ❌ `fund_metrics` 表数据 - 缺失

### 错误定位

错误发生在 `fund_data_service.py` 第 269 行：
```python
# 计算指标后尝试保存到数据库时触发
session.add(metrics_entity)
session.commit()  # <-- 这里抛出异常
```

## Root Cause

SQLAlchemy 2.0.23 与 Python 3.13 的类型系统不兼容。具体来说是：

1. Python 3.13 引入了更严格的 `TypingOnly` 基类检查
2. SQLAlchemy 2.0.23 中的 `SQLCoreOperations` 类使用了某些类型注解特性
3. 当 SQLAlchemy 尝试创建或操作数据库实体时，触发类型系统的属性检查失败

## Solution

### Option 1 (Recommended): 升级 SQLAlchemy 到兼容版本

SQLAlchemy 2.0.36+ 已修复 Python 3.13 兼容性问题：

```bash
# 升级 SQLAlchemy
pip install sqlalchemy>=2.0.36

# 或修改 requirements.txt
# sqlalchemy==2.0.36
```

然后重新安装：
```bash
cd collector
source venv/bin/activate
pip install --upgrade sqlalchemy
```

### Option 2: 降级 Python 版本

使用 Python 3.11 或 3.12（已知兼容）：

```bash
# 使用 pyenv 切换 Python 版本
pyenv install 3.12.0
pyenv local 3.12.0

# 重新创建虚拟环境
rm -rf venv
python3.12 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### Option 3: 临时禁用指标计算

如果暂时无法升级，可以修改代码跳过指标计算：

```python
# collector/services/fund_data_service.py
def calculate_metrics(self, fund_code: str, nav_list: list) -> bool:
    """计算基金指标"""
    try:
        # ... 原有计算逻辑 ...
        pass
    except Exception as e:
        logger.error(f"计算指标失败: {e}")
        logger.warning(f"基金 {fund_code} 指标计算已跳过，请手动处理")
        return False  # 返回 False 但不影响整体流程
```

### Option 4: 使用独立进程计算指标

将指标计算拆分为独立脚本，使用不同的 Python 版本运行：

```bash
#!/bin/bash
# calculate_metrics.sh
# 使用 Python 3.12 运行指标计算
python3.12 -m metrics_calculator --fund-code $1
```

## Action Items

- [x] **立即处理**: 尝试升级 SQLAlchemy 到 2.0.36+
  ```bash
  pip install sqlalchemy==2.0.36
  ```
- [ ] **验证**: 升级后重新采集一只基金，验证指标是否正常计算
- [ ] **备选方案**: 如升级无效，考虑降级到 Python 3.12
- [ ] **文档更新**: 在 requirements.txt 中标注 Python 版本要求
- [ ] **CI/CD**: 添加 Python 版本兼容性测试

## Quick Fix

**立即执行以下命令尝试修复：**

```bash
cd collector
source venv/bin/activate

# 升级 SQLAlchemy 到最新兼容版本
pip install sqlalchemy>=2.0.36

# 验证安装
python -c "import sqlalchemy; print(sqlalchemy.__version__)"

# 重启采集服务
python app.py
```

如果升级后仍有问题，降级 Python：

```bash
# 安装 Python 3.12
brew install python@3.12

# 重建虚拟环境
rm -rf venv
/opt/homebrew/bin/python3.12 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Prevention

### 1. 锁定 Python 版本

在 `requirements.txt` 或 `pyproject.toml` 中指定 Python 版本：

```toml
[project]
requires-python = ">=3.11,<3.13"
```

或在 README 中明确说明：
```markdown
## Python 版本要求

- 推荐: Python 3.11 或 3.12
- 不支持: Python 3.13 (存在 SQLAlchemy 兼容性问题)
```

### 2. 添加依赖版本检查

在应用启动时检查依赖版本：

```python
# collector/app.py
import sys
import sqlalchemy

def check_dependencies():
    """检查依赖版本兼容性"""
    py_version = sys.version_info
    sa_version = tuple(map(int, sqlalchemy.__version__.split('.')[:3]))

    if py_version >= (3, 13) and sa_version < (2, 0, 36):
        logger.warning(f"Python {py_version.major}.{py_version.minor} 需要 SQLAlchemy >= 2.0.36")
        logger.warning(f"当前 SQLAlchemy {sqlalchemy.__version__}，请升级!")

check_dependencies()
```

### 3. 使用 Docker 锁定环境

```dockerfile
FROM python:3.12-slim

# 明确使用 Python 3.12
RUN python --version

COPY requirements.txt .
RUN pip install -r requirements.txt
```

## Reference

- SQLAlchemy Python 3.13 Support: https://docs.sqlalchemy.org/en/20/changelog/changelog_20.html
- Python 3.13 Release Notes: https://docs.python.org/3/whatsnew/3.13.html
- Related SQLAlchemy Issue: https://github.com/sqlalchemy/sqlalchemy/issues/11126
- `requirements.txt`: `sqlalchemy==2.0.23`
- Error Location: `collector/services/fund_data_service.py:269`
