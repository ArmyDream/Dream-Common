package com.dream.common.core.annotation;

import com.dream.common.core.enums.LogType;
import com.dream.common.core.enums.OperationType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，用于声明该接口的操作日志元数据。
 * 框架由 {@code OperationLogAspect}（framework 模块）拦截并采集模块标题、操作类型、
 * 请求参数、响应结果、执行耗时与异常信息，封装为 {@code OperationLog}。
 * </p>
 * <p>
 * 日志去向由 {@code OperationLogHandler}（framework 模块 SPI）决定：业务方注册实现即接管
 * （补全操作人后落库、异步上报等）；未注册时框架只打印一条 INFO 日志。
 * 框架<b>不内置操作日志表</b>，也不感知登录框架，故模型中无用户 ID/用户名。
 * 开关：{@code dream.framework.operation-log.enabled=false} 整体关闭采集。
 * </p>
 *
 * <h2>日志内容</h2>
 * <ul>
 *   <li>请求信息（请求 URI、HTTP 方法、客户端 IP、请求参数）</li>
 *   <li>操作结果（成功/失败、耗时、响应数据、异常信息）</li>
 *   <li>链路信息（traceId）</li>
 *   <li>操作类型与模块标题（由本注解声明）</li>
 * </ul>
 *
 * <h2>敏感数据脱敏</h2>
 * <p>
 * 框架总是按内置关键字（password / token / secret / captcha 等）把命中的字段值替换为
 * {@code ***}；额外可通过 {@link #excludeParamNames()} 让指定字段<b>整个不写入日志</b>。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 基础用法：记录新增操作
 * @Log(title = "用户管理", operationType = OperationType.INSERT)
 * @PostMapping("/add")
 * public R<Void> add(@RequestBody User user) { ... }
 *
 * // 记录修改操作，不记录请求参数和响应数据
 * @Log(title = "用户管理", operationType = OperationType.UPDATE,
 *      isSaveRequestData = false, isSaveResponseData = false)
 * @PutMapping("/update")
 * public R<Void> update(@RequestBody User user) { ... }
 *
 * // 排除密码字段，避免敏感信息泄漏
 * @Log(title = "用户管理", operationType = OperationType.UPDATE,
 *      excludeParamNames = {"password", "oldPassword"})
 * @PutMapping("/updatePwd")
 * public R<Void> updatePassword(@RequestBody PasswordDTO dto) { ... }
 *
 * // 导出操作（记录为系统日志）
 * @Log(title = "用户管理", operationType = OperationType.EXPORT,
 *      logType = LogType.SYSTEM)
 * @PostMapping("/export")
 * public void export(HttpServletResponse response) { ... }
 *
 * // 异常日志（仅记录异常情况）
 * @Log(title = "定时任务", operationType = OperationType.OTHER,
 *      logType = LogType.EXCEPTION)
 * @PostMapping("/trigger")
 * public R<Void> triggerJob(@RequestBody JobDTO dto) { ... }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-24
 * @see Permission
 * @see OperationType
 * @see LogType
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 模块标题
     * <p>
     * 用于标识当前操作所属的业务模块，如 "用户管理"、"角色管理"、"字典管理"。
     * 日志列表中作为一级分类展示。
     * </p>
     *
     * @return 模块标题
     */
    String title() default "";

    /**
     * 操作类型
     * <p>
     * 标识当前操作的类型（新增、修改、删除、查询、导出等），
     * 默认 {@link OperationType#OTHER}。
     * </p>
     *
     * @return 操作类型
     */
    OperationType operationType() default OperationType.OTHER;

    /**
     * 日志类型
     * <p>
     * 标识日志归类（操作日志、登录日志、异常日志、系统日志等），
     * 默认 {@link LogType#OPERATION}。
     * </p>
     * <p>
     * 特例：{@link LogType#EXCEPTION} 只在目标方法抛出异常时才产出日志，正常返回不记录。
     * </p>
     *
     * @return 日志类型
     */
    LogType logType() default LogType.OPERATION;

    /**
     * 是否保存请求参数
     * <p>
     * {@code true}：将请求参数（RequestBody / RequestParam / PathVariable）序列化为 JSON 存入日志（默认）。
     * {@code false}：不记录请求参数。
     * </p>
     * <p>
     * 对于参数量大或含敏感信息的接口，可设置为 {@code false}。
     * </p>
     *
     * @return 是否保存请求参数
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应数据
     * <p>
     * {@code true}：将方法返回值序列化为 JSON 存入日志。
     * {@code false}：不记录响应数据（默认）。
     * </p>
     * <p>
     * 对于列表查询、导出等返回数据量大的接口，建议设置为 {@code false}。
     * </p>
     *
     * @return 是否保存响应数据
     */
    boolean isSaveResponseData() default false;

    /**
     * 排除的请求参数字段名
     * <p>
     * 序列化请求参数时，命中名称的字段<b>整个不写入日志</b>（内置敏感关键字只是把值替换为
     * {@code ***}，本属性是把键一并去掉）。匹配规则：按<b>字段名</b>递归匹配参数对象的任意层级，
     * 不区分大小写；写成 {@code "user.password"} 时只取末段 {@code password} 参与比较，
     * 嵌套前缀仅为可读性。
     * </p>
     * <p>
     * 示例：{@code {"password", "oldPassword", "confirmPassword"}}
     * </p>
     *
     * @return 排除的参数字段名数组
     */
    String[] excludeParamNames() default {};

    /**
     * 是否记录方法执行耗时（毫秒）
     * <p>
     * 默认 {@code true}，在日志中记录接口耗时，便于性能分析。
     * </p>
     *
     * @return 是否记录耗时
     */
    boolean isRecordCostTime() default true;
}
