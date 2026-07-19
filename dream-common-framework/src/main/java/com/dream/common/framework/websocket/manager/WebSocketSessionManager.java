package com.dream.common.framework.websocket.manager;

import com.dream.common.framework.websocket.model.WebSocketSessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * <p>
 * 管理所有 WebSocket 连接，支持多层级结构：
 * <ul>
 *   <li>groupId → userId → Set{@code <Session>}</li>
 *   <li>支持同一用户多端登录（一个 userId 对应多个 Session）</li>
 *   <li>支持同一用户在不同分组建立多个连接</li>
 * </ul>
 * </p>
 * <p>
 * 所有操作均为线程安全（基于 {@link ConcurrentHashMap}）。
 * </p>
 *
 * @author dream
 */
@Slf4j
public class WebSocketSessionManager {

    /** sessionId → WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** sessionId → WebSocketSessionInfo */
    private final Map<String, WebSocketSessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    /** group → userId → Set&lt;sessionId&gt; */
    private final Map<String, Map<String, Set<String>>> groupUserSessionMap = new ConcurrentHashMap<>();

    /** userId → Set&lt;group&gt;（快速查询用户所在分组） */
    private final Map<String, Set<String>> userGroupMap = new ConcurrentHashMap<>();

    /**
     * 注册会话
     *
     * @param session WebSocket 会话
     * @param group   分组
     * @param userId  用户标识
     */
    public void addSession(WebSocketSession session, String group, String userId) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        WebSocketSessionInfo info = WebSocketSessionInfo.of(sessionId, group, userId);
        info.setRemoteAddress(getRemoteAddress(session));
        info.setUri(session.getUri() != null ? session.getUri().toString() : null);
        sessionInfoMap.put(sessionId, info);

        // group → user → sessions
        Map<String, Set<String>> userSessionMap = groupUserSessionMap
                .computeIfAbsent(group, k -> new ConcurrentHashMap<>());
        userSessionMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // user → groups
        userGroupMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(group);

        log.debug("会话已注册: group={}, userId={}, sessionId={}", group, userId, sessionId);
    }

    /**
     * 移除会话
     *
     * @param sessionId 会话 ID
     */
    public void removeSession(String sessionId) {
        WebSocketSessionInfo info = sessionInfoMap.remove(sessionId);
        sessions.remove(sessionId);

        if (info == null) {
            return;
        }

        String group = info.getGroup();
        String userId = info.getUserId();

        // 从 groupUserSessionMap 移除
        Map<String, Set<String>> userSessionMap = groupUserSessionMap.get(group);
        if (userSessionMap != null) {
            Set<String> sessionSet = userSessionMap.get(userId);
            if (sessionSet != null) {
                sessionSet.remove(sessionId);
                if (sessionSet.isEmpty()) {
                    userSessionMap.remove(userId);
                }
            }
            if (userSessionMap.isEmpty()) {
                groupUserSessionMap.remove(group);
            }
        }

        // 从 userGroupMap 移除
        Set<String> groupSet = userGroupMap.get(userId);
        if (groupSet != null) {
            groupSet.remove(group);
            if (groupSet.isEmpty()) {
                userGroupMap.remove(userId);
            }
        }

        log.debug("会话已移除: group={}, userId={}, sessionId={}", group, userId, sessionId);
    }

    /**
     * 获取单个会话
     *
     * @param sessionId 会话 ID
     * @return WebSocketSession；不存在返回 null
     */
    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 获取用户的所有会话（指定分组）
     *
     * @param group  分组
     * @param userId 用户标识
     * @return 会话集合；无会话返回空列表
     */
    public Collection<WebSocketSession> getSessions(String group, String userId) {
        Map<String, Set<String>> userSessionMap = groupUserSessionMap.get(group);
        if (userSessionMap == null) {
            return Collections.emptyList();
        }
        Set<String> sessionIds = userSessionMap.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<WebSocketSession> result = new ArrayList<>();
        for (String sid : sessionIds) {
            WebSocketSession s = sessions.get(sid);
            if (s != null && s.isOpen()) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 获取分组内的所有会话
     *
     * @param group 分组
     * @return 会话集合
     */
    public Collection<WebSocketSession> getSessionsByGroup(String group) {
        Map<String, Set<String>> userSessionMap = groupUserSessionMap.get(group);
        if (userSessionMap == null) {
            return Collections.emptyList();
        }
        List<WebSocketSession> result = new ArrayList<>();
        for (Set<String> sessionIds : userSessionMap.values()) {
            for (String sid : sessionIds) {
                WebSocketSession s = sessions.get(sid);
                if (s != null && s.isOpen()) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    /**
     * 获取用户所在的所有分组
     *
     * @param userId 用户标识
     * @return 分组集合
     */
    public Set<String> getGroupsByUser(String userId) {
        Set<String> groups = userGroupMap.get(userId);
        return groups != null ? Collections.unmodifiableSet(groups) : Collections.emptySet();
    }

    /**
     * 判断用户是否在线（指定分组）
     *
     * @param group  分组
     * @param userId 用户标识
     * @return true=至少有一个活跃会话
     */
    public boolean isOnline(String group, String userId) {
        Map<String, Set<String>> userSessionMap = groupUserSessionMap.get(group);
        if (userSessionMap == null) {
            return false;
        }
        Set<String> sessionIds = userSessionMap.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return false;
        }
        for (String sid : sessionIds) {
            WebSocketSession s = sessions.get(sid);
            if (s != null && s.isOpen()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有在线用户 ID（去重）
     *
     * @return 用户 ID 集合
     */
    public Set<String> getAllOnlineUserIds() {
        return Collections.unmodifiableSet(userGroupMap.keySet());
    }

    /**
     * 获取会话信息
     *
     * @param sessionId 会话 ID
     * @return 会话信息；不存在返回 null
     */
    public WebSocketSessionInfo getSessionInfo(String sessionId) {
        return sessionInfoMap.get(sessionId);
    }

    /**
     * 获取用户的所有会话信息
     *
     * @param userId 用户标识
     * @return 会话信息列表
     */
    public List<WebSocketSessionInfo> getSessionInfosByUser(String userId) {
        List<WebSocketSessionInfo> result = new ArrayList<>();
        for (WebSocketSessionInfo info : sessionInfoMap.values()) {
            if (userId.equals(info.getUserId())) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * 获取所有会话信息（用于监控/管理）
     *
     * @return 会话信息列表
     */
    public List<WebSocketSessionInfo> getAllSessionInfos() {
        return Collections.unmodifiableList(new ArrayList<>(sessionInfoMap.values()));
    }

    // ==================== 统计 ====================

    /**
     * 获取在线会话总数
     *
     * @return 会话数量
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 获取在线用户数量（所有分组去重）
     *
     * @return 用户数量
     */
    public int getOnlineUserCount() {
        return userGroupMap.size();
    }

    /**
     * 获取分组数量
     *
     * @return 分组数量
     */
    public int getGroupCount() {
        return groupUserSessionMap.size();
    }

    // ==================== 内部方法 ====================

    /**
     * 获取客户端远程 IP 地址
     *
     * @param session WebSocket 会话
     * @return IP 地址；获取失败返回 null
     */
    private String getRemoteAddress(WebSocketSession session) {
        try {
            InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();
            return address != null ? address.getAddress().getHostAddress() : null;
        } catch (Exception e) {
            return null;
        }
    }
}