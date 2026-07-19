package com.dream.common.framework.websocket.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * WebSocket 会话信息
 * <p>
 * 封装 WebSocket 连接的会话元数据，用于业务层获取连接信息。
 * </p>
 *
 * @author dream
 */
@Data
public class WebSocketSessionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话 ID（WebSocket 分配的唯一标识） */
    private String sessionId;

    /** 分组 */
    private String group;

    /** 用户标识（业务绑定的用户ID） */
    private String userId;

    /** 连接建立时间 */
    private LocalDateTime connectTime;

    /** 客户端 IP 地址 */
    private String remoteAddress;

    /** 请求 URI */
    private String uri;

    /**
     * 创建会话信息
     *
     * @param sessionId 会话 ID
     * @param group     分组
     * @param userId    用户标识
     * @return WebSocketSessionInfo
     */
    public static WebSocketSessionInfo of(String sessionId, String group, String userId) {
        WebSocketSessionInfo info = new WebSocketSessionInfo();
        info.setSessionId(sessionId);
        info.setGroup(group);
        info.setUserId(userId);
        info.setConnectTime(LocalDateTime.now());
        return info;
    }
}