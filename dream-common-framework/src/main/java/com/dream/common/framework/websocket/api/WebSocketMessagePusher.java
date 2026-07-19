package com.dream.common.framework.websocket.api;

import com.dream.common.framework.websocket.model.WebSocketMessage;

import java.util.Collection;

/**
 * WebSocket 消息推送接口
 * <p>
 * 定义消息推送的标准能力，包括单播、组播、广播、离线缓存。
 * 默认实现由 {@link com.dream.common.framework.websocket.handler.DreamWebSocketHandler} 提供。
 * </p>
 *
 * @author dream
 */
public interface WebSocketMessagePusher {

    /**
     * 推送消息给指定用户（指定分组）
     *
     * @param group   分组
     * @param userId  目标用户标识
     * @param message 消息对象
     * @return true=至少一个会话推送成功
     */
    boolean sendToUser(String group, String userId, WebSocketMessage message);

    /**
     * 推送消息给指定分组内的所有用户
     *
     * @param group   分组
     * @param message 消息对象
     * @return 成功推送的会话数量
     */
    int sendToGroup(String group, WebSocketMessage message);

    /**
     * 广播消息给所有在线用户（所有分组）
     *
     * @param message 消息对象
     * @return 成功推送的会话数量
     */
    int broadcast(WebSocketMessage message);

    /**
     * 推送消息给指定用户，若用户离线则缓存消息
     *
     * @param group   分组
     * @param userId  目标用户标识
     * @param message 消息对象
     */
    void sendOrCache(String group, String userId, WebSocketMessage message);

    /**
     * 判断用户是否在线（指定分组）
     *
     * @param group  分组
     * @param userId 用户标识
     * @return true=在线
     */
    boolean isOnline(String group, String userId);

    /**
     * 获取在线用户数量（所有分组去重）
     *
     * @return 在线用户数
     */
    int getOnlineCount();
}