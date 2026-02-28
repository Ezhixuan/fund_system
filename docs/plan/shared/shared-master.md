# Shared: 跨模块计划 - 主计划

> 贯穿全项目的共享计划

---

## 一、子计划清单

| 编号 | 名称 | 说明 |
|------|------|------|
| S-01 | 部署方案实施 | Docker Compose配置、CI/CD |
| S-02 | 测试策略 | 单元测试、集成测试、压测 |
| S-03 | 数据备份与恢复 | 定期备份、灾难恢复 |

---

## 二、S-01: 部署方案

### Docker Compose完整配置
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: fund-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASS:-root123}
      MYSQL_DATABASE: fund_system
      MYSQL_USER: fund
      MYSQL_PASSWORD: ${DB_PASS:-fund123}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    networks:
      - fund_net

  redis:
    image: redis:7-alpine
    container_name: fund-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - fund_net

  java-backend:
    build: 
      context: ./fund-service
      dockerfile: Dockerfile
    container_name: fund-api
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis
    networks:
      - fund_net

volumes:
  mysql_data:
  redis_data:

networks:
  fund_net:
    driver: bridge
```

---

## 三、S-02: 测试策略

### 测试分层
```
单元测试 (JUnit/TestNG)
    ↓
集成测试 (SpringBootTest)
    ↓
API测试 (Postman/JMeter)
    ↓
端到端测试 (Selenium)
```

### 核心测试用例
1. **基金检索API测试**
   - 关键词搜索
   - 拼音搜索
   - 分页功能

2. **数据管道测试**
   - 采集->校验->合并流程
   - 异常重试
   - 数据一致性

3. **决策信号测试**
   - 不同场景信号生成
   - 信号历史复盘

---

## 四、S-03: 数据备份

### 备份策略
```bash
#!/bin/bash
# backup.sh - 每日凌晨2点执行

DATE=$(date +%Y%m%d)
BACKUP_DIR="/backup/fund-system"

# MySQL备份
mysqldump -h localhost -u root -p fund_system > $BACKUP_DIR/fund_system_$DATE.sql

# Redis备份
redis-cli BGSAVE
cp /var/lib/redis/dump.rdb $BACKUP_DIR/redis_$DATE.rdb

# 保留最近7天
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.rdb" -mtime +7 -delete
```

---

**更新日期**：2026-02-28
