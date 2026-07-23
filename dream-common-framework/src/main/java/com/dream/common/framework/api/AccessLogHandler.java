package com.dream.common.framework.api;

import com.dream.common.framework.model.AccessLog;

/**
 * 请求访问日志处理接口（SPI）。
 * <p>
 * 框架的 {@code AccessLogFilter} 负责采集每次请求的 method/uri/ip/状态码/耗时/traceId，
 * 并封装为 {@link AccessLog}。业务方可实现此接口以自定义处理逻辑（如落库、异步上报、监控埋点等）。
 * </p>
 * <p>
 * 未注册实现时，框架使用默认行为（打印一条访问日志）；注册后由业务实现接管。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class DbAccessLogHandler implements AccessLogHandler {
 *     @Override
 *     public void handle(AccessLog accessLog) {
 *         accessLogService.saveAsync(accessLog);
 *     }
 * }
 * }</pre>
 *
 * @author dream
 */
public interface AccessLogHandler {

    /**
     * 处理一条请求访问日志。
     *
     * @param accessLog 访问日志数据
     */
    void handle(AccessLog accessLog);
}
