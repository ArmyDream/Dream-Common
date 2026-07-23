package com.dream.common.framework.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 请求访问日志数据模型。
 * <p>
 * 由 {@code AccessLogFilter} 采集并封装单次请求的关键信息，
 * 传递给 {@code AccessLogHandler} 供业务方自定义处理（如落库、上报）。
 * </p>
 *
 * @author dream
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 请求方法（GET/POST 等）
     */
    private String method;

    /**
     * 请求 URI（含查询串）
     */
    private String uri;

    /**
     * 客户端 IP 地址
     */
    private String ip;

    /**
     * 响应状态码
     */
    private int status;

    /**
     * 请求耗时（毫秒）
     */
    private long cost;

    /**
     * 请求发生时间戳（毫秒）
     */
    private long timestamp;
}
