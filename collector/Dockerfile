FROM nginx:alpine

WORKDIR /app

# 安装 Python3 和 pip
RUN apk add --no-cache python3 py3-pip py3-flask py3-requests py3-pandas py3-sqlalchemy curl

# 复制应用代码
COPY . .

# 安装剩余依赖
RUN pip3 install --no-cache-dir akshare -i https://pypi.tuna.tsinghua.edu.cn/simple --break-system-packages || true

# 设置环境变量
ENV FLASK_APP=app.py
ENV COLLECTOR_PORT=5000
ENV PYTHONUNBUFFERED=1

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5000/health || exit 1

EXPOSE 5000

CMD ["python3", "app.py"]
