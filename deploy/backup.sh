#!/bin/bash
# 基金系统数据备份脚本
# 建议添加到 crontab: 0 2 * * * /path/to/backup.sh

set -e

# 配置
BACKUP_DIR="/backup/fund-system"
DATE=$(date +%Y%m%d_%H%M%S)
KEEP_DAYS=7

# 创建备份目录
mkdir -p "$BACKUP_DIR"

echo "=========================================="
echo "    基金系统数据备份"
echo "    时间: $(date)"
echo "=========================================="

# MySQL 备份
echo "[1/3] 备份 MySQL 数据库..."
docker exec fund-mysql mysqldump -u root -p"${DB_ROOT_PASS:-root123}" --single-transaction fund_system > "$BACKUP_DIR/fund_system_$DATE.sql"

if [ $? -eq 0 ]; then
    echo "  ✓ MySQL 备份成功: fund_system_$DATE.sql"
    # 压缩
    gzip "$BACKUP_DIR/fund_system_$DATE.sql"
    echo "  ✓ 压缩完成: fund_system_$DATE.sql.gz"
else
    echo "  ✗ MySQL 备份失败"
fi

# Redis 备份
echo "[2/3] 备份 Redis 数据..."
docker exec fund-redis redis-cli BGSAVE
docker cp fund-redis:/data/dump.rdb "$BACKUP_DIR/redis_$DATE.rdb"

if [ $? -eq 0 ]; then
    echo "  ✓ Redis 备份成功: redis_$DATE.rdb"
else
    echo "  ✗ Redis 备份失败"
fi

# 清理旧备份
echo "[3/3] 清理旧备份（保留 $KEEP_DAYS 天）..."
find "$BACKUP_DIR" -name "fund_system_*.sql.gz" -mtime +$KEEP_DAYS -delete
find "$BACKUP_DIR" -name "redis_*.rdb" -mtime +$KEEP_DAYS -delete
echo "  ✓ 清理完成"

# 备份统计
echo ""
echo "备份统计:"
echo "  备份目录: $BACKUP_DIR"
echo "  备份数量: $(ls -1 $BACKUP_DIR | wc -l) 个文件"
echo "  占用空间: $(du -sh $BACKUP_DIR | cut -f1)"
echo ""
echo "=========================================="
echo "    备份完成: $(date)"
echo "=========================================="
