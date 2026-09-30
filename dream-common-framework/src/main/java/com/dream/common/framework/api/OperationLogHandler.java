package com.dream.common.framework.api;

import com.dream.common.framework.model.OperationLog;

/**
 * 操作日志处理接口（SPI）。
 * <p>
 * {@code OperationLogAspect} 拦截 {@code com.dream.common.core.annotation.Log} 标注的方法，
 * 把注解元数据 + 本次调用的参数/结果/耗时/IP 等封装为 {@link OperationLog}，交给本接口处理。
 * </p>
 * <p>
 * 未注册实现时框架只打印一条 INFO 日志；注册后由业务实现接管，典型用法是补全操作人信息后落库
 * （表结构由业务侧持有，框架不预设）或异步上报。实现需自行保证幂等与不抛异常，
 * 抛出异常会被切面捕获并记录，不会影响业务调用。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class DbOperationLogHandler implements OperationLogHandler {
 *     @Override
 *     public void handle(OperationLog log) {
 *         SysOperLog entity = new SysOperLog();
 *         entity.setTitle(log.getTitle());
 *         entity.setOperName(SecurityUtils.getUsername());   // 操作人由业务侧补全
 *         entity.setParams(log.getParams());
 *         entity.setStatus(log.isSuccess() ? 0 : 1);
 *         entity.setErrorMsg(log.getErrorMsg());
 *         entity.setCostTime(log.getCost());
 *         operLogService.saveAsync(entity);
 *     }
 * }
 * }</pre>
 *
 * @author dream
 * @see OperationLog
 */
public interface OperationLogHandler {

    /**
     * 处理一条操作日志。
     *
     * @param operationLog 操作日志数据
     */
    void handle(OperationLog operationLog);
}
