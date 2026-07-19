package com.dream.common.framework.websocket.config;

import com.dream.common.framework.websocket.api.WebSocketEventHandler;
import com.dream.common.framework.websocket.api.WebSocketMessageCache;
import com.dream.common.framework.websocket.api.WebSocketMessagePusher;
import com.dream.common.framework.websocket.cache.DefaultWebSocketMessageCache;
import com.dream.common.framework.websocket.handler.DreamWebSocketHandler;
import com.dream.common.framework.websocket.interceptor.WebSocketPathParamInterceptor;
import com.dream.common.framework.websocket.manager.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * WebSocket 自动装配配置
 * <p>
 * 基于 Spring Boot 2.7.18 自动装配机制，当满足以下条件时自动生效：
 * <ul>
 *   <li>Web 应用环境（Servlet）</li>
 *   <li>配置项 {@code dream.websocket.enabled=true}（默认开启）</li>
 * </ul>
 * </p>
 *
 * <h2>可覆盖的 Bean</h2>
 * <ul>
 *   <li>{@link WebSocketMessageCache}：默认使用 {@link DefaultWebSocketMessageCache}（基于 Caffeine）</li>
 *   <li>{@link WebSocketEventHandler}：默认空实现，业务方必须实现</li>
 *   <li>{@link WebSocketSessionManager}：默认内存管理，通常无需替换</li>
 * </ul>
 *
 * <h2>配置项</h2>
 * <pre>
 * dream:
 *   websocket:
 *     enabled: true              # 是否启用（默认 true）
 * </pre>
 *
 * @author dream
 */
@Slf4j
@Configuration
@EnableWebSocket
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "dream.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(TextWebSocketHandler.class)  // // 关键：缺少spring-boot-starter-websocket依赖时不加载
public class WebSocketAutoConfig {

    /**
     * 默认路径模板：/ws/{group}/{userId}
     */
    private static final String DEFAULT_PATH_PATTERN = "/ws/{group}/{userId}";

    /**
     * 会话管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketSessionManager webSocketSessionManager() {
        log.info("WebSocket SessionManager initialized");
        return new WebSocketSessionManager();
    }

    /**
     * 默认消息缓存实现（基于 CachesUtils / Caffeine）
     * <p>
     * 业务方可注册自己的 {@link WebSocketMessageCache} Bean 覆盖此默认实现。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketMessageCache.class)
    public WebSocketMessageCache webSocketMessageCache() {
        log.info("WebSocket MessageCache initialized (default)");
        return new DefaultWebSocketMessageCache();
    }

    /**
     * 默认事件处理器（空实现）
     * <p>
     * 业务方应注册自己的 {@link WebSocketEventHandler} Bean 覆盖此默认实现。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketEventHandler.class)
    public WebSocketEventHandler webSocketEventHandler() {
        log.info("WebSocket EventHandler initialized (default: no-op)");
        return new WebSocketEventHandler() {
        };
    }

    /**
     * 核心 WebSocket 处理器（同时实现 {@link WebSocketMessagePusher} 接口）
     */
    @Bean
    public DreamWebSocketHandler dreamWebSocketHandler(WebSocketSessionManager sessionManager,
                                                       WebSocketEventHandler eventHandler,
                                                       WebSocketMessageCache messageCache) {
        log.info("WebSocket Handler initialized");
        return new DreamWebSocketHandler(sessionManager, eventHandler, messageCache);
    }

    /**
     * 消息推送器 Bean
     * <p>
     * 业务方可直接注入 {@link WebSocketMessagePusher} 使用。
     * </p>
     */
    @Bean
    public WebSocketMessagePusher webSocketMessagePusher(DreamWebSocketHandler handler) {
        return handler;
    }

    /**
     * WebSocket 端点注册配置
     */
    @Bean
    public WebSocketConfigurer webSocketConfigurer(DreamWebSocketHandler handler) {
        return registry -> {
            registry.addHandler(handler, DEFAULT_PATH_PATTERN)
                    .addInterceptors(
                            new HttpSessionHandshakeInterceptor(),
                            new WebSocketPathParamInterceptor(DEFAULT_PATH_PATTERN)
                    )
                    .setAllowedOrigins("*");

            log.info("WebSocket endpoint registered: {}", DEFAULT_PATH_PATTERN);
        };
    }
}