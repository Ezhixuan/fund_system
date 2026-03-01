# 基金交易系统部署手册

## 环境要求

### 必需环境
- **Java**: 17或更高版本
- **MySQL**: 8.0或更高版本
- **Redis**: 7.0或更高版本
- **Node.js**: 18或更高版本（前端开发）
- **Python**: 3.11或更高版本（数据采集）

### 可选环境
- **Nginx**: 用于反向代理和静态资源服务
- **Docker**: 用于容器化部署

---

## 快速启动

### 1. 数据库初始化
```bash
# 创建数据库
mysql -uroot -p -e "CREATE DATABASE fund_system CHARACTER SET utf8mb4;"

# 创建用户
mysql -uroot -p -e "CREATE USER 'fund'@'%' IDENTIFIED BY 'fund123';"
mysql -uroot -p -e "GRANT ALL PRIVILEGES ON fund_system.* TO 'fund'@'%';"
```

### 2. 后端启动
```bash
cd fund-service
mvn spring-boot:run
```
后端服务默认运行在 http://localhost:8080

### 3. 前端启动
```bash
cd fund-view
npm install
npm run dev
```
前端开发服务器默认运行在 http://localhost:5173

### 4. Python数据采集
```bash
cd collector
pip install -r requirements.txt
python3 collector.py
```

---

## 生产环境部署

### 后端打包
```bash
cd fund-service
mvn clean package -DskipTests
```

### 前端打包
```bash
cd fund-view
npm run build
```

### Nginx配置示例
```nginx
server {
    listen 80;
    server_name fund.example.com;

    location / {
        root /path/to/fund-view/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 验证部署

### API健康检查
```bash
curl http://localhost:8080/api/monitor/health
```

### Swagger文档
访问 http://localhost:8080/swagger-ui.html
