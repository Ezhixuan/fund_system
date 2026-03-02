#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金估值采集服务
Flask API + APScheduler 定时任务
"""

import os
import sys
from datetime import datetime
from flask import Flask, jsonify, request
from flask_cors import CORS
from loguru import logger

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from config.settings import Config
from collectors.akshare_collector import AkshareCollector
from collectors.eastmoney_collector import EastmoneyCollector
from collectors.danjuan_collector import DanjuanCollector
from services.data_source_manager import DataSourceManager

# 初始化Flask应用
app = Flask(__name__)
app.config.from_object(Config)
CORS(app)

# 初始化数据源管理器
data_manager = DataSourceManager()

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        'status': 'ok',
        'timestamp': datetime.now().isoformat(),
        'service': 'fund-collector'
    })

@app.route('/api/collect/estimate', methods=['POST'])
def collect_estimate():
    """
    采集单只基金估值
    Request: { "fundCode": "005827" }
    """
    try:
        data = request.get_json()
        if not data or 'fundCode' not in data:
            return jsonify({'success': False, 'error': '缺少fundCode参数'}), 400
        
        fund_code = data['fundCode']
        logger.info(f"开始采集基金估值: {fund_code}")
        
        # 使用数据源管理器采集（带备用切换）
        result = data_manager.collect_with_fallback(fund_code)
        
        if result:
            return jsonify({
                'success': True,
                'data': {
                    'fundCode': fund_code,
                    'fundName': result.get('fund_name', ''),
                    'estimateTime': datetime.now().isoformat(),
                    'estimateNav': result.get('nav'),
                    'estimateChangePct': result.get('change_pct'),
                    'preCloseNav': result.get('pre_close'),
                    'dataSource': result.get('source', 'unknown')
                }
            })
        else:
            return jsonify({
                'success': False, 
                'error': '所有数据源均采集失败'
            }), 500
            
    except Exception as e:
        logger.error(f"采集失败: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/collect/batch', methods=['POST'])
def collect_batch():
    """
    批量采集基金估值
    Request: { "fundCodes": ["005827", "000001"] }
    """
    try:
        data = request.get_json()
        if not data or 'fundCodes' not in data:
            return jsonify({'success': False, 'error': '缺少fundCodes参数'}), 400
        
        fund_codes = data['fundCodes']
        logger.info(f"开始批量采集，共 {len(fund_codes)} 只基金")
        
        results = []
        errors = []
        
        for fund_code in fund_codes:
            try:
                result = data_manager.collect_with_fallback(fund_code)
                if result:
                    results.append({
                        'fundCode': fund_code,
                        'estimateNav': result.get('nav'),
                        'estimateChangePct': result.get('change_pct'),
                        'dataSource': result.get('source')
                    })
                else:
                    errors.append({'fundCode': fund_code, 'error': '采集失败'})
            except Exception as e:
                logger.error(f"采集 {fund_code} 失败: {e}")
                errors.append({'fundCode': fund_code, 'error': str(e)})
        
        return jsonify({
            'success': True,
            'data': {
                'total': len(fund_codes),
                'success': len(results),
                'failed': len(errors),
                'results': results,
                'errors': errors
            }
        })
        
    except Exception as e:
        logger.error(f"批量采集失败: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/collect/status', methods=['GET'])
def get_collector_status():
    """获取采集器状态"""
    return jsonify({
        'success': True,
        'data': {
            'dataSources': data_manager.get_data_source_status(),
            'timestamp': datetime.now().isoformat()
        }
    })



@app.route('/api/collect/fund', methods=['POST'])
def collect_fund_data():
    """
    采集基金完整数据（基础信息 + 净值历史 + 指标）
    Request: { "fundCode": "005827" }
    """
    try:
        data = request.get_json()
        if not data or 'fundCode' not in data:
            return jsonify({'success': False, 'error': '缺少fundCode参数'}), 400
        
        fund_code = data['fundCode']
        logger.info(f"开始采集基金完整数据: {fund_code}")
        
        # 导入采集函数
        from services.fund_data_service import FundDataService
        
        service = FundDataService()
        result = service.collect_fund_complete(fund_code)
        
        if result['success']:
            return jsonify({
                'success': True,
                'data': {
                    'fundCode': fund_code,
                    'fundName': result.get('fund_name', ''),
                    'navCount': result.get('nav_count', 0),
                    'metricsCalculated': result.get('metrics_calculated', False),
                    'message': '基金数据采集完成'
                }
            })
        else:
            return jsonify({
                'success': False,
                'error': result.get('error', '采集失败')
            }), 500
            
    except Exception as e:
        logger.error(f"采集基金数据失败: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500




# ============ 新增：基金详情页数据查询接口 ============

@app.route('/api/collect/fund/<string:fund_code>', methods=['GET'])
def get_fund_info(fund_code):
    """查询基金基本信息"""
    try:
        logger.info(f"查询基金基本信息: {fund_code}")
        
        from services.fund_data_service import FundDataService
        service = FundDataService()
        fund_info = service.get_fund_info(fund_code)
        
        if fund_info:
            return jsonify({
                'success': True,
                'data': {
                    'fundCode': fund_code,
                    'fundName': fund_info.get('fund_name', ''),
                    'fundType': fund_info.get('fund_type', ''),
                    'managerName': fund_info.get('manager_name', ''),
                    'companyName': fund_info.get('company_name', ''),
                    'riskLevel': fund_info.get('risk_level'),
                    'establishDate': fund_info.get('establish_date'),
                    'benchmark': fund_info.get('benchmark'),
                    'dataSource': 'database'
                }
            })
        else:
            return jsonify({
                'success': False,
                'errorCode': 'FUND_NOT_FOUND',
                'message': f'基金 {fund_code} 未找到'
            }), 404
            
    except Exception as e:
        logger.error(f"查询基金信息失败: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/api/collect/metrics/<string:fund_code>', methods=['GET'])
def get_fund_metrics(fund_code):
    """查询基金指标数据"""
    try:
        logger.info(f"查询基金指标: {fund_code}")
        
        from services.fund_data_service import FundDataService
        service = FundDataService()
        metrics = service.get_fund_metrics(fund_code)
        
        if metrics:
            return jsonify({
                'success': True,
                'data': {
                    'fundCode': fund_code,
                    'calcDate': metrics.get('calc_date'),
                    'return1m': metrics.get('return_1m'),
                    'return3m': metrics.get('return_3m'),
                    'return1y': metrics.get('return_1y'),
                    'sharpeRatio1y': metrics.get('sharpe_ratio_1y'),
                    'maxDrawdown1y': metrics.get('max_drawdown_1y'),
                    'volatility1y': metrics.get('volatility_1y'),
                    'qualityLevel': metrics.get('quality_level'),
                    'dataSource': 'database'
                }
            })
        else:
            return jsonify({
                'success': False,
                'errorCode': 'METRICS_NOT_FOUND',
                'message': f'基金 {fund_code} 指标数据未找到'
            }), 404
            
    except Exception as e:
        logger.error(f"查询基金指标失败: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/api/collect/nav/<string:fund_code>', methods=['GET'])
def get_fund_nav_history(fund_code):
    """查询基金NAV历史"""
    try:
        logger.info(f"查询基金NAV历史: {fund_code}")
        
        from services.fund_data_service import FundDataService
        service = FundDataService()
        nav_list = service.get_nav_history(fund_code, limit=30)
        
        if nav_list:
            return jsonify({
                'success': True,
                'data': nav_list,
                'count': len(nav_list)
            })
        else:
            return jsonify({
                'success': False,
                'errorCode': 'NAV_NOT_FOUND',
                'message': f'基金 {fund_code} NAV历史未找到'
            }), 404
            
    except Exception as e:
        logger.error(f"查询NAV历史失败: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


def init_scheduler():
    """初始化定时任务调度器"""
    try:
        from scheduler.intraday_scheduler import IntradayScheduler
        scheduler = IntradayScheduler(data_manager)
        scheduler.start()
        logger.info("定时任务调度器已启动")
        return scheduler
    except Exception as e:
        logger.error(f"定时任务调度器启动失败: {e}")
        return None

if __name__ == '__main__':
    # 启动定时任务
    scheduler = init_scheduler()
    
    # 启动Flask服务
    port = int(os.environ.get('COLLECTOR_PORT', 5000))
    logger.info(f"启动基金采集服务，端口: {port}")
    app.run(host='0.0.0.0', port=port, debug=False)
