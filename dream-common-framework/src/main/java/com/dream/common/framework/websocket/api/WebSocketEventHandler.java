package com.dream.common.framework.websocket.api;

import com.dream.common.framework.websocket.model.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 事件处理接口
 * <p>
 * 定义连接生命周期中的回调钩子，业务方实现此接口以处理自定义业务逻辑。
 * 所有方法提供默认空实现，业务方按需覆盖。
 * </p>
 *
 * @author dream
 */
public interface WebSocketEventHandler {

    /**
     * 连接建立时的回调
     * <p>
     * 此时会话已注册，可在此执行用户上线通知、推送离线消息等操作。
     * </p>
     *
     * @param session WebSocket 会话
     * @param group   分组
     * @param userId  用户标识
     */
    default void onConnected(WebSocketSession session, String group, String userId) {}

    /**
     * 收到客户端消息时的回调
     * <p>
     * 消息已解析为 {@link WebSocketMessage} 对象。
     * </p>
     *
     * @param session WebSocket 会话
     * @param group   分组
     * @param userId  用户标识
     * @param message 解析后的消息对象
     */
    default void onMessage(WebSocketSession session, String group, String userId, WebSocketMessage message) {}

    /**
     * 收到原始文本消息时的回调（在 JSON 解析之前）
     * <p>
     * 可用于日志记录、消息路由等前置处理。
     * </p>
     *
     * @param session WebSocket 会话
     * @param group   分组
     * @param userId  用户标识
     * @param rawText 原始文本
     */
    default void onRawMessage(WebSocketSession session, String group, String userId, String rawText) {}

    /**
     * 连接关闭时的回调
     * <p>
     * 可在此执行用户下线通知、清理资源等操作。
     * </p>
     *
     * @param session WebSocket 会话
     * @param group   分组
     * @param userId  用户标识
     */
    default void onDisconnected(WebSocketSession session, String group, String userId) {}

    /**
     * 传输错误时的回调
     *
     * @param session   WebSocket 会话
     * @param group     分组（可能为 null）
     * @param userId    用户标识（可能为 null）
     * @param exception 异常
     */
    default void onError(WebSocketSession session, String group, String userId, Throwable exception) {}
}