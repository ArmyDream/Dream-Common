package com.dream.common.core.constant;

/**
 * HTTP 相关常量
 *
 * @author dream
 */
public final class Http {
    private Http() {
    }

    // ==================== 请求头 ====================
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_CONNECTION = "Connection";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_ORIGIN = "Origin";
    public static final String HEADER_HOST = "Host";
    public static final String HEADER_X_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    //自定义 头 X-Client-Type 请求端类型 如 PC/APP/H5
    public static final String HEADER_X_CLIENT_TYPE = "X-Client-Type";
    //X-Crypto-Secret-Key 请求头 加密密钥
    public static final String HEADER_X_CRYPTO_SECRET_KEY = "X-Crypto-Secret-Key";
    //X-Trace-Id 请求头 链路追踪ID
    public static final String HEADER_X_TRACE_ID = "X-Trace-Id";

    /**
     * 跨域相关
     */
    public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    /**
     * 认证相关
     */
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization";

    /**
     * 条件请求
     */
    public static final String HEADER_IF_MATCH = "If-Match";
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String HEADER_ETAG = "ETag";

    /**
     * 安全相关响应头
     */
    public static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String HEADER_X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String HEADER_X_XSS_PROTECTION = "X-XSS-Protection";
    public static final String HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String HEADER_REFERRER_POLICY = "Referrer-Policy";
    public static final String HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES = "X-Permitted-Cross-Domain-Policies";
    public static final String HEADER_X_DOWNLOAD_OPTIONS = "X-Download-Options";
    public static final String HEADER_CROSS_ORIGIN_EMBEDDER_POLICY = "Cross-Origin-Embedder-Policy";
    public static final String HEADER_CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy";
    public static final String HEADER_CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy";
    public static final String HEADER_PERMISSIONS_POLICY = "Permissions-Policy";
    public static final String HEADER_CLEAR_SITE_DATA = "Clear-Site-Data";
    public static final String HEADER_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String HEADER_ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    public static final String HEADER_EXPIRES = "Expires";
    public static final String HEADER_PRAGMA = "Pragma";


}
