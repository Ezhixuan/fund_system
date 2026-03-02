
---

## 最终结论

经过多次尝试，**Python 采集服务在 Alpine 容器内运行存在以下困难**：

1. **依赖复杂**：akshare 依赖 numpy、pandas 等需要编译的包
2. **编译困难**：Alpine 使用 musl libc，与 manylinux  wheels 不兼容
3. **构建时间长**：即使安装 build-base，编译也需要 5-10 分钟
4. **安装失败**：即使编译完成，部分依赖仍可能安装失败

## 可行方案

### 方案 1: 继续使用宿主机 Python 服务（推荐）

**当前配置**：
```yaml
java-backend:
  environment:
    - COLLECTOR_URL=http://host.docker.internal:5001
```

**启动方式**：
```bash
cd collector
source venv/bin/activate
export COLLECTOR_PORT=5001
python app.py
```

**优点**：
- ✅ 已验证稳定运行
- ✅ 所有依赖完整可用
- ✅ 无需额外配置

---

### 方案 2: 使用非 Alpine 的 Python 镜像

**Dockerfile 示例**：
```dockerfile
FROM python:3.11-slim-bookworm

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
ENV COLLECTOR_PORT=5000
EXPOSE 5000
CMD ["python", "app.py"]
```

**前提**：需要能稳定拉取 `python:3.11-slim-bookworm` 镜像

---

### 方案 3: 分离服务架构

将 Python 服务拆分为：
- **API 网关**（Docker）：提供 REST API 接口给 Java
- **采集Worker**（宿主机）：实际执行 akshare 采集

通过 Redis 或消息队列通信。

---

## 当前部署状态

| 服务 | 位置 | 端口 | 状态 |
|------|------|------|------|
| Nginx (前端) | Docker | 8888 | ✅ 运行中 |
| Java 后端 | Docker | 18080 | ✅ Healthy |
| MySQL | Docker | 13306 | ✅ Healthy |
| Redis | Docker | 16379 | ✅ Healthy |
| Python 采集 | 宿主机 | 5001 | ✅ 运行中 |

**建议**：当前使用方案 1，所有服务已正常运行。
