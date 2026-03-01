# 基金交易决策辅助系统

<div align="center">

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.0-4FC08D.svg)](https://vuejs.org/)
[![Python](https://img.shields.io/badge/Python-3.11-yellow.svg)](https://www.python.org/)
[![Docker](https://img.shields.io/badge/Docker-✓-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

**个人基金投资分析与决策辅助系统**

[快速开始](#快速开始) • [功能特性](#功能特性) • [技术架构](#技术架构) • [API文档](#api文档) • [部署指南](#部署指南)

</div>

---

## 📖 项目简介

基金交易决策辅助系统是一款面向个人投资者的基金分析工具，通过数据采集、指标计算、智能评分和决策信号，帮助用户做出更理性的基金投资决策。

### 核心能力

- 📊 **数据采集** - 自动采集基金净值、持仓、指数等数据
- 📈 **全维指标** - 计算夏普比率、索提诺比率、卡玛比率等专业指标
- 🎯 **智能评分** - 多维度评分模型，S/A/B/C/D 等级评定
- 🚦 **决策信号** - 基于估值和指标的买卖建议
- 💼 **持仓管理** - 记录交易、分析收益
- 📱 **可视化** - 直观的图表展示和数据可视化

---

## 🚀 快速开始

### 方式一：Docker Compose 一键部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/Ezhixuan/fund_system.git
cd fund_system/deploy

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件设置密码

# 3. 启动服务
./start.sh

# 4. 访问系统
# 前端界面: http://localhost
# API接口: http://localhost:8080/api
```

### 方式二：本地开发环境

```bash
# 1. 启动数据库
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root123 mysql:8.0
docker run -d -p 6379:6379 redis:7-alpine

# 2. 初始化数据库
# 执行 docs/design/schema.sql

# 3. 启动后端
cd fund-service
mvn spring-boot:run

# 4. 启动前端
cd fund-view
npm install
npm run dev
```

---

## ✨ 功能特性

### 1. 基金数据管理
- ✅ 基金基础信息查询（支持拼音搜索）
- ✅ 净值历史数据展示
- ✅ 基金经理信息
- ✅ 持仓明细分析

### 2. 专业指标计算
| 指标类别 | 具体指标 |
|---------|---------|
| 收益指标 | 1月/3月/1年/3年/5年收益率 |
| 风险指标 | 最大回撤、波动率 |
| 风险调整后 | 夏普比率、索提诺比率、卡玛比率 |
| 风险因子 | 阿尔法、贝塔、信息比率 |
| 估值指标 | PE/PB 分位数 |

### 3. 智能评分系统
- 📊 五维评分（收益/风险/稳定性/规模/费用）
- 🏆 等级评定：S/A/B/C/D
- 🔝 TOP排名推荐
- 📋 基金对比分析

### 4. 决策信号引擎
- 🟢 买入信号 - 低估值+高质量
- 🟡 持有信号 - 估值合理
- 🔴 卖出信号 - 高估值/高风险
- 📈 信号历史复盘

### 5. 持仓管理
- 📝 交易记录录入
- 💰 持仓收益分析
- 📊 收益率计算
- 🎯 成本价跟踪

---

## 🏗️ 技术架构

### 后端技术栈
- **语言**: Java 17
- **框架**: Spring Boot 3.2
- **ORM**: MyBatis-Plus
- **缓存**: Redis + Redisson
- **数据库**: MySQL 8.0
- **监控**: Spring Boot Actuator + Micrometer

### 前端技术栈
- **框架**: Vue 3 + Vite
- **UI组件**: Element Plus
- **图表**: ECharts
- **HTTP**: Axios
- **状态管理**: Pinia

### 数据采集
- **语言**: Python 3.11
- **数据源**: akshare
- **调度**: APScheduler
- **数据处理**: pandas

### 部署运维
- **容器化**: Docker + Docker Compose
- **CI/CD**: GitHub Actions
- **监控**: Prometheus（预留）
- **备份**: 自动定时备份脚本

---

## 📚 API文档

启动服务后访问：
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

### 核心接口

#### 基金相关
```
GET    /api/funds                    # 基金列表
GET    /api/funds/{code}             # 基金详情
GET    /api/funds/{code}/metrics     # 基金指标
GET    /api/funds/{code}/nav         # 净值历史
GET    /api/funds/top                # TOP排名
GET    /api/funds/search             # 搜索基金
```

#### 决策信号
```
GET    /api/funds/{code}/signal      # 获取决策信号
GET    /api/funds/{code}/estimate    # 当日估值
```

#### 持仓管理
```
POST   /api/portfolio/trade          # 录入交易
GET    /api/portfolio/holdings       # 持仓列表
GET    /api/portfolio/analysis       # 持仓分析
```

#### 管理接口
```
GET    /admin/cache/stats            # 缓存统计
GET    /admin/perf/overview          # 性能概览
GET    /actuator/health              # 健康检查
GET    /actuator/metrics             # 指标列表
```

---

## 📦 部署指南

### 环境要求
- Docker 20.10+
- Docker Compose 2.0+
- 内存：建议 4GB+
- 磁盘：建议 20GB+

### 生产部署

```bash
# 1. 配置生产环境变量
cd deploy
vi .env

# 2. 启动服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f

# 4. 配置定时备份
crontab -e
# 添加: 0 2 * * * /path/to/deploy/backup.sh
```

### 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up -d --build

# 清理旧镜像
docker image prune -f
```

---

## 📁 项目结构

```
fund-system/
├── collector/              # Python 数据采集
│   ├── fund_collector/    # 采集模块
│   └── requirements.txt   # 依赖
├── fund-service/          # Java 后端服务
│   ├── src/main/java/     # 源代码
│   ├── src/main/resources/# 配置
│   └── Dockerfile         # 容器镜像
├── fund-view/             # Vue3 前端
│   ├── src/              # 源代码
│   └── dist/             # 构建输出
├── deploy/                # 部署配置
│   ├── docker-compose.yml
│   ├── nginx.conf
│   └── start.sh
├── docs/                  # 项目文档
│   ├── design/           # 设计文档
│   ├── plan/             # 实施计划
│   └── tasks/            # 任务记录
└── .github/workflows/     # CI/CD配置
```

---

## 🛠️ 开发指南

### 后端开发

```bash
cd fund-service

# 编译
mvn clean compile

# 运行
mvn spring-boot:run

# 测试
mvn test

# 打包
mvn clean package -DskipTests
```

### 前端开发

```bash
cd fund-view

# 安装依赖
npm install

# 开发模式
npm run dev

# 构建
npm run build

# 预览
npm run preview
```

### Python采集

```bash
cd collector

# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 运行采集
python -m fund_collector
```

---

## 📝 配置说明

### 环境变量

| 变量 | 说明 | 默认值 |
|-----|------|--------|
| DB_ROOT_PASS | MySQL root密码 | root123 |
| DB_PASS | 应用数据库密码 | fund123 |
| JAVA_OPTS | JVM参数 | -Xms1g -Xmx1g |
| SPRING_PROFILES_ACTIVE | Spring环境 | prod |

### 配置文件

- **后端**: `fund-service/src/main/resources/application.yml`
- **前端**: `fund-view/.env`
- **部署**: `deploy/.env`

---

## 🔒 安全建议

1. **修改默认密码** - 生产环境务必修改默认数据库密码
2. **配置HTTPS** - 使用 Nginx + Let's Encrypt 配置 SSL
3. **限制访问** - 管理接口配置 IP 白名单
4. **定期更新** - 及时更新依赖版本修复安全漏洞
5. **数据备份** - 配置定时备份，保留多个备份版本

---

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 开源协议

本项目基于 [MIT](LICENSE) 协议开源。

---

## 🙏 致谢

- [akshare](https://www.akshare.xyz/) - 金融数据采集库
- [Spring Boot](https://spring.io/projects/spring-boot) - Java后端框架
- [Vue.js](https://vuejs.org/) - 前端框架
- [Element Plus](https://element-plus.org/) - UI组件库

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐ Star！**

[回到顶部](#基金交易决策辅助系统)

</div>

---

## 🐛 故障排除

### Docker 部署后无法访问前端 (macOS)

**问题现象**: Docker Compose 启动成功，但浏览器无法访问 `http://127.0.0.1:10080`

**解决方案**:

1. **运行网络诊断工具**:
   ```bash
   make check
   # 或
   ./scripts/network-check.sh
   ```

2. **检查防火墙设置** (macOS):
   ```bash
   # 检查防火墙状态
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
   
   # 如果已启用，临时关闭测试
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
   ```

3. **使用备选方案 - 前端开发模式**:
   ```bash
   # 停止 Nginx
   cd deploy && docker-compose stop nginx
   
   # 启动前端开发服务器
   cd ../fund-view && npm run dev
   
   # 访问 http://localhost:5174
   ```

4. **检查端口占用**:
   ```bash
   lsof -i:10080
   # 如果被占用，修改 docker-compose.yml 中的端口映射
   ```

### 常见问题

| 问题 | 解决方案 |
|------|----------|
| 数据库连接失败 | 检查 MySQL 容器健康状态: `docker logs fund-mysql` |
| API 返回 500 错误 | 检查后端日志: `docker logs fund-api` |
| 前端白屏 | 检查 Nginx 配置和前端构建: `docker logs fund-nginx` |
| 端口冲突 | 修改 docker-compose.yml 中的端口映射 |

---

## 📞 技术支持

如遇到问题，请按以下顺序排查:

1. 查看本文档的 **故障排除** 章节
2. 运行 `./scripts/network-check.sh` 进行诊断
3. 查看 Issue 文档: `docs/issues/`
4. 提交 Issue 到 GitHub: https://github.com/Ezhixuan/fund_system/issues
