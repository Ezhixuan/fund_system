# Task: 修复 config.settings 导入属性错误

**Created**: 2026-03-04 03:35
**Category**: config
**Status**: Completed
**Priority**: P1

## 任务描述

启动 Python 采集服务时出现错误：
```
ERROR | __main__:init_startup_sync:424 - 初始化启动同步失败: module 'config.settings' has no attribute 'get_db_config'
```

问题根因：Python 模块导入路径解析问题，存在两套配置系统导致导入歧义。

## 执行步骤

### Step 1: 诊断问题
- [x] 分析错误堆栈和导入路径
- [x] 检查发现 `config/` 目录存在两套配置：
  - `config/__init__.py` - pydantic Settings 配置
  - `config/settings.py` - Flask Config 类配置
- [x] 确认问题：当执行 `from config import settings` 时，Python 可能因 `settings.py` 文件存在而直接导入该模块

### Step 2: 统一配置系统
- [x] 简化 `config/__init__.py`，只作为导出入口
- [x] 统一配置逻辑到 `config/settings.py`
- [x] 在 `config/settings.py` 中添加 `_SettingsCompat` 类提供统一接口
- [x] 添加模块级别兼容函数 `get_db_config()` 和 `get_db_url()`

### Step 3: 测试验证
- [x] 清除 Python 缓存
- [x] 测试配置导入
- [x] 验证所有配置属性可访问

**完成时间**: 2026-03-04 03:37

## 实现详情

### 文件修改

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `config/__init__.py` | 重写 | 简化为导出入口，统一从 settings.py 导入 |
| `config/settings.py` | 修改 | 添加 `_SettingsCompat` 类和模块级兼容函数 |

### 代码变更

**config/__init__.py**:
```python
# 统一从 settings.py 导入所有配置
from config.settings import Config, settings

__all__ = ['Config', 'settings']
```

**config/settings.py**:
```python
# 添加兼容类
class _SettingsCompat:
    """兼容层：提供与 pydantic Settings 相同的接口"""

    @property
    def mysql_host(self):
        return Config.MYSQL_HOST

    def get_db_config(self) -> dict:
        return Config.get_db_config()

    # ... 其他属性和方法

settings = _SettingsCompat()

# 模块级别兼容函数
def get_db_config() -> dict:
    return Config.get_db_config()
```

## 测试验证

### TC1: 配置导入测试
**目的**: 验证 `from config import settings` 正常工作
**步骤**:
1. 清除 Python 缓存
2. 执行导入测试

**实际结果**:
```
get_db_config: True
DB Config: {'host': '127.0.0.1', 'port': 3307, 'user': 'root', ...}
```
**状态**: ✅ PASS

### TC2: 所有配置属性测试
**目的**: 验证所有需要的配置属性都可访问
**实际结果**:
- ✅ settings.get_db_config()
- ✅ settings.get_db_url()
- ✅ settings.mysql_host / mysql_port / mysql_user
- ✅ settings.env
- ✅ settings.request_delay / retry_times / retry_delay

**状态**: ✅ PASS

### TC3: Config 类导入测试
**目的**: 验证 `from config import Config` 正常工作
**实际结果**: ✅ Config 类可正常导入，类方法可用

**状态**: ✅ PASS

## 测试结果汇总

| 测试项 | 总数 | 通过 | 失败 | 跳过 |
|--------|------|------|------|------|
| 配置导入测试 | 3 | 3 | 0 | 0 |
| 属性访问测试 | 10 | 10 | 0 | 0 |
| **合计** | **13** | **13** | **0** | **0** |

## 执行结果

- **状态**: ✅ 成功
- **完成时间**: 2026-03-04 03:37
- **耗时**: 2 分钟

## 备注

- 现在所有代码使用统一的配置导入方式：`from config import settings`
- Config 类仍然可用：`from config import Config` 或 `from config.settings import Config`
- 开发环境默认使用端口 3307，生产环境使用端口 13306
