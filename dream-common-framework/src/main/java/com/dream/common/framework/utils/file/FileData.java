package com.dream.common.framework.utils.file;

import com.dream.common.core.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
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

    @Override
    public String toString() {
        return String.format("FileData{originalName='%s', storedName='%s', size=%s, mimeType='%s'}",
                originalName, storedName, getFormattedSize(), mimeType);
    }
}