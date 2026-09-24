package com.dream.common.security.support;

import com.dream.common.core.constant.Security;
import com.dream.common.framework.cache.CachesUtils;
import com.dream.common.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权白名单路径提供者。
 * <p>
 * 每次请求实时合并三个来源，支持运行期热更新（无需重启）：
 * </p>
 * <ul>
 *   <li>内置：{@code /error}（Spring Boot 错误页，必须放行否则异常响应被二次拦截）</li>
 *   <li>配置：{@code dream.security.filter.exclude-paths}（Ant 风格）</li>
 *   <li>缓存：{@link CachesUtils} key = {@link Security#CACHE_PREFIX_EXCLUDE_PATHS}（逗号分隔）</li>
 * </ul>
 * <p>
 * 不引入本地缓存层：{@link CachesUtils} 自身已具备 Redis 优先 + Caffeine 兜底 + 熔断降级，
 * 叠加本地缓存会导致白名单热更新延迟。
 * </p>
 *
 * @author dream
 * @since 2026-09-24
 */
@Slf4j
public class SecurityExcludePathProvider {

    /** Spring Boot 默认错误页路径 */
    private static final String BUILTIN_EXCLUDE_ERROR = "/error";

    private final SecurityProperties properties;

    /**
     * 构造器注入安全配置。
     *
     * @param properties 安全配置属性
     */
    public SecurityExcludePathProvider(SecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取合并后的白名单路径列表（内置 + 配置 + 缓存）。
     * <p>
     * 每次调用实时读取，保证缓存写入后立即生效。
     * </p>
     *
     * @return 白名单路径列表，至少包含内置的 {@code /error}
     */
    public List<String> getExcludePaths() {
        List<String> result = new ArrayList<>();
        result.add(BUILTIN_EXCLUDE_ERROR);
        mergeConfiguredPaths(result);
        mergeCachedPaths(result);
        if (log.isDebugEnabled()) {
            log.debug("[SaToken] 白名单路径: {}", result);
        }
        return result;
    }

    /**
     * 合并配置文件中的静态白名单。
     *
     * @param result 待合并的结果列表
     */
    private void mergeConfiguredPaths(List<String> result) {
        List<String> configPaths = properties.getFilter().getExcludePaths();
        if (configPaths == null) {
            return;
        }
        for (String path : configPaths) {
            addIfAbsent(result, path);
        }
    }

    /**
     * 合并缓存中的动态白名单（key = {@link Security#CACHE_PREFIX_EXCLUDE_PATHS}，逗号分隔）。
     * <p>
     * 缓存读取异常时降级为仅使用已有路径，不阻断鉴权流程。
     * </p>
     *
     * @param result 待合并的结果列表
     */
    private void mergeCachedPaths(List<String> result) {
        String cached;
        try {
            cached = CachesUtils.get(Security.CACHE_PREFIX_EXCLUDE_PATHS);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[SaToken] 从缓存读取白名单失败，仅使用配置路径", e);
            }
            return;
        }
        if (cached == null || cached.trim().isEmpty()) {
            return;
        }
        for (String path : cached.split(",")) {
            addIfAbsent(result, path);
        }
    }

    /**
     * 去除首尾空白后加入列表（已存在或空白则跳过）。
     *
     * @param result 目标列表
     * @param path   待加入路径
     */
    private void addIfAbsent(List<String> result, String path) {
        if (path == null) {
            return;
        }
        String trimmed = path.trim();
        if (!trimmed.isEmpty() && !result.contains(trimmed)) {
            result.add(trimmed);
        }
    }
}
