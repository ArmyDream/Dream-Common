package com.dream.common.tool.utils.http;

import cn.hutool.http.*;
import com.dream.common.core.exception.BusinessException;
import com.dream.common.tool.utils.StringUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 增强工具类（继承 {@link cn.hutool.http.HttpUtil}，基于 Hutool-http 5.8.46，JDK 8）
 * <p>
 * <b>本类不覆盖任何父类静态方法</b>，所有基础请求、文件下载等请直接使用父类方法。
 * 在此基础上，提供企业级增强：
 * <ul>
 *     <li>可配置的连接/读取超时、自动重试</li>
 *     <li>全局默认 Content-Type</li>
 *     <li>可插拔的日志/监控拦截器 {@link HttpInterceptor}（默认 Slf4j 日志）</li>
 *     <li>请求前/后处理器（Consumer 注入，用于业务逻辑）</li>
 *     <li>全局或单次请求的 Token（Bearer）自动注入</li>
 *     <li>链式构建器 {@link RequestBuilder}，覆盖全部 HTTP 方法</li>
 *     <li>自定义异常体系</li>
 * </ul>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 * // 使用构建器发起 POST JSON 请求，带 Token
 * String result = HttpUtils.builderPost("https://api.example.com/data")
 *         .json()
 *         .body("{\"key\":\"value\"}")
 *         .bearerAuth("your-jwt-token")
 *         .executeString();
 *
 * // 自定义拦截器（上报监控）
 * HttpUtils.setGlobalConfig(HttpUtils.HttpConfig.builder()
 *         .interceptor(new MyMonitorInterceptor())
 *         .defaultContentType("application/json")
 *         .maxRetries(1)
 *         .build());
 * String resp = HttpUtils.builderGet("https://api.example.com/ping")
 *         .executeString();
 *
 * // 父类方法完全可用
 * String html = HttpUtils.downloadString("https://www.example.com", StandardCharsets.UTF_8);
 * }</pre>
 *
 * @author dream
 */
@Slf4j
public final class HttpUtils extends HttpUtil {

    /** 默认连接超时（毫秒） */
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    /** 默认读取超时（毫秒） */
    public static final int DEFAULT_READ_TIMEOUT = 30000;

    /** 全局默认配置（线程安全） */
    private static volatile HttpConfig globalConfig;

    static {
        globalConfig = HttpConfig.builder().build();
    }

    private HttpUtils() {
        // 工具类禁止实例化
    }

    // ==================== 全局配置 ====================

    /**
     * 设置全局默认配置（对所有通过本类增强方法发起的请求生效，线程安全）
     *
     * @param config 自定义配置，为 null 时重置为全默认配置
     */
    public static synchronized void setGlobalConfig(HttpConfig config) {
        globalConfig = (config != null) ? config : HttpConfig.builder().build();
    }

    /**
     * 获取当前全局配置
     *
     * @return 全局配置对象，不可为 null
     */
    public static HttpConfig getGlobalConfig() {
        return globalConfig;
    }

    // ==================== 增强执行入口 ====================

    /**
     * 使用全局配置执行 {@link HttpRequest}（提供重试、日志、Token 等增强）
     *
     * @param request 已构建的请求对象
     * @return 响应对象
     */
    public static HttpResponse execute(HttpRequest request) {
        return execute(request, globalConfig);
    }

    /**
     * 使用指定配置执行 {@link HttpRequest}，提供完整增强能力
     *
     * @param request 已构建的请求对象
     * @param config  自定义配置，为 null 时使用全局配置
     * @return 响应对象
     */
    public static HttpResponse execute(HttpRequest request, HttpConfig config) {
        final HttpConfig cfg = config != null ? config : globalConfig;
        return executeWithRetry(request, cfg);
    }

    // ==================== 构建器入口（与父类方法无冲突） ====================

    public static RequestBuilder builderGet(String url) {
        return new RequestBuilder(Method.GET, url);
    }

    public static RequestBuilder builderPost(String url) {
        return new RequestBuilder(Method.POST, url);
    }

    public static RequestBuilder builderPut(String url) {
        return new RequestBuilder(Method.PUT, url);
    }

    public static RequestBuilder builderDelete(String url) {
        return new RequestBuilder(Method.DELETE, url);
    }

    public static RequestBuilder builderPatch(String url) {
        return new RequestBuilder(Method.PATCH, url);
    }

    // ==================== 内部核心逻辑 ====================

