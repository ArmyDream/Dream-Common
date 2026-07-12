package com.dream.common.core.enums;

/**
 * HTTP 请求方法枚举
 *
 * @author dream
 */
public enum HttpMethod implements BaseEnum<String> {

    GET("GET", "GET"),
    POST("POST", "POST"),
    PUT("PUT", "PUT"),
    DELETE("DELETE", "DELETE"),
    PATCH("PATCH", "PATCH"),
    HEAD("HEAD", "HEAD"),
    OPTIONS("OPTIONS", "OPTIONS"),
    TRACE("TRACE", "TRACE");

    private final String value;
    private final String label;

    HttpMethod(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * 判断是否为幂等请求方法
     * GET / PUT / DELETE / HEAD / OPTIONS 是幂等的
     */
    public boolean isIdempotent() {
        return this == GET || this == PUT || this == DELETE || this == HEAD || this == OPTIONS;
    }

    /**
     * 判断是否为安全请求方法（不会修改服务器状态）
     * GET / HEAD / OPTIONS 是安全的
     */
    public boolean isSafe() {
        return this == GET || this == HEAD || this == OPTIONS;
    }

    public static HttpMethod resolve( String method) {
        for (HttpMethod httpMethod : values()) {
            if (httpMethod.name().equalsIgnoreCase(method)) {
                return httpMethod;
            }
        }
        return null;
    }

    public boolean matches(String method) {
        return (this == resolve(method));
    }

    @Override
    public String getColor() {
        switch (this) {
            case GET:
                return "#52C41A";
            case POST:
                return "#1890FF";
            case PUT:
                return "#FAAD14";
            case DELETE:
                return "#FF4D4F";
            case PATCH:
                return "#722ED1";
            default:
                return "#8C8C8C";
        }
    }

    public static HttpMethod fromValue(String value) {
        return BaseEnum.fromValue(HttpMethod.class, value);
    }

}