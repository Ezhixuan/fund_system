"""
数据库连接工具
"""
from contextlib import contextmanager
from typing import Generator

from sqlalchemy import create_engine, Engine, text
from sqlalchemy.pool import QueuePool

from config import settings


class DatabaseManager:
    """数据库管理器"""
    
    _instance: 'DatabaseManager' = None
    _engine: Engine = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def get_engine(self) -> Engine:
        """获取数据库引擎（单例）"""
        if self._engine is None:
            conn_str = (
                f"mysql+pymysql://{settings.mysql_user}:{settings.mysql_password}"
                f"@{settings.mysql_host}:{settings.mysql_port}/{settings.mysql_db}"
                f"?charset=utf8mb4"
            )
            self._engine = create_engine(
                conn_str,
                poolclass=QueuePool,
                pool_size=5,
                max_overflow=10,
                pool_pre_ping=True,
                pool_recycle=3600,
                echo=False
            )
        return self._engine
    
    @contextmanager
    def get_connection(self) -> Generator:
        """获取数据库连接上下文"""
        conn = self.get_engine().connect()
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()
    
    def test_connection(self) -> bool:
        """测试数据库连接"""
        try:
            with self.get_connection() as conn:
                result = conn.execute(text("SELECT 1"))
                return result.scalar() == 1
        except Exception as e:
            print(f"数据库连接失败: {e}")
            return False
    
    def execute(self, sql: str, params: dict = None) -> int:
        """执行SQL语句"""
        with self.get_connection() as conn:
            result = conn.execute(text(sql), params or {})
            return result.rowcount
    
    def fetch_one(self, sql: str, params: dict = None) -> dict:
        """查询单条记录"""
        with self.get_connection() as conn:
            result = conn.execute(text(sql), params or {})
            row = result.fetchone()
            if row:
                return dict(row._mapping)
            return None
    
    def fetch_all(self, sql: str, params: dict = None) -> list:
        """查询多条记录"""
        with self.get_connection() as conn:
            result = conn.execute(text(sql), params or {})
            return [dict(row._mapping) for row in result]


# 全局数据库管理器实例
db = DatabaseManager()
