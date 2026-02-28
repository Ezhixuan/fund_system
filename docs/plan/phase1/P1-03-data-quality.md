# P1-03: 数据质量与校验 - 执行计划

> 所属阶段：Phase 1 数据基建层  
> 计划工期：3天  
> 前置依赖：P1-02 Python采集模块

---

## 一、任务目标
建立数据质量保障机制，确保采集数据的准确性、完整性和一致性。

---

## 二、执行步骤

### Day 1: 数据校验规则实现

#### 任务 1.1: 校验规则引擎
```python
# core/data_validator.py
import pandas as pd
import numpy as np
from typing import Dict, List, Tuple
from dataclasses import dataclass
import logging

logger = logging.getLogger(__name__)

@dataclass
class ValidationRule:
    """校验规则定义"""
    name: str
    check_func: callable
    error_msg: str
    severity: str = 'error'  # error/warning

@dataclass
class ValidationResult:
    """校验结果"""
    is_valid: bool
    passed_rules: List[str]
    failed_rules: List[Tuple[str, str]]  # (规则名, 错误信息)
    stats: Dict

class DataValidator:
    """数据校验器"""
    
    def __init__(self):
        self.rules = []
    
    def add_rule(self, rule: ValidationRule):
        """添加校验规则"""
        self.rules.append(rule)
    
    def validate(self, df: pd.DataFrame) -> ValidationResult:
        """执行所有校验规则"""
        passed = []
        failed = []
        
        for rule in self.rules:
            try:
                is_pass, msg = rule.check_func(df)
                if is_pass:
                    passed.append(rule.name)
                else:
                    failed.append((rule.name, msg or rule.error_msg))
            except Exception as e:
                failed.append((rule.name, f"校验异常: {e}"))
        
        # 生成统计信息
        stats = {
            'total_records': len(df),
            'null_counts': df.isnull().sum().to_dict(),
            'duplicated': df.duplicated().sum()
        }
        
        # 只有error级别失败才算失败
        critical_failed = [f for f in failed if self._get_rule_severity(f[0]) == 'error']
        
        return ValidationResult(
            is_valid=len(critical_failed) == 0,
            passed_rules=passed,
            failed_rules=failed,
            stats=stats
        )
    
    def _get_rule_severity(self, rule_name: str) -> str:
        for rule in self.rules:
            if rule.name == rule_name:
                return rule.severity
        return 'error'
```

#### 任务 1.2: 净值数据校验规则
```python
# validators/nav_validators.py
import pandas as pd
import numpy as np
from core.data_validator import ValidationRule

def create_nav_validators() -> List[ValidationRule]:
    """创建净值数据校验规则"""
    
    # 规则1：净值范围检查
    def check_nav_range(df: pd.DataFrame) -> Tuple[bool, str]:
        invalid = df[(df['unit_nav'] <= 0) | (df['unit_nav'] > 1000)]
        if len(invalid) > 0:
            return False, f"{len(invalid)}条记录净值超出正常范围(0,1000)"
        return True, None
    
    # 规则2：日涨跌幅范围检查
    def check_return_range(df: pd.DataFrame) -> Tuple[bool, str]:
        if 'daily_return' not in df.columns:
            return True, None
        invalid = df[(df['daily_return'] < -20) | (df['daily_return'] > 20)]
        if len(invalid) > 0:
            samples = invalid[['fund_code', 'daily_return']].head(3).to_dict('records')
            return False, f"{len(invalid)}条记录涨跌幅异常: {samples}"
        return True, None
    
    # 规则3：必填字段检查
    def check_required_fields(df: pd.DataFrame) -> Tuple[bool, str]:
        required = ['fund_code', 'nav_date', 'unit_nav']
        for field in required:
            null_count = df[field].isnull().sum()
            if null_count > 0:
                return False, f"字段{field}有{null_count}条空值"
        return True, None
    
    # 规则4：重复数据检查
    def check_duplicates(df: pd.DataFrame) -> Tuple[bool, str]:
        dups = df[df.duplicated(['fund_code', 'nav_date'], keep=False)]
        if len(dups) > 0:
            groups = dups.groupby(['fund_code', 'nav_date']).size()
            return False, f"{len(groups)}组重复数据"
        return True, None
    
    # 规则5：基金代码格式检查
    def check_fund_code_format(df: pd.DataFrame) -> Tuple[bool, str]:
        invalid = df[~df['fund_code'].str.match(r'^\d{6}$', na=False)]
        if len(invalid) > 0:
            return False, f"{len(invalid)}条记录基金代码格式不正确"
        return True, None
    
    # 规则6：数据时效性检查（警告级别）
    def check_data_freshness(df: pd.DataFrame) -> Tuple[bool, str]:
        from datetime import datetime, timedelta
        if 'nav_date' not in df.columns:
            return True, None
        
        latest_date = pd.to_datetime(df['nav_date']).max()
        today = datetime.now()
        
        # 允许周末不更新
        days_diff = (today - latest_date).days
        if days_diff > 3:  # 超过3天警告
            return False, f"数据可能滞后{days_diff}天"
        return True, None
    
    return [
        ValidationRule('nav_range', check_nav_range, '净值超出正常范围'),
        ValidationRule('return_range', check_return_range, '涨跌幅异常'),
        ValidationRule('required_fields', check_required_fields, '必填字段缺失'),
        ValidationRule('duplicates', check_duplicates, '存在重复数据'),
        ValidationRule('fund_code_format', check_fund_code_format, '基金代码格式错误'),
        ValidationRule('data_freshness', check_data_freshness, '数据时效性警告', 'warning'),
    ]
```

