package com.dream.common.security.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.dream.common.core.annotation.Permission;
import com.dream.common.core.config.ControllerConfig;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限校验切面，拦截标注 {@link Permission} 注解的方法，基于 Sa-Token 进行权限校验。
 * <p>支持通过 {@link ControllerConfig#getPermissionPrefix()} 动态拼接权限前缀。</p>
 *
 * @author dream
 * @since 2026-08-24
 */
@Slf4j
@Aspect
public class PermissionAspect {

    private static final Map<Class<?>, Method> CONFIG_METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * 环绕通知：校验权限后执行目标方法。
     *
     * @param joinPoint  连接点
     * @param permission 权限注解
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常或权限校验异常
     */
    @Around("@annotation(permission)")
    public Object around(ProceedingJoinPoint joinPoint, Permission permission) throws Throwable {
        String[] permissions = resolvePermissions(joinPoint, permission);
        if (permissions.length == 0) {
            throw new BusinessException(ResponseCode.FORBIDDEN,
                    "权限校验配置错误：@Permission 未解析出有效权限标识，方法="
                            + joinPoint.getSignature().toShortString());
        }

        checkPermissions(permissions, permission.logical());
        if (log.isDebugEnabled()) {
            log.debug("权限校验通过：{}", String.join(",", permissions));
        }
        return joinPoint.proceed();
    }

    /**
     * 解析完整权限标识数组。
     *
     * @param joinPoint  连接点
     * @param permission 注解
     * @return 权限标识数组
     */
    private String[] resolvePermissions(ProceedingJoinPoint joinPoint, Permission permission) {
        String[] values = permission.value();
        if (values.length == 0) {
            return new String[0];
        }

        String prefix = getPermissionPrefix(joinPoint.getTarget());
        List<String> resolved = new ArrayList<>(values.length);
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (value.contains(":")) {
                resolved.add(value);
            } else if (prefix != null && !prefix.trim().isEmpty()) {
                resolved.add(prefix + ":" + value);
            } else {
                resolved.add(value);
            }
        }
        return resolved.toArray(new String[0]);
    }

    /**
     * 通过反射获取目标对象的权限前缀。
     *
     * @param target 目标对象
     * @return 权限前缀，若获取失败返回 {@code null}
     * @throws BusinessException 当方法调用异常或返回类型不匹配时抛出
     */
    private String getPermissionPrefix(Object target) {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), "getControllerConfig");
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(target);
            if (result == null) {
                return null;
            }
            if (result instanceof ControllerConfig) {
                return ((ControllerConfig) result).getPermissionPrefix();
            }
            throw new BusinessException(ResponseCode.FORBIDDEN,
                    "权限前缀解析失败：getControllerConfig() 返回类型异常，class="
                            + target.getClass().getName() + ", actual=" + result.getClass().getName());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.FORBIDDEN,
                    "权限前缀解析失败：getControllerConfig() 调用异常，class="
                            + target.getClass().getName(), e);
        }
    }

    /**
     * 沿继承链查找指定方法（支持 protected/private）。
     *
     * @param clazz      类
     * @param methodName 方法名
     * @return 方法对象，未找到返回 {@code null}
     */
    private Method findMethod(Class<?> clazz, String methodName) {
        return CONFIG_METHOD_CACHE.computeIfAbsent(clazz, c -> {
            Class<?> current = c;
            while (current != null && current != Object.class) {
                try {
                    Method method = current.getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                }
                current = current.getSuperclass();
            }
            return null;
        });
    }

    /**
     * 执行 Sa-Token 权限校验。
     *
     * @param permissions 权限标识数组
     * @param logical     逻辑关系（AND/OR）
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