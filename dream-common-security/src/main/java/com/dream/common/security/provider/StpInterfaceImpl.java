package com.dream.common.security.provider;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.dream.common.core.constant.Security;
import com.dream.common.core.vo.sys.RoleVo;
import com.dream.common.core.vo.sys.UserProfileVo;
import com.dream.common.framework.cache.CachesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色数据提供者。
 * <p>
 * 从 {@link CachesUtils} 读取 {@link UserProfileVo}（key =
 * {@link Security#CACHE_PREFIX_USER} + userId），提取权限标识列表与角色编码列表。
 * </p>
 * <p>
 * Sa-Token 在调用 {@link StpUtil#checkPermission(String)} /
 * {@link StpUtil#checkRole(String)} 时回调本实现，因此读缓存而非读库，
 * 避免每次鉴权触发数据库查询；缓存由 {@link com.dream.common.security.utils.SecurityUtils#login}
 * 在登录时写入。
 * </p>
 * <p>
 * 由 {@link com.dream.common.security.config.SecurityAutoConfiguration} 通过 {@code @Import}
 * 注册，不标注 {@code @Component}，避免与组件扫描形成双重注册。
 * </p>
 *
 * @author dream
 * @since 2026-09-03
 */
@Slf4j
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpInterface")
@ConditionalOnMissingBean(StpInterface.class)
@ConditionalOnProperty(prefix = "dream.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回一个账号所拥有的权限码集合。
     *
     * @param loginId   账号ID
     * @param loginType 账号类型
     * @return 权限码列表，无数据返回空列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        UserProfileVo loginUser = loadLoginUser(loginId.toString());
        if (loginUser == null) {
            return Collections.emptyList();
        }
        List<String> permissions = loginUser.getPermissions();
        return permissions == null ? Collections.emptyList() : permissions;
    }

    /**
     * 返回一个账号所拥有的角色标识集合（权限与角色可分开校验）。
     *
     * @param loginId   账号ID
     * @param loginType 账号类型
     * @return 角色编码列表，无数据返回空列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        UserProfileVo loginUser = loadLoginUser(loginId.toString());
        if (loginUser == null) {
            return Collections.emptyList();
        }
        List<RoleVo> roles = loginUser.getRoles();
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleCodes = new ArrayList<>(roles.size());
        for (RoleVo role : roles) {
            if (role != null && role.getRoleCode() != null) {
                roleCodes.add(role.getRoleCode());
            }
        }
        return roleCodes;
    }

    /**
     * 从缓存加载用户档案。
     * <p>
     * 缓存未命中时交给 {@link #onCacheMiss(String)} 处理；缓存读取异常时返回 {@code null}，
     * 由调用方降级为空权限/角色列表（即鉴权不通过），不阻断鉴权流程。
     * </p>
     *
     * @param userId 用户ID
     * @return UserProfileVo，缓存不存在或异常返回 {@code null}
     */
    private UserProfileVo loadLoginUser(String userId) {
        try {
            UserProfileVo profile = CachesUtils.get(Security.CACHE_PREFIX_USER + userId);
            if (profile == null) {
                return onCacheMiss(userId);
            }
            return profile;
        } catch (Exception e) {
            log.warn("[StpInterface] 从缓存加载用户失败 | userId={} | err={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 用户档案缓存缺失时的回源钩子，默认返回 {@code null}。
     * <p>
     * 默认实现下，缓存过期或被清理后，token 仍有效的用户会因拿不到权限/角色而全部鉴权失败。
     * 需要自动恢复的业务可提供自定义 {@code StpInterface} Bean 继承本类并覆写本方法，
     * 从数据库重建用户档案并回写缓存（本类带 {@code @ConditionalOnMissingBean}，自定义 Bean 优先）。
     * 注意本方法在每次鉴权回调中都可能被触发，回源实现必须自行控制开销。
     * </p>
     *
     * @param userId 用户ID
     * @return 重建后的用户档案；返回 {@code null} 表示按空权限/角色处理
     */
    protected UserProfileVo onCacheMiss(String userId) {
        log.warn("[StpInterface] 用户档案缓存缺失，本次鉴权按空权限/角色处理 | userId={}", userId);
        return null;
    }
}
