# 基金交易系统运维手册

## 日常检查清单

### 每日检查
- [ ] 数据采集是否成功（检查/api/monitor/collection/stats）
- [ ] API响应时间是否正常（检查/api/monitor/api/performance）
- [ ] 是否有告警（检查/api/monitor/alerts/current）

### 每周检查
- [ ] 数据库备份
- [ ] 日志清理
- [ ] 磁盘空间检查

---

## 常用命令

### 查看日志
```bash
# 后端日志
tail -f fund-service/logs/application.log

# Python采集日志
tail -f collector/logs/collector.log
```

### 重启服务
```bash
# 重启后端
pkill -f "spring-boot:run"
cd fund-service && mvn spring-boot:run

# 重启前端
pkill -f "vite"
cd fund-view && npm run dev
```

### 数据库操作
```bash
# 备份数据库
mysqldump -h127.0.0.1 -P3307 -ufund -pfund123 fund_system > backup.sql

# 恢复数据库
mysql -h127.0.0.1 -P3307 -ufund -pfund123 fund_system < backup.sql
```

---

## 故障处理

### API响应慢
1. 检查数据库连接池状态
2. 检查Redis缓存命中率
3. 查看慢查询日志

### 数据采集失败
1. 检查网络连接
2. 检查akshare接口状态
3. 查看采集日志

### 内存不足
1. 调整JVM参数：-Xmx2g
2. 清理Redis缓存
3. 重启服务

---

## 监控地址

- **健康状态**: http://localhost:8080/api/monitor/health
- **API性能**: http://localhost:8080/api/monitor/api/performance
- **当前告警**: http://localhost:8080/api/monitor/alerts/current
- **Swagger文档**: http://localhost:8080/swagger-ui.html
