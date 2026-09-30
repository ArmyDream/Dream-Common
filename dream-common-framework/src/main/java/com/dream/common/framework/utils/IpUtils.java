package com.dream.common.framework.utils;

import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * IP 地址工具类（Web 扩展版）
 * <p>
 * 提供 IP 地址解析、校验、格式化、网段匹配等能力。
 * 纯 IP 校验/转换/脱敏等能力由 {@link com.dream.common.framework.utils.IpUtils} 提供。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>{@link #getIpAddress()} —— 获取客户端真实 IP（自动从当前请求获取）</li>
 *   <li>{@link #getIpAddress(HttpServletRequest)} —— 获取客户端真实 IP（支持多级代理）</li>
 *   <li>{@link #isIp(String)} / {@link #isIPv6(String)} —— IP 地址格式校验</li>
 *   <li>{@link #internalIp(String)} / {@link #isPrivateIp(String)} —— 内网/私网判断</li>
 *   <li>{@link #ip2Long(String)} / {@link #long2Ip(long)} —— IP ↔ long 互转</li>
 *   <li>{@link #isMatchedIp(String, String)} —— IP 白名单/黑名单匹配</li>
 *   <li>{@link #ipInRangeCidr(String, String)} —— CIDR 网段范围检查</li>
 *   <li>{@link #maskIp(String)} —— IP 脱敏</li>
 *   <li>{@link #getHostIp()} / {@link #getLocalIp()} —— 获取本机 IP</li>
 *   <li>{@link #getClientInfo(HttpServletRequest)} —— 获取客户端信息摘要</li>
 *   <li>{@link #isMobileRequest(HttpServletRequest)} —— 判断是否为移动端</li>
 * </ul>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings("all")
public final class IpUtils {

    // ==================== 常量 ====================

    /** 本地回环 IPv4 */
    public static final String LOCALHOST_IPV4 = "127.0.0.1";

    /** 本地回环 IPv6 */
    public static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    /** 未知 IP 标识 */
    public static final String UNKNOWN = "unknown";

    /** IPv4 地址正则（如 192.168.1.1） */
    public static final String REGX_IPV4 =
            "((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)";

    /** 带通配符的 IPv4 正则（如 192.168.*.*） */
    public static final String REGX_IPV4_WILDCARD =
            "((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d|\\*)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d|\\*)";

    /** IPv4 网段正则（如 10.0.0.1-10.0.0.255） */
    public static final String REGX_IPV4_SEGMENT = "(" + REGX_IPV4 + "\\-" + REGX_IPV4 + ")";

    /** IPv6 地址正则（简化版，匹配 8 组 4 位十六进制，支持 :: 压缩） */
    private static final Pattern PATTERN_IPV6 = Pattern.compile(
            "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|" +
                    "([0-9a-fA-F]{1,4}:){1,7}:|" +
                    "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|" +
                    "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|" +
                    "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
                    "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|" +
                    "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
                    "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|" +
                    ":((:[0-9a-fA-F]{1,4}){1,7}|:)|" +
                    "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|" +
                    "::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|" +
                    "([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))$");

    /** CIDR 正则（如 192.168.1.0/24） */
    private static final Pattern PATTERN_CIDR = Pattern.compile(
            "^(" + REGX_IPV4 + ")/([0-9]|[1-2][0-9]|3[0-2])$");

    /** HTTP 请求头中 IP 字段列表（按优先级排序） */
    public static final List<String> IP_HEADERS = Arrays.asList(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP"
    );

    private IpUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 获取客户端 IP ====================

    /**
     * 获取客户端 IP 地址（自动从当前请求获取）
     *
     * @return 客户端 IP 地址；获取失败返回 "unknown"
     */
    public static String getIpAddress() {
        return getIpAddress(ServletUtils.getRequest());
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 按优先级依次检查请求头，支持多级反向代理。
     * <p>
     * 请求头优先级：X-Forwarded-For → Proxy-Client-IP → WL-Proxy-Client-IP →
     * HTTP_CLIENT_IP → X-Real-IP → RemoteAddr
     * </p>
     *
     * <h2>格式校验</h2>
     * <p>
     * 代理头完全由客户端控制，值可能是任意字符串。解析结果必须是合法 IPv4/IPv6，
     * 否则回退 {@code RemoteAddr}——避免把伪造字符串写进访问日志，
     * 或作为缓存 / 防重复提交的 Key 组成部分（脏值会分裂或放大 Key 空间）。
     * </p>
     *
     * <h2>可伪造性提醒（使用方须知）</h2>
     * <p>
     * 只要客户端与本机之间没有可信反向代理统一改写 {@code X-Forwarded-For}，
     * 该头就可以被任意伪造（本方法无法在不了解拓扑的前提下判断真伪）。
     * 因此<b>不要把返回值当作可信来源做鉴权、风控或限流判定</b>；
     * 防重复提交建议优先使用 {@code includeUserId}，{@code includeIp} 仅在
     * 网关统一覆盖 XFF 的部署下使用。
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址；无法获取返回 "unknown"
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = null;
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (!isUnknown(ip)) {
                break;
            }
            ip = null;
        }

        // 代理头缺失，或值不是合法 IP（客户端伪造/乱填）时，回退到连接真实来源
        if (ip == null || !isValidIp(firstReverseProxyIp(ip))) {
            ip = request.getRemoteAddr();
        }

        // IPv6 本地回环转 IPv4
        if (LOCALHOST_IPV6.equals(ip)) {
            return LOCALHOST_IPV4;
        }

        String result = getMultistageReverseProxyIp(ip);
        if (log.isDebugEnabled()) {
            log.debug("客户端 IP 解析结果: {}", result);
        }
        return result;
    }

    /**
     * 取多级代理链中的第一段（最接近客户端的地址），用于格式合法性预判。
     *
     * @param ip 原始请求头值，可能是逗号分隔的代理链
     * @return 第一段地址
     */
    private static String firstReverseProxyIp(String ip) {
        int index = ip.indexOf(',');
        return index > -1 ? ip.substring(0, index).trim() : ip.trim();
    }

    // ==================== IP 格式校验 ====================

    /**
     * 判断是否为合法 IPv4 地址
     *
     * @param ip 待校验字符串
     * @return true 表示合法 IPv4
     */
    public static boolean isIp(String ip) {
        return StringUtils.isObjectNotBlank(ip) && ip.matches(REGX_IPV4);
    }

    /**
     * 判断是否为合法 IPv6 地址
     *
     * @param ip 待校验字符串
     * @return true 表示合法 IPv6
     */
    public static boolean isIPv6(String ip) {
        if (!StringUtils.isObjectNotBlank(ip)) {
            return false;
        }
        return PATTERN_IPV6.matcher(ip.trim()).matches();
    }

    /**
     * 判断是否为合法 IP 地址（IPv4 或 IPv6）
     *
     * @param ip 待校验字符串
     * @return true 表示合法 IP
     */
    public static boolean isValidIp(String ip) {
        return isIp(ip) || isIPv6(ip);
    }

    /**
     * 是否为带通配符的 IPv4 地址
     *
     * @param ip 待校验字符串
     * @return true 表示匹配通配符格式
     */
    public static boolean isIpWildcard(String ip) {
        return StringUtils.isObjectNotBlank(ip) && ip.matches(REGX_IPV4_WILDCARD);
    }

    /**
     * 是否为特定格式网段字符串（如 "10.0.0.1-10.0.0.255"）
     *
     * @param ipSeg 网段字符串
     * @return true 表示合法网段格式
     */
    public static boolean isIpSegment(String ipSeg) {
        return StringUtils.isObjectNotBlank(ipSeg) && ipSeg.matches(REGX_IPV4_SEGMENT);
    }

    /**
     * 是否为 CIDR 格式（如 "192.168.1.0/24"）
     *
     * @param cidr CIDR 字符串
     * @return true 表示合法 CIDR 格式
     */
    public static boolean isCidr(String cidr) {
        return StringUtils.isObjectNotBlank(cidr) && PATTERN_CIDR.matcher(cidr.trim()).matches();
    }

    /**
     * 检测字符串是否为 unknown 标识
     *
     * @param checkString 被检测字符串
     * @return true 表示 unknown
     */
    public static boolean isUnknown(String checkString) {
        return !StringUtils.isObjectNotBlank(checkString) || UNKNOWN.equalsIgnoreCase(checkString);
    }

    // ==================== 内网 / 私网判断 ====================

    /**
     * 判断是否为内网 IP 地址（包含回环地址）
     * <p>
     * 内网范围：
     * <ul>
     *   <li>10.0.0.0 ~ 10.255.255.255（10.0.0.0/8）</li>
     *   <li>172.16.0.0 ~ 172.31.255.255（172.16.0.0/12）</li>
     *   <li>192.168.0.0 ~ 192.168.255.255（192.168.0.0/16）</li>
     *   <li>127.0.0.1（本地回环）</li>
     *   <li>169.254.0.0 ~ 169.254.255.255（链路本地地址）</li>
     * </ul>
     *
     * @param ip IP 地址
     * @return true 表示内网 IP
     */
    public static boolean internalIp(String ip) {
        if (!StringUtils.isObjectNotBlank(ip)) {
            return false;
        }

        if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip) || "::1".equals(ip)) {
            return true;
        }

        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr);
    }

    /**
     * 判断是否为私网 IP（不含回环，仅 RFC1918 范围）
     *
     * @param ip IP 地址
     * @return true 表示私网 IP
     */
    public static boolean isPrivateIp(String ip) {
        if (!StringUtils.isObjectNotBlank(ip)) {
            return false;
        }
        byte[] addr = textToNumericFormatV4(ip);
        return isPrivateIp(addr);
    }

    /**
     * 判断是否为本地回环地址
     *
     * @param ip IP 地址
     * @return true 表示本地回环
     */
    public static boolean isLocalHost(String ip) {
        if (!StringUtils.isObjectNotBlank(ip)) {
            return false;
        }
        return LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)
                || "::1".equals(ip) || ip.startsWith("127.");
    }

    /**
     * 内部 IP 字节判断
     */
    private static boolean internalIp(byte[] addr) {
        if (addr == null || addr.length < 4) {
            return false;
        }

        final int b0 = addr[0] & 0xFF;
        final int b1 = addr[1] & 0xFF;

        // 10.0.0.0/8
        if (b0 == 10) {
            return true;
        }
        // 172.16.0.0/12
        if (b0 == 172 && b1 >= 16 && b1 <= 31) {
            return true;
        }
        // 192.168.0.0/16
        if (b0 == 192 && b1 == 168) {
            return true;
        }
        // 127.0.0.0/8 回环段
        if (b0 == 127) {
            return true;
        }
        // 169.254.0.0/16 链路本地地址
        if (b0 == 169 && b1 == 254) {
            return true;
        }
        return false;
    }

    /**
     * 私网 IP 字节判断（不含回环）
     */
    private static boolean isPrivateIp(byte[] addr) {
        if (addr == null || addr.length < 4) {
            return false;
        }

        final int b0 = addr[0] & 0xFF;
        final int b1 = addr[1] & 0xFF;

        if (b0 == 10) {
            return true;
        }
        if (b0 == 172 && b1 >= 16 && b1 <= 31) {
            return true;
        }
        if (b0 == 192 && b1 == 168) {
            return true;
        }
        return false;
    }

    // ==================== IP 解析 ====================

    /**
     * 将 IPv4 地址字符串转换成字节数组
     *
     * @param text IPv4 地址
     * @return 4 字节数组；格式无效返回 null
     */
    public static byte[] textToNumericFormatV4(String text) {
        if (!StringUtils.isObjectNotBlank(text)) {
            return null;
        }

        String[] parts = text.split("\\.");
        if (parts.length != 4) {
            return null;
        }

        try {
            byte[] bytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                int val = Integer.parseInt(parts[i]);
                if (val < 0 || val > 255) {
                    return null;
                }
                bytes[i] = (byte) val;
            }
            return bytes;
        } catch (NumberFormatException e) {
            log.warn("IP 地址解析失败: {}", text, e);
            return null;
        }
    }

    /**
     * 从多级反向代理中提取第一个非 unknown IP
     *
     * @param ip 可能包含多个 IP 的字符串（逗号分隔）
     * @return 第一个非 unknown 的 IP
     */
    public static String getMultistageReverseProxyIp(String ip) {
        if (ip == null) {
            return UNKNOWN;
        }

        ip = ip.trim();
        if (ip.contains(",")) {
            for (String subIp : ip.split(",")) {
                String trimmed = subIp.trim();
                if (!isUnknown(trimmed)) {
                    ip = trimmed;
                    break;
                }
            }
        }

        return ip.length() > 255 ? ip.substring(0, 255) : ip;
    }

    // ==================== IP ↔ long 转换 ====================

    /**
     * IPv4 地址字符串转 long
     *
     * @param ipStr IPv4 地址字符串
     * @return IP 对应的 long 值；格式无效返回 -1
     */
    public static long ip2Long(String ipStr) {
        if (!StringUtils.isObjectNotBlank(ipStr)) {
            return -1L;
        }

        String[] parts = ipStr.split("\\.");
        if (parts.length != 4) {
            return -1L;
        }

        try {
            long result = 0L;
            for (String part : parts) {
                long val = Long.parseLong(part);
                if (val < 0 || val > 255) {
                    return -1L;
                }
                result = (result << 8) | val;
            }
            return result;
        } catch (NumberFormatException e) {
            log.warn("IP 转 long 失败: {}", ipStr, e);
            return -1L;
        }
    }

    /**
     * long 值转 IPv4 地址字符串
     *
     * @param ipLong IP 对应的 long 值
     * @return IPv4 地址字符串
     */
    public static String long2Ip(long ipLong) {
        return ((ipLong >>> 24) & 0xFF) + "."
                + ((ipLong >>> 16) & 0xFF) + "."
                + ((ipLong >>> 8) & 0xFF) + "."
                + (ipLong & 0xFF);
    }

    // ==================== CIDR 网段 ====================

    /**
     * 判断 IP 是否在 CIDR 网段范围内
     *
     * @param ip   待检查的 IP 地址
     * @param cidr CIDR 网段（如 "192.168.1.0/24"）
     * @return true 表示 IP 在网段内
     */
    public static boolean ipInRangeCidr(String ip, String cidr) {
        if (!StringUtils.isObjectNotBlank(ip) || !StringUtils.isObjectNotBlank(cidr)) {
            return false;
        }

        if (!isCidr(cidr)) {
            return false;
        }

        String[] parts = cidr.split("/");
        String network = parts[0];
        int prefix;

        try {
            prefix = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("CIDR 解析失败: {}", cidr, e);
            return false;
        }

        long networkLong = ip2Long(network);
        long ipLong = ip2Long(ip);

        if (networkLong == -1L || ipLong == -1L) {
            return false;
        }

        if (prefix == 0) {
            return true;
        }

        long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        return (networkLong & mask) == (ipLong & mask);
    }

    /**
     * 获取 CIDR 网段的起始 IP
     *
     * @param cidr CIDR 网段
     * @return 起始 IP；无效返回 null
     */
    public static String getCidrStartIp(String cidr) {
        if (!isCidr(cidr)) {
            return null;
        }

        String[] parts = cidr.split("/");
        String network = parts[0];
        int prefix = Integer.parseInt(parts[1]);

        long networkLong = ip2Long(network);
        long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long startLong = networkLong & mask;

        return long2Ip(startLong);
    }

    /**
     * 获取 CIDR 网段的结束 IP
     *
     * @param cidr CIDR 网段
     * @return 结束 IP；无效返回 null
     */
    public static String getCidrEndIp(String cidr) {
        if (!isCidr(cidr)) {
            return null;
        }

        String[] parts = cidr.split("/");
        String network = parts[0];
        int prefix = Integer.parseInt(parts[1]);

        long networkLong = ip2Long(network);
        long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long startLong = networkLong & mask;
        long endLong = startLong | (~mask & 0xFFFFFFFFL);

        return long2Ip(endLong);
    }

    // ==================== 通配符 / 网段匹配 ====================

    /**
     * 检测 IP 是否匹配通配符模式
     *
     * @param wildcard 通配符模式（如 "192.168.*.*"）
     * @param ip       待检查的 IP
     * @return true 表示匹配
     */
    public static boolean ipIsInWildcard(String wildcard, String ip) {
        if (!StringUtils.isObjectNotBlank(wildcard) || !StringUtils.isObjectNotBlank(ip)) {
            return false;
        }

        String[] wParts = wildcard.split("\\.");
        String[] iParts = ip.split("\\.");

        if (wParts.length != 4 || iParts.length != 4) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            if (!"*".equals(wParts[i]) && !wParts[i].equals(iParts[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检测 IP 是否在指定网段范围内
     *
     * @param ipRange 网段范围（如 "10.0.0.1-10.0.0.255"）
     * @param ip      待检查的 IP
     * @return true 表示在范围内
     */
    public static boolean ipIsInNet(String ipRange, String ip) {
        if (!StringUtils.isObjectNotBlank(ipRange) || !StringUtils.isObjectNotBlank(ip)) {
            return false;
        }

        int idx = ipRange.indexOf('-');
        if (idx <= 0 || idx >= ipRange.length() - 1) {
            return false;
        }

        long startIp = ip2Long(ipRange.substring(0, idx).trim());
        long endIp = ip2Long(ipRange.substring(idx + 1).trim());
        long targetIp = ip2Long(ip.trim());

        if (startIp == -1L || endIp == -1L || targetIp == -1L) {
            return false;
        }

        // 自动修正顺序
        if (startIp > endIp) {
            long temp = startIp;
            startIp = endIp;
            endIp = temp;
        }

        return targetIp >= startIp && targetIp <= endIp;
    }

    /**
     * 校验 IP 是否符合过滤规则（支持精确匹配、通配符、网段、CIDR）
     * <p>
     * filter 格式：多个规则用分号 ";" 分隔
     *
     * @param filter 过滤规则列表（分号分隔）
     * @param ip     待校验 IP 地址
     * @return true 表示匹配任一规则
     */
    public static boolean isMatchedIp(String filter, String ip) {
        if (!StringUtils.isObjectNotBlank(filter) || !StringUtils.isObjectNotBlank(ip)) {
            return false;
        }

        String ipTrimmed = ip.trim();

        for (String rule : filter.split(";")) {
            String ruleTrimmed = rule.trim();
            if (!StringUtils.isObjectNotBlank(ruleTrimmed)) {
                continue;
            }

            // 精确匹配
            if (isIp(ruleTrimmed) && ruleTrimmed.equals(ipTrimmed)) {
                return true;
            }

            // CIDR 匹配
            if (isCidr(ruleTrimmed)) {
                if (ipInRangeCidr(ipTrimmed, ruleTrimmed)) {
                    return true;
                }
                continue;
            }

            // 网段匹配
            if (isIpSegment(ruleTrimmed)) {
                if (ipIsInNet(ruleTrimmed, ipTrimmed)) {
                    return true;
                }
                continue;
            }

            // 通配符匹配
            if (isIpWildcard(ruleTrimmed)) {
                if (ipIsInWildcard(ruleTrimmed, ipTrimmed)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ==================== IP 脱敏 ====================

    /**
     * IP 地址脱敏（保留前两段，后两段用 * 代替）
     * <pre>
     * maskIp("192.168.1.100") → "192.168.*.*"
     * </pre>
     *
     * @param ip IP 地址
     * @return 脱敏后的 IP
     */
    public static String maskIp(String ip) {
        return maskIp(ip, 2);
    }

    /**
     * IP 地址脱敏（自定义保留段数）
     *
     * @param ip        IP 地址
     * @param keepParts 保留的段数（1-3）
     * @return 脱敏后的 IP
     */
    public static String maskIp(String ip, int keepParts) {
        if (!StringUtils.isObjectNotBlank(ip)) {
            return ip;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4 || keepParts < 1 || keepParts > 3) {
            return ip;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                sb.append(".");
            }
            sb.append(i < keepParts ? parts[i] : "*");
        }
        return sb.toString();
    }

    // ==================== 获取本机信息 ====================

    /**
     * 获取本机主机名
     *
     * @return 主机名；获取失败返回 "未知"
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
            log.warn("获取本机主机名失败");
            return "未知";
        }
    }

    /**
     * 获取本机 IP 地址（可能返回虚拟网卡 IP）
     *
     * @return 本机 IP 地址
     */
    public static String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
            log.warn("获取本机 IP 失败，使用默认值 127.0.0.1");
            return LOCALHOST_IPV4;
        }
    }

    /**
     * 获取本机第一个非回环 IPv4 地址
     *
     * @return 本机非回环 IPv4 地址；获取失败返回 127.0.0.1
     */
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取本机真实 IP 失败", e);
        }
        return LOCALHOST_IPV4;
    }

    /**
     * 获取本机所有非回环 IPv4 地址
     *
     * @return IP 地址列表
     */
    public static List<String> getAllLocalIps() {
        List<String> ips = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取所有本机 IP 失败", e);
        }

        if (ips.isEmpty()) {
            ips.add(LOCALHOST_IPV4);
        }
        return ips;
    }

    // ==================== 客户端信息 ====================

    /**
     * 获取客户端 User-Agent
     *
     * @param request HttpServletRequest
     * @return User-Agent 字符串；不存在返回空字符串
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ua = request.getHeader("User-Agent");
        return StringUtils.isObjectNotBlank(ua) ? ua : "";
    }

    /**
     * 获取客户端信息摘要（IP + User-Agent）
     * <pre>
     * getClientInfo(request) → "192.168.1.100 | Mozilla/5.0 ..."
     * </pre>
     *
     * @param request HttpServletRequest
     * @return 客户端信息摘要
     */
    public static String getClientInfo(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = getIpAddress(request);
        String ua = getUserAgent(request);
        return ip + " | " + ua;
    }

    /**
     * 判断客户端是否为移动端浏览器
     *
     * @param request HttpServletRequest
     * @return true 表示移动端
     */
    public static boolean isMobileRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String ua = getUserAgent(request).toLowerCase();
        return ua.contains("mobile") || ua.contains("android")
                || ua.contains("iphone") || ua.contains("ipad")
                || ua.contains("windows phone");
    }
}