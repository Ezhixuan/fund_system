package com.fund.service;

import com.fund.dto.CollectionStatsDTO;
import com.fund.dto.TableStatusDTO;
import com.fund.mapper.FundInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控服务
 */
@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private final DataSource dataSource;
    private final FundInfoMapper fundInfoMapper;

    public MonitorService(DataSource dataSource,
                         FundInfoMapper fundInfoMapper) {
        this.dataSource = dataSource;
        this.fundInfoMapper = fundInfoMapper;
    }

    /**
     * 获取数据表状态
     */
    public List<TableStatusDTO> getTableStatus() {
        List<TableStatusDTO> results = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 监控的表
        String[][] tables = {
            {"fund_nav", "nav_date"},
            {"fund_metrics", "calc_date"},
            {"fund_score", "calc_date"}
        };

        try (Connection conn = dataSource.getConnection()) {
            for (String[] table : tables) {
                TableStatusDTO status = new TableStatusDTO();
                status.setTableName(table[0]);

                try (Statement stmt = conn.createStatement()) {
                    // 查询最新日期和记录数
                    String sql = String.format(
                        "SELECT MAX(%s) as latest_date, COUNT(*) as total_count FROM %s",
                        table[1], table[0]
                    );
                    ResultSet rs = stmt.executeQuery(sql);

                    if (rs.next()) {
                        String latestDate = rs.getString("latest_date");
                        int recordCount = rs.getInt("total_count");

                        status.setLatestDate(latestDate);
                        status.setRecordCount(recordCount);

                        // 计算延迟天数
                        if (latestDate != null) {
                            LocalDate date = LocalDate.parse(latestDate);
                            int delayDays = (int) ChronoUnit.DAYS.between(date, today);
                            status.setDelayDays(delayDays);
                            status.setIsFresh(delayDays <= 1);
                        } else {
                            status.setDelayDays(-1);
                            status.setIsFresh(false);
                        }
                    }
                } catch (Exception e) {
                    log.error("查询表 {} 失败: {}", table[0], e.getMessage());
                    status.setLatestDate(null);
                    status.setRecordCount(0);
                    status.setDelayDays(-1);
                    status.setIsFresh(false);
                }

                results.add(status);
            }
        } catch (Exception e) {
            log.error("获取数据库连接失败: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 获取采集统计
     */
    public CollectionStatsDTO getCollectionStats(String date) {
        if (date == null) {
            date = LocalDate.now().toString();
        }

        CollectionStatsDTO stats = new CollectionStatsDTO();
        stats.setDate(date);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 查询总基金数
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM fund_info");
            if (rs.next()) {
                int totalFunds = rs.getInt(1);
                stats.setTotalFunds(totalFunds);

                // 查询今日采集数
                String collectedSql = String.format(
                    "SELECT COUNT(DISTINCT fund_code) FROM fund_nav WHERE nav_date = '%s'",
                    date
                );
                ResultSet collectedRs = stmt.executeQuery(collectedSql);
                int collectedFunds = 0;
                if (collectedRs.next()) {
                    collectedFunds = collectedRs.getInt(1);
                }

                stats.setCollectedFunds(collectedFunds);
                stats.setFailedCount(totalFunds - collectedFunds);

                // 计算成功率
                double successRate = totalFunds > 0
                    ? (double) collectedFunds / totalFunds * 100
                    : 0;
                stats.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
            }
        } catch (Exception e) {
            log.error("获取采集统计失败: {}", e.getMessage());
            stats.setTotalFunds(0);
            stats.setCollectedFunds(0);
            stats.setSuccessRate(0.0);
            stats.setFailedCount(0);
        }

        return stats;
    }

    /**
     * 获取数据质量报告
     */
    public Map<String, Object> getDataQualityReport() {
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. 检查净值是否为正
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM fund_nav WHERE unit_nav <= 0"
            );
            int navCheck = rs.next() ? rs.getInt(1) : 0;
            checks.add(createCheckResult("净值大于0", navCheck == 0, navCheck));

            // 2. 检查累计净值
            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM fund_nav WHERE accum_nav < unit_nav"
            );
            int accNavCheck = rs.next() ? rs.getInt(1) : 0;
            checks.add(createCheckResult("累计净值>=单位净值", accNavCheck == 0, accNavCheck));

            // 3. 检查必填字段
            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM fund_info WHERE fund_code IS NULL OR fund_name IS NULL"
            );
            int nullCheck = rs.next() ? rs.getInt(1) : 0;
            checks.add(createCheckResult("必填字段不为空", nullCheck == 0, nullCheck));

            // 4. 检查夏普比率为空但其他指标存在的记录
            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM fund_metrics WHERE sharpe_ratio_1y IS NULL"
            );
            int sharpeCheck = rs.next() ? rs.getInt(1) : 0;
            checks.add(createCheckResult("夏普比率已计算", sharpeCheck == 0, sharpeCheck));

        } catch (Exception e) {
            log.error("获取数据质量报告失败: {}", e.getMessage());
        }

        report.put("checks", checks);
        report.put("totalChecks", checks.size());
        report.put("passedChecks", checks.stream()
            .filter(c -> (Boolean) c.get("passed"))
            .count());

        return report;
    }

    private Map<String, Object> createCheckResult(String rule, boolean passed, int failedCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("rule", rule);
        result.put("passed", passed);
        result.put("failedCount", failedCount);
        return result;
    }

    /**
     * 获取系统健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();

        List<TableStatusDTO> tableStatus = getTableStatus();
        CollectionStatsDTO collectionStats = getCollectionStats(null);
        Map<String, Object> qualityReport = getDataQualityReport();

        // 判断健康状态
        boolean allFresh = tableStatus.stream().allMatch(TableStatusDTO::getIsFresh);
        boolean collectionOk = collectionStats.getSuccessRate() >= 95;

        String healthStatus;
        if (allFresh && collectionOk) {
            healthStatus = "healthy";
        } else if (allFresh) {
            healthStatus = "warning";
        } else {
            healthStatus = "error";
        }

        status.put("status", healthStatus);
        status.put("timestamp", java.time.Instant.now().toString());
        status.put("tables", tableStatus);
        status.put("collection", collectionStats);
        status.put("quality", qualityReport);

        return status;
    }
}
