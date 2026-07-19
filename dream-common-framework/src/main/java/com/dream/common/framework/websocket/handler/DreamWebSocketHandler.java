package com.dream.common.framework.websocket.handler;

import com.alibaba.fastjson2.JSON;
import com.dream.common.framework.websocket.api.WebSocketEventHandler;
import com.dream.common.framework.websocket.api.WebSocketMessageCache;
import com.dream.common.framework.websocket.api.WebSocketMessagePusher;
import com.dream.common.framework.websocket.interceptor.WebSocketPathParamInterceptor;
import com.dream.common.framework.websocket.manager.WebSocketSessionManager;
import com.dream.common.framework.websocket.model.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 核心处理器
 * <p>
 * 基于 Spring {@link TextWebSocketHandler} 实现，负责：
 * <ul>
 *   <li>连接建立时从 attributes 获取 group/userId 并注册会话</li>
 *   <li>接收消息时解析为 {@link WebSocketMessage} 并回调事件处理器</li>
 *   <li>连接关闭/异常时清理会话资源</li>
 *   <li>实现 {@link WebSocketMessagePusher} 接口提供消息推送能力</li>
 * </ul>
 * </p>
 * <p>
 * 路径参数由 {@link WebSocketPathParamInterceptor} 注入到 attributes 中。
 * </p>
 *
 * @author dream
 */
@Slf4j
public class DreamWebSocketHandler extends TextWebSocketHandler implements WebSocketMessagePusher {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketEventHandler eventHandler;
    private final WebSocketMessageCache messageCache;

    /**
     * 构造器
     *
     * @param sessionManager 会话管理器
     * @param eventHandler   事件处理器（业务方实现）
     * @param messageCache   消息缓存（用于离线消息）
     */
    public DreamWebSocketHandler(WebSocketSessionManager sessionManager,
                                 WebSocketEventHandler eventHandler,
                                 WebSocketMessageCache messageCache) {
        this.sessionManager = sessionManager;
        this.eventHandler = eventHandler;
        this.messageCache = messageCache;
    }

    // ==================== 生命周期回调 ====================

