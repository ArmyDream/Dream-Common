package com.dream.common.core.vo.sys;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * 登录结果 VO。
 *
 * <p>登录成功或刷新令牌时返回，包含令牌和本次会话的上下文信息
 * （登录方式、登录平台、客户端标识等）。<b>不含用户属性</b>——
 * 用户档案通过 {@code GET /user/profile} 独立获取。</p>
 *
 * <h2>字段分组</h2>
 * <ul>
 *   <li><b>令牌</b>：accessToken / refreshToken / tokenType</li>
 *   <li><b>有效期</b>：expiresIn / expiresAt / refreshExpiresIn / issuedAt</li>
 *   <li><b>会话上下文</b>：loginType / loginPlatform / clientId / loginTime</li>
 *   <li><b>扩展</b>：remark / extra</li>
 * </ul>
 *
 * <h2>响应示例</h2>
 * <pre>{@code
 * {
 *   "accessToken": "eyJhbGc...",
 *   "refreshToken": "eyJhbGc...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 1800,
 *   "expiresAt": 1735689600,
 *   "refreshExpiresIn": 604800,
 *   "issuedAt": 1735687800,
 *   "loginType": "PASSWORD",
 *   "loginPlatform": "PC",
 *   "clientId": "web-admin",
 *   "loginTime": 1735687800000
 * }
 * }</pre>
 *
 * <h2>字段语义</h2>
 * <ul>
 *   <li>{@code expiresIn} —— 有效期<b>时长</b>（秒）</li>
 *   <li>{@code expiresAt} —— 到期<b>时刻</b>（时间戳，秒）</li>
 *   <li>{@code issuedAt} —— 签发<b>时刻</b>（时间戳，秒）</li>
 *   <li>{@code loginTime} —— 登录<b>时刻</b>（时间戳，毫秒）</li>
 * </ul>
 *
 * @author dream
 * @since 2026-09-10
 */
public class LoginResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 令牌 ====================

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌类型（默认 {@code Bearer}）
     */
    private String tokenType;

    // ==================== 有效期 ====================

    /**
     * 访问令牌有效期（秒），-1 表示永久
     */
    private Long expiresIn;

    /**
     * 访问令牌到期时刻（时间戳，秒）
     */
    private Long expiresAt;

    /**
     * 刷新令牌有效期（秒），-1 表示永久
     */
    private Long refreshExpiresIn;

    /**
     * 刷新令牌到期时刻（时间戳，秒）
     */
    private Long refreshExpiresAt;

    /**
     * 令牌签发时刻（时间戳，秒，对应 OIDC {@code iat}）
     */
    private Long issuedAt;

    // ==================== 会话上下文 ====================

    /**
     * 登录方式。
     *
     * <p>取值来源：{@link com.dream.common.core.enums.LoginType#getValue()}，
     * 如 {@code PASSWORD}（账号密码）/ {@code SMS}（短信）/ {@code QR_CODE}（扫码）
     * / {@code SSO}（单点登录）/ {@code OAUTH}（第三方）。</p>
     *
     * @see com.dream.common.core.enums.LoginType
     */
    private String loginType;

    /**
     * 登录平台。
     *
     * <p>取值来源：{@link com.dream.common.core.enums.Platform#getValue()}，
     * 如 {@code PC} / {@code APP} / {@code MINI}（小程序）/ {@code H5} / {@code PAD}。</p>
     *
     * @see com.dream.common.core.enums.Platform
     */
    private String loginPlatform;

    /**
     * 客户端应用标识。
     *
     * <p>微服务多应用场景下区分来源，如 {@code web-admin} / {@code mobile-app}。</p>
     */
    private String clientId;

    /**
     * 登录时刻（时间戳，毫秒，对应 OIDC {@code auth_time}）
     */
    private Long loginTime;

    // ==================== 扩展 ====================

    /**
     * 备注
     */
    private String remark;

    /**
     * 业务扩展属性
     */
    private Map<String, Object> extra;

    // ==================== Getter / Setter ====================

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 获取令牌类型，默认返回 {@code Bearer}。
     *
     * @return 令牌类型，永不为 {@code null}
     */
    public String getTokenType() {
        return (tokenType == null || tokenType.isEmpty()) ? "Bearer" : tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * 获取到期时刻。
     *
     * <p>若显式设置了 {@code expiresAt} 则直接返回；
     * 否则基于 {@code issuedAt + expiresIn} 计算。</p>
     *
     * <p><b>注意</b>：本字段为派生字段，序列化时会输出计算结果，
     * 反序列化时会被 JSON 中的值覆盖（若存在）。</p>
     *
     * @return 到期时间戳（秒）；无法计算时返回 {@code null}
     */
    public Long getExpiresAt() {
        if (expiresAt != null) {
            return expiresAt;
        }
        return computeExpiresAt();
    }

    /**
     * 计算到期时刻（不修改字段）。
     *
     * @return 计算结果
     */
    private Long computeExpiresAt() {
        if (issuedAt != null && expiresIn != null && expiresIn > 0) {
            return issuedAt + expiresIn;
        }
        return null;
    }

    /**
     * 设置到期时刻。
     *
     * <p>若传入 {@code null}，则依赖 {@link #computeExpiresAt()} 动态计算。</p>
     */
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(Long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public Long getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public void setRefreshExpiresAt(Long refreshExpiresAt) {
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getLoginPlatform() {
        return loginPlatform;
    }

    public void setLoginPlatform(String loginPlatform) {
        this.loginPlatform = loginPlatform;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取扩展属性（空安全）。
     *
     * @return 扩展属性 Map，永不为 {@code null}
     */
    public Map<String, Object> getExtra() {
        return extra == null ? Collections.emptyMap() : extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}