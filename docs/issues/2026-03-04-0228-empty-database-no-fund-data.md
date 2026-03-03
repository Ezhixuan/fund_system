# Issue: 数据库为空时无法获取基金信息

**Created**: 2026-03-04 02:28
**Status**: Open
**Priority**: High

## Problem Description

系统启动时如果数据库中没有基金信息，不会调用 collect 端获取数据，导致：
1. 基金列表查询返回空结果
2. 搜索建议功能无法使用
3. 用户无法知道有哪些基金可以查询
4. 无法进行基金搜索等功能

## Environment

- Project: fund-system
- Location:
  - `fund-service/src/main/java/com/fund/service/impl/FundServiceImpl.java`
  - `fund-service/src/main/java/com/fund/controller/FundController.java`
  - `fund-service/src/main/java/com/fund/service/FundDataFetchService.java`
- Database: MySQL 8.0 (fund_system 数据库)
- Related Service: Python Collector (端口 5005)

## Analysis

### 当前数据获取机制

1. **单个基金查询** (`/api/funds/{fundCode}`):
   - 已实现自动采集机制
   - 当基金不存在时，会调用 `CollectClient.collectFundData(fundCode)`
   - `FundDataFetchService.getFundInfo()` 也有自动补全逻辑

2. **基金列表查询** (`/api/funds`):
   - 仅查询本地数据库 `fundInfoMapper.selectPage()`
   - **不会触发任何数据采集**
   - 数据库为空时返回空列表

3. **搜索建议** (`/api/funds/search/suggest`):
   - 仅查询本地数据库
   - **不会触发数据采集**

### 问题根因

1. **缺乏初始化机制**: 系统没有启动时自动导入基础基金数据的逻辑
2. **列表查询不触发采集**: 批量查询接口不会主动去采集服务获取数据
3. **用户体验断层**: 新部署系统后，用户看到空列表，不知道需要输入具体基金代码才能触发采集

## Root Cause

```
用户访问基金列表
    ↓
调用 FundService.listFunds()
    ↓
查询 fund_info 表（空表）
    ↓
返回空列表（不会触发采集）
    ↓
用户无法看到任何基金
```

对比单个基金查询：
```
用户访问 /api/funds/000001
    ↓
查询 fund_info 表（不存在）
    ↓
触发 CollectClient.collectFundData("000001")
    ↓
从 Python 服务采集数据
    ↓
保存到数据库并返回
```

**核心问题**: 列表查询和搜索接口没有集成自动采集机制，系统缺乏初始数据导入方案。

## Solution

### Option 1 (Recommended): 启动时自动初始化热门基金数据

在应用启动时，自动从 Python 采集服务获取一批热门基金数据：

```java
@Component
public class FundDataInitializer implements CommandLineRunner {

    @Autowired
    private CollectClient collectClient;

    @Autowired
    private FundInfoMapper fundInfoMapper;

    @Override
    public void run(String... args) {
        // 检查数据库是否为空
        Long count = fundInfoMapper.selectCount(null);
        if (count == 0) {
            log.info("数据库为空，开始初始化热门基金数据...");
            // 获取热门基金列表
            List<String> hotFunds = List.of("000001", "000002", "005827", ...);
            for (String fundCode : hotFunds) {
                collectClient.collectFundData(fundCode);
            }
        }
    }
}
```

**优点**:
- 部署后自动完成数据初始化
- 用户首次访问即可看到基金列表

**缺点**:
- 启动时间增加
- 需要维护热门基金代码列表

### Option 2: 提供手动初始化接口

添加管理接口，允许手动触发基金数据导入：

```java
@PostMapping("/admin/init/funds")
public ApiResponse<String> initFundData(
    @RequestParam(required = false) List<String> fundCodes) {
    // 如果没有指定代码，使用默认热门基金列表
    if (fundCodes == null || fundCodes.isEmpty()) {
        fundCodes = getDefaultFundList();
    }
    // 批量采集
    for (String code : fundCodes) {
        collectClient.collectFundData(code);
    }
    return ApiResponse.success("初始化完成，共导入 " + fundCodes.size() + " 只基金");
}
```

**优点**:
- 灵活控制初始化时机
- 可以指定导入特定基金

**缺点**:
- 需要手动调用
- 增加运维步骤

### Option 3: 搜索无结果时提示用户

当搜索不到基金时，返回提示信息引导用户：

```java
@GetMapping("/search/suggest")
public ApiResponse<List<FundInfoVO>> searchSuggest(@RequestParam String keyword) {
    List<FundInfoVO> results = fundService.searchSuggest(keyword, limit);

    // 如果数据库为空，返回提示
    if (results.isEmpty() && isDatabaseEmpty()) {
        return ApiResponse.warning("暂无基金数据，请先访问 /api/funds/{fundCode} 触发采集");
    }

    return ApiResponse.success(results);
}
```

**优点**:
- 实现简单
- 明确告知用户原因

**缺点**:
- 用户体验不佳
- 需要用户手动操作

## Action Items

- [ ] 评估 Option 1 和 Option 2 的实现成本和收益
- [ ] 确定默认热门基金列表（建议 50-100 只）
- [ ] 实现数据初始化机制（启动时自动或手动接口）
- [ ] 添加初始化状态监控和日志
- [ ] 更新部署文档，说明首次启动的数据初始化过程
- [ ] 考虑添加定时任务，定期同步热门基金列表

## Related

- `FundDataFetchService.java`: 单个基金自动采集实现
- `CollectClient.java`: 采集服务客户端接口
- `FundController.java`: 基金列表和搜索接口
- 相关 Issue: `2026-03-02-001-fund-data-missing.md`
- Python Collector: `collector/app.py` (端口 5005)
