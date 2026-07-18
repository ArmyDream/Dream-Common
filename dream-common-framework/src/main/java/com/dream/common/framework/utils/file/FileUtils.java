package com.dream.common.framework.utils.file;

import com.dream.common.core.enums.FileCat;
import com.dream.common.core.enums.FileType;
import com.dream.common.core.enums.StringPool;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
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
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[32];
            int read = fis.read(header);
            if (read > 0) {
                String magicHex = bytesToHex(header, read);
                // 从长到短尝试匹配
                for (int len = Math.min(read * 2, 16); len >= 4; len -= 2) {
                    if (len <= magicHex.length()) {
                        FileType type = FileType.fromMagic(magicHex.substring(0, len));
                        if (type != null) {
                            log.debug("Detected file type by magic: {} -> {}", file.getName(), type);
                            return type;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("detectFileType magic detection failed: {}", file.getName(), e);
        }

        // 2. 回退到扩展名检测
        FileType type = FileType.fromExtension(getExtension(file));
        if (type != null) {
            log.debug("Detected file type by extension: {} -> {}", file.getName(), type);
        }
        return type;
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
     * 自动生成唯一文件名，防止覆盖。返回 {@link FileData} 包含文件元数据。
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

        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                originalName = "unknown";
            }
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
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .storagePath(savePath)
                    .fullStoragePath(fullPath)
                    .uploadTime(LocalDateTime.now())
                    .persisted(true)
                    .build();

            // 尝试设置文件类型
            FileType type = FileType.fromExtension(extension);
            if (type != null) {
                // FileData 没有直接设置 FileType 的方法，但可以通过其他方式
                // 这里预留扩展点
            }

            log.info("upload: {} -> {}, size: {}", originalName, storedName, file.getSize());
            return fileData;
        } catch (IOException e) {
            log.error("upload failed: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * 上传字节数组到磁盘
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
            String extension = getExtension(fileName);
            String storedName = generateStoredName(fileName);

            if (!mkdirs(savePath)) {
                log.error("upload: failed to create directory: {}", savePath);
                return null;
            }

            String fullPath = savePath + File.separator + storedName;
            Files.write(Paths.get(fullPath), data);

            FileData fileData = FileData.builder()
                    .originalName(fileName)
                    .storedName(storedName)
                    .extension(extension)
                    .size((long) data.length)
                    .storagePath(savePath)
                    .fullStoragePath(fullPath)
                    .uploadTime(LocalDateTime.now())
                    .persisted(true)
                    .build();

            log.info("upload (bytes): {} -> {}, size: {}", fileName, storedName, data.length);
            return fileData;
        } catch (IOException e) {
            log.error("upload (bytes) failed: {}", fileName, e);
            return null;
        }
    }

    // ==================== 文件名生成 ====================

    /**
     * 生成存储文件名
     * <p>
     * 格式：yyyyMMddHHmmssSSS_随机8位.扩展名
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
        String extension = getExtension(originalName);

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
        if (StringUtils.isObjectBlank(extension)) {
            return timestamp + "_" + random;
        }
        return timestamp + "_" + random + StringPool.DOT.getValue() + extension.toLowerCase();
    }

    // ==================== 文件上传（带类型限制） ====================

    /**
     * 上传文件到磁盘（带允许的文件类型限制）
     * <p>
     * 校验文件扩展名是否在允许列表中，通过后执行上传。
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
            log.warn("upload: file type not allowed: {}", file.getOriginalFilename());
            return null;
        }
        return upload(file, savePath);
    }

    /**
     * 上传文件到磁盘（带允许的文件分类限制）
     * <p>
     * 校验文件扩展名是否属于允许的分类，通过后执行上传。
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
            log.warn("upload: file category not allowed: {}", file.getOriginalFilename());
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
            log.warn("upload: file type not allowed: {}", fileName);
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
            log.warn("upload: file category not allowed: {}", fileName);
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
     * 校验文件类型（基于 File 对象 + 魔数检测）
     * <p>
     * 先通过魔数检测真实类型，再与允许类型对比，防止文件伪造。
     * </p>
     *
     * <pre>{@code
     * boolean valid = FileUtils.validateFileType(file, FileType.JPEG, FileType.PNG);
     * }</pre>
     *
     * @param file         文件对象
     * @param allowedTypes 允许的文件类型
     * @return true 如果文件类型在允许列表中
     */
    public static boolean validateFileType(File file, FileType... allowedTypes) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        if (allowedTypes == null || allowedTypes.length == 0) {
            return false;
        }
        // 先通过魔数检测
        FileType detectedType = detectFileType(file);
        if (detectedType != null) {
            for (FileType type : allowedTypes) {
                if (type == detectedType) {
                    return true;
                }
            }
        }
        // 魔数检测失败，回退到扩展名检测
        return validateFileType(file.getName(), allowedTypes);
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