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

    // ==================== 缓存 Key 前缀 ====================
    public static final String CACHE_PREFIX_TOKEN = "token:";
    public static final String CACHE_PREFIX_USER = "user:";
    public static final String CACHE_PREFIX_ROLE = "role:";
    public static final String CACHE_PREFIX_PERM = "perm:";
    public static final String CACHE_PREFIX_PERMISSION = "permission:";
    public static final String CACHE_PREFIX_MENU = "menu:";
    public static final String CACHE_PREFIX_DICT = "dict:";
    public static final String CACHE_PREFIX_CONFIG = "config:";
    public static final String CACHE_PREFIX_CAPTCHA = "captcha:";
    public static final String CACHE_PREFIX_RATE_LIMIT = "rate_limit:";
    public static final String CACHE_PREFIX_LOCK = "lock:";
    public static final String CACHE_PREFIX_FAIL = "login_fail:";
    public static final String CACHE_PREFIX_BLACKLIST = "blacklist:";

    /**
     * 令牌秘钥
     */
    public final static String SECRET = "kQw@IOrY#vnXmS%D^kw&E*i(Fn)g{r.KidMcdrg!Kor";

}