"""
配置模块
支持 development/production 环境切换

统一从 config.settings 导入配置
"""

# 统一从 settings.py 导入所有配置
from config.settings import Config, settings

__all__ = ['Config', 'settings']
