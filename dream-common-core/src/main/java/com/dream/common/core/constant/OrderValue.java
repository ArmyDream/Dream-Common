package com.dream.common.core.constant;

/**
 * 加载 优先级 常量
 * 值越大，执行越靠后。
 *
 * @author dream
 */
public interface OrderValue {
    /**
     * 最小，最先加载
     */
    int MIN_VALUE = Integer.MIN_VALUE;

    /**
     * 最大 ,最后加载
     */
    int MAX_VALUE = Integer.MAX_VALUE;

    /**
     * RestControllerAdvice  、ExceptionHandler
     */
    int ADVICE = Integer.MAX_VALUE;

    /**
     * CORS_FILTER_ORDER
     */
    int CORS_FILTER = -102;

    /**
     * SA_TOKEN_FILTER_ORDER
     */
    int SA_TOKEN_FILTER = -101;

}