# ISSUE-001 实施计划

## 任务列表

### Task 1: 修复 Docker 网络配置
**状态**: 🔄 进行中
**负责人**: OpenClaw
**优先级**: P0

#### 执行内容
- [x] 分析网络问题根因
- [ ] 修改 docker-compose.yml 端口绑定
- [ ] 修改 Nginx 配置支持 localhost
- [ ] 测试修复效果

#### 修改文件
1. `deploy/docker-compose.yml`
2. `deploy/nginx.conf`

---

### Task 2: 添加网络诊断工具
**状态**: ⏳ 待开始
**负责人**: OpenClaw
**优先级**: P1

#### 执行内容
- [ ] 创建网络诊断脚本
- [ ] 添加端口连通性检查
- [ ] 添加防火墙状态检查

#### 修改文件
1. `scripts/network-check.sh`

---

### Task 3: 提供备选访问方案
**状态**: ✅ 已完成
**负责人**: OpenClaw
**优先级**: P1

#### 执行内容
- [x] 启动前端开发服务器 (Vite)
- [x] 验证 http://localhost:5174/ 可访问
- [x] 更新文档说明

---

### Task 4: 更新部署文档
**状态**: ⏳ 待开始
**负责人**: OpenClaw
**优先级**: P2

#### 执行内容
- [ ] 记录已知问题和解决方案
- [ ] 更新 README 的故障排除部分
- [ ] 添加 macOS 特殊说明

---

## 实施步骤

### Step 1: 修复端口绑定 (5分钟)

```yaml
# deploy/docker-compose.yml
services:
  nginx:
    ports:
      - "127.0.0.1:10080:80"  # 明确绑定到 localhost
```

### Step 2: 修复 Nginx 配置 (5分钟)

```nginx
server {
    listen 80;
    server_name localhost 127.0.0.1;
    # ... 其他配置
}
```

### Step 3: 重启服务并测试 (5分钟)

```bash
docker-compose down
docker-compose up -d
curl http://127.0.0.1:10080/
```

### Step 4: 浏览器验证 (5分钟)

在浏览器中访问:
- http://127.0.0.1:10080
- http://localhost:10080

---

## 验收标准

- [ ] 浏览器能正常访问 http://127.0.0.1:10080
- [ ] 浏览器能正常访问 http://localhost:10080
- [ ] 所有服务保持 healthy 状态
- [ ] API 接口正常工作

---

## 备选方案

如果修复无效，提供以下备选:

1. **使用开发服务器**: `npm run dev` (端口 5174)
2. **直接使用 IP**: 使用本机 IP 而非 localhost
3. **使用 Docker Host 网络**: 仅限 Linux 环境
