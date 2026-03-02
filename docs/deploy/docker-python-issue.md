# Python Docker 构建问题记录

## 问题描述

尝试将 Python 采集服务打包到 Docker 中，但遇到以下问题：

1. **镜像拉取超时**
   - python:3.11-slim - TLS handshake timeout
   - alpine:3.19 - digest mismatch error
   - registry.cn-hangzhou.aliyuncs.com - 无响应

2. **可能原因**
   - Docker Desktop 网络配置问题
   - 国内网络环境问题
   - Docker 镜像仓库访问不稳定

## 可行方案

### 方案 1: 继续使用宿主机 Python 服务（当前使用）

**配置**:
```yaml
java-backend:
  environment:
    - COLLECTOR_URL=http://host.docker.internal:5001
```

**优点**:
- 已验证工作正常
- 无需解决 Docker 网络问题
- Python 依赖安装方便

**缺点**:
- 服务不在同一网络内
- 宿主机重启后需手动启动 Python

---

### 方案 2: 使用 Docker Host 网络模式

**docker-compose.yml**:
```yaml
fund-collect:
  image: python:3.11-slim  # 或本地构建的镜像
  network_mode: host
  # ... 其他配置
```

**优点**:
- 网络性能最好
- 配置简单

**缺点**:
- 仅支持 Linux
- 端口可能冲突

---

### 方案 3: 排查 Docker 后重新构建

**建议步骤**:
```bash
# 1. 清理 Docker 缓存
docker system prune -a

# 2. 检查 Docker 网络
docker network ls

# 3. 尝试拉取镜像
docker pull python:3.11-alpine

# 4. 使用代理或镜像加速
# 在 Docker Desktop 设置中配置镜像加速器
```

**推荐镜像加速器**:
- 阿里云: https://<你的ID>.mirror.aliyuncs.com
- 网易云: https://hub-mirror.c.163.com
- 中科大: https://docker.mirrors.ustc.edu.cn

---

## 当前状态

- ✅ Java 服务: Docker 中运行正常
- ✅ 前端服务: Docker 中运行正常  
- ✅ MySQL/Redis: Docker 中运行正常
- ⚠️ Python 服务: 宿主机运行，Docker 构建待解决

## 建议

当前使用**方案 1**（宿主机运行）是最稳定的选择。如需将 Python 打包到 Docker，建议：

1. 先检查 Docker Desktop 的网络设置
2. 配置镜像加速器
3. 在非高峰时段重新尝试构建
