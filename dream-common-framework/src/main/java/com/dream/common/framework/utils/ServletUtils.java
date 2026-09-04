package com.dream.common.framework.utils;

import com.dream.common.core.enums.CharsetEnum;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Servlet 客户端工具类
 * <p>
 * 封装 Spring {@link RequestContextHolder}，提供对当前 HTTP 请求/响应的便捷访问。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>{@link #getRequest()} / {@link #getResponse()} —— 获取当前请求/响应对象</li>
 *   <li>{@link #getParameter(String)} / {@link #getParameterMap()} —— 请求参数读取</li>
 *   <li>{@link #getHeader(String)} / {@link #getHeaders()} —— 请求头读取</li>
 *   <li>{@link #getCookieValue(String)} / {@link #setCookie} —— Cookie 操作</li>
 *   <li>{@link #urlEncode(String)} / {@link #urlDecode(String)} —— URL 编解码</li>
 *   <li>{@link #isAjaxRequest()} —— AJAX 请求判断</li>
 *   <li>{@link #getBaseUrl()} —— 构建基础 URL</li>
 *   <li>{@link #renderJson(HttpServletResponse, String)} —— 响应 JSON 输出</li>
 * </ul>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings("all")
public final class ServletUtils {

    private ServletUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 核心：获取 Request / Response / Session ====================

    /**
     * 获取当前 HTTP 请求
     *
     * @return HttpServletRequest；非 Web 上下文返回 null
     */
    public static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attrs = getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("获取 HttpServletRequest 失败，可能不在 Web 上下文中", e);
            }
            return null;
        }
    }

    /**
     * 获取当前 HTTP 响应
     *
     * @return HttpServletResponse；非 Web 上下文返回 null
     */
    public static HttpServletResponse getResponse() {
        try {
            ServletRequestAttributes attrs = getRequestAttributes();
            return attrs != null ? attrs.getResponse() : null;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("获取 HttpServletResponse 失败，可能不在 Web 上下文中", e);
            }
            return null;
        }
    }

    /**
     * 获取当前 Session（不创建）
     *
     * @return HttpSession；不存在或非 Web 上下文返回 null
     */
    public static HttpSession getSession() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getSession(false) : null;
    }

    /**
     * 获取当前 Session（不存在则创建）
     *
     * @return HttpSession
     */
    public static HttpSession getOrCreateSession() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getSession(true) : null;
    }

    /**
     * 获取 Spring 的 RequestAttributes
     *
     * @return ServletRequestAttributes；非 Web 上下文返回 null
     */
    public static ServletRequestAttributes getRequestAttributes() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            return (ServletRequestAttributes) attributes;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("获取 RequestAttributes 失败", e);
            }
            return null;
        }
    }

    // ==================== 请求参数 ====================

    /**
     * 获取请求参数
     *
     * @param name 参数名
     * @return 参数值；不存在返回 null
     */
    public static String getParameter(String name) {
        HttpServletRequest request = getRequest();
        return request != null ? request.getParameter(name) : null;
    }

    /**
     * 获取请求参数（不存在时返回默认值）
     *
     * @param name         参数名
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public static String getParameter(String name, String defaultValue) {
        String value = getParameter(name);
        return StringUtils.isObjectNotBlank(value) ? value : defaultValue;
    }

    /**
     * 获取所有请求参数
     *
     * @return 参数名 → 参数值 Map
     */
    public static Map<String, String> getParameterMap() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return Collections.emptyMap();
        }
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            result.put(entry.getKey(), values != null && values.length > 0 ? values[0] : null);
        }
        return result;
    }

    /**
     * 获取所有请求参数（多值版）
     *
     * @return 参数名 → 参数值数组 Map
     */
    public static Map<String, String[]> getParameterValuesMap() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getParameterMap() : Collections.emptyMap();
    }

    /**
     * 获取查询字符串
     *
     * @return 查询字符串（如 "name=abc&age=1"）；不存在返回 null
     */
    public static String getQueryString() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getQueryString() : null;
    }

    // ==================== 请求头 ====================

    /**
     * 获取请求头
     *
     * @param name 请求头名称
     * @return 请求头值（URL 解码后）；不存在返回空字符串
     */
    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        return request != null ? getHeader(request, name) : "";
    }

    /**
     * 获取请求头（原始值，不解码）
     *
     * @param request HttpServletRequest
     * @param name    请求头名称
     * @return 请求头值
     */
    public static String getHeader(HttpServletRequest request, String name) {
        if (request == null || StringUtils.isBlank(name)) {
            return "";
        }
        String value = request.getHeader(name);
        if (!StringUtils.isObjectNotBlank(value)) {
            return "";
        }
        return urlDecode(value);
    }

    /**
     * 获取所有请求头
     *
     * @return 请求头名 → 值 Map
     */
    public static Map<String, String> getHeaders() {
        HttpServletRequest request = getRequest();
        return request != null ? getHeaders(request) : Collections.emptyMap();
    }

    /**
     * 获取所有请求头
     *
     * @param request HttpServletRequest
     * @return 请求头名 → 值 Map
     */
    public static Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> map = new LinkedHashMap<>();
        if (request == null) {
            return map;
        }
        Enumeration<String> enumeration = request.getHeaderNames();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement();
                map.put(key, request.getHeader(key));
            }
        }
        return map;
    }

    // ==================== Cookie ====================

    /**
     * 获取所有 Cookie
     *
     * @return Cookie 数组；无 Cookie 返回空数组
     */
    public static Cookie[] getCookies() {
        HttpServletRequest request = getRequest();
        Cookie[] cookies = request != null ? request.getCookies() : null;
        return cookies != null ? cookies : new Cookie[0];
    }

    /**
     * 获取指定名称的 Cookie 值
     *
     * @param name Cookie 名称
     * @return Cookie 值；不存在返回 null
     */
    public static String getCookieValue(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (Cookie cookie : getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 获取指定名称的 Cookie 值（不存在返回默认值）
     *
     * @param name         Cookie 名称
     * @param defaultValue 默认值
     * @return Cookie 值或默认值
     */
    public static String getCookieValue(String name, String defaultValue) {
        String value = getCookieValue(name);
        return StringUtils.isObjectNotBlank(value) ? value : defaultValue;
    }

    /**
     * 设置 Cookie
     *
     * @param name     Cookie 名称
     * @param value    Cookie 值
     * @param maxAge   最大存活时间（秒），-1 为会话级，0 为删除
     * @param path     路径
     * @param domain   域名（可为 null）
     * @param httpOnly 是否 HttpOnly
     * @param secure   是否仅 HTTPS
     */
    public static void setCookie(String name, String value, int maxAge, String path,
                                 String domain, boolean httpOnly, boolean secure) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            log.warn("设置 Cookie 失败：无法获取 HttpServletResponse");
            return;
        }
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(StringUtils.isObjectNotBlank(path) ? path : "/");
        if (StringUtils.isObjectNotBlank(domain)) {
            cookie.setDomain(domain);
        }
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        response.addCookie(cookie);
        if (log.isDebugEnabled()) {
            log.debug("设置 Cookie: {}={}, maxAge={}, path={}", name, value, maxAge, cookie.getPath());
        }
    }

    /**
     * 设置会话级 Cookie
     *
     * @param name  Cookie 名称
     * @param value Cookie 值
     */
    public static void setCookie(String name, String value) {
        setCookie(name, value, -1, null, null, false, false);
    }

    /**
     * 设置带过期时间的 Cookie（路径为 /）
     *
     * @param name   Cookie 名称
     * @param value  Cookie 值
     * @param maxAge 最大存活时间（秒）
     */
    public static void setCookie(String name, String value, int maxAge) {
        setCookie(name, value, maxAge, "/", null, false, false);
    }

    /**
     * 删除 Cookie
     *
     * @param name Cookie 名称
     */
    public static void removeCookie(String name) {
        setCookie(name, "", 0, "/", null, false, false);
        if (log.isDebugEnabled()) {
            log.debug("删除 Cookie: {}", name);
        }
    }

    // ==================== 请求信息 ====================

    /**
     * 获取请求方法（GET / POST / PUT / DELETE 等）
     *
     * @return 请求方法名；非 Web 上下文返回空字符串
     */
    public static String getMethod() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getMethod() : "";
    }

    /**
     * 获取 Content-Type
     *
     * @return Content-Type 值
     */
    public static String getContentType() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getContentType() : "";
    }

    /**
     * 获取请求 URI（不含查询参数）
     *
     * @return 请求 URI
     */
    public static String getRequestURI() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : "";
    }

    /**
     * 获取请求 URL（含查询参数）
     *
     * @return 完整请求 URL
     */
    public static String getRequestURL() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURL().toString() : "";
    }

    /**
     * 获取 Context Path
     *
     * @return Context Path
     */
    public static String getContextPath() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getContextPath() : "";
    }

    /**
     * 获取远程主机地址
     *
     * @return 远程主机 IP
     */
    public static String getRemoteHost() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRemoteHost() : "";
    }

    /**
     * 获取服务器名称
     *
     * @return 服务器主机名
     */
    public static String getServerName() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getServerName() : "";
    }

    /**
     * 获取服务器端口
     *
     * @return 端口号
     */
    public static int getServerPort() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getServerPort() : -1;
    }

    /**
     * 获取请求协议（http 或 https）
     *
     * @return 协议名称
     */
    public static String getScheme() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getScheme() : "";
    }

    /**
     * 判断当前请求是否为 HTTPS
     *
     * @return true 表示 HTTPS
     */
    public static boolean isSecure() {
        HttpServletRequest request = getRequest();
        return request != null && request.isSecure();
    }

    /**
     * 判断当前请求是否为 AJAX 请求
     *
     * @return true 表示 AJAX 请求
     */
    public static boolean isAjaxRequest() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return false;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    /**
     * 判断当前请求是否接受 JSON
     *
     * @return true 表示 Accept 包含 application/json
     */
    public static boolean isJsonRequest() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /**
     * 构建基础 URL（scheme + host + port + contextPath）
     * <pre>
     * getBaseUrl() → "http://localhost:8080/myapp"
     * </pre>
     *
     * @return 基础 URL
     */
    public static String getBaseUrl() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (("http".equals(request.getScheme()) && port != 80)
                || ("https".equals(request.getScheme()) && port != 443)) {
            sb.append(":").append(port);
        }
        sb.append(request.getContextPath());
        return sb.toString();
    }

    public static String getRequestUri(WebRequest request) {
        if (request instanceof NativeWebRequest) {
            HttpServletRequest httpReq = ((NativeWebRequest) request).getNativeRequest(HttpServletRequest.class);
            if (httpReq != null) {
                String uri = httpReq.getRequestURI();
                String qs = httpReq.getQueryString();
                return qs != null ? uri + "?" + qs : uri;
            }
        }
        if (request != null) {
            return request.getDescription(false);
        }
        return "unknown";
    }

    // ==================== URL 编解码 ====================

    /**
     * URL 编码（默认 UTF-8）
     *
     * @param str 待编码内容
     * @return 编码后的内容
     */
    public static String urlEncode(String str) {
        return urlEncode(str, null);
    }

    /**
     * URL 编码
     *
     * @param str 待编码内容
     * @param enc 字符集（null 默认 UTF-8）
     * @return 编码后的内容
     */
    public static String urlEncode(String str, String enc) {
        if (str == null) {
            return null;
        }
        try {
            String charset = StringUtils.isObjectNotBlank(enc) ? enc : CharsetEnum.UTF_8.getValue();
            return java.net.URLEncoder.encode(str, charset);
        } catch (Exception e) {
            log.warn("URL 编码失败: {}", str, e);
            return str;
        }
    }

    /**
     * URL 解码（默认 UTF-8）
     *
     * @param str 待解码内容
     * @return 解码后的内容
     */
    public static String urlDecode(String str) {
        return urlDecode(str, null);
    }

    /**
     * URL 解码
     *
     * @param str 待解码内容
     * @param enc 字符集（null 默认 UTF-8）
     * @return 解码后的内容
     */
    public static String urlDecode(String str, String enc) {
        if (str == null) {
            return null;
        }
        try {
            String charset = StringUtils.isObjectNotBlank(enc) ? enc : CharsetEnum.UTF_8.getValue();
            return java.net.URLDecoder.decode(str, charset);
        } catch (Exception e) {
            log.warn("URL 解码失败: {}", str, e);
            return str;
        }
    }

    // ==================== 响应输出 ====================

    /**
     * 向客户端输出 JSON 字符串
     *
     * @param response HttpServletResponse
     * @param json     JSON 字符串
     */
    public static void renderJson(HttpServletResponse response, String json) {
        if (response == null) {
            log.warn("renderJson 失败：HttpServletResponse 为 null");
            return;
        }
        if (json == null) {
            json = "";
        }
        response.setContentType("application/json;charset=" + CharsetEnum.UTF_8.getValue());
        response.setCharacterEncoding(CharsetEnum.UTF_8.getValue());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            log.error("输出 JSON 失败", e);
        }
    }

    /**
     * 向客户端输出文本
     *
     * @param response HttpServletResponse
     * @param text     文本内容
     */
    public static void renderText(HttpServletResponse response, String text) {
        if (response == null) {
            log.warn("renderText 失败：HttpServletResponse 为 null");
            return;
        }
        if (text == null) {
            text = "";
        }
        response.setContentType("text/plain;charset=" + CharsetEnum.UTF_8.getValue());
        response.setCharacterEncoding(CharsetEnum.UTF_8.getValue());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            log.error("输出文本失败", e);
        }
    }

    /**
     * 向客户端重定向
     *
     * @param url 重定向目标 URL
     */
    public static void redirect(String url) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            log.warn("redirect 失败：无法获取 HttpServletResponse");
            return;
        }
        if (StringUtils.isBlank(url)) {
            return;
        }
        try {
            response.sendRedirect(url);
            if (log.isDebugEnabled()) {
                log.debug("重定向至: {}", url);
            }
        } catch (IOException e) {
            log.error("重定向失败: {}", url, e);
        }
    }
}