package com.dream.common.framework.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dream.common.core.annotation.Log;
import com.dream.common.core.config.ControllerConfig;
import com.dream.common.core.constant.OrderValue;
import com.dream.common.core.constant.SysKey;
import com.dream.common.core.enums.LogType;
import com.dream.common.core.utils.DesensitizeUtils;
import com.dream.common.framework.api.OperationLogHandler;
import com.dream.common.framework.model.OperationLog;
import com.dream.common.framework.support.SpelSupport;
import com.dream.common.framework.utils.IpUtils;
import com.dream.common.framework.utils.ServletUtils;
import com.dream.common.tool.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作日志切面：{@link Log} 注解的实际执行者。
 * <p>
 * 拦截标注 {@link Log} 的方法，采集模块标题、操作类型、请求参数、响应结果、耗时与异常信息，
 * 封装为 {@link OperationLog} 后交给 {@link OperationLogHandler}：
 * </p>
 * <ul>
 *   <li>容器中存在 {@link OperationLogHandler} Bean → 由业务实现接管（落库 / 上报）</li>
 *   <li>不存在 → 打印一条 INFO 日志，行为与 {@code AccessLogFilter} 的默认策略一致</li>
 * </ul>
 *
 * <h2>不影响业务</h2>
 * <p>
 * 日志采集全程在 {@code finally} 中执行且内部吞掉所有异常；目标方法的返回值与异常原样上送。
 * 参数/响应 JSON 均按 {@value #MAX_CONTENT_LENGTH} 字符截断，避免大对象刷爆日志表。
 * </p>
 *
 * <h2>敏感信息</h2>
 * <p>
 * 两层处理，命中即处理整棵对象树的同名字段（不区分大小写）：
 * </p>
 * <ol>
 *   <li>{@link Log#excludeParamNames()} 声明的字段名——<b>整个键移除</b>，如
 *       {@code "password"} 或 {@code "user.password"}（只取末段 {@code password} 比较）</li>
 *   <li>{@link DesensitizeUtils#isSensitiveParamName(String)} 内置的凭据类关键字——值替换为
 *       {@value #MASKED_VALUE}，如 token / secret / captcha</li>
 * </ol>
 * <p>
 * 注意：匹配的是 JSON 键名，因此 DTO <b>字段名</b>始终可用；只有最外层<b>方法参数名</b>
 * 需要编译期参数名信息（{@code -parameters} 或 {@code -g}），缺失时以 {@code arg0} 呈现。
 * </p>
 *
 * <h2>操作人信息</h2>
 * <p>
 * 本切面位于 framework 层，不感知具体认证框架，因此 {@link OperationLog} 不含用户 ID/用户名；
 * 需要操作人时在业务侧的 {@link OperationLogHandler} 实现里补全。
 * </p>
 *
 * <h2>模块标题</h2>
 * <p>
 * {@code @Log(title)} 留空时反射读取目标 Controller 的 {@code getControllerConfig().getLogTitle()}
 * （如「用户管理」）兜底，仍取不到则退到操作类型中文名。反射而非接口是因为 framework 层不依赖
 * data 模块，且业务侧的 {@code getControllerConfig()} 通常是 {@code protected}——与
 * {@code PermissionAspect} 取权限前缀的方式一致。详见 {@code resolveTitle}。
 * </p>
 *
 * <h2>开关</h2>
 * <p>{@code dream.framework.operation-log.enabled=false} 可整体关闭本切面。</p>
 *
 * @author dream
 * @see Log
 * @see OperationLog
 * @see OperationLogHandler
 */
@Slf4j
@Aspect
@Component
@Order(OrderValue.ASPECT_OPERATION_LOG)
@ConditionalOnProperty(prefix = "dream.framework.operation-log", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class OperationLogAspect {

    /** 参数 / 响应 JSON 的最大长度，超出截断 */
    private static final int MAX_CONTENT_LENGTH = 4000;

    /** 异常信息的最大长度，超出截断 */
    private static final int MAX_ERROR_LENGTH = 2000;

    /** 敏感字段掩码值 */
    private static final String MASKED_VALUE = "***";

    /** 内容被截断时的后缀 */
    private static final String TRUNCATED_SUFFIX = "...(truncated)";

    /** 不写入日志的参数类型（容器对象 / 大文件 / 校验结果，序列化无意义或代价高） */
    private static final Class<?>[] SKIPPED_PARAM_TYPES = {
            ServletRequest.class, ServletResponse.class, HttpSession.class,
            MultipartFile.class, BindingResult.class, Principal.class
    };

    /** 业务侧日志处理器（可选） */
    private final ObjectProvider<OperationLogHandler> handlerProvider;

    /**
     * {@code getControllerConfig} 方法缓存；用 {@link Optional} 包装以支持「类未声明该方法」的空缓存。
     */
    private static final Map<Class<?>, Optional<Method>> CONFIG_METHOD_CACHE = new ConcurrentHashMap<>();

    public OperationLogAspect(ObjectProvider<OperationLogHandler> handlerProvider) {
        this.handlerProvider = handlerProvider;
    }

    // ==================== 核心拦截 ====================

    /**
     * 环绕拦截 {@link Log} 注解方法，无论成功或异常都产出日志。
     *
     * @param joinPoint    连接点
     * @param annotation   注解实例（由切点绑定）
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常原样上送
     */
    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log annotation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                record(joinPoint, annotation, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("操作日志采集失败: method={}", joinPoint.toShortString(), e);
            }
        }
    }

    // ==================== 采集 ====================

    /**
     * 构建并分发一条操作日志。
     *
     * @param joinPoint  连接点
     * @param annotation 注解实例
     * @param result     方法返回值，异常时为 {@code null}
     * @param error      方法抛出的异常，成功时为 {@code null}
     * @param cost       执行耗时（毫秒）
     */
    private void record(ProceedingJoinPoint joinPoint, Log annotation, Object result,
                        Throwable error, long cost) {
        if (annotation.logType() == LogType.EXCEPTION && error == null) {
            return;
        }
        Method targetMethod = resolveTargetMethod(joinPoint);
        OperationLog entry = new OperationLog();
        entry.setTraceId(MDC.get(SysKey.TRACE_ID));
        entry.setTitle(resolveTitle(annotation, joinPoint.getTarget()));
        entry.setOperationType(annotation.operationType().getValue());
        entry.setLogType(annotation.logType().getValue());
        entry.setClassName(targetMethod.getDeclaringClass().getName());
        entry.setMethodName(targetMethod.getName());
        fillRequestInfo(entry);
        entry.setSuccess(error == null);
        entry.setCost(annotation.isRecordCostTime() ? cost : null);
        entry.setTimestamp(System.currentTimeMillis());

        if (annotation.isSaveRequestData()) {
            entry.setParams(serializeArgs(targetMethod, joinPoint.getArgs(), annotation.excludeParamNames()));
        }
        if (annotation.isSaveResponseData() && error == null && result != null) {
            entry.setResponseData(truncate(safeToJson(result), MAX_CONTENT_LENGTH));
        }
        if (error != null) {
            entry.setErrorMsg(truncate(error.getClass().getName() + ": " + error.getMessage(), MAX_ERROR_LENGTH));
        }
        dispatch(entry);
    }

    /**
     * 解析模块标题，优先级：{@link Log#title()} &gt; 目标 Controller 的
     * {@code ControllerConfig.logTitle}（通用 CRUD 基类的注解是共享的，标题只能在运行时取）
     * &gt; {@link Log#operationType()} 的中文名（如「新增」），避免标题列为空。
     *
     * @param annotation 注解实例
     * @param target     目标对象（通常是 Controller 实例），可能为 {@code null}
     * @return 模块标题，非 {@code null}
     */
    private String resolveTitle(Log annotation, Object target) {
        String title = annotation.title();
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        String logTitle = readLogTitle(target);
        if (logTitle != null && !logTitle.trim().isEmpty()) {
            return logTitle;
        }
        return annotation.operationType().getLabel();
    }

    /**
     * 反射读取 Controller 声明的模块名；任何失败都只当作「未配置」，不影响日志采集。
     *
     * @param target 目标对象，可为 {@code null}
     * @return 模块名；无 {@code getControllerConfig()} 或未配置 logTitle 时为 {@code null}
     */
    private String readLogTitle(Object target) {
        if (target == null) {
            return null;
        }
        Method method = CONFIG_METHOD_CACHE
                .computeIfAbsent(target.getClass(), c -> Optional.ofNullable(findControllerConfigMethod(c)))
                .orElse(null);
        if (method == null) {
            return null;
        }
        try {
            Object config = method.invoke(target);
            return config instanceof ControllerConfig ? ((ControllerConfig) config).getLogTitle() : null;
        } catch (Exception e) {
            log.debug("读取模块标题失败: class={}", target.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 沿继承链查找 {@code getControllerConfig()}（业务侧多为 {@code protected}），并放开访问权限。
     *
     * @param clazz 目标类
     * @return 方法对象，未找到返回 {@code null}
     */
    private Method findControllerConfigMethod(Class<?> clazz) {
        Method method = ReflectionUtils.findMethod(clazz, "getControllerConfig");
        if (method == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(method);
        return method;
    }

    /**
     * 补充当前 Web 请求的 URI / HTTP 方法 / 客户端 IP；非 Web 上下文保持为 {@code null}。
     *
     * @param entry 待填充的日志对象
     */
    private void fillRequestInfo(OperationLog entry) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        String queryString = request.getQueryString();
        entry.setRequestUri(queryString != null
                ? request.getRequestURI() + "?" + DesensitizeUtils.maskSensitiveQuery(queryString)
                : request.getRequestURI());
        entry.setHttpMethod(request.getMethod());
        entry.setIp(IpUtils.getIpAddress(request));
    }

    /**
     * 将方法入参序列化为 JSON，并剔除 / 掩码敏感字段。
     *
     * @param method            目标方法（用于取参数名）
     * @param args              实参数组
     * @param excludeParamNames 注解声明的排除字段名
     * @return 参数 JSON；无可用参数时返回 {@code null}
     */
    private String serializeArgs(Method method, Object[] args, String[] excludeParamNames) {
        if (args == null || args.length == 0) {
            return null;
        }
        String[] names = SpelSupport.getNameDiscoverer().getParameterNames(method);
        JSONObject holder = new JSONObject();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || isSkipped(arg)) {
                continue;
            }
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            holder.put(name, safeToJsonTree(arg));
        }
        if (holder.isEmpty()) {
            return null;
        }
        Set<String> excludes = toLeafNames(excludeParamNames);
        sanitize(holder, excludes);
        return truncate(holder.toJSONString(), MAX_CONTENT_LENGTH);
    }

    /**
     * 递归处理参数树：命中排除字段名则移除，命中内置敏感关键字则掩码。
     *
     * @param node     当前 JSON 节点
     * @param excludes 需移除的叶子字段名集合（已转小写）
     */
    private void sanitize(Object node, Set<String> excludes) {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            for (String key : new HashSet<>(object.keySet())) {
                if (excludes.contains(key.toLowerCase(java.util.Locale.ROOT))) {
                    object.remove(key);
                    continue;
                }
                Object value = object.get(key);
                if (DesensitizeUtils.isSensitiveParamName(key) && value != null) {
                    object.put(key, MASKED_VALUE);
                } else {
                    sanitize(value, excludes);
                }
            }
            return;
        }
        if (node instanceof JSONArray) {
            for (Object item : (JSONArray) node) {
                sanitize(item, excludes);
            }
        }
    }

    // ==================== 分发 ====================

    /**
     * 分发日志：优先业务实现，否则打印默认 INFO 日志。
     *
     * @param entry 操作日志
     */
    private void dispatch(OperationLog entry) {
        OperationLogHandler handler = handlerProvider.getIfAvailable();
        if (handler != null) {
            try {
                handler.handle(entry);
            } catch (Exception e) {
                log.error("自定义操作日志处理失败: title={}, method={}#{}",
                        entry.getTitle(), entry.getClassName(), entry.getMethodName(), e);
            }
            return;
        }
        log.info("操作日志: traceId={}, title={}, operationType={}, method={}#{}, ip={}, uri={}, "
                        + "success={}, cost={}ms, error={}",
                entry.getTraceId(), entry.getTitle(), entry.getOperationType(),
                entry.getClassName(), entry.getMethodName(), entry.getIp(), entry.getRequestUri(),
                entry.isSuccess(), entry.getCost(), entry.getErrorMsg());
    }

    // ==================== 工具 ====================

    /**
     * 注解声明的排除字段名转为叶子名集合（{@code "user.password"} 取 {@code "password"}）。
     *
     * @param excludeParamNames 注解数组，可为空
     * @return 小写叶子名集合
     */
    private Set<String> toLeafNames(String[] excludeParamNames) {
        Set<String> leaves = new HashSet<>();
        if (excludeParamNames == null) {
            return leaves;
        }
        for (String name : excludeParamNames) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            String trimmed = name.trim();
            int dot = trimmed.lastIndexOf('.');
            String leaf = dot >= 0 ? trimmed.substring(dot + 1) : trimmed;
            if (!leaf.isEmpty()) {
                leaves.add(leaf.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return leaves;
    }

    /**
     * 参数是否无需写入日志（容器对象 / 上传文件 / 校验结果）。
     *
     * @param arg 实参
     * @return true 表示跳过
     */
    private boolean isSkipped(Object arg) {
        for (Class<?> skipped : SKIPPED_PARAM_TYPES) {
            if (skipped.isInstance(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对象转 JSON 文本，序列化失败时降级为 {@code String#valueOf}，不中断日志采集。
     *
     * @param value 目标对象
     * @return JSON 文本或 toString 结果
     */
    private String safeToJson(Object value) {
        try {
            return JsonUtils.toJson(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 对象转 JSON 树（用于递归脱敏），失败时降级为字符串节点。
     *
     * @param value 目标对象
     * @return JSON 节点或原始值
     */
    private Object safeToJsonTree(Object value) {
        try {
            return JSON.toJSON(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 超长截断。
     *
     * @param content   原文
     * @param maxLength 最大长度
     * @return 截断后的文本；入参为 {@code null} 时返回 {@code null}
     */
    private String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + TRUNCATED_SUFFIX;
    }

    /**
     * 解析目标类上的实际执行方法（避免 JDK 代理下接口方法缺失注解/参数名）。
     *
     * @param joinPoint 连接点
     * @return 目标方法
     */
    private Method resolveTargetMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (joinPoint.getTarget() == null || method.getDeclaringClass() == joinPoint.getTarget().getClass()) {
            return method;
        }
        try {
            Method specific = ReflectionUtils.findMethod(joinPoint.getTarget().getClass(),
                    method.getName(), method.getParameterTypes());
            return specific != null ? specific : method;
        } catch (Exception e) {
            return method;
        }
    }
}
