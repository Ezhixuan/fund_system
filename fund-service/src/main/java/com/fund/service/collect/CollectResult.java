package com.fund.service.collect;

/**
 * Python采集结果通用封装类
 * 支持泛型数据携带和错误码定义
 *
 * @param <T> 数据类型
 */
public class CollectResult<T> {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 数据（成功时有值）
     */
    private T data;
    
    /**
     * 错误码（失败时）
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String message;
    
    // ============ 错误码常量 ============
    
    /**
     * 基金不存在（Python端也查不到）
     */
    public static final String ERR_FUND_NOT_FOUND = "FUND_NOT_FOUND";
    
    /**
     * 采集超时
     */
    public static final String ERR_TIMEOUT = "TIMEOUT";
    
    /**
     * 服务异常
     */
    public static final String ERR_SERVICE_ERROR = "SERVICE_ERROR";
    
    /**
     * 参数错误
     */
    public static final String ERR_INVALID_PARAM = "INVALID_PARAM";
    
    // ============ 构造方法 ============
    
    public CollectResult() {
    }
    
    public CollectResult(boolean success, T data, String errorCode, String message) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.message = message;
    }
    
    // ============ Getter/Setter ============
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // ============ Builder ============
    
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    public static class Builder<T> {
        private boolean success;
        private T data;
        private String errorCode;
        private String message;
        
        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }
        
        public Builder<T> errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }
        
        public CollectResult<T> build() {
            return new CollectResult<>(success, data, errorCode, message);
        }
    }
    
    // ============ 便捷方法 ============
    
    /**
     * 创建成功结果
     */
    public static <T> CollectResult<T> success(T data) {
        return CollectResult.<T>builder()
                .success(true)
                .data(data)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static <T> CollectResult<T> fail(String errorCode, String message) {
        return CollectResult.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
    
    /**
     * 创建"基金不存在"失败结果
     */
    public static <T> CollectResult<T> notFound(String fundCode) {
        return fail(ERR_FUND_NOT_FOUND, "基金不存在: " + fundCode);
    }
    
    /**
     * 创建超时失败结果
     */
    public static <T> CollectResult<T> timeout(String message) {
        return fail(ERR_TIMEOUT, "采集超时: " + message);
    }
    
    /**
     * 创建服务异常失败结果
     */
    public static <T> CollectResult<T> serviceError(String message) {
        return fail(ERR_SERVICE_ERROR, "服务异常: " + message);
    }
    
    @Override
    public String toString() {
        return "CollectResult{" +
                "success=" + success +
                ", data=" + data +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
