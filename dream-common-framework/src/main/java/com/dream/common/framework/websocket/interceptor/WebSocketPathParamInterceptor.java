package com.dream.common.framework.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;

/**
 * WebSocket 路径参数拦截器
 * <p>
 * 从 WebSocket 连接 URL 中提取路径参数，并放入 attributes 中供后续处理器使用。
 * </p>
 * <p>
 * 支持路径格式：/ws/{group}/{userId}
 * </p>
 *
 * @author dream
 */
@Slf4j
public class WebSocketPathParamInterceptor implements HandshakeInterceptor {

    private final String pathPattern;

    /**
     * 构造器
     *
     * @param pathPattern 路径模板，如 /ws/{group}/{userId}
     */
    public WebSocketPathParamInterceptor(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    /**
     * 握手前处理
     * <p>
     * 从请求路径中提取 group 和 userId，存入 attributes 中。
     * </p>
     *
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 握手属性（用于传递给 WebSocketSession）
     * @return true 表示继续握手
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) throws Exception {
        String path = request.getURI().getPath();

        UriTemplate template = new UriTemplate(pathPattern);
        if (template.matches(path)) {
            Map<String, String> pathVariables = template.match(path);
            String group = pathVariables.get("group");
            String userId = pathVariables.get("userId");

            if (group != null && userId != null) {
                attributes.put("group", group);
                attributes.put("userId", userId);
                log.debug("提取路径参数: group={}, userId={}", group, userId);
            } else {
                log.warn("路径参数不完整: group={}, userId={}", group, userId);
            }
        } else {
            log.warn("路径不匹配模板: {} 不匹配 {}", path, pathPattern);
        }

        return true;
    }

    /**
     * 握手后处理（空实现）
     */
    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
        // 无需处理
    }
}