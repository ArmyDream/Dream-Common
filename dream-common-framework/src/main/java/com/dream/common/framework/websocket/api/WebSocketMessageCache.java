package com.dream.common.framework.websocket.api;

import com.dream.common.framework.websocket.model.WebSocketMessage;

import java.util.List;

/**
 * WebSocket 消息缓存接口
 * <p>
 * 用于缓存离线消息，支持按分组和用户维度存储。
 * 默认实现基于 {@code CachesUtils}（Caffeine），业务方可替换为 Redis、数据库等存储方案。
 * </p>
 *
 * @author dream
 */
public interface WebSocketMessageCache {

    /**
     * 缓存消息（用户离线时暂存）
     *
     * @param group   分组
     * @param userId  目标用户标识
     * @param message 消息
     */
    void save(String group, String userId, WebSocketMessage message);

    /**
     * 获取用户的缓存消息列表（如离线期间的消息）
     *
     * @param group  分组
     * @param userId 用户标识
     * @return 消息列表（不为 null）
     */
    List<WebSocketMessage> getMessages(String group, String userId);

    /**
     * 删除用户的所有缓存消息（通常在用户上线并拉取后调用）
     *
     * @param group  分组
     * @param userId 用户标识
     */
    void removeMessages(String group, String userId);

    /**
     * 获取用户缓存消息数量
     *
     * @param group  分组
     * @param userId 用户标识
     * @return 消息数量
     */
    int count(String group, String userId);
}