    /**
     * 连接建立后回调
     * <p>
     * 从 attributes 中获取 group 和 userId，注册会话。
     * 若参数缺失则拒绝连接。
     * </p>
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String group = (String) session.getAttributes().get("group");
        String userId = (String) session.getAttributes().get("userId");

        // 降级：尝试从查询参数获取
        if (group == null || userId == null) {
            if (session.getUri() != null && session.getUri().getQuery() != null) {
                String query = session.getUri().getQuery();
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if ("group".equals(kv[0]) && kv.length == 2) {
                        group = kv[1];
                    }
                    if ("userId".equals(kv[0]) && kv.length == 2) {
                        userId = kv[1];
                    }
                }
            }
        }

        if (userId == null || group == null) {
            log.warn("WebSocket 连接拒绝：无法获取 group/userId, sessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // 注册会话
        sessionManager.addSession(session, group, userId);

        log.info("WebSocket 连接建立: group={}, userId={}, sessionId={}, onlineCount={}",
                group, userId, session.getId(), sessionManager.getOnlineUserCount());

        // 回调业务处理器
        if (eventHandler != null) {
            eventHandler.onConnected(session, group, userId);
        }

        // 推送离线消息
        if (messageCache != null) {
            java.util.List<WebSocketMessage> cached = messageCache.getMessages(group, userId);
            if (!cached.isEmpty()) {
                for (WebSocketMessage msg : cached) {
                    doSend(session, msg);
                }
                messageCache.removeMessages(group, userId);
                log.debug("已推送 {} 条离线消息给 {}/{}", cached.size(), group, userId);
            }
        }
    }

    /**
     * 收到文本消息时回调
     * <p>
     * 先回调原始消息事件，再尝试解析为 {@link WebSocketMessage} 对象，
     * 解析失败则包装为纯文本消息，最后回调业务消息事件。
     * </p>
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String group = (String) session.getAttributes().get("group");
        String userId = (String) session.getAttributes().get("userId");
        String rawText = message.getPayload();

        // 原始消息事件
        if (eventHandler != null) {
            eventHandler.onRawMessage(session, group, userId, rawText);
        }

        // 尝试解析 JSON
        WebSocketMessage wsMessage;
        try {
            wsMessage = JSON.parseObject(rawText, WebSocketMessage.class);
        } catch (Exception e) {
            wsMessage = WebSocketMessage.of(userId, null, rawText);
        }

        // 业务消息事件
        if (eventHandler != null) {
            eventHandler.onMessage(session, group, userId, wsMessage);
        }
    }

    /**
     * 连接关闭后回调
     * <p>
     * 从会话管理器中移除该会话。
     * </p>
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String group = (String) session.getAttributes().get("group");
        String userId = (String) session.getAttributes().get("userId");

        sessionManager.removeSession(session.getId());

        log.info("WebSocket 连接关闭: group={}, userId={}, sessionId={}, reason={}, onlineCount={}",
                group, userId, session.getId(), status.getReason(),
                sessionManager.getOnlineUserCount());

        if (eventHandler != null && userId != null) {
            eventHandler.onDisconnected(session, group, userId);
        }
    }

    /**
     * 传输错误时回调
     */
    @Override
    public void handleTransportError(WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String group = (String) session.getAttributes().get("group");
        String userId = (String) session.getAttributes().get("userId");

        log.error("WebSocket 传输错误: group={}, userId={}, sessionId={}",
                group, userId, session.getId(), exception);

        if (eventHandler != null) {
            eventHandler.onError(session, group, userId, exception);
        }

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    // ==================== WebSocketMessagePusher 实现 ====================

    /**
     * 发送消息给指定用户（指定分组）
     *
     * @param group   分组
     * @param userId  目标用户
     * @param message 消息对象
     * @return true=至少一个会话发送成功
     */
    @Override
    public boolean sendToUser(String group, String userId, WebSocketMessage message) {
        if (group == null || userId == null || message == null) {
            return false;
        }
        message.setTo(userId);
        java.util.Collection<WebSocketSession> sessions = sessionManager.getSessions(group, userId);
        if (sessions.isEmpty()) {
            return false;
        }
        boolean success = false;
        for (WebSocketSession session : sessions) {
            if (doSend(session, message)) {
                success = true;
            }
        }
        return success;
    }

    /**
     * 发送消息给分组内所有用户
     *
     * @param group   分组
     * @param message 消息对象
     * @return 成功发送的会话数
     */
    @Override
    public int sendToGroup(String group, WebSocketMessage message) {
        if (group == null || message == null) {
            return 0;
        }
        java.util.Collection<WebSocketSession> sessions = sessionManager.getSessionsByGroup(group);
        if (sessions.isEmpty()) {
            return 0;
        }
        AtomicInteger count = new AtomicInteger(0);
        for (WebSocketSession session : sessions) {
            if (doSend(session, message)) {
                count.incrementAndGet();
            }
        }
        log.debug("分组消息发送: group={}, sent={}", group, count.get());
        return count.get();
    }

    /**
     * 广播消息给所有在线用户
     *
     * @param message 消息对象
     * @return 成功发送的会话数
     */
    @Override
    public int broadcast(WebSocketMessage message) {
        int count = 0;
        for (String userId : sessionManager.getAllOnlineUserIds()) {
            for (String group : sessionManager.getGroupsByUser(userId)) {
                if (sendToUser(group, userId, message)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 发送或缓存消息
     * <p>
     * 若用户在线则直接发送，离线则缓存消息。
     * </p>
     *
     * @param group   分组
     * @param userId  目标用户
     * @param message 消息对象
     */
    @Override
    public void sendOrCache(String group, String userId, WebSocketMessage message) {
        if (group == null || userId == null || message == null) {
            return;
        }
        if (sessionManager.isOnline(group, userId)) {
            sendToUser(group, userId, message);
        } else if (messageCache != null) {
            messageCache.save(group, userId, message);
            log.debug("用户 {}/{} 离线，消息已缓存", group, userId);
        }
    }

    /**
     * 判断用户是否在线（指定分组）
     *
     * @param group  分组
     * @param userId 用户标识
     * @return true=在线
     */
    @Override
    public boolean isOnline(String group, String userId) {
        return sessionManager.isOnline(group, userId);
    }

    /**
     * 获取在线用户数量（所有分组去重）
     *
     * @return 在线用户数
     */
    @Override
    public int getOnlineCount() {
        return sessionManager.getOnlineUserCount();
    }

    // ==================== 内部方法 ====================

    /**
     * 向单个会话发送消息
     *
     * @param session WebSocket 会话
     * @param message 消息对象
     * @return true=发送成功
     */
    private boolean doSend(WebSocketSession session, WebSocketMessage message) {
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            String json = JSON.toJSONString(message);
            session.sendMessage(new TextMessage(json));
            return true;
        } catch (IOException e) {
            log.error("消息发送失败: sessionId={}", session.getId(), e);
            return false;
        }
    }
}