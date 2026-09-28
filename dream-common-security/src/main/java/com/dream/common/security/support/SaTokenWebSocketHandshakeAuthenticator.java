package com.dream.common.security.support;

import com.dream.common.framework.websocket.api.WebSocketHandshakeAuthenticator;
import com.dream.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;

/**
 * 基于 sa-token 的 WebSocket 握手鉴权实现。
 * <p>
 * 校验两件事，缺一不可：
 * <ol>
 *   <li>当前握手请求已登录（sa-token 会话有效）；</li>
 *   <li>URL 中的 {@code userId} 与当前登录 ID <b>完全一致</b>。</li>
 * </ol>
 * 第二条是关键：路径里的 {@code userId} 由客户端填写，若不做等值校验，
 * 任何已登录用户都能连 {@code /ws/x/victimId} 订阅他人消息。
 * </p>
 *
 * <h2>令牌如何带入握手请求</h2>
 * <p>
 * 浏览器 WebSocket 无法自定义请求头，故 sa-token 需从 query 参数或 Cookie 读令牌。
 * 前端连接示例：{@code /ws/{group}/{userId}?sptoken=xxx}
 * （参数名随 {@code sa-token.is-read-body} / {@code token-name} 配置而变）。
 * </p>
 *
 * <h2>覆盖方式</h2>
 * <p>
 * 业务侧若需要更细粒度控制（如按 group 校验成员关系、一次性换票），
 * 注册自己的 {@link WebSocketHandshakeAuthenticator} Bean 即可，本实现带
 * {@code @ConditionalOnMissingBean} 会自动让位。
 * </p>
 *
 * @author dream
 * @since 2026-09-28
 */
@Slf4j
public class SaTokenWebSocketHandshakeAuthenticator implements WebSocketHandshakeAuthenticator {

    /**
     * 校验握手方已登录，且其登录 ID 与 URL 中的 {@code userId} 一致。
     *
     * @param group   URL 路径分组参数
     * @param userId  URL 路径用户标识参数
     * @param request 握手请求
     * @return 校验通过返回 {@code true}
     */
    @Override
    public boolean authenticate(String group, String userId, ServerHttpRequest request) {
        try {
            if (!SecurityUtils.isLogin()) {
                return false;
            }
            String loginId = SecurityUtils.getUserId();
            if (!userId.equals(loginId)) {
                log.warn("[WebSocket] userId 与登录身份不匹配，拒绝握手: pathUserId={}, loginId={}",
                        userId, loginId);
                return false;
            }
            return true;
        } catch (Exception e) {
            // 含 NotLoginException：未登录/会话异常一律拒绝
            log.debug("[WebSocket] 握手鉴权未通过: userId={}, cause={}", userId, e.getMessage());
            return false;
        }
    }
}
