package com.dream.common.core.enums;

/**
 * 网络协议枚举
 *
 */
public enum Protocol implements BaseEnum<String> {

    /**
     * HTTP
     */
    HTTP("http", "HTTP", 80),

    /**
     * HTTPS
     */
    HTTPS("https", "HTTPS", 443),

    /**
     * WebSocket
     */
    WS("ws", "WebSocket", 80),

    /**
     * WebSocket Secure
     */
    WSS("wss", "WebSocket Secure", 443),

    /**
     * FTP
     */
    FTP("ftp", "FTP", 21),

    /**
     * FTPS
     */
    FTPS("ftps", "FTPS", 990),

    /**
     * SFTP（SSH 文件传输）
     */
    SFTP("sftp", "SFTP", 22),

    /**
     * SSH
     */
    SSH("ssh", "SSH", 22),

    /**
     * Telnet
     */
    TELNET("telnet", "Telnet", 23),

    /**
     * SMTP
     */
    SMTP("smtp", "SMTP", 25),

    /**
     * SMTPS
     */
    SMTPS("smtps", "SMTPS", 465),

    /**
     * POP3
     */
    POP3("pop3", "POP3", 110),

    /**
     * POP3S
     */
    POP3S("pop3s", "POP3S", 995),

    /**
     * IMAP
     */
    IMAP("imap", "IMAP", 143),

    /**
     * IMAPS
     */
    IMAPS("imaps", "IMAPS", 993),

    /**
     * RMI
     */
    RMI("rmi", "RMI", 1099),

    /**
     * TCP
     */
    TCP("tcp", "TCP", 0),

    /**
     * UDP
     */
    UDP("udp", "UDP", 0);

    private final String value;
    private final String label;
    private final int defaultPort;

    Protocol(String value, String label, int defaultPort) {
        this.value = value;
        this.label = label;
        this.defaultPort = defaultPort;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    /**
     * 是否为安全协议
     */
    public boolean isSecure() {
        return this == HTTPS || this == WSS || this == FTPS
                || this == SMTPS || this == POP3S || this == IMAPS;
    }

    /**
     * 是否为 Web 协议
     */
    public boolean isWebProtocol() {
        return this == HTTP || this == HTTPS || this == WS || this == WSS;
    }

    /**
     * 是否为邮件协议
     */
    public boolean isMailProtocol() {
        return this == SMTP || this == SMTPS || this == POP3
                || this == POP3S || this == IMAP || this == IMAPS;
    }

    /**
     * 是否为文件传输协议
     */
    public boolean isFileTransferProtocol() {
        return this == FTP || this == FTPS || this == SFTP;
    }

    @Override
    public String getColor() {
        switch (this) {
            case HTTP:
                return "#1890FF";
            case HTTPS:
                return "#52C41A";
            case WS:
                return "#FAAD14";
            case WSS:
                return "#722ED1";
            case FTP:
            case FTPS:
            case SFTP:
                return "#FF7A45";
            case SSH:
            case TELNET:
                return "#8C8C8C";
            case SMTP:
            case SMTPS:
            case POP3:
            case POP3S:
            case IMAP:
            case IMAPS:
                return "#13C2C2";
            case RMI:
                return "#FF4D4F";
            default:
                return "#8C8C8C";
        }
    }

    public static Protocol fromValue(String value) {
        return BaseEnum.fromValue(Protocol.class, value);
    }
}