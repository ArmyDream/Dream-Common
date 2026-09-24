package com.dream.common.security.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.dream.common.core.constant.Security;
import com.dream.common.core.vo.sys.UserProfileVo;
import com.dream.common.framework.cache.CachesUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 安全工具类，基于 Sa-Token 封装常用的登录、权限、角色、会话操作。
 * <p>
 * 登录用户档案 {@link UserProfileVo} 缓存于 {@link CachesUtils}，
 * key = {@link Security#CACHE_PREFIX_USER} + userId（Redis 优先，不可用降级 Caffeine）。
 * Sa-Token 会话（token → loginId 映射）由 StpUtil 自行维护。
 * </p>
 *
 * <h2>职责划分</h2>
 * <ul>
 *   <li>会话生命周期：{@link #login} / {@link #logout} / {@link #logout(String)}</li>
 *   <li>当前用户读取：{@link #getUserId} / {@link #getUsername} / {@link #getLoginUser()} / {@link #getLoginUser(Supplier)}</li>
 *   <li>角色 / 权限查询：{@link #hasRole} / {@link #hasPermission} 等</li>
 *   <li>编程式校验（抛异常）：{@link #requireLogin} / {@link #requirePermission} / {@link #requireRole}</li>
 *   <li>Token 与会话属性：{@link #getTokenValue} / {@link #setSessionAttribute} 等</li>
 * </ul>
 *
 * @author dream
 * @since 2026-08-24
 */
@Slf4j
public final class SecurityUtils {

    /** 登录用户档案缓存默认有效期（秒），与 Sa-Token token 超时保持一致 */
    private static final long LOGIN_USER_CACHE_EXPIRE_SECONDS = 1800L;

    /** 会话中存取用户名的 key */
    private static final String SESSION_KEY_USERNAME = "username";

    private SecurityUtils() {
    }

    // ==================== 登录 / 登出 ====================

    /**
     * 登录用户（默认 30 分钟有效期）。
     *
     * @param loginUser 登录用户信息，必须包含 userId
     * @throws IllegalArgumentException 当 loginUser 或 userId 为 null 时抛出
     */
    public static void login(UserProfileVo loginUser) {
        login(loginUser, LOGIN_USER_CACHE_EXPIRE_SECONDS);
    }

    /**
     * 登录用户并指定有效期。
     *
     * @param loginUser 登录用户信息
     * @param timeout   令牌有效期（秒）
     * @throws IllegalArgumentException 当 loginUser 或 userId 为 null 时抛出
     */
    public static void login(UserProfileVo loginUser, long timeout) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new IllegalArgumentException("登录用户信息或 userId 不能为空");
        }

        StpUtil.login(loginUser.getUserId(), new SaLoginParameter().setTimeout(timeout));
        StpUtil.getSession().set(SESSION_KEY_USERNAME, loginUser.getUsername());
        cacheLoginUser(loginUser, timeout);
        log.info("用户登录成功 | userId={} | username={} | timeout={}s",
                loginUser.getUserId(), loginUser.getUsername(), timeout);
    }

    /**
     * 登出当前用户。
     */
    public static void logout() {
        if (!StpUtil.isLogin()) {
            return;
        }
        String userId = getUserId();
        String username = getUsername();
        StpUtil.logout();
        removeLoginUserFromCache(userId);
        log.info("用户登出 | userId={} | username={}", userId, username);
    }

    /**
     * 强制登出指定用户（踢人）。
     *
     * @param userId 用户ID
     */
    public static void logout(String userId) {
        if (userId == null) {
            return;
        }
        StpUtil.logout(userId);
        removeLoginUserFromCache(userId);
        log.info("强制用户登出 | userId={}", userId);
    }

    // ==================== 当前用户信息 ====================

    /**
     * 判断当前请求是否已登录。
     *
     * @return true 已登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 获取当前用户ID。
     *
     * @return 用户ID字符串
     * @throws cn.dev33.satoken.exception.NotLoginException 未登录时抛出
     */
    public static String getUserId() {
        return StpUtil.getLoginIdAsString();
    }

    /**
     * 获取当前用户名（从会话中读取）。
     *
     * @return 用户名，未登录返回 {@code null}
     */
    public static String getUsername() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        Object username = StpUtil.getSession().get(SESSION_KEY_USERNAME);
        return username != null ? username.toString() : null;
    }

    /**
     * 从缓存获取当前登录用户完整档案。
     *
     * @return UserProfileVo，未登录或缓存不存在返回 {@code null}
     */
    public static UserProfileVo getLoginUser() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        String userId = StpUtil.getLoginIdAsString();
        return CachesUtils.get(buildUserCacheKey(userId));
    }

    /**
     * 获取当前登录用户信息，缓存未命中时通过加载器从数据库加载并回填缓存。
     * <p>
     * 加载器返回 {@code null} 时视为用户不存在，不回填缓存以避免缓存穿透。
     * </p>
     *
     * @param loader 数据加载器，返回 UserProfileVo（可为 null）
     * @return 用户信息，未登录或加载失败返回 {@code null}
     */
    public static UserProfileVo getLoginUser(Supplier<UserProfileVo> loader) {
        if (!StpUtil.isLogin() || loader == null) {
            return null;
        }
        String userId = StpUtil.getLoginIdAsString();
        String cacheKey = buildUserCacheKey(userId);
        UserProfileVo cached = CachesUtils.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        UserProfileVo user = loader.get();
        if (user != null) {
            cacheLoginUser(user);
            log.debug("懒加载登录用户并回填缓存 | userId={}", userId);
        } else {
            log.warn("懒加载登录用户返回 null，不回填缓存 | userId={}", userId);
        }
        return user;
    }

    /**
     * 手动刷新当前用户的缓存档案。
     *
     * @param loginUser 最新的用户信息
     */
    public static void refreshLoginUser(UserProfileVo loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            return;
        }
        cacheLoginUser(loginUser);
        log.debug("刷新登录用户缓存 | userId={}", loginUser.getUserId());
    }

    // ==================== 角色校验 ====================

    /**
     * 检查是否拥有指定角色。
     *
     * @param role 角色编码
     * @return true 拥有
     */
    public static boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    /**
     * 检查是否拥有任意一个角色。
     *
     * @param roles 角色编码列表
     * @return true 拥有至少一个
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (StpUtil.hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否拥有全部角色。
     *
     * @param roles 角色编码列表
     * @return true 拥有全部
     */
    public static boolean hasAllRoles(String... roles) {
        for (String role : roles) {
            if (!StpUtil.hasRole(role)) {
                return false;
            }
        }
        return true;
    }

    // ==================== 权限校验 ====================

    /**
     * 检查是否拥有指定权限。
     *
     * @param permission 权限标识
     * @return true 拥有
     */
    public static boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }

    /**
     * 检查是否拥有任意一个权限。
     *
     * @param permissions 权限标识列表
     * @return true 拥有至少一个
     */
    public static boolean hasAnyPermission(String... permissions) {
        for (String perm : permissions) {
            if (StpUtil.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否拥有全部权限。
     *
     * @param permissions 权限标识列表
     * @return true 拥有全部
     */
    public static boolean hasAllPermissions(String... permissions) {
        for (String perm : permissions) {
            if (!StpUtil.hasPermission(perm)) {
                return false;
            }
        }
        return true;
    }

    // ==================== 编程式校验（抛异常） ====================

    /**
     * 强制校验登录状态（未登录抛出 {@code NotLoginException}）。
     */
    public static void requireLogin() {
        StpUtil.checkLogin();
    }

    /**
     * 强制校验权限（无权限抛出 {@code NotPermissionException}）。
     *
     * @param permission 权限标识
     */
    public static void requirePermission(String permission) {
        StpUtil.checkPermission(permission);
    }

    /**
     * 强制校验角色（无角色抛出 {@code NotRoleException}）。
     *
     * @param role 角色编码
     */
    public static void requireRole(String role) {
        StpUtil.checkRole(role);
    }

    // ==================== Token 信息 ====================

    /**
     * 获取当前 Token 值。
     *
     * @return Token 字符串，未登录返回 {@code null}
     */
    public static String getTokenValue() {
        return StpUtil.isLogin() ? StpUtil.getTokenValue() : null;
    }

    /**
     * 获取 Token 剩余有效期（秒）。
     *
     * @return 剩余秒数，-2 表示未获取到 Token
     */
    public static long getTokenTimeout() {
        return StpUtil.getTokenTimeout();
    }

    /**
     * 获取 Token 名称（请求头/参数名）。
     *
     * @return Token 名称
     */
    public static String getTokenName() {
        return StpUtil.getTokenName();
    }

    // ==================== 会话属性 ====================

    /**
     * 设置当前会话的属性。
     *
     * @param key   属性键
     * @param value 属性值
     */
    public static void setSessionAttribute(String key, Object value) {
        StpUtil.getSession().set(key, value);
    }

    /**
     * 获取当前会话的属性。
     *
     * @param key 属性键
     * @param <V> 属性类型
     * @return 属性值，不存在返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <V> V getSessionAttribute(String key) {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return (V) StpUtil.getSession().get(key);
    }

    /**
     * 移除当前会话的某个属性。
     *
     * @param key 属性键
     */
    public static void removeSessionAttribute(String key) {
        if (!StpUtil.isLogin()) {
            return;
        }
        StpUtil.getSession().delete(key);
    }

    // ==================== 便捷方法 ====================

    /**
     * 判断当前用户是否为超级管理员（角色包含 "admin" 或 userId 为 "1"）。
     *
     * @return true 是管理员
     */
    public static boolean isAdmin() {
        return StpUtil.isLogin() && (StpUtil.hasRole("admin") || "1".equals(StpUtil.getLoginIdAsString()));
    }

    // ==================== 内部方法 ====================

    /**
     * 构造登录用户档案缓存 key。
     *
     * @param userId 用户ID
     * @return 缓存 key
     */
    private static String buildUserCacheKey(String userId) {
        return Security.CACHE_PREFIX_USER + userId;
    }

    /**
     * 以默认有效期缓存登录用户档案。
     *
     * @param loginUser 登录用户信息
     */
    private static void cacheLoginUser(UserProfileVo loginUser) {
        cacheLoginUser(loginUser, LOGIN_USER_CACHE_EXPIRE_SECONDS);
    }

    /**
     * 以指定有效期缓存登录用户档案。
     *
     * @param loginUser 登录用户信息
     * @param expire    有效期（秒）
     */
    private static void cacheLoginUser(UserProfileVo loginUser, long expire) {
        String cacheKey = buildUserCacheKey(loginUser.getUserId());
        CachesUtils.set(cacheKey, loginUser, expire, TimeUnit.SECONDS);
    }

    /**
     * 从缓存移除登录用户档案。
     *
     * @param userId 用户ID
     */
    private static void removeLoginUserFromCache(String userId) {
        if (userId != null) {
            CachesUtils.delete(buildUserCacheKey(userId));
        }
    }
}