**检查点**：
- [ ] 6条校验规则实现
- [ ] 规则可配置
- [ ] 支持错误/警告级别

---

### Day 2: 数据合并与管道

#### 任务 2.1: 数据管道实现
```python
# core/data_pipeline.py
from sqlalchemy import create_engine, text
from core.data_validator import DataValidator, ValidationResult
from validators.nav_validators import create_nav_validators
from datetime import datetime
import pandas as pd
import logging

logger = logging.getLogger(__name__)

class DataPipeline:
    """
    数据管道：临时表 -> 校验 -> 合并到正式表
    """
    
    def __init__(self, db_config: dict):
        self.engine = create_engine(
            f"mysql+pymysql://{db_config['user']}:{db_config['password']}"
            f"@{db_config['host']}:{db_config['port']}/{db_config['database']}"
        )
        self.validator = DataValidator()
    
    def process_nav_data(self, table_name: str = 'fund_nav') -> ValidationResult:
        """
        处理净值数据
        流程：临时表 -> 校验 -> 更新状态 -> 合并 -> 记录日志
        """
        try:
            # 1. 从临时表读取数据
            df = pd.read_sql("SELECT * FROM tmp_fund_nav WHERE check_status = 0", self.engine)
            
            if df.empty:
                logger.info("临时表无待处理数据")
                return ValidationResult(True, [], [], {})
            
            logger.info(f"从临时表读取{len(df)}条待处理数据")
            
            # 2. 执行校验
            self._setup_nav_validators()
            result = self.validator.validate(df)
            
            # 3. 更新临时表状态
            self._update_temp_status(df, result)
            
            # 4. 如果校验通过，合并到正式表
            if result.is_valid:
                merged_count = self._merge_to_production(df)
                self._log_update(table_name, merged_count, 'SUCCESS')
                logger.info(f"数据合并完成：{merged_count}条")
            else:
                # 部分通过的处理
                valid_df = df[df['check_status'] == 1] if 'check_status' in df.columns else pd.DataFrame()
                if not valid_df.empty:
                    merged_count = self._merge_to_production(valid_df)
                    self._log_update(table_name, merged_count, 'PARTIAL', 
                                   str(result.failed_rules))
                else:
                    self._log_update(table_name, 0, 'FAILURE', str(result.failed_rules))
            
            return result
            
        except Exception as e:
            logger.error(f"数据处理异常: {e}")
            self._log_update(table_name, 0, 'FAILURE', str(e))
            raise
    
    def _setup_nav_validators(self):
        """配置净值校验规则"""
        rules = create_nav_validators()
        for rule in rules:
            self.validator.add_rule(rule)
    
    def _update_temp_status(self, df: pd.DataFrame, result: ValidationResult):
        """更新临时表校验状态"""
        from sqlalchemy import text
        
        with self.engine.connect() as conn:
            # 通过的记录
            if result.passed_rules:
                conn.execute(text("""
                    UPDATE tmp_fund_nav 
                    SET check_status = 1, check_msg = '校验通过'
                    WHERE id IN :ids
                """), {'ids': tuple(df['id'].tolist()) if 'id' in df.columns else ()})
            
            # 失败的记录
            for rule_name, error_msg in result.failed_rules:
                # 标记具体错误（简化处理）
                pass
            
            conn.commit()
    
    def _merge_to_production(self, df: pd.DataFrame) -> int:
        """合并到正式表"""
        from sqlalchemy import text
        
        # 使用INSERT ON DUPLICATE KEY UPDATE
        records = df[['fund_code', 'nav_date', 'unit_nav', 'accum_nav', 'daily_return', 'source']].to_dict('records')
        
        with self.engine.connect() as conn:
            for record in records:
                conn.execute(text("""
                    INSERT INTO fund_nav 
                    (fund_code, nav_date, unit_nav, accum_nav, daily_return, source, created_at)
                    VALUES 
                    (:fund_code, :nav_date, :unit_nav, :accum_nav, :daily_return, :source, NOW())
                    ON DUPLICATE KEY UPDATE
                        unit_nav = VALUES(unit_nav),
                        accum_nav = VALUES(accum_nav),
                        daily_return = VALUES(daily_return),
                        source = VALUES(source),
                        created_at = VALUES(created_at)
                """), record)
            
            conn.commit()
        
        return len(records)
    
    def _log_update(self, table_name: str, record_count: int, status: str, error_msg: str = None):
        """记录更新日志"""
        from sqlalchemy import text
        
        with self.engine.connect() as conn:
            conn.execute(text("""
                INSERT INTO data_update_log 
                (table_name, update_date, record_count, status, error_msg, end_time)
                VALUES (:table_name, :update_date, :record_count, :status, :error_msg, NOW())
            """), {
                'table_name': table_name,
                'update_date': datetime.now().date(),
                'record_count': record_count,
                'status': status,
                'error_msg': error_msg[:1000] if error_msg else None  # 限制长度
            })
            conn.commit()
```

