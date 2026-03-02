FROM python:3.11-alpine

WORKDIR /app

# 安装编译工具
RUN apk add --no-cache curl build-base

# 安装所有依赖
RUN pip install --no-cache-dir \
    flask==3.0.0 \
    flask-cors==4.0.0 \
    requests==2.31.0 \
    python-dotenv==1.0.0 \
    loguru==0.7.2 \
    apscheduler==3.10.4 \
    pydantic==2.5.0 \
    pydantic-settings==2.1.0 \
    pymysql==1.1.0 \
    sqlalchemy==2.0.23 \
    redis==5.0.1 \
    numpy==1.26.2 \
    pandas==2.1.3 \
    akshare==1.11.0 \
    -i https://pypi.tuna.tsinghua.edu.cn/simple 2>&1 || true

# 复制代码
COPY . .

ENV FLASK_APP=app.py
ENV COLLECTOR_PORT=5000
ENV PYTHONUNBUFFERED=1

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5000/health || exit 1

EXPOSE 5000

CMD ["python", "app.py"]
