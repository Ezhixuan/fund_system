# P2-02: 基金检索API - 执行计划

> 工期：4天 | 依赖：P2-01

---

## Day 1-2: 基础检索

### 实体与VO
```java
// dto/FundVO.java
@Data
public class FundVO {
    private String fundCode;
    private String fundName;
    private String namePinyin;
    private String fundType;
    private String managerName;
    private String companyName;
    private LocalDate establishDate;
    private BigDecimal currentScale;
}

// dto/FundSearchRequest.java
@Data
public class FundSearchRequest {
    private String keyword;       // 搜索关键词
    private String fundType;      // 基金类型筛选
    private Integer riskLevel;    // 风险等级
    private String orderBy;       // 排序字段
    private String order;         // 排序方向 asc/desc
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
```

### Service实现
```java
// service/FundService.java
public interface FundService {
    PageResult<FundVO> searchFunds(FundSearchRequest request);
    FundVO getFundDetail(String fundCode);
}

// service/impl/FundServiceImpl.java
@Service
public class FundServiceImpl implements FundService {
    
    @Autowired
    private FundInfoMapper fundInfoMapper;
    
    @Override
    public PageResult<FundVO> searchFunds(FundSearchRequest request) {
        Page<FundInfo> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        LambdaQueryWrapper<FundInfo> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（代码/名称/拼音）
        if (StringUtils.hasText(request.getKeyword())) {
            String kw = request.getKeyword();
            wrapper.and(w -> w
                .like(FundInfo::getFundCode, kw)
                .or()
                .like(FundInfo::getFundName, kw)
                .or()
                .like(FundInfo::getNamePinyin, kw.toLowerCase())
            );
        }
        
        // 类型筛选
        if (StringUtils.hasText(request.getFundType())) {
            wrapper.eq(FundInfo::getFundType, request.getFundType());
        }
        
        // 风险等级
        if (request.getRiskLevel() != null) {
            wrapper.eq(FundInfo::getRiskLevel, request.getRiskLevel());
        }
        
        // 排序
        if ("establishDate".equals(request.getOrderBy())) {
            wrapper.orderBy(true, "asc".equals(request.getOrder()), FundInfo::getEstablishDate);
        } else {
            wrapper.orderByDesc(FundInfo::getCurrentScale);
        }
        
        Page<FundInfo> result = fundInfoMapper.selectPage(page, wrapper);
        
        List<FundVO> voList = result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }
    
    private FundVO convertToVO(FundInfo info) {
        FundVO vo = new FundVO();
        BeanUtils.copyProperties(info, vo);
        return vo;
    }
}
```

---

## Day 3-4: Controller与拼音

### Controller
```java
@RestController
@RequestMapping("/api/funds")
public class FundController {
    
    @Autowired
    private FundService fundService;
    
    @PostMapping("/search")
    public ApiResponse<PageResult<FundVO>> search(@RequestBody FundSearchRequest request) {
        return ApiResponse.success(fundService.searchFunds(request));
    }
    
    @GetMapping("/{code}")
    public ApiResponse<FundVO> detail(@PathVariable String code) {
        return ApiResponse.success(fundService.getFundDetail(code));
    }
    
    @GetMapping("/types")
    public ApiResponse<List<String>> getTypes() {
        return ApiResponse.success(Arrays.asList(
            "股票型", "混合型", "债券型", "指数型", "QDII", "FOF"
        ));
    }
}
```

### 拼音工具
```java
// utils/PinyinUtil.java
public class PinyinUtil {
    
    public static String getFirstLetters(String chinese) {
        if (!StringUtils.hasText(chinese)) return "";
        
        StringBuilder result = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c);
            if (pinyin != null && pinyin.length > 0) {
                result.append(pinyin[0].charAt(0));
            } else {
                result.append(c);
            }
        }
        return result.toString().toLowerCase();
    }
}
```

---

## 验收清单
- [ ] POST /api/funds/search 正常
- [ ] 拼音搜索可用（输入zs匹配招商）
- [ ] 分页功能正常
- [ ] 响应时间<200ms
