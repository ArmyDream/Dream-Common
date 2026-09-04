package com.dream.common.security.provider;

import cn.dev33.satoken.stp.StpInterface;
import com.dream.common.core.constant.Security;
import com.dream.common.framework.cache.CachesUtils;
import com.dream.common.security.vo.LoginUser;
import com.dream.common.security.vo.RoleInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据提供者。
 * <p>从 {@link CachesUtils} 缓存中读取 {@link LoginUser}，提取权限和角色列表。</p>
 *
 * @author dream
 * @since 2026-09-03
 */
@Slf4j
@Component
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpInterface")
@ConditionalOnMissingBean(StpInterface.class)
@ConditionalOnProperty(prefix = "dream.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        LoginUser loginUser = loadLoginUser(loginId.toString());
        if (loginUser == null || loginUser.getPermissions() == null) {
            return Collections.emptyList();
        }
        return loginUser.getPermissions();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        LoginUser loginUser = loadLoginUser(loginId.toString());
        if (loginUser == null || loginUser.getRoles() == null) {
            return Collections.emptyList();
        }
        List<RoleInfo> roles = loginUser.getRoles();
        List<String> roleCodes = new ArrayList<>(roles.size());
        for (RoleInfo role : roles) {
            if (role.getRoleCode() != null) {
                roleCodes.add(role.getRoleCode());
            }
        }
        return roleCodes;
    }

    /**
     * 从缓存加载用户信息。
     *
     * @param userId 用户ID
     * @return LoginUser，缓存不存在时返回 {@code null}
     */
    private LoginUser loadLoginUser(String userId) {
        try {
            return CachesUtils.get(Security.CACHE_PREFIX_USER + userId);
        } catch (Exception e) {
            log.warn("[StpInterface] 从缓存加载用户失败 | userId={} | err={}", userId, e.getMessage());
            return null;
        }
    }
}