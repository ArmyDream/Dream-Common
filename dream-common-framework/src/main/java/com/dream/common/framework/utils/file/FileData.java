package com.dream.common.framework.utils.file;

import com.dream.common.core.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件数据实体类
 * <p>
 * 封装文件上传、下载、处理过程中的所有数据信息。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>存储文件元数据（名称、大小、类型等）</li>
 *   <li>存储文件内容（字节数组或输出流）</li>
 *   <li>支持链式调用（Builder 模式）</li>
 *   <li>提供便捷的文件名生成方法</li>
 * </ul>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 使用 Builder 创建
 * FileData fileData = FileData.builder()
 *     .originalName("image.jpg")
 *     .content(bytes)
 *     .size(bytes.length)
 *     .build();
 *
 * // 2. 快速创建
 * FileData fileData = FileData.of("image.jpg", bytes);
 *
 * // 3. 生成新文件名
 * String newName = fileData.generateStoredName();
 * // => "20250115143022123_abc123.jpg"
 * }</pre>
 *
 * @author dream
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public class FileData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日期时间格式化（用于生成文件名） */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 文件原始名称（上传时的文件名） */
    private String originalName;

    /** 文件存储名称（生成的新名称） */
    private String storedName;

    /** 文件扩展名（小写，不含点号） */
    private String extension;

    /** 文件 MIME 类型 */
    private String mimeType;

    /** 文件大小（字节） */
    private Long size;

    /** 文件存储路径（不含文件名） */
    private String storagePath;

    /** 文件完整存储路径（含文件名） */
    private String fullStoragePath;

    /** 文件访问 URL */
    private String accessUrl;

    /** 文件内容（字节数组） */
    private byte[] content;

    /** 文件输出流（用于流式处理） */
    private transient ByteArrayOutputStream outputStream;

    /** 文件哈希值（用于去重） */
    private String hash;

    /** 文件是否已持久化 */
    @Builder.Default
    private boolean persisted = false;

    /** 上传者 ID */
    private String uploaderId;

    /** 上传时间 */
    @Builder.Default
    private LocalDateTime uploadTime = LocalDateTime.now();

    // ==================== 静态工厂方法 ====================

    /**
     * 快速创建 FileData 实例
     *
     * @param originalName 原始文件名
     * @param content      文件内容
     * @return FileData 实例
     */
    public static FileData of(String originalName, byte[] content) {
        return FileData.builder()
                .originalName(originalName)
                .content(content)
                .size(content != null ? (long) content.length : 0L)
                .uploadTime(LocalDateTime.now())
                .build()
                .autoFill();
    }

    /**
     * 快速创建 FileData 实例（指定文件类型）
     *
     * @param originalName 原始文件名
     * @param content      文件内容
     * @param fileType     文件类型枚举
     * @return FileData 实例
     */
    public static FileData of(String originalName, byte[] content, FileType fileType) {
        return FileData.builder()
                .originalName(originalName)
                .content(content)
                .size(content != null ? (long) content.length : 0L)
                .extension(fileType.getExtension())
                .mimeType(fileType.getMimeType())
                .uploadTime(LocalDateTime.now())
                .build();
    }

    // ==================== 便捷方法 ====================

    /**
     * 自动填充文件信息（扩展名、MIME 类型等）
     *
     * @return this
     */
    public FileData autoFill() {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            this.extension = ext;

            FileType type = FileType.fromExtension(ext);
            if (type != null) {
                this.mimeType = type.getMimeType();
            }
        }
        return this;
    }

    /**
     * 生成新的存储文件名
     * <p>
     * 格式：yyyyMMddHHmmssSSS_随机6位.扩展名
     *
     * @return 新文件名
     */
    public String generateStoredName() {
        if (this.storedName != null && !this.storedName.isEmpty()) {
            return this.storedName;
        }

        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String random = generateRandomString(6);
        String ext = extension != null ? extension : "tmp";

        this.storedName = timestamp + "_" + random + "." + ext;
        return this.storedName;
    }

    /**
     * 生成新的存储文件名（指定扩展名）
     *
     * @param extension 扩展名（不含点号）
     * @return 新文件名
     */
    public String generateStoredName(String extension) {
        this.extension = extension;
        return generateStoredName();
    }

    /**
     * 生成随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 获取文件大小（格式化输出）
     *
     * @return 格式化的大小字符串，如 "1.5 MB"
     */
    public String getFormattedSize() {
        if (size == null || size == 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = (int) (Math.log(size) / Math.log(1024));
        double value = size / Math.pow(1024, index);
        return String.format("%.2f %s", value, units[index]);
    }

    /**
     * 判断文件是否为空
     *
     * @return true 如果内容为空或大小为 0
     */
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    /**
     * 将输出流转换为字节数组
     *
     * @return this
     */
    public FileData flushToBytes() {
        if (outputStream != null) {
            this.content = outputStream.toByteArray();
            if (size == null) {
                this.size = (long) this.content.length;
            }
        }
        return this;
    }

    /**
     * 清空输出流
     *
     * @return this
     */
    public FileData clearOutputStream() {
        if (outputStream != null) {
            outputStream.reset();
        }
        return this;
    }

    /**
     * 获取文件扩展名（如果原始文件名为空则返回 null）
     *
     * @return 扩展名（小写，不含点号）
     */
    public String getExtension() {
        if (extension != null) {
            return extension;
        }
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        return null;
    }

    /**
     * 获取文件类型枚举
     *
     * @return 文件类型枚举，无法识别返回 null
     */
    public FileType getFileType() {
        String ext = getExtension();
        if (ext == null) {
            return null;
        }
        return FileType.fromExtension(ext);
    }

    /**
     * 是否为图片文件
     */
    public boolean isImage() {
        FileType type = getFileType();
        return type != null && type.isImage();
    }

    /**
     * 是否为文档文件
     */
    public boolean isDocument() {
        FileType type = getFileType();
        return type != null && type.isDocument();
    }

    /**
     * 是否为压缩包文件
     */
    public boolean isArchive() {
        FileType type = getFileType();
        return type != null && type.isArchive();
    }

    /**
     * 是否为音视频文件
     */
    public boolean isMedia() {
        FileType type = getFileType();
        return type != null && type.isMedia();
    }

    /**
     * 是否可安全预览（图片、文档等）
     */
    public boolean isPreviewable() {
        FileType type = getFileType();
        if (type == null) {
            return false;
        }
        return type.isImage() || type.isDocument() || type == FileType.TXT || type == FileType.PDF;
    }

    // ==================== 链式操作方法 ====================

    /**
     * 设置内容并自动更新大小
     *
     * @param content 文件内容
     * @return this
     */
    public FileData setContentAndSize(byte[] content) {
        this.content = content;
        this.size = content != null ? (long) content.length : 0L;
        return this;
    }

    /**
     * 设置原始文件名并自动填充信息
     *
     * @param originalName 原始文件名
     * @return this
     */
    public FileData setOriginalNameAndFill(String originalName) {
        this.originalName = originalName;
        return autoFill();
    }

    // ==================== 校验方法 ====================

    /**
     * 校验文件大小是否在允许范围内
     *
     * @param maxSize 最大字节数
     * @return true 表示文件大小合法
     */
    public boolean validateSize(long maxSize) {
        return size != null && size > 0 && size <= maxSize;
    }

    /**
     * 校验文件扩展名是否在允许列表中
     *
     * @param allowedExtensions 允许的扩展名列表（不含点号，小写）
     * @return true 表示扩展名合法
     */
    public boolean validateExtension(String... allowedExtensions) {
        String ext = getExtension();
        if (ext == null || allowedExtensions == null || allowedExtensions.length == 0) {
            return false;
        }
        for (String allowed : allowedExtensions) {
            if (allowed != null && allowed.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验文件类型是否在允许列表中
     *
     * @param allowedTypes 允许的文件类型
     * @return true 表示文件类型合法
     */
    public boolean validateFileType(FileType... allowedTypes) {
        FileType type = getFileType();
        if (type == null || allowedTypes == null || allowedTypes.length == 0) {
            return false;
        }
        for (FileType allowed : allowedTypes) {
            if (allowed == type) {
                return true;
            }
        }
        return false;
    }

    // ==================== 哈希计算 ====================

    /**
     * 计算文件内容的 MD5 哈希值并缓存到 {@link #hash}
     *
     * @return MD5 哈希字符串（32 位小写十六进制），内容为空返回 null
     */
    public String calculateMd5() {
        return calculateHash("MD5");
    }

    /**
     * 计算文件内容的 SHA-256 哈希值并缓存到 {@link #hash}
     *
     * @return SHA-256 哈希字符串（64 位小写十六进制），内容为空返回 null
     */
    public String calculateSha256() {
        return calculateHash("SHA-256");
    }

    /**
     * 计算文件内容的哈希值
     *
     * @param algorithm 哈希算法（MD5 / SHA-1 / SHA-256）
     * @return 哈希字符串（小写十六进制），内容为空返回 null
     */
    public String calculateHash(String algorithm) {
        if (content == null || content.length == 0) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            this.hash = sb.toString();
            return this.hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("calculateHash failed: algorithm={}", algorithm, e);
            return null;
        }
    }

    // ==================== 文件输出 ====================

    /**
     * 将文件内容写入输出流（不关闭流）
     *
     * @param os 输出流
     * @return this
     * @throws IOException 写入失败时抛出
     */
    public FileData writeTo(OutputStream os) throws IOException {
        if (os == null || content == null) {
            return this;
        }
        os.write(content);
        os.flush();
        return this;
    }

    /**
     * 将文件内容持久化到磁盘
     *
     * @param filePath 文件完整路径
     * @return this
     * @throws IOException 写入失败时抛出
     */
    public FileData writeTo(String filePath) throws IOException {
        if (content == null) {
            return this;
        }
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(path, content);
        this.fullStoragePath = filePath;
        this.storagePath = parent != null ? parent.toString() : null;
        this.storedName = path.getFileName().toString();
        this.persisted = true;
        return this;
    }

    /**
     * 追加内容到现有文件内容
     *
     * @param extra 额外内容
     * @return this
     */
    public FileData appendContent(byte[] extra) {
        if (extra == null || extra.length == 0) {
            return this;
        }
        if (content == null) {
            content = extra.clone();
        } else {
            byte[] merged = new byte[content.length + extra.length];
            System.arraycopy(content, 0, merged, 0, content.length);
            System.arraycopy(extra, 0, merged, content.length, extra.length);
            content = merged;
        }
        size = (long) content.length;
        return this;
    }

    /**
     * 释放资源（清空内容与输出流）
     */
    public void release() {
        if (content != null) {
            content = null;
        }
        if (outputStream != null) {
            outputStream.reset();
            outputStream = null;
        }
    }

    @Override
    public String toString() {
        return String.format("FileData{originalName='%s', storedName='%s', size=%s, mimeType='%s'}",
                originalName, storedName, getFormattedSize(), mimeType);
    }
}