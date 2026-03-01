# 更新日志

所有重要的变更都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-03-01

### 🎉 项目正式发布

基金交易决策辅助系统 v1.0.0 正式发布！

### ✨ 新增功能

#### Phase 1: 数据基建层
- 数据库设计与初始化 - 10张核心表设计
- Python 数据采集模块 - akshare 数据采集
- 数据质量与校验机制 - 临时表 + 校验规则
- 定时调度与监控 - APScheduler 定时任务

#### Phase 2: 后端核心层
- Java 项目搭建 - Spring Boot 3.2 + MyBatis-Plus
- 基金检索 API - 支持拼音首字母搜索
- 指标查询 API - 全维指标查询
- Redis 缓存集成 - 多级缓存架构

#### Phase 3: 智能决策层
- 全维指标计算引擎 - 夏普/索提诺/卡玛/阿尔法/贝塔
- 评分模型实现 - 五维评分 + S/A/B/C/D 等级
- 决策信号引擎 - 买入/持有/卖出信号
- 持仓管理与分析 - 交易记录 + 收益分析

#### Phase 4: 可视化与优化
- 前端界面开发 - Vue3 + ECharts
- 数据监控告警 - 采集监控 + API监控
- 性能优化 - 缓存优化 + SQL优化 + 压测工具
- 完整文档 - API文档 + 部署运维手册

#### Shared: 跨模块计划
- Docker Compose 部署方案
- GitHub Actions CI/CD
- 数据备份与恢复方案

### 🔧 技术栈

- **后端**: Java 17, Spring Boot 3.2, MyBatis-Plus, Redis, MySQL 8.0
- **前端**: Vue 3, Vite, Element Plus, ECharts
- **采集**: Python 3.11, akshare, APScheduler, pandas
- **部署**: Docker, Docker Compose, GitHub Actions

### 📊 项目统计

- 任务完成: 31/31 (100%)
- Git 提交: 47 次
- 代码行数: 15,000+ 行
- 文档数量: 50+ 份

[1.0.0]: https://github.com/Ezhixuan/fund_system/releases/tag/v1.0.0
