package com.dream.common.framework.websocket.model;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * WebSocket 消息体
 * <p>
 * 统一的消息传输格式，包含消息类型、发送者、接收者和消息内容。
 * 支持 JSON 序列化传输。
 * </p>
 *
 * @author dream
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息 ID（可选，用于消息去重和追踪） */
    private String msgId;

    /** 消息类型（如：text、notify、command 等，业务自定义） */
    private String type = "text";

    /** 发送者标识（用户ID / 系统 / 服务端） */
    private String from;

    /** 接收者标识（用户ID，为空时表示广播） */
    private String to;

    /** 消息内容（业务数据，JSON 字符串或纯文本） */
    private String content;

    /** 消息时间戳 */
    private LocalDateTime timestamp;

    /** 扩展属性（业务自定义） */
    private Object extra;

    /**
     * 快速创建文本消息
     *
     * @param from    发送者
     * @param to      接收者
     * @param content 消息内容
     * @return WebSocketMessage
     */
    public static WebSocketMessage of(String from, String to, String content) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        return msg;
    }

    /**
     * 从 JSON 字符串解析为 WebSocketMessage
     *
     * @param json JSON 字符串
     * @return WebSocketMessage
     */
    public static WebSocketMessage fromJson(String json) {
        return JSON.parseObject(json, WebSocketMessage.class);
    }

    /**
     * 将 WebSocketMessage 序列化为 JSON 字符串
     *
     * @param message 消息对象
     * @return JSON 字符串
     */
    public static String toJson(WebSocketMessage message) {
        return JSON.toJSONString(message);
    }
}