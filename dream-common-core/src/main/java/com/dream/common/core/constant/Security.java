package com.dream.common.core.constant;

/**
 * 安全/权限常量
 *
 * @author dream
 */
public final class Security {
    private Security() {
    }

    // ==================== Token ====================
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_KEY = "token";
    public static final String TOKEN_REFRESH_KEY = "refreshToken";

    // ==================== 认证方案 ====================
    public static final String AUTH_BEARER = "Bearer ";
    public static final String AUTH_BASIC = "Basic";
    public static final String AUTH_DIGEST = "Digest";

    // ==================== 角色 ====================
    /**
     * 超级管理员角色编码。
     * <p>
     * 拥有此角色的用户视为超级管理员（如 {@code SecurityUtils.isAdmin()}），
     * 管理员判定不设任何基于 userId 的硬编码后门。
     * </p>
     */
    public static final String ROLE_ADMIN = "admin";

    // ==================== 缓存 Key 前缀 ====================
    public static final String CACHE_PREFIX_TOKEN = "dream:security:token:";
    public static final String CACHE_PREFIX_SESSION = "dream:security:session:";
    public static final String CACHE_PREFIX_USER = "dream:security:user:";
    public static final String CACHE_PREFIX_ROLE = "dream:security:role:";
    public static final String CACHE_PREFIX_PERM = "dream:security:perm:";
    /** 在线用户列表（用于踢人、监控） */
    public static final String CACHE_PREFIX_ONLINE = "dream:security:online:";
    public static final String CACHE_PREFIX_MENU = "dream:sys:menu:";
    public static final String CACHE_PREFIX_DICT = "dream:sys:dict:";
    public static final String CACHE_PREFIX_CONFIG = "dream:sys:config:";
    public static final String CACHE_PREFIX_CAPTCHA = "captcha:";
    public static final String CACHE_PREFIX_RATE_LIMIT = "rate_limit:";
    public static final String CACHE_PREFIX_LOCK = "lock:";
    public static final String CACHE_PREFIX_FAIL = "login_fail:";
    public static final String CACHE_PREFIX_BLACKLIST = "blacklist:";

    /**
     * 路由白名单路径缓存 key。
     * <p>
     * 业务系统可通过 {@code CachesUtils.set(CACHE_PREFIX_EXCLUDE_PATHS, "/api/**,/login")}
     * 动态注入白名单路径，缓存中多个路径以逗号隔开。
     * 每次请求与 {@code dream.security.filter.exclude-paths} 配置项实时合并（支持热更新）。
     * </p>
     */
    public static final String CACHE_PREFIX_EXCLUDE_PATHS = "dream:security:exclude-paths:";

    /**
     * 令牌秘钥
     */
    public final static String SECRET = "kQw@IOrY#vnXmS%D^kw&E*i(Fn)g{r.KidMcdrg!Kor";

}