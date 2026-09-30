package com.dream.common.framework.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 操作日志数据模型。
 * <p>
 * 由 {@code OperationLogAspect} 依据 {@code com.dream.common.core.annotation.Log} 注解
 * 与当次调用上下文采集，交给 {@code OperationLogHandler} 处理（默认打印，业务方可落库/上报）。
 * </p>
 * <p>
 * 本模型只承载框架<b>无需依赖认证体系即可获得</b>的信息；操作人（用户 ID / 用户名）由业务方
 * 在自己的 {@code OperationLogHandler} 实现里补齐——framework 层不感知具体登录框架。
 * </p>
 *
 * @author dream
 * @see com.dream.common.core.annotation.Log
 * @see com.dream.common.framework.api.OperationLogHandler
 */
@Data
@NoArgsConstructor
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 链路追踪 ID（与访问日志、MDC 同源，可空） */
    private String traceId;

    /** 模块标题，来自 {@code @Log#title()} */
    private String title;

    /** 操作类型值，来自 {@code @Log#operationType()} 的 value（如 insert / update） */
    private String operationType;

    /** 日志类型值，来自 {@code @Log#logType()} 的 value（如 operation / exception） */
    private String logType;

    /** 目标类全限定名 */
    private String className;

    /** 目标方法名 */
    private String methodName;

    /** 请求 URI（含脱敏后的查询串，非 Web 上下文为 null） */
    private String requestUri;

    /** HTTP 方法（GET/POST 等，非 Web 上下文为 null） */
    private String httpMethod;

    /** 客户端 IP（非 Web 上下文为 null） */
    private String ip;

    /** 请求参数 JSON（已按 {@code excludeParamNames} 剔除、敏感字段掩码、超长截断），未开启记录时为 null */
    private String params;

    /** 响应数据 JSON（已截断），未开启记录或调用异常时为 null */
    private String responseData;

    /** 是否执行成功（未抛异常） */
    private boolean success;

    /** 异常信息（类名 + 消息），成功时为 null */
    private String errorMsg;

    /** 执行耗时（毫秒），{@code isRecordCostTime = false} 时为 null */
    private Long cost;

    /** 日志采集时间戳（毫秒） */
    private long timestamp;
}
