#!/bin/bash
# 代码统计脚本

echo "=========================================="
echo "    基金系统 - 代码统计"
echo "=========================================="
echo ""

echo "📊 Java 后端代码统计:"
echo "  文件数: $(find fund-service/src -name "*.java" | wc -l)"
echo "  代码行数: $(find fund-service/src -name "*.java" -exec wc -l {} + | tail -1 | awk '{print $1}')"
echo ""

echo "📊 Vue 前端代码统计:"
echo "  Vue文件: $(find fund-view/src -name "*.vue" 2>/dev/null | wc -l)"
echo "  TS/JS文件: $(find fund-view/src -name "*.ts" -o -name "*.js" 2>/dev/null | wc -l)"
echo ""

echo "📊 Python 采集代码统计:"
echo "  文件数: $(find collector -name "*.py" | wc -l)"
echo "  代码行数: $(find collector -name "*.py" -exec wc -l {} + | tail -1 | awk '{print $1}')"
echo ""

echo "📊 文档统计:"
echo "  Markdown文件: $(find docs -name "*.md" | wc -l)"
echo ""

echo "📊 Git 统计:"
echo "  提交次数: $(git log --oneline | wc -l)"
echo "  贡献者: $(git log --format='%an' | sort -u | wc -l)"
echo ""

echo "=========================================="
