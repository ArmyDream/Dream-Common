package com.dream.common.framework.websocket.cache;

import com.dream.common.framework.websocket.api.WebSocketMessageCache;
import com.dream.common.framework.websocket.model.WebSocketMessage;
import com.dream.common.tool.utils.CachesUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 CachesUtils（Caffeine）的默认消息缓存实现
 * <p>
 * 使用本地 Caffeine 缓存存储离线消息，默认过期时间 24 小时，单用户最多 1000 条。
 * 适用于单机部署场景。分布式部署请替换为 Redis 实现。
 * </p>
 *
 * <h2>缓存 Key 规则</h2>
 * <p>
 * 前缀：{@code ws:msg:} + group + ":" + userId
 * </p>
 *
 * @author dream
 */
@Slf4j
public class DefaultWebSocketMessageCache implements WebSocketMessageCache {

    /** 缓存 Key 前缀 */
    private static final String KEY_PREFIX = "ws:msg:";

    /** 默认过期时间：24 小时（单位：秒） */
    private static final long EXPIRE_SECONDS = 86400L;

    /** 最大缓存消息条数（单用户） */
    private static final int MAX_MESSAGES = 1000;

    @Override
    public void save(String group, String userId, WebSocketMessage message) {
        if (group == null || userId == null || message == null) {
            return;
        }
        String key = buildKey(group, userId);
        List<WebSocketMessage> messages = CachesUtils.get(key);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        if (messages.size() >= MAX_MESSAGES) {
            messages.remove(0);
            log.warn("用户 {}/{} 离线消息超过 {} 条，已移除最早的消息", group, userId, MAX_MESSAGES);
        }
        messages.add(message);
        CachesUtils.set(key, messages, EXPIRE_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public List<WebSocketMessage> getMessages(String group, String userId) {
        if (group == null || userId == null) {
            return Collections.emptyList();
        }
        List<WebSocketMessage> messages = CachesUtils.get(buildKey(group, userId));
        return messages != null ? new ArrayList<>(messages) : Collections.emptyList();
    }

    @Override
    public void removeMessages(String group, String userId) {
        if (group == null || userId == null) {
            return;
        }
        CachesUtils.delete(buildKey(group, userId));
    }

    @Override
    public int count(String group, String userId) {
        return getMessages(group, userId).size();
    }

    /**
     * 构建缓存 Key
     *
     * @param group  分组
     * @param userId 用户标识
     * @return 缓存 Key
     */
    private String buildKey(String group, String userId) {
        return KEY_PREFIX + group + ":" + userId;
    }
}