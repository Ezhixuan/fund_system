# 基金数据采集指南

## 数据采集机制

### 1. 采集方式

本项目使用 **Python + akshare** 进行数据采集，支持两种模式：

#### 模式一：手动采集（当前模式）
```bash
cd collector
source venv/bin/activate
python main.py --action list          # 采集基金列表
python main.py --action basic         # 采集基础信息
python main.py --action nav           # 采集净值数据
python main.py --action pipeline      # 执行数据管道
```

#### 模式二：定时自动采集（需要配置）
```bash
python main.py --action scheduler     # 启动定时调度器
```

### 2. 为什么现在没有数据？

**原因**: 
1. 数据采集是**手动触发**的，需要运行 Python 脚本
2. Docker 只部署了后端服务，没有自动运行采集
3. 数据库刚初始化，还没有执行过采集任务

### 3. 立即获取数据

#### 步骤 1: 进入采集目录
```bash
cd /Users/ezhixuan/Projects/fund-system/collector
source venv/bin/activate
```

#### 步骤 2: 执行完整采集流程
```bash
# 1. 采集基金列表（约2分钟）
python main.py --action list

# 2. 采集基础信息（约5分钟，采集100只基金）
python main.py --action basic --limit 100

# 3. 采集今日净值（约3分钟）
python main.py --action nav

# 4. 执行数据管道（校验+合并）
python main.py --action pipeline

# 5. 计算指标
python main.py --action metrics
```

#### 步骤 3: 验证数据
```bash
# 检查数据库中的数据量
python main.py --action health
```

### 4. 配置定时自动采集

#### 使用系统定时任务 (crontab)
```bash
# 编辑 crontab
crontab -e

# 添加定时任务（每天下午3点执行采集）
0 15 * * * cd /Users/ezhixuan/Projects/fund-system/collector && /usr/bin/python3 main.py --action nav && /usr/bin/python3 main.py --action pipeline

# 每天晚上8点计算指标
0 20 * * * cd /Users/ezhixuan/Projects/fund-system/collector && /usr/bin/python3 main.py --action metrics
```

#### 使用 Python 调度器
```bash
# 启动内置调度器（前台运行）
python main.py --action scheduler

# 或使用 nohup 后台运行
nohup python main.py --action scheduler > logs/scheduler.log 2>&1 &
echo $! > scheduler.pid
```

### 5. 采集的数据类型

| 数据类型 | 说明 | 采集频率 |
|---------|------|---------|
| fund_info | 基金基础信息 | 每周一次 |
| fund_nav | 基金净值历史 | 每日收盘后 |
| fund_metrics | 全维指标计算 | 每日净值更新后 |
| fund_score | 基金评分 | 指标计算后 |
| fund_holding | 基金持仓 | 每季度（财报季后） |

### 6. 数据源

- **akshare**: 东方财富、天天基金等公开数据
- **更新延迟**: T+1（当日数据次日更新）
- **数据完整性**: 取决于 akshare 和数据源

### 7. 快速开始脚本

创建一个一键采集脚本：

```bash
#!/bin/bash
# quick-collect.sh

cd /Users/ezhixuan/Projects/fund-system/collector
source venv/bin/activate

echo "开始数据采集..."
python main.py --action list
python main.py --action basic --limit 100
python main.py --action nav
python main.py --action pipeline
python main.py --action metrics

echo "采集完成！"
python main.py --action health
```

运行：
```bash
chmod +x quick-collect.sh
./quick-collect.sh
```

---

## 注意事项

1. **首次采集需要较长时间**（约10-15分钟）
2. **akshare 依赖网络**，请确保网络通畅
3. **采集频率不宜过高**，避免被封IP
4. **建议在交易日下午3点后采集**（当天数据已更新）
