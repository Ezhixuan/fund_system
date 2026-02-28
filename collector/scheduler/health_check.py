#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
健康检查HTTP接口 (Flask)

用法:
    python health_check.py              # 启动服务 (默认端口5000)
    python health_check.py --port 8080  # 指定端口

接口:
    GET /health          # 健康检查
    GET /metrics         # 指标数据
    GET /status          # 系统状态
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import argparse
from flask import Flask, jsonify
from sqlalchemy import text
from datetime import datetime
import logging

from config import settings
from utils.database import db
from core.alert_trigger import AlertTrigger

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)


@app.route('/health')
def health():
    """
    健康检查接口
    返回: {"status": "healthy"|"unhealthy", "checks": {...}}
    """
    checks = {}
    
    # 1. 检查数据库连接
    try:
        with db.get_connection() as conn:
            result = conn.execute(text("SELECT 1"))
            result.scalar()
        checks['database'] = {'status': 'ok', 'message': '连接正常'}
    except Exception as e:
        checks['database'] = {'status': 'error', 'message': str(e)}
    
    # 2. 检查今日净值数据
    try:
        with db.get_connection() as conn:
            result = conn.execute(text("""
                SELECT COUNT(*) as count 
                FROM fund_nav 
                WHERE nav_date = CURDATE()
            """))
            count = result.scalar()
        checks['today_nav'] = {
            'status': 'ok' if count > 0 else 'warning',
            'message': f'{count}条今日净值数据'
        }
    except Exception as e:
        checks['today_nav'] = {'status': 'error', 'message': str(e)}
    
    # 3. 检查临时表
    try:
        with db.get_connection() as conn:
            result = conn.execute(text("""
                SELECT 
                    SUM(CASE WHEN check_status = 0 THEN 1 ELSE 0 END) as pending,
                    SUM(CASE WHEN check_status = 2 THEN 1 ELSE 0 END) as failed
                FROM tmp_fund_nav
            """))
            row = result.fetchone()
            pending = row[0] or 0
            failed = row[1] or 0
        
        if pending > 10000:
            status = 'warning'
        elif failed > 0:
            status = 'warning'
        else:
            status = 'ok'
        
        checks['temp_table'] = {
            'status': status,
            'message': f'待处理{pending}条, 失败{failed}条'
        }
    except Exception as e:
        checks['temp_table'] = {'status': 'error', 'message': str(e)}
    
    # 整体状态
    errors = [c for c in checks.values() if c['status'] == 'error']
    warnings = [c for c in checks.values() if c['status'] == 'warning']
    
    if errors:
        overall_status = 'unhealthy'
        http_code = 503
    elif warnings:
        overall_status = 'degraded'
        http_code = 200
    else:
        overall_status = 'healthy'
        http_code = 200
    
    return jsonify({
        'status': overall_status,
        'timestamp': datetime.now().isoformat(),
        'checks': checks
    }), http_code


@app.route('/metrics')
def metrics():
    """
    指标数据接口
    返回: 系统关键指标
    """
    metrics_data = {
        'timestamp': datetime.now().isoformat()
    }
    
    try:
        # 数据库统计
        with db.get_connection() as conn:
            # 基金数量
            result = conn.execute(text("SELECT COUNT(*) FROM fund_info"))
            metrics_data['fund_count'] = result.scalar()
            
            # 净值记录数
            result = conn.execute(text("SELECT COUNT(*) FROM fund_nav"))
            metrics_data['nav_records'] = result.scalar()
            
            # 今日净值
            result = conn.execute(text("""
                SELECT COUNT(*) FROM fund_nav WHERE nav_date = CURDATE()
            """))
            metrics_data['today_nav_count'] = result.scalar()
            
            # 临时表数量
            result = conn.execute(text("SELECT COUNT(*) FROM tmp_fund_nav"))
            metrics_data['temp_records'] = result.scalar()
            
            # 数据更新日志统计
            result = conn.execute(text("""
                SELECT 
                    COUNT(*) as total_jobs,
                    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_jobs
                FROM data_update_log
                WHERE created_at > DATE_SUB(NOW(), INTERVAL 24 HOUR)
            """))
            row = result.fetchone()
            metrics_data['jobs_24h'] = row[0] or 0
            metrics_data['jobs_success_24h'] = row[1] or 0
    
    except Exception as e:
        metrics_data['error'] = str(e)
    
    return jsonify(metrics_data)


@app.route('/status')
def status():
    """
    系统状态接口
    返回: 详细的系统状态信息
    """
    try:
        trigger = AlertTrigger()
        health = trigger.get_system_health()
        return jsonify(health)
    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now().isoformat()
        }), 500


@app.route('/')
def index():
    """首页"""
    return jsonify({
        'service': '基金数据采集系统',
        'version': '1.0.0',
        'endpoints': [
            {'path': '/health', 'desc': '健康检查'},
            {'path': '/metrics', 'desc': '指标数据'},
            {'path': '/status', 'desc': '系统状态'}
        ]
    })


def main():
    parser = argparse.ArgumentParser(description='健康检查服务')
    parser.add_argument('--port', '-p', type=int, default=5000, help='服务端口 (默认5000)')
    parser.add_argument('--host', '-H', default='0.0.0.0', help='绑定地址 (默认0.0.0.0)')
    args = parser.parse_args()
    
    logger.info(f"="*60)
    logger.info(f"健康检查服务启动")
    logger.info(f"接口地址: http://{args.host}:{args.port}")
    logger.info(f"="*60)
    
    app.run(host=args.host, port=args.port, debug=False)


if __name__ == '__main__':
    main()
