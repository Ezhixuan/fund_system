# ISSUE-001 技术设计方案

## 问题定位

经过分析，问题的核心是 **macOS Docker Desktop 的网络隔离机制**:

```
macOS Docker 网络架构:
┌─────────────────────────────────────────────────────────┐
│                      macOS 宿主机                        │
│  ┌─────────────┐     ┌──────────────────────────────┐  │
│  │   浏览器     │────>│     Docker Desktop VM        │  │
│  │  localhost  │     │  ┌────────────────────────┐  │  │
│  └─────────────┘     │  │    Linux 虚拟机         │  │  │
│                      │  │  ┌──────────────────┐  │  │  │
│                      │  │  │  Docker Network  │  │  │  │
│                      │  │  │ ┌─────┐ ┌─────┐ │  │  │  │
│                      │  │  │ │Nginx│ │Java │ │  │  │  │
│                      │  │  │ │:80  │ │:8080│ │  │  │  │
│                      │  │  │ └─────┘ └─────┘ │  │  │  │
│                      │  │  └──────────────────┘  │  │  │
│                      │  └────────────────────────┘  │  │
│                      └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
         ↑                           ↑
    浏览器直接访问              端口映射到 VM
    无法到达容器                容器可正常访问
```

## 根本原因

1. **Docker Desktop for Mac** 运行在一个 Linux 虚拟机中
2. 端口映射 `0.0.0.0:10080->80` 实际上是映射到 **VM 的端口**
3. 虽然 curl 可以工作（通过 Docker 的端口转发），但浏览器直接访问 localhost 时，可能因为防火墙或网络配置问题无法到达

## 技术方案

### 方案 1: 修复端口绑定 (推荐)

修改 docker-compose.yml，明确绑定到 127.0.0.1:

```yaml
nginx:
  ports:
    - "127.0.0.1:10080:80"  # 明确绑定到 localhost
```

同时修改 Nginx 配置，添加对 localhost 的响应支持。

### 方案 2: 使用 host.docker.internal

在容器内部访问宿主机服务，但这里我们需要的是宿主机访问容器。

### 方案 3: 检查并配置 macOS 防火墙

```bash
# 允许 Docker 网络访问
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /Applications/Docker.app/Contents/MacOS/Docker
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp /Applications/Docker.app/Contents/MacOS/Docker
```

### 方案 4: 使用 docker.for.mac.host.internal

在 Nginx 配置中使用特殊的 DNS 名称。

## 最终方案: 多维度修复

### 1. 修改 docker-compose.yml
- 明确端口绑定到 127.0.0.1
- 移除 IPv6 绑定（避免冲突）
- 添加网络模式配置

### 2. 修改 Nginx 配置
- 添加 server_name localhost 127.0.0.1
- 添加 access log 便于调试
- 配置错误页面

### 3. 添加诊断脚本
- 网络连通性测试
- 端口占用检查
- 防火墙状态检查

### 4. 提供备选访问方式
- 使用后端 API 直接访问
- 使用前端开发服务器 (vite)
