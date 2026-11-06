package com.dream.common.security.utils;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.dream.common.core.constant.Security;
import com.dream.common.core.vo.sys.UserProfileVo;
import com.dream.common.framework.cache.CachesUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 安全工具类，基于 Sa-Token 封装常用的登录、权限、角色、会话操作。
 * <p>
 * 登录用户档案 {@link UserProfileVo} 缓存于 {@link CachesUtils}，
 * key = {@link Security#CACHE_PREFIX_USER} + userId（Redis 优先，不可用降级 Caffeine）。
 * Sa-Token 会话（token → loginId 映射）由 StpUtil 自行维护。
 * </p>
 * <p>
 * 有效期以 sa-token 为准：登录时缓存 TTL 与 token TTL 一致（全局配置
 * {@code sa-token.timeout} 或 {@link #login(UserProfileVo, long)} 显式值），
 * 懒加载/刷新回填时取当前 token 剩余时间，登出/踢人时同步删除缓存。
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

    /** 会话中存取用户名的 key */
    private static final String SESSION_KEY_USERNAME = "username";

    /** 会话中存取租户ID的 key */
    private static final String SESSION_KEY_TENANT_ID = "tenantId";

    /** 会话中存取数据范围的 key */
    private static final String SESSION_KEY_DATA_SCOPE = "dataScope";

    /** 会话中存取机构子部门ID列表的 key */
    private static final String SESSION_KEY_ORG_CHILD_DEPT_IDS = "orgChildDeptIds";

    /** 会话中存取本部门子部门ID列表的 key */
    private static final String SESSION_KEY_DEPT_CHILD_IDS = "childDeptIds";

    /** 会话中存取所属机构ID的 key */
    private static final String SESSION_KEY_ORG_ID = "orgId";

    private SecurityUtils() {
    }

    // ==================== 登录 / 登出 ====================

    /**
     * 登录用户，token 与用户档案缓存有效期均取 sa-token 全局配置（{@code sa-token.timeout}，单位秒）。
     *
     * @param loginUser 登录用户信息，必须包含 userId
     * @throws IllegalArgumentException 当 loginUser 或 userId 为 null 时抛出
     */
    public static void login(UserProfileVo loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new IllegalArgumentException("登录用户信息或 userId 不能为空");
        }

        // 不传 SaLoginParameter，token 有效期直接采用 sa-token 全局配置
        StpUtil.login(loginUser.getUserId());
        syncLoginContextToSession(loginUser);
        cacheLoginUser(loginUser, SaManager.getConfig().getTimeout());
        log.info("用户登录成功 | userId={} | username={} | timeout={}s(全局配置)",
                loginUser.getUserId(), loginUser.getUsername(), SaManager.getConfig().getTimeout());
    }

    /**
     * 登录用户并显式指定有效期，token 与用户档案缓存同 TTL（缓存随 token 过期自动失效）。
     *
     * @param loginUser 登录用户信息，必须包含 userId
     * @param timeout   令牌有效期（秒），须为正数
     * @throws IllegalArgumentException 当 loginUser / userId 为 null 或 timeout 非正数时抛出
     */
    public static void login(UserProfileVo loginUser, long timeout) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new IllegalArgumentException("登录用户信息或 userId 不能为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("令牌有效期必须为正数（秒），实际=" + timeout);
        }

        StpUtil.login(loginUser.getUserId(), new SaLoginParameter().setTimeout(timeout));
        syncLoginContextToSession(loginUser);
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

    // ==================== 静默访问（无 SaTokenContext 线程安全） ====================

    /**
     * 静默判断登录状态：SaTokenContext 未初始化（定时任务 / 异步线程 / 启动期）时返回
     * {@code false} 而非抛出异常。
     *
     * @return true 已登录
     */
    public static boolean isLoginQuietly() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 静默获取当前用户ID：未登录或 SaTokenContext 未初始化时返回 {@code null}。
     *
     * @return 用户ID字符串，可能为 {@code null}
     */
    public static String getUserIdQuietly() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 静默获取当前登录用户名：未登录或 SaTokenContext 未初始化时返回 {@code null}。
     * <p>取值来源为 Sa Session 中登录时写入的 {@code username}，不依赖用户档案缓存。</p>
     *
     * @return 登录账号，可能为 {@code null}
     */
    public static String getUsernameQuietly() {
        try {
            return StpUtil.isLogin() ? getUsername() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 静默获取当前登录用户完整档案：未登录、缓存不存在或 SaTokenContext 未初始化时返回 {@code null}。
     *
     * @return UserProfileVo，可能为 {@code null}
     */
    public static UserProfileVo getLoginUserQuietly() {
        try {
            return getLoginUser();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 登录上下文（token 级，存 Sa Session） ====================

    /**
     * 获取当前会话的租户ID。
     * <p>token 级数据（多端独立），登录时由 {@link #login} 从用户档案同步写入；
     * 未登录或无上下文时返回 {@code null}，调用方自行兜底。</p>
     *
     * @return 租户ID，可能为 {@code null}
     */
    public static String getTenantId() {
        return getSessionAttributeQuietly(SESSION_KEY_TENANT_ID);
    }

    /**
     * 获取当前会话的数据范围（多角色合并结果，英文字面量如 ALL/DEPT/SELF）。
     *
     * @return 数据范围字面量，未登录或无上下文时返回 {@code null}
     */
    public static String getDataScope() {
        return getSessionAttributeQuietly(SESSION_KEY_DATA_SCOPE);
    }

    /**
     * 获取当前会话的所属机构子部门ID列表（登录时预计算）。
     *
     * @return 子部门ID列表，未登录或无上下文时返回空列表（不为 {@code null}）
     */
    @SuppressWarnings("unchecked")
    public static List<String> getOrgChildDeptIds() {
        Object value = getSessionAttributeQuietly(SESSION_KEY_ORG_CHILD_DEPT_IDS);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    /**
     * 获取当前会话的本部门子部门ID列表（登录时预计算，供 DEPT_AND_CHILD 数据范围使用）。
     *
     * @return 子部门ID列表，未登录或无上下文时返回空列表（不为 {@code null}）
     */
    @SuppressWarnings("unchecked")
    public static List<String> getChildDeptIds() {
        Object value = getSessionAttributeQuietly(SESSION_KEY_DEPT_CHILD_IDS);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    /**
     * 获取当前会话的所属机构ID（沿部门 ancestors 向上最近的机构节点）。
     *
     * @return 机构ID，未登录、无部门归属或无上下文时返回 {@code null}
     */
    public static String getOrgId() {
        return getSessionAttributeQuietly(SESSION_KEY_ORG_ID);
    }

    /**
     * 切换当前会话的租户（仅写 Sa Session，token 级，不影响其他端）。
     * <p>调用方需同步更新线程上下文（如 TenantContextHolder）；
     * 用户档案缓存中的归属租户不变。</p>
     *
     * @param tenantId 目标租户ID
     */
    public static void switchTenant(String tenantId) {
        if (tenantId != null && isLoginQuietly()) {
            StpUtil.getSession().set(SESSION_KEY_TENANT_ID, tenantId);
        }
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
     * 手动刷新当前用户的缓存档案，并同步刷新 Sa Session 中的登录上下文
     * （仅当刷新的档案属于当前登录用户时）。
     *
     * @param loginUser 最新的用户信息
     */
    public static void refreshLoginUser(UserProfileVo loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            return;
        }
        cacheLoginUser(loginUser);
        if (loginUser.getUserId().equals(getUserIdQuietly())) {
            syncLoginContextToSession(loginUser);
        }
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
     * 判断当前用户是否为超级管理员（拥有 {@link Security#ROLE_ADMIN} 角色）。
     * <p>
     * 管理员判定只依赖角色数据（可由权限系统动态调整），
     * 不设置任何基于 userId 的硬编码后门。
     * </p>
     *
     * @return true 是管理员
     */
    public static boolean isAdmin() {
        return StpUtil.isLogin() && StpUtil.hasRole(Security.ROLE_ADMIN);
    }

    // ==================== 内部方法 ====================

    /**
     * 登录 / 刷新时将用户档案中的上下文数据同步写入 Sa Session（token 级）。
     * <p>username / tenantId / dataScope / orgId 为 {@code null} 时跳过，避免覆盖已有会话数据；
     * orgChildDeptIds / deptChildIds 始终写入（getter 空安全），确保刷新后能清除旧值。</p>
     *
     * @param loginUser 登录用户信息
     */
    private static void syncLoginContextToSession(UserProfileVo loginUser) {
        if (loginUser.getUsername() != null) {
            StpUtil.getSession().set(SESSION_KEY_USERNAME, loginUser.getUsername());
        }
        if (loginUser.getTenantId() != null) {
            StpUtil.getSession().set(SESSION_KEY_TENANT_ID, loginUser.getTenantId());
        }
        if (loginUser.getDataScope() != null) {
            StpUtil.getSession().set(SESSION_KEY_DATA_SCOPE, loginUser.getDataScope());
        }
        StpUtil.getSession().set(SESSION_KEY_ORG_CHILD_DEPT_IDS, loginUser.getOrgChildDeptIds());
        StpUtil.getSession().set(SESSION_KEY_DEPT_CHILD_IDS, loginUser.getDeptChildIds());
        if (loginUser.getOrg() != null && loginUser.getOrg().getId() != null) {
            StpUtil.getSession().set(SESSION_KEY_ORG_ID, loginUser.getOrg().getId());
        }
    }

    /**
     * 静默读取当前会话属性：未登录或 SaTokenContext 未初始化时返回 {@code null}。
     *
     * @param key 属性键
     * @param <V> 属性类型
     * @return 属性值，可能为 {@code null}
     */
    @SuppressWarnings("unchecked")
    private static <V> V getSessionAttributeQuietly(String key) {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return (V) StpUtil.getSession().get(key);
        } catch (Exception e) {
            return null;
        }
    }

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
     * 以当前 token 剩余有效期缓存登录用户档案（缓存与 token 同步过期）。
     *
     * @param loginUser 登录用户信息
     */
    private static void cacheLoginUser(UserProfileVo loginUser) {
        cacheLoginUser(loginUser, currentTokenTimeout());
    }

    /**
     * 获取当前 token 剩余有效期（秒）。
     * <p>
     * 有效期以 sa-token 为准：token 剩余时间取 {@code StpUtil.getTokenTimeout()}
     * （-1 永不过期 / -2 未获取到）时，回退 sa-token 全局配置 {@code sa-token.timeout}；
     * 两者均非正数（永不过期场景）时返回 {@link Integer#MAX_VALUE} 秒作为近似永久 TTL。
     * </p>
     *
     * @return 缓存有效期（秒），恒为正数
     */
    private static long currentTokenTimeout() {
        long timeout = StpUtil.getTokenTimeout();
        if (timeout <= 0) {
            timeout = SaManager.getConfig().getTimeout();
        }
        return timeout > 0 ? timeout : Integer.MAX_VALUE;
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
