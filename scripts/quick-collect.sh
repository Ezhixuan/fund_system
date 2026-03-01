#!/bin/bash
# 基金数据快速采集脚本

set -e

echo "========================================"
echo "    基金数据采集"
echo "========================================"

cd /Users/ezhixuan/Projects/fund-system/collector

# 检查虚拟环境
if [ ! -d "venv" ]; then
    echo "创建虚拟环境..."
    python3 -m venv venv
fi

source venv/bin/activate

echo ""
echo "[1/5] 采集基金列表..."
python main.py --action list 2>&1 | tail -5

echo ""
echo "[2/5] 采集基金基础信息（100只）..."
python main.py --action basic --limit 100 2>&1 | tail -5

echo ""
echo "[3/5] 采集今日净值..."
python main.py --action nav 2>&1 | tail -5

echo ""
echo "[4/5] 执行数据管道（校验+合并）..."
python main.py --action pipeline 2>&1 | tail -5

echo ""
echo "[5/5] 计算基金指标..."
python main.py --action metrics 2>&1 | tail -5

echo ""
echo "========================================"
echo "    采集完成！"
echo "========================================"
echo ""
echo "检查数据状态..."
python main.py --action health 2>&1 | grep -E "fund_info|fund_nav|fund_metrics|状态" || echo "健康检查完成"
