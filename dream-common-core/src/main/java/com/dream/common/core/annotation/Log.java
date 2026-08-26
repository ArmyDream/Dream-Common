package com.dream.common.core.annotation;

import com.dream.common.core.enums.LogType;
import com.dream.common.core.enums.OperationType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，用于声明该接口的操作日志元数据。
 * 框架通过 AOP 切面拦截标注本注解的方法，自动记录操作人、请求参数、
 * 响应结果、执行耗时、异常信息等，并持久化到操作日志表。
 * </p>
 *
 * <h2>日志内容</h2>
 * <ul>
 *   <li>操作人信息（用户 ID、用户名、登录 IP、登录地点）</li>
 *   <li>请求信息（请求 URL、HTTP 方法、请求参数、请求头）</li>
 *   <li>操作结果（成功/失败、耗时、响应数据、异常信息）</li>
 *   <li>操作类型与模块标题（由本注解声明）</li>
 * </ul>
 *
 * <h2>敏感数据脱敏</h2>
 * <p>
 * 通过 {@link #excludeParamNames()} 可排除敏感参数（如密码、身份证号），
 * 避免敏感信息明文写入日志表。
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
     * 序列化请求参数时，排除这些字段，避免敏感信息（如密码、身份证号、银行卡号）写入日志。
     * 支持嵌套属性，如 {@code "user.password"}。
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