#### 任务 2.2: 多源数据校验
```python
# core/multi_source_validator.py
import akshare as ak
import pandas as pd
from typing import Dict, Optional
import logging

logger = logging.getLogger(__name__)

class MultiSourceValidator:
    """多源数据一致性校验"""
    
    def __init__(self):
        self.threshold = 0.01  # 1%差异阈值
    
    def validate_nav_consistency(self, fund_code: str, date: str) -> Dict:
        """
        校验多源净值数据一致性
        """
        sources = {}
        
        # 源1: akshare
        try:
            df = ak.fund_net_value_em(fund_code)
            if not df.empty:
                row = df[df['净值日期'] == date]
                if not row.empty:
                    sources['akshare'] = float(row['单位净值'].iloc[0])
        except Exception as e:
            logger.warning(f"akshare获取失败: {e}")
            sources['akshare'] = None
        
        # 源2: 东方财富（简化示例，实际需爬虫）
        # sources['eastmoney'] = self._get_eastmoney_nav(fund_code, date)
        
        # 计算差异
        valid_values = [v for v in sources.values() if v is not None]
        
        if len(valid_values) >= 2:
            max_val = max(valid_values)
            min_val = min(valid_values)
            diff_pct = (max_val - min_val) / min_val if min_val > 0 else 0
            
            result = {
                'is_consistent': diff_pct <= self.threshold,
                'max_diff_pct': round(diff_pct * 100, 4),
                'sources': sources,
                'recommend': min_val if diff_pct <= self.threshold else None
            }
            
            if diff_pct > self.threshold:
                result['alert'] = f"数据源差异{diff_pct*100:.2f}%，需人工确认"
                logger.warning(result['alert'])
            
            return result
        
        return {
            'is_consistent': True,  # 单源数据无法校验
            'sources': sources,
            'note': '仅单源数据可用'
        }
```

**检查点**：
- [ ] 数据管道流程完整
- [ ] 校验状态可追踪
- [ ] 多源校验可用

---

### Day 3: 异常告警机制

