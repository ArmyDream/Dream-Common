package com.dream.common.security.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录用户信息
 * <p>
 * 封装当前登录用户的完整上下文（基础信息、机构/部门、角色、权限），
 * 登录成功后通过 {@code SecurityUtils.login()} 写入 Sa-Token 会话并缓存到 CachesUtils。
 * </p>
 *
 * <h2>缓存策略</h2>
 * <p>
 * 缓存 Key：{@code "user:" + userId}（{@link com.dream.common.core.constant.Security#CACHE_PREFIX_USER}）<br>
 * 缓存过期：与 Sa-Token 令牌有效期同步，默认 30 分钟
 * </p>
 *
 * @author dream
 * @since 2026-08-24
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID（Sa-Token loginId）
     */
    private String userId;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 昵称/姓名
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别（0 未知 / 1 男 / 2 女）
     */
    private String gender;

    /**
     * 机构信息
     */
    private OrgInfo org;

    /**
     * 部门信息
     */
    private DeptInfo dept;

    /**
     * 角色列表
     */
    private List<RoleInfo> roles = new ArrayList<>();

    /**
     * 权限标识列表（如 "system:user:list"）
     */
    private List<String> permissions = new ArrayList<>();

    /**
     * 登录时间（时间戳毫秒）
     */
    private Long loginTime;

    /**
     * 登录 IP
     */
    private String loginIp;

    /**
     * 登录地点（IP 解析）
     */
    private String loginLocation;

    /**
     * 令牌过期时间（时间戳毫秒）
     */
    private Long expireTime;

    /**
     * 扩展属性（业务自定义数据）
     */
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 设置扩展属性
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取扩展属性
     *
     * @param key 属性键
     * @param <V> 属性值类型
     * @return 属性值；不存在返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <V> V getAttribute(String key) {
        return (V) attributes.get(key);
    }

    /**
     * 获取角色编码列表
     *
     * @return 角色编码集合
     */
    public List<String> getRoleCodes() {
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> codes = new ArrayList<>(roles.size());
        for (RoleInfo role : roles) {
            if (role.getRoleCode() != null) {
                codes.add(role.getRoleCode());
            }
        }
        return codes;
    }

    /**
     * 获取机构 ID
     *
     * @return 机构 ID；无机构返回 {@code null}
     */
    public String getOrgId() {
        return org != null ? org.getOrgId() : null;
    }

    /**
     * 获取部门 ID
     *
     * @return 部门 ID；无部门返回 {@code null}
     */
    public String getDeptId() {
        return dept != null ? dept.getDeptId() : null;
    }
}
