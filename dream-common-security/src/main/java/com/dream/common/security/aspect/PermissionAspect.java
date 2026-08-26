package com.dream.common.security.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.dream.common.core.annotation.Permission;
import com.dream.common.core.config.ControllerConfig;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限校验切面
 * <p>
 * 拦截标注 {@link Permission} 注解的 Controller 方法，基于 Sa-Token 进行权限校验。
 * 支持动态前缀拼接：当目标对象存在 {@code getPermissionPrefix()} 方法时，
 * 自动拼接 {@code 前缀 + ":" + 动作后缀} 为完整权限标识。
 * </p>
 *
 * <h2>权限解析规则</h2>
 * <ul>
 *   <li>当 {@link Permission#value()} 不含冒号（如 {@code "list"}），
 *       完整权限 = {@code getPermissionPrefix() + ":" + value}</li>
 *   <li>当 {@link Permission#value()} 含冒号（如 {@code "system:user:list"}），
 *       直接作为完整权限使用</li>
 *   <li>目标对象无 {@code getPermissionPrefix()} 方法时，{@code value} 原样使用</li>
 * </ul>
 *
 * <h2>多权限逻辑</h2>
 * <ul>
 *   <li>{@link Permission.Logical#AND}：用户必须拥有全部权限（默认）</li>
 *   <li>{@link Permission.Logical#OR}：用户拥有任意一个权限即可</li>
 * </ul>
 *
 * <h2>异常处理</h2>
 * <p>
 * Sa-Token 校验失败时抛出 {@code NotPermissionException}（无权限）或
 * {@code NotLoginException}（未登录），异常直接上抛，由全局异常处理器统一处理。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // CommonController 子类自动生效
 * @RestController
 * @RequestMapping("/user")
 * public class UserController extends CommonController<UserService, User> {
 *     public UserController(UserService baseService) { super(baseService); }
 *
 *     @Override
 *     protected String getPermissionPrefix() { return "system:user"; }
 * }
 *
 * // 也支持独立 Controller 直接标注
 * @Permission("system:user:export")
 * @PostMapping("/export")
 * public R<Void> export() { ... }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-24
 * @see Permission
 */
@Slf4j
@Aspect
public class PermissionAspect {

    /**
     * 拦截所有标注 {@link Permission} 注解的方法，执行权限校验。
     *
     * @param joinPoint  AOP 连接点
     * @param permission 权限注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常或权限校验失败
     */
    @Around("@annotation(permission)")
    public Object around(ProceedingJoinPoint joinPoint, Permission permission) throws Throwable {
        // 1. 解析完整权限标识
        String[] permissions = resolvePermissions(joinPoint, permission);

        if (permissions.length == 0) {
            log.warn("权限标识为空，跳过校验：{}", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        // 2. 执行 Sa-Token 权限校验
        checkPermissions(permissions, permission.logical());

        log.debug("权限校验通过：{}", String.join(",", permissions));
        return joinPoint.proceed();
    }

    /**
     * 解析完整权限标识数组
     * <p>
     * 对不含冒号的 {@code value}，通过反射调用目标对象的 {@code getPermissionPrefix()}
     * 拼接完整权限标识；含冒号的直接使用。
     * </p>
     *
     * @param joinPoint  AOP 连接点
     * @param permission 权限注解
     * @return 完整权限标识数组
     */
    private String[] resolvePermissions(ProceedingJoinPoint joinPoint, Permission permission) {
        String[] values = permission.value();
        if (values.length == 0) {
            return new String[0];
        }

        // 获取权限前缀（反射调用 getPermissionPrefix）
        String prefix = getPermissionPrefix(joinPoint.getTarget());

        List<String> resolved = new ArrayList<>(values.length);
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (value.contains(":")) {
                // 含冒号，直接作为完整权限标识
                resolved.add(value);
            } else if (prefix != null && !prefix.trim().isEmpty()) {
                // 拼接前缀
                resolved.add(prefix + ":" + value);
            } else {
                // 无前缀，原样使用
                resolved.add(value);
            }
        }
        return resolved.toArray(new String[0]);
    }

    /**
     * 通过反射获取目标对象的权限前缀
     * <p>
     * 尝试调用 {@code getControllerConfig()} 方法；若目标对象无此方法，返回 {@code null}。
     * </p>
     *
     * @param target 目标对象
     * @return 权限前缀；不存在返回 {@code null}
     */
    private String getPermissionPrefix(Object target) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod("getControllerConfig");
            method.setAccessible(true);
            Object result = method.invoke(target);
            if (result instanceof ControllerConfig) {
                return ((ControllerConfig) result).getPermissionPrefix();
            }
        } catch (NoSuchMethodException e) {
            // 目标对象无 getControllerConfig 方法，返回 null
            log.debug("目标对象无 getControllerConfig 方法：{}", target.getClass().getName());
        } catch (Exception e) {
            log.warn("调用 getControllerConfig 失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 执行 Sa-Token 权限校验
     *
     * @param permissions 完整权限标识数组
     * @param logical     多权限逻辑关系
     */
    private void checkPermissions(String[] permissions, Permission.Logical logical) {
        if (permissions.length == 1) {
            StpUtil.checkPermission(permissions[0]);
        } else if (logical == Permission.Logical.OR) {
            StpUtil.checkPermissionOr(permissions);
        } else {
            StpUtil.checkPermissionAnd(permissions);
        }
    }
}
