"""
配置模块
"""
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """应用配置"""
    
    # MySQL配置
    mysql_host: str = "127.0.0.1"
    mysql_port: int = 3307
    mysql_user: str = "fund"
    mysql_password: str = "fund123"
    mysql_db: str = "fund_system"
    
    # akshare配置
    request_delay: float = 0.5  # 请求间隔（秒）
    retry_times: int = 3        # 重试次数
    retry_delay: float = 1.0    # 重试延迟基数
    
    # 日志配置
    log_level: str = "INFO"
    log_file: str = "logs/collector.log"
    log_max_bytes: int = 10 * 1024 * 1024  # 10MB
    log_backup_count: int = 5
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """获取配置单例"""
    return Settings()


settings = get_settings()
