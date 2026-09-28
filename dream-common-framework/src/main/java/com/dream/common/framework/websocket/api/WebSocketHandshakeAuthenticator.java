package com.dream.common.framework.websocket.api;

import org.springframework.http.server.ServerHttpRequest;

/**
 * WebSocket 握手鉴权 SPI。
 * <p>
 * 连接建立前校验"发起握手的调用方是否有权以 {@code userId} 身份接入"。
 * 框架层只定义契约，不绑定任何具体认证体系；
 * dream-common-security 提供 sa-token 默认实现
 * （要求已登录且登录 ID 与 {@code userId} 一致）。
 * </p>
 *
 * <h2>为什么必须有这个扩展点</h2>
 * <p>
 * 端点路径 {@code /ws/{group}/{userId}} 中的 {@code userId} 由客户端在 URL 里自行填写，
 * 若不做握手校验，任何人连接 {@code /ws/anyGroup/victimId} 即可冒充该用户
 * 实时接收其消息并领取其离线缓存消息。因此框架默认<b>失败关闭</b>：
 * 容器中没有本接口实现且未显式关闭鉴权时，所有握手都会被拒绝。
 * </p>
 *
 * <h2>业务自定义示例</h2>
 * <pre>{@code
 * @Component
 * public class MyWsAuthenticator implements WebSocketHandshakeAuthenticator {
 *     @Override
 *     public boolean authenticate(String group, String userId, ServerHttpRequest request) {
 *         // 例如：从 query 参数取一次性签名令牌，校验后确认 userId 与其匹配
 *         return tokenService.verify(request.getURI().getQuery(), userId);
 *     }
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-09-28
 */
public interface WebSocketHandshakeAuthenticator {

    /**
     * 校验握手请求是否允许以给定身份接入。
     *
     * @param group   URL 路径中的分组参数，非空
     * @param userId  URL 路径中的用户标识参数，非空
     * @param request 原始 HTTP 握手请求（可读取 header、query、远端地址等）
     * @return {@code true} 允许握手；{@code false} 拒绝（返回 403，连接不会建立）
     */
    boolean authenticate(String group, String userId, ServerHttpRequest request);
}
