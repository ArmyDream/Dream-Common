package com.dream.common.framework.websocket.interceptor;

import com.dream.common.framework.websocket.api.WebSocketHandshakeAuthenticator;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * WebSocket 握手拦截器：路径参数提取 + 身份鉴权。
 * <p>
 * 从连接 URL 中提取 {@code group}、{@code userId} 放入 attributes 供处理器使用，
 * 支持路径格式：{@code /ws/{group}/{userId}}。
 * </p>
 *
 * <h2>鉴权语义（失败关闭）</h2>
 * <p>
 * {@code userId} 是客户端在 URL 里自行填写的，等于把"我要以谁的身份收消息"交给客户端决定。
 * 因此握手必须校验：
 * </p>
 * <ol>
 *   <li>路径参数存在、URL 解码成功、且只含安全字符（防止日志注入与异常 key）；</li>
 *   <li>容器中存在 {@link WebSocketHandshakeAuthenticator} 实现并校验通过。</li>
 * </ol>
 * <p>
 * 若一个实现都没有：默认拒绝所有握手并告警；
 * 只有显式配置 {@code dream.websocket.auth-enabled=false}（纯内网/压测等场景）才放行。
 * </p>
 *
 * @author dream
 */
@Slf4j
public class WebSocketPathParamInterceptor implements HandshakeInterceptor {

    /**
     * 路径参数安全字符集：字母、数字、下划线、中划线、冒号、点，长度 1~64。
     * <p>用于阻断换行/控制字符进入 session key 与日志。</p>
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-.:]{1,64}$");

    private final String pathPattern;

    private final ObjectProvider<WebSocketHandshakeAuthenticator> authenticatorProvider;

    /** 是否强制握手鉴权 */
    private final boolean authEnabled;

    /** 未配置鉴权实现时的一次性告警标记 */
    private volatile boolean warnedNoAuthenticator;

    /**
     * 构造器。
     *
     * @param pathPattern           路径模板，如 /ws/{group}/{userId}
     * @param authenticatorProvider 握手鉴权实现（可为空，行为取决于 {@code authEnabled}）
     * @param authEnabled           是否强制鉴权；false 表示显式放行未鉴权连接
     */
    public WebSocketPathParamInterceptor(String pathPattern,
                                         ObjectProvider<WebSocketHandshakeAuthenticator> authenticatorProvider,
                                         boolean authEnabled) {
        this.pathPattern = pathPattern;
        this.authenticatorProvider = authenticatorProvider;
        this.authEnabled = authEnabled;
    }

    /**
     * 握手前处理：提取并校验路径参数，再执行鉴权。
     *
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 握手属性（用于传递给 WebSocketSession）
     * @return {@code true} 继续握手；{@code false} 拒绝（响应 403）
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {
        String path = request.getURI().getPath();

        UriTemplate template = new UriTemplate(pathPattern);
        if (!template.matches(path)) {
            log.warn("路径不匹配模板: {} 不匹配 {}", path, pathPattern);
            return reject(response, "路径不合法");
        }

        Map<String, String> pathVariables = template.match(path);
        String group = decode(pathVariables.get("group"));
        String userId = decode(pathVariables.get("userId"));

        if (!isSafeParam(group) || !isSafeParam(userId)) {
            log.warn("握手被拒绝：路径参数缺失或非法, group={}, userId={}, remote={}",
                    group, userId, request.getRemoteAddress());
            return reject(response, "路径参数不合法");
        }

        if (!authenticate(group, userId, request)) {
            log.warn("握手鉴权失败: group={}, userId={}, remote={}",
                    group, userId, request.getRemoteAddress());
            return reject(response, "未授权");
        }

        attributes.put("group", group);
        attributes.put("userId", userId);
        log.debug("提取路径参数: group={}, userId={}", group, userId);
        return true;
    }

    /**
     * 握手后处理（空实现）。
     */
    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
        // 无需处理
    }

    /**
     * 执行鉴权：有实现则委托实现；无实现按 {@code authEnabled} 决定放行或拒绝。
     *
     * @param group   分组参数
     * @param userId  用户标识参数
     * @param request 握手请求
     * @return 允许握手返回 {@code true}
     */
    private boolean authenticate(String group, String userId, ServerHttpRequest request) {
        WebSocketHandshakeAuthenticator authenticator = authenticatorProvider.getIfAvailable();
        if (authenticator == null) {
            if (!authEnabled) {
                warnNoAuthenticatorOnce("dream.websocket.auth-enabled=false，已放行未鉴权的 WebSocket 连接");
                return true;
            }
            warnNoAuthenticatorOnce("未找到 WebSocketHandshakeAuthenticator 实现，握手已被拒绝；"
                    + "请注册该 Bean（security 模块已提供 sa-token 默认实现），"
                    + "或确认无需鉴权后设置 dream.websocket.auth-enabled=false");
            return false;
        }
        try {
            return authenticator.authenticate(group, userId, request);
        } catch (Exception e) {
            // 鉴权实现抛异常一律视为失败，避免异常路径绕过校验
            log.error("WebSocket 握手鉴权实现抛异常，按失败处理: authenticator={}",
                    authenticator.getClass().getName(), e);
            return false;
        }
    }

    /**
     * 输出一次性告警，避免逐连接刷屏。
     *
     * @param message 告警内容
     */
    private void warnNoAuthenticatorOnce(String message) {
        if (warnedNoAuthenticator) {
            return;
        }
        warnedNoAuthenticator = true;
        log.warn("[WebSocket] {}", message);
    }

    /**
     * 判断路径参数是否存在且只含安全字符。
     *
     * @param value 参数值
     * @return 合法返回 {@code true}
     */
    private static boolean isSafeParam(String value) {
        return value != null && PARAM_PATTERN.matcher(value).matches();
    }

    /**
     * URL 解码路径参数（握手 URL 由客户端构造，可能被编码）。
     *
     * @param raw 原始值
     * @return 解码结果；解码失败返回 {@code null}
     */
    private static String decode(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return URLDecoder.decode(raw, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            log.warn("路径参数解码失败: {}", raw);
            return null;
        }
    }

    /**
     * 拒绝握手：置 403 并返回 false。
     *
     * @param response HTTP 响应
     * @param reason   拒绝原因（仅日志用途，不回显细节）
     * @return 恒为 {@code false}
     */
    private boolean reject(ServerHttpResponse response, String reason) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        if (log.isDebugEnabled()) {
            log.debug("拒绝 WebSocket 握手: {}", reason);
        }
        return false;
    }
}
