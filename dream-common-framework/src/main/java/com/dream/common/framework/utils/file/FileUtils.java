package com.dream.common.framework.utils.file;

import com.dream.common.core.enums.FileCat;
import com.dream.common.core.enums.FileType;
import com.dream.common.core.enums.StringPool;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作工具类
 * <p>
 * 基于 {@link java.nio.file} 现代 API，提供全面的文件操作能力。
 * 所有方法均做 null 安全和异常保护，操作失败时记录日志并返回安全默认值。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #getExtension(String)} / {@link #getExtension(File)} —— 文件扩展名提取</li>
 *   <li>{@link #readBytes(String)} / {@link #readString(String)} —— 文件读取</li>
 *   <li>{@link #readBytesMapped(String)} —— 大文件内存映射读取</li>
 *   <li>{@link #readClasspathBytes(String)} —— ClassPath 资源读取</li>
 *   <li>{@link #writeString(String, String)} / {@link #writeBytes(String, byte[])} —— 文件写入</li>
 *   <li>{@link #copy(String, String)} / {@link #move(String, String)} —— 文件复制/移动</li>
 *   <li>{@link #delete(String)} / {@link #deleteDirectory(String)} —— 文件/目录删除</li>
 *   <li>{@link #upload(MultipartFile, String)} —— 文件上传（返回 {@link FileData}）</li>
 *   <li>{@link #generateStoredName(String)} —— 唯一文件名生成</li>
 *   <li>{@link #detectFileType(File)} —— 基于魔数的文件类型检测</li>
 *   <li>{@link #formatSize(long)} —— 文件大小格式化</li>
 *   <li>{@link #list(String)} / {@link #listByExtension(String, String)} —— 目录列表</li>
 * </ul>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 读取文件
 * byte[] bytes = FileUtils.readBytes("/path/to/file.txt");
 * String content = FileUtils.readString("/path/to/file.txt");
 *
 * // 2. 大文件读取（内存映射）
 * byte[] data = FileUtils.readBytesMapped("/path/to/large.bin");
 *
 * // 3. 写入文件
 * FileUtils.writeString("/path/to/file.txt", "Hello World");
 * FileUtils.writeBytes("/path/to/file.bin", bytes);
 *
 * // 4. 文件上传
 * FileData fileData = FileUtils.upload(multipartFile, "/upload/path");
 *
 * // 5. 文件操作
 * FileUtils.copy("/source.txt", "/target.txt");
 * FileUtils.move("/source.txt", "/target.txt");
 * FileUtils.delete("/path/to/file.txt");
 *
 * // 6. 工具方法
 * String ext = FileUtils.getExtension("image.jpg");  // "jpg"
 * String newName = FileUtils.generateStoredName("image.jpg");  // "20250115143022_abc123.jpg"
 * boolean isImage = FileUtils.isImage("photo.jpg");
 *
 * // 7. 魔数检测
 * FileType type = FileUtils.detectFileType(new File("/path/to/image.png"));
 * // => FileType.PNG
 *
 * // 8. 目录操作
 * List<String> files = FileUtils.list("/path/to/dir");
 * List<String> txtFiles = FileUtils.listByExtension("/path/to/dir", "txt");
 * FileUtils.deleteDirectory("/path/to/dir");
 * }</pre>
 *
 * @author dream
 * @see FileData
 * @see FileType
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class FileUtils {

    /**
     * 默认缓冲区大小（8KB）
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * 文件名日期格式
     */
    private static final DateTimeFormatter FILE_NAME_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 默认字符集 UTF-8
     */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 随机文件名字符
     */
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 魔数检测所需的头部字节数（当前最长魔数为 6 字节，留足余量）
     */
    private static final int MAGIC_HEADER_SIZE = 32;

    /**
     * 客户端文件名为空或清洗后为空时的占位名
     */
    private static final String UNKNOWN_FILE_NAME = "unknown";

    /**
     * 客户端文件名保留的最大长度，防止超长名称进入日志与数据库
     */
    private static final int MAX_FILE_NAME_LENGTH = 200;

    /**
     * 控制字符（含 CR/LF/TAB/NUL），用于日志与文件名清洗
     */
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");

    /**
     * 可安全用于落盘文件名的扩展名形式
     */
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,16}");

    private FileUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== 文件类型/扩展名 ====================

    /**
     * 获取文件扩展名（不含点号，小写）
     *
     * <pre>{@code
     * String ext = FileUtils.getExtension("dream.txt");
     * // => "txt"
     * }</pre>
     *
     * @param fileName 文件名
     * @return 扩展名（小写），无扩展名返回空字符串
     */
    public static String getExtension(String fileName) {
        if (StringUtils.isObjectBlank(fileName)) {
            return StringPool.EMPTY.getValue();
        }
        int dotIndex = fileName.lastIndexOf(StringPool.DOT.getValue());
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return StringPool.EMPTY.getValue();
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 获取文件扩展名
     *
     * @param file 文件对象
     * @return 扩展名（小写），无扩展名返回空字符串
     */
    public static String getExtension(File file) {
        if (file == null) {
            return StringPool.EMPTY.getValue();
        }
        return getExtension(file.getName());
    }

    /**
     * 获取文件名（不含路径）
     *
     * <pre>{@code
     * String name = FileUtils.getFileName("/path/to/dream.txt");
     * // => "dream.txt"
     * }</pre>
     *
     * @param filePath 文件路径
     * @return 文件名，失败返回空字符串
     */
    public static String getFileName(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return StringPool.EMPTY.getValue();
        }
        try {
            return Paths.get(filePath).getFileName().toString();
        } catch (Exception e) {
            log.warn("getFileName failed: {}", filePath, e);
            return StringPool.EMPTY.getValue();
        }
    }

    /**
     * 获取文件名（不含扩展名）
     *
     * <pre>{@code
     * String name = FileUtils.getFileNameWithoutExt("dream.txt");
     * // => "dream"
     * }</pre>
     *
     * @param fileName 文件名
     * @return 不含扩展名的文件名
     */
    public static String getFileNameWithoutExt(String fileName) {
        if (StringUtils.isObjectBlank(fileName)) {
            return StringPool.EMPTY.getValue();
        }
        int dotIndex = fileName.lastIndexOf(StringPool.DOT.getValue());
        if (dotIndex < 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    /**
     * 根据扩展名获取 {@link FileType} 枚举
     *
     * @param extension 扩展名（不含点号）
     * @return FileType，无法识别返回 null
     */
    public static FileType getFileType(String extension) {
        if (StringUtils.isObjectBlank(extension)) {
            return null;
        }
        return FileType.fromExtension(extension);
    }

    /**
     * 根据文件魔数检测真实文件类型
     * <p>
     * 读取文件头部字节，与 {@link FileType} 中定义的魔数进行匹配。
     * 如果魔数无法识别，回退到扩展名检测。
     * </p>
     *
     * <pre>{@code
     * FileType type = FileUtils.detectFileType(new File("/path/to/image.jpg"));
     * // => FileType.JPEG
     * }</pre>
     *
     * @param file 文件
     * @return 检测到的文件类型，无法识别返回 null
     */
    public static FileType detectFileType(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }

        // 1. 魔数检测
        byte[] header = new byte[MAGIC_HEADER_SIZE];
        int read = 0;
        try (FileInputStream fis = new FileInputStream(file)) {
            read = fis.read(header);
        } catch (IOException e) {
            log.warn("detectFileType magic detection failed: {}", file.getName(), e);
        }
        FileType type = matchMagic(header, read);
        if (type != null) {
            log.debug("Detected file type by magic: {} -> {}", file.getName(), type);
            return type;
        }

        // 2. 回退到扩展名检测
        type = FileType.fromExtension(getExtension(file));
        if (type != null) {
            log.debug("Detected file type by extension: {} -> {}", file.getName(), type);
        }
        return type;
    }

    /**
     * 基于内存字节检测真实文件类型（仅魔数匹配，不回退扩展名）
     * <p>
     * 用于上传场景：文件还在内存里、尚未落盘，只需比对头部字节。
     * </p>
     *
     * @param data 文件内容（仅前 32 字节参与匹配）
     * @return 检测到的文件类型，无法识别返回 null
     */
    public static FileType detectFileType(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        return matchMagic(data, Math.min(data.length, MAGIC_HEADER_SIZE));
    }

    /**
     * 按魔数匹配文件类型，从长到短截取头部字节尝试
     *
     * @param header 头部字节
     * @param length 参与匹配的有效字节数
     * @return 匹配到的类型，未匹配返回 null
     */
    private static FileType matchMagic(byte[] header, int length) {
        if (header == null || length <= 0) {
            return null;
        }
        String magicHex = bytesToHex(header, length);
        for (int len = Math.min(length * 2, 16); len >= 4; len -= 2) {
            if (len > magicHex.length()) {
                continue;
            }
            FileType type = FileType.fromMagic(magicHex.substring(0, len));
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断头部字节是否以指定魔数开头
     *
     * @param header   头部字节
     * @param magicHex 十六进制魔数
     * @return true 表示内容以该魔数开头
     */
    private static boolean startsWithMagic(byte[] header, String magicHex) {
        if (header == null || StringUtils.isObjectBlank(magicHex) || magicHex.length() % 2 != 0) {
            return false;
        }
        int magicBytes = magicHex.length() / 2;
        if (header.length < magicBytes) {
            return false;
        }
        return magicHex.equalsIgnoreCase(bytesToHex(header, magicBytes));
    }

    /**
     * 读取 MultipartFile 头部字节用于内容校验
     *
     * @param file 上传文件
     * @return 头部字节，读取失败返回 null
     */
    private static byte[] readHeader(MultipartFile file) {
        byte[] header = new byte[MAGIC_HEADER_SIZE];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(header);
            return read <= 0 ? new byte[0] : Arrays.copyOf(header, read);
        } catch (IOException e) {
            log.warn("readHeader failed: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * 判断是否为图片文件
     *
     * @param fileName 文件名
     * @return true 如果是图片
     */
    public static boolean isImage(String fileName) {
        FileType type = getFileType(getExtension(fileName));
        return type != null && type.isImage();
    }

    /**
     * 判断是否为文档文件
     *
     * @param fileName 文件名
     * @return true 如果是文档
     */
    public static boolean isDocument(String fileName) {
        FileType type = getFileType(getExtension(fileName));
        return type != null && type.isDocument();
    }

    /**
     * 判断是否为压缩包文件
     *
     * @param fileName 文件名
     * @return true 如果是压缩包
     */
    public static boolean isArchive(String fileName) {
        FileType type = getFileType(getExtension(fileName));
        return type != null && type.isArchive();
    }

    /**
     * 判断是否为音视频文件
     *
     * @param fileName 文件名
     * @return true 如果是音视频
     */
    public static boolean isMedia(String fileName) {
        FileType type = getFileType(getExtension(fileName));
        return type != null && type.isMedia();
    }

    // ==================== 文件读取 ====================

    /**
     * 读取文件为字节数组
     *
     * <pre>{@code
     * byte[] data = FileUtils.readBytes("/path/to/file.txt");
     * }</pre>
     *
     * @param filePath 文件路径
     * @return 字节数组，失败返回 null
     */
    public static byte[] readBytes(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return null;
        }
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("readBytes failed: {}", filePath, e);
            return null;
        }
    }

    /**
     * 读取文件为字节数组（使用 MappedByteBuffer，适合大文件）
     * <p>
     * 使用内存映射文件的方式读取，对于大文件性能更好。
     * 如果文件超过 2GB，自动降级为普通读取。
     * </p>
     *
     * @param filePath 文件路径
     * @return 字节数组，失败返回 null
     */
    public static byte[] readBytesMapped(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return null;
        }
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r");
             FileChannel fc = raf.getChannel()) {
            long size = fc.size();
            if (size > Integer.MAX_VALUE) {
                log.warn("readBytesMapped: file too large ({}B), fallback to normal read", size);
                return readBytes(filePath);
            }
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, size).load();
            byte[] result = new byte[(int) size];
            buffer.get(result);
            return result;
        } catch (IOException e) {
            log.error("readBytesMapped failed: {}", filePath, e);
            return null;
        }
    }

    /**
     * 读取文件为字符串（UTF-8）
     *
     * <pre>{@code
     * String content = FileUtils.readString("/path/to/file.txt");
     * }</pre>
     *
     * @param filePath 文件路径
     * @return 文件内容，失败返回 null
     */
    public static String readString(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), DEFAULT_CHARSET);
        } catch (IOException e) {
            log.error("readString failed: {}", filePath, e);
            return null;
        }
    }

    /**
     * 读取文件为字符串（指定字符集）
     *
     * @param filePath 文件路径
     * @param charset  字符集
     * @return 文件内容，失败返回 null
     */
    public static String readString(String filePath, Charset charset) {
        if (StringUtils.isObjectBlank(filePath)) {
            return null;
        }
        try {
            Charset cs = charset != null ? charset : DEFAULT_CHARSET;
            return new String(Files.readAllBytes(Paths.get(filePath)), cs);
        } catch (IOException e) {
            log.error("readString failed: {}", filePath, e);
            return null;
        }
    }

    /**
     * 按行读取文件
     *
     * <pre>{@code
     * List<String> lines = FileUtils.readLines("/path/to/file.txt");
     * }</pre>
     *
     * @param filePath 文件路径
     * @return 行列表，失败返回空列表
     */
    public static List<String> readLines(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(Paths.get(filePath), DEFAULT_CHARSET);
        } catch (IOException e) {
            log.error("readLines failed: {}", filePath, e);
            return Collections.emptyList();
        }
    }

    /**
     * 读取 ClassPath 资源为字节数组
     *
     * <pre>{@code
     * byte[] data = FileUtils.readClasspathBytes("/config/app.json");
     * }</pre>
     *
     * @param resourcePath ClassPath 资源路径（如 /config/app.json）
     * @return 字节数组，失败返回 null
     */
    public static byte[] readClasspathBytes(String resourcePath) {
        if (StringUtils.isObjectBlank(resourcePath)) {
            return null;
        }
        try (InputStream is = new ClassPathResource(resourcePath).getInputStream()) {
            return readInputStream(is);
        } catch (IOException e) {
            log.error("readClasspathBytes failed: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * 读取 ClassPath 资源为字符串
     *
     * @param resourcePath ClassPath 资源路径
     * @return 资源内容字符串，失败返回 null
     */
    public static String readClasspathString(String resourcePath) {
        byte[] bytes = readClasspathBytes(resourcePath);
        if (bytes == null) {
            return null;
        }
        return new String(bytes, DEFAULT_CHARSET);
    }

    /**
     * 读取输入流为字节数组
     *
     * @param inputStream 输入流（调用后流会被关闭）
     * @return 字节数组，失败返回 null
     */
    public static byte[] readInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("readInputStream failed", e);
            return null;
        }
    }

    // ==================== 文件写入 ====================

    /**
     * 写入字符串到文件（覆盖）
     *
     * <pre>{@code
     * boolean success = FileUtils.writeString("/path/to/file.txt", "Hello World");
     * }</pre>
     *
     * @param filePath 文件路径
     * @param content  写入内容
     * @return 是否成功
     */
    public static boolean writeString(String filePath, String content) {
        return writeString(filePath, content, false);
    }

    /**
     * 写入字符串到文件
     *
     * @param filePath 文件路径
     * @param content  写入内容
     * @param append   是否追加
     * @return 是否成功
     */
    public static boolean writeString(String filePath, String content, boolean append) {
        if (StringUtils.isObjectBlank(filePath)) {
            return false;
        }
        try {
            ensureParentDirectory(filePath);
            byte[] bytes = (content != null ? content : "")
                    .getBytes(DEFAULT_CHARSET);
            if (append) {
                Files.write(Paths.get(filePath), bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.write(Paths.get(filePath), bytes);
            }
            log.debug("writeString: {} (append={})", filePath, append);
            return true;
        } catch (IOException e) {
            log.error("writeString failed: {}", filePath, e);
            return false;
        }
    }

    /**
     * 写入字节数组到文件（覆盖）
     *
     * <pre>{@code
     * boolean success = FileUtils.writeBytes("/path/to/file.bin", data);
     * }</pre>
     *
     * @param filePath 文件路径
     * @param data     字节数组
     * @return 是否成功
     */
    public static boolean writeBytes(String filePath, byte[] data) {
        if (StringUtils.isObjectBlank(filePath) || data == null) {
            return false;
        }
        try {
            ensureParentDirectory(filePath);
            Files.write(Paths.get(filePath), data);
            log.debug("writeBytes: {}, size: {} bytes", filePath, data.length);
            return true;
        } catch (IOException e) {
            log.error("writeBytes failed: {}", filePath, e);
            return false;
        }
    }

    /**
     * 追加字节数组到文件
     *
     * @param filePath 文件路径
     * @param data     字节数组
     * @return 是否成功
     */
    public static boolean appendBytes(String filePath, byte[] data) {
        if (StringUtils.isObjectBlank(filePath) || data == null) {
            return false;
        }
        try {
            ensureParentDirectory(filePath);
            Files.write(Paths.get(filePath), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.debug("appendBytes: {}, size: {} bytes", filePath, data.length);
            return true;
        } catch (IOException e) {
            log.error("appendBytes failed: {}", filePath, e);
            return false;
        }
    }

    // ==================== 文件复制/移动 ====================

    /**
     * 复制文件
     *
     * <pre>{@code
     * boolean success = FileUtils.copy("/path/to/source.txt", "/path/to/target.txt");
     * }</pre>
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否成功
     */
    public static boolean copy(String sourcePath, String targetPath) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(targetPath)) {
            return false;
        }
        try {
            ensureParentDirectory(targetPath);
            Files.copy(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
            log.debug("copy: {} -> {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            log.error("copy failed: {} -> {}", sourcePath, targetPath, e);
            return false;
        }
    }

    /**
     * 移动/重命名文件
     *
     * <pre>{@code
     * boolean success = FileUtils.move("/path/to/old.txt", "/path/to/new.txt");
     * }</pre>
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否成功
     */
    public static boolean move(String sourcePath, String targetPath) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(targetPath)) {
            return false;
        }
        try {
            ensureParentDirectory(targetPath);
            Files.move(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
            log.debug("move: {} -> {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            log.error("move failed: {} -> {}", sourcePath, targetPath, e);
            return false;
        }
    }

    /**
     * 重命名文件
     *
     * <pre>{@code
     * boolean success = FileUtils.rename("/path/to/old.txt", "new.txt");
     * }</pre>
     *
     * @param filePath 文件路径
     * @param newName  新文件名
     * @return 是否成功
     */
    public static boolean rename(String filePath, String newName) {
        if (StringUtils.isObjectBlank(filePath) || StringUtils.isObjectBlank(newName)) {
            return false;
        }
        try {
            Path source = Paths.get(filePath);
            Path target = source.getParent().resolve(newName);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("rename: {} -> {}", filePath, newName);
            return true;
        } catch (IOException e) {
            log.error("rename failed: {} -> {}", filePath, newName, e);
            return false;
        }
    }

    // ==================== 文件删除 ====================

    /**
     * 删除文件
     *
     * <pre>{@code
     * boolean deleted = FileUtils.delete("/path/to/file.txt");
     * }</pre>
     *
     * @param filePath 文件路径
     * @return 是否成功
     */
    public static boolean delete(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return false;
        }
        try {
            boolean result = Files.deleteIfExists(Paths.get(filePath));
            if (result) {
                log.debug("delete: {}", filePath);
            }
            return result;
        } catch (IOException e) {
            log.error("delete failed: {}", filePath, e);
            return false;
        }
    }

    /**
     * 删除目录及其所有内容（递归）
     * <p>
     * 会递归删除目录下所有文件和子目录。
     *
     * <pre>{@code
     * boolean success = FileUtils.deleteDirectory("/path/to/dir");
     * }</pre>
     *
     * @param dirPath 目录路径
     * @return 是否成功
     */
    public static boolean deleteDirectory(String dirPath) {
        if (StringUtils.isObjectBlank(dirPath)) {
            return false;
        }
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) {
            return false;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("deleteDirectory: {}", dirPath);
            return true;
        } catch (IOException e) {
            log.error("deleteDirectory failed: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 清空目录（保留目录本身）
     *
     * @param dirPath 目录路径
     * @return 是否成功
     */
    public static boolean cleanDirectory(String dirPath) {
        if (StringUtils.isObjectBlank(dirPath)) {
            return false;
        }
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> paths = stream.collect(Collectors.toList());
            for (Path path : paths) {
                if (Files.isDirectory(path)) {
                    deleteDirectory(path.toString());
                } else {
                    Files.deleteIfExists(path);
                }
            }
            log.debug("cleanDirectory: {}", dirPath);
            return true;
        } catch (IOException e) {
            log.error("cleanDirectory failed: {}", dirPath, e);
            return false;
        }
    }

    // ==================== 目录操作 ====================

    /**
     * 创建目录（含父目录）
     *
     * <pre>{@code
     * boolean success = FileUtils.mkdirs("/path/to/dir");
     * }</pre>
     *
     * @param dirPath 目录路径
     * @return 是否成功
     */
    public static boolean mkdirs(String dirPath) {
        if (StringUtils.isObjectBlank(dirPath)) {
            return false;
        }
        try {
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.debug("mkdirs: {}", dirPath);
            }
            return true;
        } catch (IOException e) {
            log.error("mkdirs failed: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 列出目录下的文件和子目录
     *
     * <pre>{@code
     * List<String> files = FileUtils.list("/path/to/dir");
     * }</pre>
     *
     * @param dirPath 目录路径
     * @return 文件名列表，失败返回空列表
     */
    public static List<String> list(String dirPath) {
        if (StringUtils.isObjectBlank(dirPath)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(Paths.get(dirPath))) {
            return stream
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("list failed: {}", dirPath, e);
            return Collections.emptyList();
        }
    }

    /**
     * 列出目录下指定扩展名的文件
     *
     * <pre>{@code
     * List<String> txtFiles = FileUtils.listByExtension("/path/to/dir", "txt");
     * }</pre>
     *
     * @param dirPath   目录路径
     * @param extension 扩展名（不含点号）
     * @return 匹配的文件名列表，失败返回空列表
     */
    public static List<String> listByExtension(String dirPath, String extension) {
        if (StringUtils.isObjectBlank(dirPath) || StringUtils.isObjectBlank(extension)) {
            return Collections.emptyList();
        }
        String lowerExt = extension.toLowerCase();
        try (Stream<Path> stream = Files.list(Paths.get(dirPath))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        int dotIndex = name.lastIndexOf('.');
                        if (dotIndex < 0) {
                            return false;
                        }
                        return name.substring(dotIndex + 1).toLowerCase().equals(lowerExt);
                    })
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("listByExtension failed: {}", dirPath, e);
            return Collections.emptyList();
        }
    }

    // ==================== 文件信息 ====================

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean exists(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return false;
        }
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 判断是否为目录
     *
     * @param path 路径
     * @return 是否为目录
     */
    public static boolean isDirectory(String path) {
        if (StringUtils.isObjectBlank(path)) {
            return false;
        }
        return Files.isDirectory(Paths.get(path));
    }

    /**
     * 判断是否为文件
     *
     * @param path 路径
     * @return 是否为文件
     */
    public static boolean isFile(String path) {
        if (StringUtils.isObjectBlank(path)) {
            return false;
        }
        return Files.isRegularFile(Paths.get(path));
    }

    /**
     * 获取文件大小（字节）
     *
     * @param filePath 文件路径
     * @return 文件大小，失败返回 -1
     */
    public static long getSize(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return -1;
        }
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.error("getSize failed: {}", filePath, e);
            return -1;
        }
    }

    /**
     * 获取文件最后修改时间
     *
     * @param filePath 文件路径
     * @return 最后修改时间戳（毫秒），失败返回 -1
     */
    public static long getLastModified(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return -1;
        }
        try {
            return Files.getLastModifiedTime(Paths.get(filePath)).toMillis();
        } catch (IOException e) {
            log.error("getLastModified failed: {}", filePath, e);
            return -1;
        }
    }

    /**
     * 格式化文件大小
     *
     * <pre>{@code
     * String size = FileUtils.formatSize(1536);
     * // => "1.50 KB"
     * }</pre>
     *
     * @param size 字节数
     * @return 格式化后的大小字符串
     */
    public static String formatSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = (int) (Math.log(size) / Math.log(1024));
        if (index >= units.length) {
            index = units.length - 1;
        }
        double value = size / Math.pow(1024, index);
        return String.format("%.2f %s", value, units[index]);
    }

    // ==================== 文件上传 ====================

    /**
     * 上传 MultipartFile 到磁盘
     * <p>
     * 自动生成唯一文件名，防止覆盖；文件名会经 {@link #sanitizeFileName(String)} 清洗。
     * 返回 {@link FileData} 包含文件元数据。
     * </p>
     * <p>
     * <b>安全提示</b>：本重载不做类型白名单校验，仅适用于可信来源。
     * 面向终端用户的上传请使用 {@link #upload(MultipartFile, String, FileType...)}
     * 或 {@link #upload(MultipartFile, String, FileCat...)}，它们会同时校验扩展名与文件内容魔数。
     * </p>
     *
     * <pre>{@code
     * FileData data = FileUtils.upload(multipartFile, "/uploads/images");
     * }</pre>
     *
     * @param file     上传文件
     * @param savePath 保存目录（不含文件名，不存在会自动创建）
     * @return FileData 文件数据实体，失败返回 null
     */
    public static FileData upload(MultipartFile file, String savePath) {
        if (file == null || file.isEmpty()) {
            log.warn("upload: file is null or empty");
            return null;
        }
        if (StringUtils.isObjectBlank(savePath)) {
            log.warn("upload: savePath is empty");
            return null;
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
        try {
            String extension = getExtension(originalName);
            String storedName = generateStoredName(originalName);

            // 确保目录存在
            if (!mkdirs(savePath)) {
                log.error("upload: failed to create directory: {}", savePath);
                return null;
            }

            String fullPath = savePath + File.separator + storedName;
            Path targetPath = Paths.get(fullPath);
            file.transferTo(targetPath.toFile());

            FileData fileData = FileData.builder()
                    .originalName(originalName)
                    .storedName(storedName)
                    .extension(extension)
                    .mimeType(resolveMimeType(extension, file.getContentType()))
                    .size(file.getSize())
                    .storagePath(savePath)
                    .fullStoragePath(fullPath)
                    .uploadTime(LocalDateTime.now())
                    .persisted(true)
                    .build();

            log.info("upload: {} -> {}, size: {}", originalName, storedName, file.getSize());
            return fileData;
        } catch (IOException e) {
            log.error("upload failed: {}", originalName, e);
            return null;
        }
    }

    /**
     * 确定写入元数据的 MIME 类型
     * <p>
     * {@code MultipartFile#getContentType()} 完全由客户端控制，若原样落库并在下载时回显，
     * 攻击者可让浏览器把上传内容当作 HTML/脚本执行。因此当扩展名能匹配到已知
     * {@link FileType} 时以注册表中的 MIME 为准，仅在无法识别时回退到客户端值。
     * </p>
     *
     * @param extension         扩展名（不含点号）
     * @param clientContentType 客户端提交的 Content-Type
     * @return 可信 MIME 类型；无法识别时回退客户端值，回退值仍为空则返回 {@code application/octet-stream}
     */
    private static String resolveMimeType(String extension, String clientContentType) {
        FileType type = FileType.fromExtension(extension);
        if (type != null && StringUtils.isObjectNotBlank(type.getMimeType())) {
            if (StringUtils.isObjectNotBlank(clientContentType)
                    && !type.getMimeType().equalsIgnoreCase(clientContentType.trim())) {
                log.warn("upload: client Content-Type [{}] overridden by [{}] for extension [{}]",
                        clientContentType, type.getMimeType(), extension);
            }
            return type.getMimeType();
        }
        if (StringUtils.isObjectNotBlank(clientContentType)) {
            return clientContentType.trim();
        }
        return "application/octet-stream";
    }

    /**
     * 上传字节数组到磁盘
     * <p>
     * 文件名会经 {@link #sanitizeFileName(String)} 清洗。本重载不做类型白名单校验，
     * 面向终端用户的上传请使用 {@link #upload(byte[], String, String, FileType...)}。
     * </p>
     *
     * @param data     文件内容
     * @param fileName 原始文件名
     * @param savePath 保存目录
     * @return FileData 文件数据实体，失败返回 null
     */
    public static FileData upload(byte[] data, String fileName, String savePath) {
        if (data == null || data.length == 0) {
            log.warn("upload: data is null or empty");
            return null;
        }
        if (StringUtils.isObjectBlank(savePath) || StringUtils.isObjectBlank(fileName)) {
            log.warn("upload: savePath or fileName is empty");
            return null;
        }

        try {
            String safeName = sanitizeFileName(fileName);
            String extension = getExtension(safeName);
            String storedName = generateStoredName(safeName);

            if (!mkdirs(savePath)) {
                log.error("upload: failed to create directory: {}", savePath);
                return null;
            }

            String fullPath = savePath + File.separator + storedName;
            Files.write(Paths.get(fullPath), data);

            FileData fileData = FileData.builder()
                    .originalName(safeName)
                    .storedName(storedName)
                    .extension(extension)
                    .mimeType(resolveMimeType(extension, null))
                    .size((long) data.length)
                    .storagePath(savePath)
                    .fullStoragePath(fullPath)
                    .uploadTime(LocalDateTime.now())
                    .persisted(true)
                    .build();

            log.info("upload (bytes): {} -> {}, size: {}", safeName, storedName, data.length);
            return fileData;
        } catch (IOException e) {
            log.error("upload (bytes) failed: {}", sanitizeFileName(fileName), e);
            return null;
        }
    }

    /**
     * 清洗客户端提供的文件名
     * <p>
     * 客户端可完全控制 {@code MultipartFile#getOriginalFilename()}，其中可能包含
     * 目录分隔符（{@code ../../} 或绝对路径）、CR/LF 等控制字符以及超长内容。
     * 本方法只保留最后一段名称、剔除控制字符并截断超长部分，用于：
     * <ul>
     *   <li>避免文件名参与路径拼接时发生目录穿越；</li>
     *   <li>避免文件名写入日志时伪造日志行（log forging）；</li>
     *   <li>避免元数据表存储超长或含控制字符的名称。</li>
     * </ul>
     * 注意：本方法不改变落盘名，落盘名始终由 {@link #generateStoredName(String)} 生成。
     * </p>
     *
     * <pre>{@code
     * FileUtils.sanitizeFileName("..\\..\\WEB-INF\\shell.jsp");  // => "shell.jsp"
     * FileUtils.sanitizeFileName("a\r\nb.txt");                   // => "ab.txt"
     * FileUtils.sanitizeFileName(null);                           // => "unknown"
     * }</pre>
     *
     * @param fileName 客户端文件名，可为 null
     * @return 清洗后的文件名，入参为空或清洗后为空时返回 {@code "unknown"}
     */
    public static String sanitizeFileName(String fileName) {
        if (StringUtils.isObjectBlank(fileName)) {
            return UNKNOWN_FILE_NAME;
        }
        String name = fileName.replace('\\', '/');
        int slashIndex = name.lastIndexOf('/');
        if (slashIndex >= 0) {
            name = name.substring(slashIndex + 1);
        }
        name = CONTROL_CHARS.matcher(name).replaceAll("").trim();
        if (name.isEmpty()) {
            return UNKNOWN_FILE_NAME;
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(0, MAX_FILE_NAME_LENGTH) : name;
    }

    // ==================== 文件名生成 ====================

    /**
     * 生成存储文件名
     * <p>
     * 格式：yyyyMMddHHmmssSSS_随机8位.扩展名
     * </p>
     * <p>
     * 扩展名只保留 {@code [a-z0-9]} 且不超过 16 位的形式，其它字符（路径分隔符、
     * 空白、shell 元字符等）一律丢弃，扩展名不合法时按无扩展名存储。
     * </p>
     *
     * <pre>{@code
     * String name = FileUtils.generateStoredName("photo.jpg");
     * // => "20250115143022123_abc12345.jpg"
     * }</pre>
     *
     * @param originalName 原始文件名
     * @return 唯一文件名
     */
    public static String generateStoredName(String originalName) {
        String timestamp = LocalDateTime.now().format(FILE_NAME_DATE_FMT);
        String random = randomString(8);
        String extension = safeStoredExtension(getExtension(originalName));

        if (StringUtils.isObjectBlank(extension)) {
            return timestamp + "_" + random;
        }
        return timestamp + "_" + random + StringPool.DOT.getValue() + extension;
    }

    /**
     * 生成存储文件名（指定扩展名）
     *
     * @param extension 扩展名（不含点号）
     * @return 唯一文件名
     */
    public static String generateStoredNameWithExt(String extension) {
        String timestamp = LocalDateTime.now().format(FILE_NAME_DATE_FMT);
        String random = randomString(8);
        String normalized = StringUtils.isObjectBlank(extension)
                ? StringPool.EMPTY.getValue()
                : extension.trim().toLowerCase();
        if (normalized.startsWith(StringPool.DOT.getValue())) {
            normalized = normalized.substring(1);
        }
        String safeExt = safeStoredExtension(normalized);
        if (StringUtils.isObjectBlank(safeExt)) {
            return timestamp + "_" + random;
        }
        return timestamp + "_" + random + StringPool.DOT.getValue() + safeExt;
    }

    /**
     * 过滤出可安全写入磁盘的扩展名
     *
     * @param extension 小写扩展名（不含点号）
     * @return 合法扩展名；不合法或为空时返回空字符串
     */
    private static String safeStoredExtension(String extension) {
        if (StringUtils.isObjectBlank(extension) || !SAFE_EXTENSION.matcher(extension).matches()) {
            return StringPool.EMPTY.getValue();
        }
        return extension;
    }

    // ==================== 文件上传（带类型限制） ====================

    /**
     * 上传文件到磁盘（带允许的文件类型限制）
     * <p>
     * 先校验扩展名是否在允许列表中，再校验文件内容魔数，两者都通过后执行上传。
     * </p>
     *
     * <pre>{@code
     * // 只允许 JPEG 和 PNG
     * FileData data = FileUtils.upload(file, "/uploads/images", FileType.JPEG, FileType.PNG);
     * }</pre>
     *
     * @param file         上传文件
     * @param savePath     保存目录
     * @param allowedTypes 允许的文件类型
     * @return FileData，校验失败或上传失败返回 null
     */
    public static FileData upload(MultipartFile file, String savePath, FileType... allowedTypes) {
        if (!validateFileType(file, allowedTypes)) {
            log.warn("upload: file type not allowed: {}",
                    file == null ? UNKNOWN_FILE_NAME : sanitizeFileName(file.getOriginalFilename()));
            return null;
        }
        if (!validateFileContent(file, allowedTypes)) {
            return null;
        }
        return upload(file, savePath);
    }

    /**
     * 上传文件到磁盘（带允许的文件分类限制）
     * <p>
     * 先校验扩展名是否属于允许的分类，再按该分类下所有类型的魔数校验内容，通过后执行上传。
     * </p>
     *
     * <pre>{@code
     * // 只允许图片和文档
     * FileData data = FileUtils.upload(file, "/uploads/files", FileCat.IMAGE, FileCat.DOCUMENT);
     * }</pre>
     *
     * @param file          上传文件
     * @param savePath      保存目录
     * @param allowedCats   允许的文件分类
     * @return FileData，校验失败或上传失败返回 null
     */
    public static FileData upload(MultipartFile file, String savePath, FileCat... allowedCats) {
        if (!validateFileType(file, allowedCats)) {
            log.warn("upload: file category not allowed: {}",
                    file == null ? UNKNOWN_FILE_NAME : sanitizeFileName(file.getOriginalFilename()));
            return null;
        }
        if (!validateFileContent(file, typesOf(allowedCats))) {
            return null;
        }
        return upload(file, savePath);
    }

    /**
     * 上传字节数组到磁盘（带允许的文件类型限制）
     *
     * @param data         文件内容
     * @param fileName     原始文件名
     * @param savePath     保存目录
     * @param allowedTypes 允许的文件类型
     * @return FileData，校验失败或上传失败返回 null
     */
    public static FileData upload(byte[] data, String fileName, String savePath, FileType... allowedTypes) {
        if (!validateFileType(fileName, allowedTypes)) {
            log.warn("upload: file type not allowed: {}", sanitizeFileName(fileName));
            return null;
        }
        if (!validateFileContent(fileName, data, allowedTypes)) {
            return null;
        }
        return upload(data, fileName, savePath);
    }

    /**
     * 上传字节数组到磁盘（带允许的文件分类限制）
     *
     * @param data         文件内容
     * @param fileName     原始文件名
     * @param savePath     保存目录
     * @param allowedCats  允许的文件分类
     * @return FileData，校验失败或上传失败返回 null
     */
    public static FileData upload(byte[] data, String fileName, String savePath, FileCat... allowedCats) {
        if (!validateFileType(fileName, allowedCats)) {
            log.warn("upload: file category not allowed: {}", sanitizeFileName(fileName));
            return null;
        }
        if (!validateFileContent(fileName, data, typesOf(allowedCats))) {
            return null;
        }
        return upload(data, fileName, savePath);
    }

    // ==================== 文件类型校验 ====================

    /**
     * 校验文件类型（基于 MultipartFile）
     *
     * <pre>{@code
     * boolean valid = FileUtils.validateFileType(file, FileType.JPEG, FileType.PNG);
     * }</pre>
     *
     * @param file         上传文件
     * @param allowedTypes 允许的文件类型
     * @return true 如果文件类型在允许列表中
     */
    public static boolean validateFileType(MultipartFile file, FileType... allowedTypes) {
        if (file == null || file.isEmpty() || allowedTypes == null || allowedTypes.length == 0) {
            return false;
        }
        String fileName = file.getOriginalFilename();
        return validateFileType(fileName, allowedTypes);
    }

    /**
     * 校验文件类型（基于文件名）
     *
     * @param fileName     文件名
     * @param allowedTypes 允许的文件类型
     * @return true 如果文件类型在允许列表中
     */
    public static boolean validateFileType(String fileName, FileType... allowedTypes) {
        if (StringUtils.isObjectBlank(fileName) || allowedTypes == null || allowedTypes.length == 0) {
            return false;
        }
        String extension = getExtension(fileName);
        if (StringUtils.isObjectBlank(extension)) {
            return false;
        }
        FileType actualType = FileType.fromExtension(extension);
        if (actualType == null) {
            return false;
        }
        for (FileType type : allowedTypes) {
            if (type == actualType) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验文件分类（基于 MultipartFile）
     *
     * <pre>{@code
     * boolean valid = FileUtils.validateFileType(file, FileCat.IMAGE, FileCat.DOCUMENT);
     * }</pre>
     *
     * @param file        上传文件
     * @param allowedCats 允许的文件分类
     * @return true 如果文件分类在允许列表中
     */
    public static boolean validateFileType(MultipartFile file, FileCat... allowedCats) {
        if (file == null || file.isEmpty() || allowedCats == null || allowedCats.length == 0) {
            return false;
        }
        String fileName = file.getOriginalFilename();
        return validateFileType(fileName, allowedCats);
    }

    /**
     * 校验文件分类（基于文件名）
     *
     * @param fileName    文件名
     * @param allowedCats 允许的文件分类
     * @return true 如果文件分类在允许列表中
     */
    public static boolean validateFileType(String fileName, FileCat... allowedCats) {
        if (StringUtils.isObjectBlank(fileName) || allowedCats == null || allowedCats.length == 0) {
            return false;
        }
        String extension = getExtension(fileName);
        if (StringUtils.isObjectBlank(extension)) {
            return false;
        }
        FileType actualType = FileType.fromExtension(extension);
        if (actualType == null) {
            return false;
        }
        FileCat actualCat = actualType.getCategory();
        if (actualCat == null) {
            return false;
        }
        for (FileCat cat : allowedCats) {
            if (cat == actualCat) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验文件类型（基于 File 对象，扩展名白名单 + 内容魔数）
     * <p>
     * 先校验扩展名是否在允许列表中，再用文件头魔数复核内容，两步都通过才算合法，
     * 防止把其它二进制格式改个扩展名后冒充允许类型。
     * </p>
     *
     * <pre>{@code
     * boolean valid = FileUtils.validateFileType(file, FileType.JPEG, FileType.PNG);
     * }</pre>
     *
     * @param file         文件对象
     * @param allowedTypes 允许的文件类型
     * @return true 如果文件类型在允许列表中且内容与之一致
     */
    public static boolean validateFileType(File file, FileType... allowedTypes) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        if (allowedTypes == null || allowedTypes.length == 0) {
            return false;
        }
        if (!validateFileType(file.getName(), allowedTypes)) {
            return false;
        }
        byte[] header = readHeader(file);
        if (header == null) {
            return false;
        }
        return contentMatchesAllowed(file.getName(), header, allowedTypes);
    }

    // ==================== 文件内容（魔数）校验 ====================

    /**
     * 校验上传文件内容与允许类型是否一致（防扩展名伪造）
     * <p>
     * 与 {@link #validateFileType(MultipartFile, FileType...)} 配对使用：前者只看客户端
     * 提供的文件名，后者读取文件头字节复核真实格式。规则：
     * <ul>
     *   <li>魔数可识别且命中允许类型（或允许类型与其共用同一魔数，如 doc/xls/ppt）→ 通过；</li>
     *   <li>魔数可识别但不在允许类型内 → 拒绝，说明扩展名是伪造的；</li>
     *   <li>魔数不可识别（txt/json/css/svg 等无固定魔数格式）→ 不阻断，由扩展名白名单负责；</li>
     *   <li>读取文件头失败 → 拒绝（fail-closed）。</li>
     * </ul>
     * </p>
     *
     * <pre>{@code
     * // 扩展名叫 photo.jpg，内容却是 ZIP（Word 文档包）→ false
     * boolean ok = FileUtils.validateFileContent(file, FileType.JPEG, FileType.PNG);
     * }</pre>
     *
     * @param file         上传文件
     * @param allowedTypes 允许的文件类型
     * @return true 表示内容与允许类型不冲突
     */
    public static boolean validateFileContent(MultipartFile file, FileType... allowedTypes) {
        if (file == null || file.isEmpty() || allowedTypes == null || allowedTypes.length == 0) {
            return false;
        }
        byte[] header = readHeader(file);
        if (header == null) {
            return false;
        }
        return contentMatchesAllowed(file.getOriginalFilename(), header, allowedTypes);
    }

    /**
     * 校验字节内容与允许类型是否一致（防扩展名伪造）
     *
     * @param fileName     文件名，仅用于日志
     * @param data         文件内容
     * @param allowedTypes 允许的文件类型
     * @return true 表示内容与允许类型不冲突
     */
    public static boolean validateFileContent(String fileName, byte[] data, FileType... allowedTypes) {
        if (data == null || data.length == 0 || allowedTypes == null || allowedTypes.length == 0) {
            return false;
        }
        return contentMatchesAllowed(fileName, data, allowedTypes);
    }

    /**
     * 用魔数复核内容是否与允许类型一致
     *
     * @param fileName     文件名，仅用于日志
     * @param header       文件头字节
     * @param allowedTypes 允许的文件类型
     * @return true 表示内容不冲突
     */
    private static boolean contentMatchesAllowed(String fileName, byte[] header, FileType... allowedTypes) {
        FileType detected = detectFileType(header);
        if (detected == null) {
            return true;
        }
        if (magicAllowed(detected, allowedTypes)) {
            return true;
        }
        log.warn("validateFileContent: content is [{}] but not allowed, file: {}",
                detected, sanitizeFileName(fileName));
        return false;
    }

    /**
     * 判断检测出的类型是否被允许（同魔数的不同扩展名互相认可）
     *
     * @param detected     魔数检测出的类型
     * @param allowedTypes 允许的文件类型
     * @return true 表示允许
     */
    private static boolean magicAllowed(FileType detected, FileType... allowedTypes) {
        String detectedMagic = detected.getMagicHex();
        for (FileType type : allowedTypes) {
            if (type == null) {
                continue;
            }
            if (type == detected) {
                return true;
            }
            String magic = type.getMagicHex();
            if (StringUtils.isObjectNotBlank(magic) && magic.equalsIgnoreCase(detectedMagic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 展开文件分类下的所有文件类型
     *
     * @param cats 文件分类
     * @return 类型数组，分类为空时返回空数组
     */
    private static FileType[] typesOf(FileCat... cats) {
        if (cats == null || cats.length == 0) {
            return new FileType[0];
        }
        List<FileType> types = new ArrayList<>();
        for (FileCat cat : cats) {
            if (cat != null) {
                types.addAll(FileType.getTypesByCategory(cat));
            }
        }
        return types.toArray(new FileType[0]);
    }

    /**
     * 读取本地文件头部字节
     *
     * @param file 文件对象
     * @return 头部字节，读取失败返回 null
     */
    private static byte[] readHeader(File file) {
        byte[] header = new byte[MAGIC_HEADER_SIZE];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(header);
            return read <= 0 ? new byte[0] : Arrays.copyOf(header, read);
        } catch (IOException e) {
            log.warn("readHeader failed: {}", file.getName(), e);
            return null;
        }
    }

    // ==================== 获取允许类型描述 ====================

    /**
     * 获取允许类型的描述字符串（用于提示用户）
     *
     * <pre>{@code
     * String desc = FileUtils.getAllowedTypesDescription(FileType.JPEG, FileType.PNG);
     * // => "JPEG图片、PNG图片"
     * }</pre>
     *
     * @param types 文件类型
     * @return 描述字符串
     */
    public static String getAllowedTypesDescription(FileType... types) {
        if (types == null || types.length == 0) {
            return "无限制";
        }
        return Arrays.stream(types)
                .map(FileType::getLabel)
                .collect(Collectors.joining("、"));
    }

    /**
     * 获取允许分类的描述字符串
     *
     * @param cats 文件分类
     * @return 描述字符串
     */
    public static String getAllowedCategoriesDescription(FileCat... cats) {
        if (cats == null || cats.length == 0) {
            return "无限制";
        }
        return Arrays.stream(cats)
                .map(FileCat::getLabel)
                .collect(Collectors.joining("、"));
    }

    // ==================== 文件下载 ====================

    /**
     * 将本地文件作为附件下载到客户端
     * <p>
     * 设置 Content-Disposition 为 attachment，浏览器会弹出下载框。
     * </p>
     *
     * @param response  HttpServletResponse
     * @param filePath  本地文件完整路径
     * @param downloadName 下载显示的文件名（为 null 则使用原文件名）
     * @return 是否成功
     */
    public static boolean download(HttpServletResponse response, String filePath, String downloadName) {
        if (response == null || StringUtils.isObjectBlank(filePath)) {
            return false;
        }
        Path path = Paths.get(filePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("download: file not found: {}", filePath);
            return false;
        }
        String fileName = StringUtils.isObjectBlank(downloadName)
                ? path.getFileName().toString()
                : downloadName;
        try {
            String encoded = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            response.setContentLengthLong(Files.size(path));
            Files.copy(path, response.getOutputStream());
            response.getOutputStream().flush();
            log.info("download: {} -> {}", filePath, fileName);
            return true;
        } catch (IOException e) {
            log.error("download failed: {}", filePath, e);
            return false;
        }
    }

    /**
     * 将字节数组作为附件下载到客户端
     *
     * @param response     HttpServletResponse
     * @param data         文件内容
     * @param downloadName 下载文件名
     * @return 是否成功
     */
    public static boolean download(HttpServletResponse response, byte[] data, String downloadName) {
        if (response == null || data == null || data.length == 0
                || StringUtils.isObjectBlank(downloadName)) {
            return false;
        }
        try {
            String encoded = URLEncoder.encode(downloadName, "UTF-8").replaceAll("\\+", "%20");
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
            log.info("download (bytes): {}, size: {}", downloadName, data.length);
            return true;
        } catch (IOException e) {
            log.error("download (bytes) failed: {}", downloadName, e);
            return false;
        }
    }

    // ==================== 压缩/解压 ====================

    /**
     * 压缩文件或目录为 ZIP
     * <p>
     * 实际委托给 {@link CompressUtils#zip} 实现，底层使用 Apache Commons Compress，
     * 支持更多格式（tar/7z/gzip/bzip2/xz）请直接使用 {@link CompressUtils}。
     * </p>
     *
     * @param sourcePath 源文件或目录路径
     * @param zipPath   目标 ZIP 文件路径
     * @return 是否成功
     * @see CompressUtils#zip
     */
    public static boolean zip(String sourcePath, String zipPath) {
        return CompressUtils.zip(sourcePath, zipPath);
    }

    /**
     * 解压 ZIP 到目标目录
     * <p>
     * 实际委托给 {@link CompressUtils#unzip} 实现，底层使用 Apache Commons Compress，
     * 含 Zip Slip 路径穿越防护。其他格式（tar/7z/gzip/bzip2/xz）解压请使用
     * </p>
     *
     * @param zipPath  ZIP 文件路径
     * @param destDir  目标目录
     * @return 是否成功
     * @see CompressUtils#unzip
     * @see CompressUtils#decompress
     */
    public static boolean unzip(String zipPath, String destDir) {
        return CompressUtils.unzip(zipPath, destDir);
    }

    // ==================== 递归与统计 ====================

    /**
     * 递归列出目录下所有文件（不含目录）
     *
     * @param dirPath 目录路径
     * @return 文件路径列表（绝对路径），失败返回空列表
     */
    public static List<String> listAll(String dirPath) {
        if (StringUtils.isObjectBlank(dirPath)) {
            return Collections.emptyList();
        }
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> result.add(p.toAbsolutePath().toString()));
        } catch (IOException e) {
            log.error("listAll failed: {}", dirPath, e);
        }
        return result;
    }

    /**
     * 统计目录下文件数量
     *
     * @param dirPath 目录路径
     * @return 文件数量，失败返回 -1
     */
    public static long countFiles(String dirPath) {
        if (StringUtils.isObjectBlank(dirPath)) {
            return -1;
        }
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return -1;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            log.error("countFiles failed: {}", dirPath, e);
            return -1;
        }
    }

    /**
     * 统计目录总大小（字节）
     *
     * @param dirPath 目录路径
     * @return 总字节数，失败返回 -1
     */
    public static long sizeOfDirectory(String dirPath) {
        if (StringUtils.isObjectBlank(dirPath)) {
            return -1;
        }
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return -1;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            log.error("sizeOfDirectory failed: {}", dirPath, e);
            return -1;
        }
    }

    /**
     * 统计文件行数
     *
     * @param filePath 文件路径
     * @return 行数，失败返回 -1
     */
    public static long countLines(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return -1;
        }
        try (Stream<String> stream = Files.lines(Paths.get(filePath), DEFAULT_CHARSET)) {
            return stream.count();
        } catch (IOException e) {
            log.error("countLines failed: {}", filePath, e);
            return -1;
        }
    }

    // ==================== 文件操作增强 ====================

    /**
     * 复制文件到目标目录（保留原文件名）
     *
     * @param sourcePath 源文件路径
     * @param targetDir  目标目录
     * @return 是否成功
     */
    public static boolean copyToDir(String sourcePath, String targetDir) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(targetDir)) {
            return false;
        }
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            log.warn("copyToDir: source not found: {}", sourcePath);
            return false;
        }
        if (!mkdirs(targetDir)) {
            return false;
        }
        Path target = Paths.get(targetDir, source.getFileName().toString());
        return copy(sourcePath, target.toString());
    }

    /**
     * 合并两个文件为第三个文件
     *
     * @param file1Path 第一个文件路径
     * @param file2Path 第二个文件路径
     * @param outputPath 输出文件路径
     * @return 是否成功
     */
    public static boolean mergeFiles(String file1Path, String file2Path, String outputPath) {
        if (StringUtils.isObjectBlank(file1Path) || StringUtils.isObjectBlank(file2Path)
                || StringUtils.isObjectBlank(outputPath)) {
            return false;
        }
        try (OutputStream os = new FileOutputStream(outputPath)) {
            ensureParentDirectory(outputPath);
            Files.copy(Paths.get(file1Path), os);
            Files.copy(Paths.get(file2Path), os);
            log.info("mergeFiles: {} + {} -> {}", file1Path, file2Path, outputPath);
            return true;
        } catch (IOException e) {
            log.error("mergeFiles failed", e);
            return false;
        }
    }

    /**
     * 创建临时文件
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀（如 .tmp）
     * @return 临时文件路径，失败返回 null
     */
    public static String createTempFile(String prefix, String suffix) {
        try {
            Path temp = Files.createTempFile(prefix, suffix);
            log.debug("createTempFile: {}", temp);
            return temp.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("createTempFile failed", e);
            return null;
        }
    }

    /**
     * 创建临时文件并写入内容
     *
     * @param prefix  文件名前缀
     * @param suffix  文件名后缀
     * @param content 文件内容
     * @return 临时文件路径，失败返回 null
     */
    public static String createTempFile(String prefix, String suffix, byte[] content) {
        String path = createTempFile(prefix, suffix);
        if (path == null || !writeBytes(path, content)) {
            return null;
        }
        return path;
    }

    // ==================== 内容搜索 ====================

    /**
     * 在文件中搜索关键字（按行匹配）
     *
     * @param filePath 文件路径
     * @param keyword  关键字
     * @return 匹配的行列表，失败返回空列表
     */
    public static List<String> searchInFile(String filePath, String keyword) {
        if (StringUtils.isObjectBlank(filePath) || StringUtils.isObjectBlank(keyword)) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(filePath), DEFAULT_CHARSET)) {
            stream.filter(line -> line.contains(keyword))
                    .forEach(matches::add);
        } catch (IOException e) {
            log.error("searchInFile failed: {}", filePath, e);
        }
        return matches;
    }

    /**
     * 在文件中搜索关键字（按行匹配，忽略大小写）
     *
     * @param filePath 文件路径
     * @param keyword  关键字
     * @return 匹配的行列表，失败返回空列表
     */
    public static List<String> searchInFileIgnoreCase(String filePath, String keyword) {
        if (StringUtils.isObjectBlank(filePath) || StringUtils.isObjectBlank(keyword)) {
            return Collections.emptyList();
        }
        String lowerKeyword = keyword.toLowerCase();
        List<String> matches = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(filePath), DEFAULT_CHARSET)) {
            stream.filter(line -> line.toLowerCase().contains(lowerKeyword))
                    .forEach(matches::add);
        } catch (IOException e) {
            log.error("searchInFileIgnoreCase failed: {}", filePath, e);
        }
        return matches;
    }

    // ==================== 路径操作 ====================

    /**
     * 路径拼接
     *
     * @param base 基础路径
     * @param more 子路径片段
     * @return 拼接后的路径字符串
     */
    public static String joinPath(String base, String... more) {
        if (StringUtils.isObjectBlank(base)) {
            return StringPool.EMPTY.getValue();
        }
        Path path = Paths.get(base);
        if (more != null && more.length > 0) {
            for (String segment : more) {
                if (!StringUtils.isObjectBlank(segment)) {
                    path = path.resolve(segment);
                }
            }
        }
        return path.toString();
    }

    /**
     * 获取相对路径
     *
     * @param basePath 基础路径
     * @param fullPath 完整路径
     * @return 相对路径字符串，失败返回原完整路径
     */
    public static String getRelativePath(String basePath, String fullPath) {
        if (StringUtils.isObjectBlank(basePath) || StringUtils.isObjectBlank(fullPath)) {
            return fullPath;
        }
        try {
            Path base = Paths.get(basePath).toAbsolutePath().normalize();
            Path full = Paths.get(fullPath).toAbsolutePath().normalize();
            return base.relativize(full).toString();
        } catch (Exception e) {
            log.warn("getRelativePath failed: base={}, full={}", basePath, fullPath, e);
            return fullPath;
        }
    }

    /**
     * 获取父目录路径
     *
     * @param filePath 文件路径
     * @return 父目录路径，无父目录返回空字符串
     */
    public static String getParentPath(String filePath) {
        if (StringUtils.isObjectBlank(filePath)) {
            return StringPool.EMPTY.getValue();
        }
        Path parent = Paths.get(filePath).getParent();
        return parent != null ? parent.toString() : StringPool.EMPTY.getValue();
    }

    /**
     * 规范化路径（解析 . 和 ..）
     *
     * @param path 原始路径
     * @return 规范化后的绝对路径
     */
    public static String normalizePath(String path) {
        if (StringUtils.isObjectBlank(path)) {
            return StringPool.EMPTY.getValue();
        }
        try {
            return Paths.get(path).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            return path;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 确保父目录存在
     */
    private static void ensureParentDirectory(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 字节数组转十六进制（大写）
     */
    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            sb.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
            sb.append(Character.forDigit(bytes[i] & 0xF, 16));
        }
        return sb.toString().toUpperCase();
    }

    /**
     * 生成随机字符串
     */
    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }
}