#### 任务 3.1: 告警通知器
```python
# utils/alerter.py
import requests
import smtplib
from email.mime.text import MIMEText
from typing import List
import logging

logger = logging.getLogger(__name__)

class Alerter:
    """告警通知器"""
    
    def __init__(self, dingtalk_webhook: str = None, email_config: dict = None):
        self.dingtalk_webhook = dingtalk_webhook
        self.email_config = email_config
    
    def send_dingtalk(self, title: str, message: str):
        """发送钉钉通知"""
        if not self.dingtalk_webhook:
            logger.warning("钉钉webhook未配置")
            return
        
        try:
            payload = {
                'msgtype': 'markdown',
                'markdown': {
                    'title': title,
                    'text': f"### {title}\n{message}"
                }
            }
            response = requests.post(
                self.dingtalk_webhook,
                json=payload,
                timeout=10
            )
            response.raise_for_status()
            logger.info(f"钉钉通知发送成功: {title}")
        except Exception as e:
            logger.error(f"钉钉通知发送失败: {e}")
    
    def send_email(self, subject: str, body: str, to_addrs: List[str]):
        """发送邮件通知"""
        if not self.email_config:
            logger.warning("邮件配置未设置")
            return
        
        try:
            msg = MIMEText(body, 'plain', 'utf-8')
            msg['Subject'] = subject
            msg['From'] = self.email_config['from']
            msg['To'] = ', '.join(to_addrs)
            
            server = smtplib.SMTP(self.email_config['smtp_server'], self.email_config['smtp_port'])
            server.starttls()
            server.login(self.email_config['username'], self.email_config['password'])
            server.sendmail(self.email_config['from'], to_addrs, msg.as_string())
            server.quit()
            
            logger.info(f"邮件发送成功: {subject}")
        except Exception as e:
            logger.error(f"邮件发送失败: {e}")
    
    def alert_data_quality(self, table_name: str, errors: List[str], record_count: int):
        """数据质量告警"""
        title = f"[基金系统] 数据质量异常 - {table_name}"
        message = f"""
**表名**: {table_name}
**时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**记录数**: {record_count}
**异常详情**:
{chr(10).join(['- ' + e for e in errors])}

请尽快检查数据管道状态。
        """
        
        self.send_dingtalk(title, message)
        
        if self.email_config:
            self.send_email(title, message, self.email_config.get('alert_recipients', []))
```

#### 任务 3.2: 告警触发器
```python
# core/alert_trigger.py
from utils.alerter import Alerter
from datetime import datetime, timedelta
import pandas as pd
import logging

logger = logging.getLogger(__name__)

class AlertTrigger:
    """告警触发器"""
    
    def __init__(self, db_config: dict, alerter: Alerter):
        self.engine = create_engine(
            f"mysql+pymysql://{db_config['user']}:{db_config['password']}"
            f"@{db_config['host']}:{db_config['port']}/{db_config['database']}"
        )
        self.alerter = alerter
    
    def check_data_delay(self):
        """检查数据更新延迟"""
        # 查询最近24小时内的更新记录
        df = pd.read_sql("""
            SELECT table_name, MAX(update_date) as last_update, MAX(end_time) as last_time
            FROM data_update_log
            WHERE status = 'SUCCESS'
            GROUP BY table_name
        """, self.engine)
        
        alerts = []
        for _, row in df.iterrows():
            last_update = pd.to_datetime(row['last_update'])
            days_diff = (datetime.now() - last_update).days
            
            if days_diff > 1:
                alerts.append(f"{row['table_name']} 已 {days_diff} 天未更新")
        
        if alerts:
            self.alerter.alert_data_quality('数据延迟检查', alerts, 0)
    
    def check_validation_failures(self, hours: int = 24):
        """检查校验失败记录"""
        df = pd.read_sql(f"""
            SELECT table_name, update_date, record_count, error_msg
            FROM data_update_log
            WHERE status = 'FAILURE'
            AND end_time > DATE_SUB(NOW(), INTERVAL {hours} HOUR)
        """, self.engine)
        
        if not df.empty:
            errors = [f"{row['table_name']}({row['update_date']}): {row['error_msg'][:100]}" 
                     for _, row in df.iterrows()]
            self.alerter.alert_data_quality('数据校验失败', errors, len(df))
```

**验收标准**：
- [ ] 告警通知正常发送
- [ ] 钉钉/邮件双通道
- [ ] 延迟检测可用

---

## 三、验收清单

| 检查项 | 状态 | 验证方式 |
|--------|------|----------|
| 6条校验规则生效 | ☐ | 测试异常数据 |
| 临时表->正式表流程 | ☐ | 完整流程测试 |
| 更新日志记录 | ☐ | 查看data_update_log |
| 多源差异检测 | ☐ | 模拟多源数据 |
| 异常自动告警 | ☐ | 触发测试告警 |

---

**执行人**：待定  
**验收人**：待定  
**更新日期**：2026-02-28