    private static HttpResponse executeWithRetry(HttpRequest request, HttpConfig config) {
        int maxRetries = config.getMaxRetries();
        long retryInterval = config.getRetryIntervalMs();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return doExecute(request, config);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries && shouldRetry(e)) {
                    config.getInterceptor().onRetry(request, e, attempt + 1);
                    sleep(retryInterval);
                } else {
                    break;
                }
            }
        }
        throw new BusinessException("HTTP request failed after " + (maxRetries + 1) + " attempts: " +
                request.getMethod() + " " + request.getUrl());
    }

    private static HttpResponse doExecute(HttpRequest request, HttpConfig config) throws Exception {
        // 前置业务处理器
        if (config.getBeforeRequest() != null) {
            config.getBeforeRequest().accept(request);
        }

        // 自动注入全局 Bearer Token
        if (StringUtils.isNotBlank(config.getToken())) {
            if (StringUtils.isBlank(request.header("Authorization"))) {
                request.header("Authorization", "Bearer " + config.getToken());
            }
            //注入 TokenKey Token
            if(StringUtils.isNotBlank(config.getTokenKey())){
                if(StringUtils.isBlank(request.header(config.getTokenKey()))){
                    request.header(config.getTokenKey(), config.getToken());
                }
            }
        }

        // 默认 Content-Type（若请求未自行设置）
        if (StringUtils.isNotBlank(config.getDefaultContentType()) &&
                StringUtils.isBlank(request.header(Header.CONTENT_TYPE.getValue()))) {
            request.contentType(config.getDefaultContentType());
        }

        HttpInterceptor interceptor = config.getInterceptor();
        interceptor.beforeRequest(request);

        long start = System.currentTimeMillis();
        HttpResponse response;
        try {
            response = request
                    .setConnectionTimeout(config.getConnectTimeout())
                    .setReadTimeout(config.getReadTimeout())
                    .execute();
        } catch (Exception e) {
            interceptor.onError(request, e, System.currentTimeMillis() - start);
            throw e;
        }

        long cost = System.currentTimeMillis() - start;
        interceptor.afterResponse(request, response, cost);

        // 后置业务处理器
        if (config.getAfterResponse() != null) {
            config.getAfterResponse().accept(response);
        }

        return response;
    }

    static void validateStatus(HttpResponse response) {
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            throw new BusinessException("HTTP status check failed: " + status +
                    ", body: " + response.body());
        }
    }

    private static boolean shouldRetry(Exception e) {
        return e instanceof SocketTimeoutException;
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 请求构建器 ====================

    /**
     * 链式请求构建器，封装 {@link HttpRequest}，支持所有 HTTP 方法。
     * <p>
     * 最终通过 {@link #execute()} / {@link #executeString()} / {@link #executeBytes()} 执行。
     */
    public static class RequestBuilder {
        private final HttpRequest request;
        private HttpConfig config;
        private boolean validateStatus;

        private RequestBuilder(Method method, String url) {
            this.request = HttpUtil.createRequest(method, url);
            this.config = globalConfig;
            this.validateStatus = config.isValidateStatus();
        }

        /** 添加一个请求头 */
        public RequestBuilder header(String name, String value) {
            request.header(name, value);
            return this;
        }

        /** 批量添加请求头 */
        public RequestBuilder headers(Map<String, String> headers) {
            request.addHeaders(headers);
            return this;
        }

        /** 设置 Bearer Token */
        public RequestBuilder bearerAuth(String token) {
            if (StringUtils.isNotBlank(token)) {
                request.header("Authorization", "Bearer " + token);
            }
            return this;
        }

        /** 设置 header tokenKey */
        public RequestBuilder tokenKey(String tokenKey, String token) {
            if(StringUtils.isNotBlank(tokenKey) && StringUtils.isNotBlank(token)){
                request.header(tokenKey, token);
            }
            return this;
        }



        /** 设置字符串请求体 */
        public RequestBuilder body(String body) {
            request.body(body);
            return this;
        }

        /** 设置字节请求体 */
        public RequestBuilder body(byte[] bodyBytes) {
            request.body(bodyBytes);
            return this;
        }

        /** 设置表单参数（map） */
        public RequestBuilder form(Map<String, Object> formMap) {
            request.form(formMap);
            return this;
        }

        /** 添加单个表单参数 */
        public RequestBuilder form(String name, Object value) {
            request.form(name, value);
            return this;
        }

        /** 上传文件 */
        public RequestBuilder form(String name, File file) {
            request.form(name, file);
            return this;
        }

        /** 设置 Content-Type 为 application/json */
        public RequestBuilder json() {
            request.contentType(ContentType.JSON.getValue());
            return this;
        }

        /** 自定义 Content-Type */
        public RequestBuilder contentType(String contentType) {
            request.contentType(contentType);
            return this;
        }

        /** 覆盖连接/读取超时（毫秒） */
        public RequestBuilder timeout(int connectTimeout, int readTimeout) {
            this.config = this.config.toBuilder()
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .build();
            return this;
        }

        /** 是否自动校验 2xx 状态码 */
        public RequestBuilder validateStatus(boolean validate) {
            this.validateStatus = validate;
            return this;
        }

        /** 设置完整的自定义配置（为 null 则恢复全局） */
        public RequestBuilder setConfig(HttpConfig config) {
            this.config = (config != null) ? config : globalConfig;
            this.validateStatus = this.config.isValidateStatus();
            return this;
        }

        /** 获取底层的 {@link HttpRequest} */
        public HttpRequest getRequest() {
            return request;
        }

        /** 执行请求，返回原始 {@link HttpResponse} */
        public HttpResponse execute() {
            return HttpUtils.execute(request, config);
        }

        /** 执行并返回响应体字符串，若开启状态码校验则自动校验 */
        public String executeString() {
            HttpResponse response = execute();
            if (validateStatus) {
                HttpUtils.validateStatus(response);
            }
            return response.body();
        }

        /** 执行并返回响应体字节数组 */
        public byte[] executeBytes() {
            HttpResponse response = execute();
            if (validateStatus) {
                HttpUtils.validateStatus(response);
            }
            return response.bodyBytes();
        }
    }

    // ==================== HTTP 拦截器接口 ====================

    /**
     * HTTP 请求拦截器，负责请求前、响应后、异常时的处理（默认实现为 Slf4j 日志打印）。
     * <p>
     * 业务可通过实现该接口并注入 {@link HttpConfig#getInterceptor()} 来自定义日志、监控等行为。
     */
    public interface HttpInterceptor {
        /**
         * 请求发送前调用
         */
        void beforeRequest(HttpRequest request);

        /**
         * 收到响应后调用（无论成功或失败，异常场景不会触发此方法）
         */
        void afterResponse(HttpRequest request, HttpResponse response, long costMs);

        /**
         * 请求发生异常时调用（连接超时、读取异常等）
         */
        void onError(HttpRequest request, Exception e, long costMs);

        /**
         * 触发重试时调用（默认空实现）
         */
        default void onRetry(HttpRequest request, Exception e, int retryCount) {
        }
    }

    /**
     * 默认拦截器：使用 {@link Slf4j} 输出请求/响应/异常日志
     */
    @Slf4j
    static class DefaultHttpInterceptor implements HttpInterceptor {
        @Override
        public void beforeRequest(HttpRequest request) {
            log.info("HTTP request: {} {}", request.getMethod(), request.getUrl());
        }

        @Override
        public void afterResponse(HttpRequest request, HttpResponse response, long costMs) {
            log.info("HTTP response: {} {}, status={}, cost={}ms",
                    request.getMethod(), request.getUrl(), response.getStatus(), costMs);
        }

        @Override
        public void onError(HttpRequest request, Exception e, long costMs) {
            log.error("HTTP error: {} {}, cost={}ms, error={}",
                    request.getMethod(), request.getUrl(), costMs, e.getMessage(), e);
        }
    }

    // ==================== HTTP 配置类 ====================

    /**
     * HTTP 增强配置，所有属性均有合理默认值。
     */
    @Data
    @Builder(toBuilder = true)
    public static class HttpConfig {
        /** 连接超时（毫秒），默认 5000 */
        @Builder.Default
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        /** 读取超时（毫秒），默认 30000 */
        @Builder.Default
        private int readTimeout = DEFAULT_READ_TIMEOUT;

        /** 最大重试次数（0 表示不重试） */
        @Builder.Default
        private int maxRetries = 3;

        /** 重试间隔（毫秒） */
        @Builder.Default
        private long retryIntervalMs = 1000;

        /** 请求日志/监控拦截器，默认使用 Slf4j 打印 */
        @Builder.Default
        private HttpInterceptor interceptor = new DefaultHttpInterceptor();

        /** 全局默认 Content-Type（若请求未设置则自动应用） */
        @Builder.Default
        private String defaultContentType = null;

        /** 是否自动校验响应状态码为 2xx（true 时非 2xx 抛出 HttpStatusException） */
        @Builder.Default
        private boolean validateStatus = true;

        /** header 全局 Bearer Token（为空时不自动添加） */
        @Builder.Default
        private String token = null;

        /** header 全局 TokenKey（为空时不自动添加） 如 X-Token */
        @Builder.Default
        private String tokenKey = null;

        /** 请求发送前业务处理器（如签名、参数修改） */
        private java.util.function.Consumer<HttpRequest> beforeRequest;

        /** 响应接收后业务处理器（如结果解码、校验） */
        private java.util.function.Consumer<HttpResponse> afterResponse;

        /** 快速创建全默认配置 */
        public static HttpConfig createDefault() {
            return builder().build();
        }
    }


}