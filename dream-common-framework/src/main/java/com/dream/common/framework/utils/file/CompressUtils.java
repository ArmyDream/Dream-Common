package com.dream.common.framework.utils.file;

import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;

/**
 * 压缩/解压工具类（基于 Apache Commons Compress 1.21）
 * <p>
 * 统一封装多种压缩与归档格式，提供一致的操作接口。支持格式：
 * </p>
 *
 * <h2>归档格式</h2>
 * <ul>
 *   <li><b>ZIP</b>：最常见的归档格式，支持压缩与解压</li>
 *   <li><b>TAR</b>：Unix 归档格式，本身不压缩，支持归档与提取</li>
 *   <li><b>7Z</b>：高压缩比格式，支持压缩与解压</li>
 * </ul>
 *
 * <h2>压缩格式</h2>
 * <ul>
 *   <li><b>GZIP</b>：单文件压缩（.gz）</li>
 *   <li><b>BZIP2</b>：高压缩比单文件压缩（.bz2）</li>
 *   <li><b>XZ</b>：LZMA2 算法压缩（.xz，需引入 xz 依赖）</li>
 * </ul>
 *
 * <h2>组合格式</h2>
 * <ul>
 *   <li><b>TAR.GZ</b>：tar 归档后 gzip 压缩</li>
 *   <li><b>TAR.BZ2</b>：tar 归档后 bzip2 压缩</li>
 *   <li><b>TAR.XZ</b>：tar 归档后 xz 压缩</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. ZIP 压缩目录
 * CompressUtils.zip("/path/to/dir", "/path/to/archive.zip");
 *
 * // 2. ZIP 解压
 * CompressUtils.unzip("/path/to/archive.zip", "/path/to/dest");
 *
 * // 3. 自动识别格式解压
 * CompressUtils.decompress("/path/to/archive.7z", "/path/to/dest");
 * CompressUtils.decompress("/path/to/archive.tar.gz", "/path/to/dest");
 *
 * // 4. GZIP 压缩单文件
 * CompressUtils.gzip("/path/to/file.txt", "/path/to/file.txt.gz");
 *
 * // 5. 按格式名解压
 * CompressUtils.decompress("/path/to/archive.tar", "/path/to/dest", ArchiveFormat.TAR);
 * }</pre>
 *
 * <h2>「不引入不报错」</h2>
 * <p>
 * 本工具类依赖 {@code commons-compress}，当未引入该依赖时，调用方法会抛出
 * {@link NoClassDefFoundError}。建议在业务侧通过 {@code @ConditionalOnClass} 守护。
 * </p>
 *
 * @author dream
 * @since 2026-08-21
 */
@Slf4j
public final class CompressUtils {

    /** 默认缓冲区大小（8KB） */
    private static final int BUFFER_SIZE = 8192;

    /** ZIP 压缩级别：默认使用标准压缩 */
    private static final int DEFAULT_ZIP_LEVEL = Deflater.DEFAULT_COMPRESSION;

    private CompressUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== 归档格式枚举 ====================

    /**
     * 归档/压缩格式枚举
     * <p>
     * 用于明确指定 {@link #decompress(String, String, ArchiveFormat)} 的输入格式，
     * 避免自动识别失败。
     * </p>
     */
    public enum ArchiveFormat {

        /** ZIP 归档格式 */
        ZIP,

        /** TAR 归档格式（不压缩） */
        TAR,

        /** 7Z 归档格式（高压缩比） */
        SEVEN_Z,

        /** GZIP 单文件压缩格式 */
        GZIP,

        /** BZIP2 单文件压缩格式 */
        BZIP2,

        /** XZ 单文件压缩格式（LZMA2） */
        XZ,

        /** TAR + GZIP 组合格式 */
        TAR_GZ,

        /** TAR + BZIP2 组合格式 */
        TAR_BZ2,

        /** TAR + XZ 组合格式 */
        TAR_XZ
    }

    // ==================== ZIP ====================

    /**
     * 将文件或目录压缩为 ZIP
     *
     * @param sourcePath 源文件或目录路径
     * @param zipPath    目标 ZIP 文件路径
     * @return 是否成功
     */
    public static boolean zip(String sourcePath, String zipPath) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(zipPath)) {
            return false;
        }
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            log.warn("zip: source not found: {}", sourcePath);
            return false;
        }
        ensureParentDirectory(zipPath);
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipPath)))) {
            zos.setLevel(DEFAULT_ZIP_LEVEL);
            if (Files.isDirectory(source)) {
                Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(Path file,
                                                                     java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                        String entryName = source.relativize(file).toString().replace(File.separatorChar, '/');
                        addToZip(zos, file, entryName);
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
            } else {
                addToZip(zos, source, source.getFileName().toString());
            }
            zos.finish();
            log.info("zip: {} -> {}", sourcePath, zipPath);
            return true;
        } catch (IOException e) {
            log.error("zip failed: {} -> {}", sourcePath, zipPath, e);
            return false;
        }
    }

    /**
     * 解压 ZIP 到目标目录
     *
     * @param zipPath ZIP 文件路径
     * @param destDir 目标目录
     * @return 是否成功
     */
    public static boolean unzip(String zipPath, String destDir) {
        if (StringUtils.isObjectBlank(zipPath) || StringUtils.isObjectBlank(destDir)) {
            return false;
        }
        if (!Files.exists(Paths.get(zipPath))) {
            log.warn("unzip: zip not found: {}", zipPath);
            return false;
        }
        if (!ensureDirectory(destDir)) {
            return false;
        }
        Path destBase = Paths.get(destDir).toAbsolutePath().normalize();
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new BufferedInputStream(new FileInputStream(zipPath)), StandardCharsets.UTF_8.name(), true, true)) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (!handleArchiveEntry(zis, entry, entry.getName(), destBase)) {
                    continue;
                }
            }
            log.info("unzip: {} -> {}", zipPath, destDir);
            return true;
        } catch (IOException e) {
            log.error("unzip failed: {} -> {}", zipPath, destDir, e);
            return false;
        }
    }

    /**
     * 添加文件到 ZIP 输出流
     */
    private static void addToZip(ZipArchiveOutputStream zos, Path file, String entryName) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(file.toFile(), entryName);
        zos.putArchiveEntry(entry);
        Files.copy(file, zos);
        zos.closeArchiveEntry();
    }

    // ==================== TAR ====================

    /**
     * 将文件或目录归档为 TAR（不压缩）
     *
     * @param sourcePath 源文件或目录路径
     * @param tarPath   目标 TAR 文件路径
     * @return 是否成功
     */
    public static boolean tar(String sourcePath, String tarPath) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(tarPath)) {
            return false;
        }
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            log.warn("tar: source not found: {}", sourcePath);
            return false;
        }
        ensureParentDirectory(tarPath);
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(
                new BufferedOutputStream(new FileOutputStream(tarPath)))) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            if (Files.isDirectory(source)) {
                Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(Path file,
                                                                     java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                        String entryName = source.relativize(file).toString().replace(File.separatorChar, '/');
                        addToTar(tos, file, entryName);
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
            } else {
                addToTar(tos, source, source.getFileName().toString());
            }
            tos.finish();
            log.info("tar: {} -> {}", sourcePath, tarPath);
            return true;
        } catch (IOException e) {
            log.error("tar failed: {} -> {}", sourcePath, tarPath, e);
            return false;
        }
    }

    /**
     * 解压 TAR 到目标目录
     *
     * @param tarPath TAR 文件路径
     * @param destDir 目标目录
     * @return 是否成功
     */
    public static boolean untar(String tarPath, String destDir) {
        if (StringUtils.isObjectBlank(tarPath) || StringUtils.isObjectBlank(destDir)) {
            return false;
        }
        if (!Files.exists(Paths.get(tarPath))) {
            log.warn("untar: tar not found: {}", tarPath);
            return false;
        }
        if (!ensureDirectory(destDir)) {
            return false;
        }
        Path destBase = Paths.get(destDir).toAbsolutePath().normalize();
        try (TarArchiveInputStream tis = new TarArchiveInputStream(
                new BufferedInputStream(new FileInputStream(tarPath)))) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (!handleArchiveEntry(tis, entry, entry.getName(), destBase)) {
                    continue;
                }
            }
            log.info("untar: {} -> {}", tarPath, destDir);
            return true;
        } catch (IOException e) {
            log.error("untar failed: {} -> {}", tarPath, destDir, e);
            return false;
        }
    }

    /**
     * 添加文件到 TAR 输出流
     */
    private static void addToTar(TarArchiveOutputStream tos, Path file, String entryName) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), entryName);
        tos.putArchiveEntry(entry);
        Files.copy(file, tos);
        tos.closeArchiveEntry();
    }

    // ==================== 7Z ====================

    /**
     * 将文件或目录压缩为 7Z
     *
     * @param sourcePath 源文件或目录路径
     * @param sevenZPath 目标 7Z 文件路径
     * @return 是否成功
     */
    public static boolean sevenZ(String sourcePath, String sevenZPath) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(sevenZPath)) {
            return false;
        }
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            log.warn("sevenZ: source not found: {}", sourcePath);
            return false;
        }
        ensureParentDirectory(sevenZPath);
        try (SevenZOutputFile out = new SevenZOutputFile(new File(sevenZPath))) {
            if (Files.isDirectory(source)) {
                Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(Path file,
                                                                     java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                        String entryName = source.relativize(file).toString().replace(File.separatorChar, '/');
                        addToSevenZ(out, file, entryName);
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
            } else {
                addToSevenZ(out, source, source.getFileName().toString());
            }
            log.info("sevenZ: {} -> {}", sourcePath, sevenZPath);
            return true;
        } catch (IOException e) {
            log.error("sevenZ failed: {} -> {}", sourcePath, sevenZPath, e);
            return false;
        }
    }

    /**
     * 解压 7Z 到目标目录
     *
     * @param sevenZPath 7Z 文件路径
     * @param destDir    目标目录
     * @return 是否成功
     */
    public static boolean unSevenZ(String sevenZPath, String destDir) {
        if (StringUtils.isObjectBlank(sevenZPath) || StringUtils.isObjectBlank(destDir)) {
            return false;
        }
        if (!Files.exists(Paths.get(sevenZPath))) {
            log.warn("unSevenZ: 7z not found: {}", sevenZPath);
            return false;
        }
        if (!ensureDirectory(destDir)) {
            return false;
        }
        Path destBase = Paths.get(destDir).toAbsolutePath().normalize();
        try (SevenZFile sevenZFile = new SevenZFile(new File(sevenZPath))) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                Path target = destBase.resolve(entry.getName()).normalize();
                if (!target.startsWith(destBase)) {
                    log.warn("unSevenZ: skip suspicious entry: {}", entry.getName());
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(target.toFile()))) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = sevenZFile.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }
                }
            }
            log.info("unSevenZ: {} -> {}", sevenZPath, destDir);
            return true;
        } catch (IOException e) {
            log.error("unSevenZ failed: {} -> {}", sevenZPath, destDir, e);
            return false;
        }
    }

    /**
     * 添加文件到 7Z 输出文件
     */
    private static void addToSevenZ(SevenZOutputFile out, Path file, String entryName) throws IOException {
        SevenZArchiveEntry entry = out.createArchiveEntry(file.toFile(), entryName);
        out.putArchiveEntry(entry);
        try (InputStream fis = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        out.closeArchiveEntry();
    }

    // ==================== 单文件压缩（GZIP / BZIP2 / XZ） ====================

    /**
     * 使用 GZIP 压缩单文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径（.gz）
     * @return 是否成功
     */
    public static boolean gzip(String sourcePath, String targetPath) {
        return compressSingle(sourcePath, targetPath, ArchiveFormat.GZIP);
    }

    /**
     * 使用 BZIP2 压缩单文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径（.bz2）
     * @return 是否成功
     */
    public static boolean bzip2(String sourcePath, String targetPath) {
        return compressSingle(sourcePath, targetPath, ArchiveFormat.BZIP2);
    }

    /**
     * 使用 XZ 压缩单文件（需引入 xz 依赖）
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径（.xz）
     * @return 是否成功
     */
    public static boolean xz(String sourcePath, String targetPath) {
        return compressSingle(sourcePath, targetPath, ArchiveFormat.XZ);
    }

    /**
     * 单文件压缩通用实现
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @param format    压缩格式
     * @return 是否成功
     */
    private static boolean compressSingle(String sourcePath, String targetPath, ArchiveFormat format) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(targetPath)) {
            return false;
        }
        if (!Files.exists(Paths.get(sourcePath))) {
            log.warn("compress: source not found: {}", sourcePath);
            return false;
        }
        ensureParentDirectory(targetPath);
        try (InputStream fis = new BufferedInputStream(new FileInputStream(sourcePath));
             OutputStream fos = new BufferedOutputStream(new FileOutputStream(targetPath));
             OutputStream cos = createCompressorOutputStream(fos, format)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, read);
            }
            log.info("{}: {} -> {}", format, sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            log.error("{} compress failed: {} -> {}", format, sourcePath, targetPath, e);
            return false;
        }
    }

    // ==================== 组合格式（TAR.GZ / TAR.BZ2 / TAR.XZ） ====================

    /**
     * 将文件或目录压缩为 TAR.GZ
     *
     * @param sourcePath 源文件或目录路径
     * @param targetPath 目标文件路径（.tar.gz）
     * @return 是否成功
     */
    public static boolean tarGz(String sourcePath, String targetPath) {
        return tarWithCompression(sourcePath, targetPath, ArchiveFormat.TAR_GZ);
    }

    /**
     * 将文件或目录压缩为 TAR.BZ2
     *
     * @param sourcePath 源文件或目录路径
     * @param targetPath 目标文件路径（.tar.bz2）
     * @return 是否成功
     */
    public static boolean tarBz2(String sourcePath, String targetPath) {
        return tarWithCompression(sourcePath, targetPath, ArchiveFormat.TAR_BZ2);
    }

    /**
     * 将文件或目录压缩为 TAR.XZ
     *
     * @param sourcePath 源文件或目录路径
     * @param targetPath 目标文件路径（.tar.xz）
     * @return 是否成功
     */
    public static boolean tarXz(String sourcePath, String targetPath) {
        return tarWithCompression(sourcePath, targetPath, ArchiveFormat.TAR_XZ);
    }

    /**
     * TAR + 压缩 通用实现
     *
     * @param sourcePath 源文件或目录路径
     * @param targetPath 目标文件路径
     * @param format    组合格式（TAR_GZ / TAR_BZ2 / TAR_XZ）
     * @return 是否成功
     */
    private static boolean tarWithCompression(String sourcePath, String targetPath, ArchiveFormat format) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(targetPath)) {
            return false;
        }
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            log.warn("tarWithCompression: source not found: {}", sourcePath);
            return false;
        }
        ensureParentDirectory(targetPath);
        ArchiveFormat compressorFormat;
        switch (format) {
            case TAR_GZ:
                compressorFormat = ArchiveFormat.GZIP;
                break;
            case TAR_BZ2:
                compressorFormat = ArchiveFormat.BZIP2;
                break;
            case TAR_XZ:
                compressorFormat = ArchiveFormat.XZ;
                break;
            default:
                return false;
        }
        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(targetPath));
             OutputStream cos = createCompressorOutputStream(fos, compressorFormat);
             TarArchiveOutputStream tos = new TarArchiveOutputStream(cos)) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            if (Files.isDirectory(source)) {
                Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(Path file,
                                                                     java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                        String entryName = source.relativize(file).toString().replace(File.separatorChar, '/');
                        addToTar(tos, file, entryName);
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
            } else {
                addToTar(tos, source, source.getFileName().toString());
            }
            tos.finish();
            log.info("{}: {} -> {}", format, sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            log.error("{} failed: {} -> {}", format, sourcePath, targetPath, e);
            return false;
        }
    }

    // ==================== 自动识别格式解压 ====================

    /**
     * 自动识别压缩文件格式并解压到目标目录
     * <p>
     * 根据文件扩展名自动选择解压方式：
     * <ul>
     *   <li>{@code .zip} → {@link #unzip}</li>
     *   <li>{@code .tar} → {@link #untar}</li>
     *   <li>{@code .7z} → {@link #unSevenZ}</li>
     *   <li>{@code .gz} → GZIP 单文件解压</li>
     *   <li>{@code .bz2} → BZIP2 单文件解压</li>
     *   <li>{@code .xz} → XZ 单文件解压</li>
     *   <li>{@code .tar.gz} / {@code .tgz} → {@link #untarGz}</li>
     *   <li>{@code .tar.bz2} / {@code .tbz2} → {@link #untarBz2}</li>
     *   <li>{@code .tar.xz} / {@code .txz} → {@link #untarXz}</li>
     * </ul>
     * </p>
     *
     * @param sourcePath 压缩文件路径
     * @param destDir    目标目录
     * @return 是否成功
     */
    public static boolean decompress(String sourcePath, String destDir) {
        if (StringUtils.isObjectBlank(sourcePath) || StringUtils.isObjectBlank(destDir)) {
            return false;
        }
        String lowerPath = sourcePath.toLowerCase();
        if (lowerPath.endsWith(".zip")) {
            return unzip(sourcePath, destDir);
        }
        if (lowerPath.endsWith(".7z")) {
            return unSevenZ(sourcePath, destDir);
        }
        if (lowerPath.endsWith(".tar.gz") || lowerPath.endsWith(".tgz")) {
            return untarGz(sourcePath, destDir);
        }
        if (lowerPath.endsWith(".tar.bz2") || lowerPath.endsWith(".tbz2")) {
            return untarBz2(sourcePath, destDir);
        }
        if (lowerPath.endsWith(".tar.xz") || lowerPath.endsWith(".txz")) {
            return untarXz(sourcePath, destDir);
        }
        if (lowerPath.endsWith(".tar")) {
            return untar(sourcePath, destDir);
        }
        if (lowerPath.endsWith(".gz")) {
            return decompressSingle(sourcePath, destDir, ArchiveFormat.GZIP);
        }
        if (lowerPath.endsWith(".bz2")) {
            return decompressSingle(sourcePath, destDir, ArchiveFormat.BZIP2);
        }
        if (lowerPath.endsWith(".xz")) {
            return decompressSingle(sourcePath, destDir, ArchiveFormat.XZ);
        }
        log.warn("decompress: unsupported format: {}", sourcePath);
        return false;
    }

    /**
     * 按指定格式解压
     *
     * @param sourcePath 压缩文件路径
     * @param destDir    目标目录
     * @param format     归档格式
     * @return 是否成功
     */
    public static boolean decompress(String sourcePath, String destDir, ArchiveFormat format) {
        if (format == null) {
            return decompress(sourcePath, destDir);
        }
        switch (format) {
            case ZIP:
                return unzip(sourcePath, destDir);
            case TAR:
                return untar(sourcePath, destDir);
            case SEVEN_Z:
                return unSevenZ(sourcePath, destDir);
            case GZIP:
                return decompressSingle(sourcePath, destDir, ArchiveFormat.GZIP);
            case BZIP2:
                return decompressSingle(sourcePath, destDir, ArchiveFormat.BZIP2);
            case XZ:
                return decompressSingle(sourcePath, destDir, ArchiveFormat.XZ);
            case TAR_GZ:
                return untarGz(sourcePath, destDir);
            case TAR_BZ2:
                return untarBz2(sourcePath, destDir);
            case TAR_XZ:
                return untarXz(sourcePath, destDir);
            default:
                log.warn("decompress: unsupported format: {}", format);
                return false;
        }
    }

    // ==================== 组合格式解压 ====================

    /**
     * 解压 TAR.GZ 到目标目录
     *
     * @param tarGzPath TAR.GZ 文件路径
     * @param destDir   目标目录
     * @return 是否成功
     */
    public static boolean untarGz(String tarGzPath, String destDir) {
        return untarWithCompression(tarGzPath, destDir, ArchiveFormat.GZIP);
    }

    /**
     * 解压 TAR.BZ2 到目标目录
     *
     * @param tarBz2Path TAR.BZ2 文件路径
     * @param destDir    目标目录
     * @return 是否成功
     */
    public static boolean untarBz2(String tarBz2Path, String destDir) {
        return untarWithCompression(tarBz2Path, destDir, ArchiveFormat.BZIP2);
    }

    /**
     * 解压 TAR.XZ 到目标目录
     *
     * @param tarXzPath TAR.XZ 文件路径
     * @param destDir   目标目录
     * @return 是否成功
     */
    public static boolean untarXz(String tarXzPath, String destDir) {
        return untarWithCompression(tarXzPath, destDir, ArchiveFormat.XZ);
    }

    /**
     * TAR + 压缩 解压通用实现
     *
     * @param compressedPath 压缩文件路径
     * @param destDir        目标目录
     * @param compressorFormat 压缩格式（GZIP / BZIP2 / XZ）
     * @return 是否成功
     */
    private static boolean untarWithCompression(String compressedPath, String destDir,
                                                ArchiveFormat compressorFormat) {
        if (StringUtils.isObjectBlank(compressedPath) || StringUtils.isObjectBlank(destDir)) {
            return false;
        }
        if (!Files.exists(Paths.get(compressedPath))) {
            log.warn("untarWithCompression: file not found: {}", compressedPath);
            return false;
        }
        if (!ensureDirectory(destDir)) {
            return false;
        }
        Path destBase = Paths.get(destDir).toAbsolutePath().normalize();
        try (InputStream fis = new BufferedInputStream(new FileInputStream(compressedPath));
             InputStream cis = createCompressorInputStream(fis, compressorFormat);
             TarArchiveInputStream tis = new TarArchiveInputStream(cis)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (!handleArchiveEntry(tis, entry, entry.getName(), destBase)) {
                    continue;
                }
            }
            log.info("untarWithCompression ({}): {} -> {}", compressorFormat, compressedPath, destDir);
            return true;
        } catch (IOException e) {
            log.error("untarWithCompression failed: {} -> {}", compressedPath, destDir, e);
            return false;
        }
    }

    /**
     * 单文件解压（GZIP / BZIP2 / XZ）
     * <p>
     * 解压后文件名为去掉压缩扩展名的原始文件名，输出到目标目录下。
     * </p>
     *
     * @param compressedPath 压缩文件路径
     * @param destDir        目标目录
     * @param format         压缩格式
     * @return 是否成功
     */
    private static boolean decompressSingle(String compressedPath, String destDir, ArchiveFormat format) {
        if (StringUtils.isObjectBlank(compressedPath) || StringUtils.isObjectBlank(destDir)) {
            return false;
        }
        if (!Files.exists(Paths.get(compressedPath))) {
            log.warn("decompressSingle: file not found: {}", compressedPath);
            return false;
        }
        if (!ensureDirectory(destDir)) {
            return false;
        }
        // 推断解压后文件名
        String fileName = Paths.get(compressedPath).getFileName().toString();
        String outputName;
        switch (format) {
            case GZIP:
                outputName = fileName.endsWith(".gz") ? fileName.substring(0, fileName.length() - 3) : fileName + ".out";
                break;
            case BZIP2:
                outputName = fileName.endsWith(".bz2") ? fileName.substring(0, fileName.length() - 4) : fileName + ".out";
                break;
            case XZ:
                outputName = fileName.endsWith(".xz") ? fileName.substring(0, fileName.length() - 3) : fileName + ".out";
                break;
            default:
                outputName = fileName + ".out";
        }
        String outputPath = Paths.get(destDir, outputName).toString();
        try (InputStream fis = new BufferedInputStream(new FileInputStream(compressedPath));
             InputStream cis = createCompressorInputStream(fis, format);
             OutputStream fos = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            log.info("decompressSingle ({}): {} -> {}", format, compressedPath, outputPath);
            return true;
        } catch (IOException e) {
            log.error("decompressSingle failed: {} -> {}", compressedPath, outputPath, e);
            return false;
        }
    }

    // ==================== 字节数组压缩（内存操作） ====================

    /**
     * 将字节数组使用 GZIP 压缩
     *
     * @param data 原始数据
     * @return 压缩后字节数组
     */
    public static byte[] gzipBytes(byte[] data) {
        return compressBytes(data, ArchiveFormat.GZIP);
    }

    /**
     * 将字节数组使用 BZIP2 压缩
     *
     * @param data 原始数据
     * @return 压缩后字节数组
     */
    public static byte[] bzip2Bytes(byte[] data) {
        return compressBytes(data, ArchiveFormat.BZIP2);
    }

    /**
     * 将字节数组使用 XZ 压缩
     *
     * @param data 原始数据
     * @return 压缩后字节数组
     */
    public static byte[] xzBytes(byte[] data) {
        return compressBytes(data, ArchiveFormat.XZ);
    }

    /**
     * 字节数组压缩通用实现
     *
     * @param data   原始数据
     * @param format 压缩格式
     * @return 压缩后字节数组
     */
    private static byte[] compressBytes(byte[] data, ArchiveFormat format) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             OutputStream cos = createCompressorOutputStream(bos, format)) {
            cos.write(data);
            cos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("compressBytes failed: format={}", format, e);
            return new byte[0];
        }
    }

    /**
     * 解压字节数组（GZIP）
     *
     * @param compressedData 压缩数据
     * @return 解压后字节数组
     */
    public static byte[] ungzipBytes(byte[] compressedData) {
        return decompressBytes(compressedData, ArchiveFormat.GZIP);
    }

    /**
     * 解压字节数组（BZIP2）
     *
     * @param compressedData 压缩数据
     * @return 解压后字节数组
     */
    public static byte[] unbzip2Bytes(byte[] compressedData) {
        return decompressBytes(compressedData, ArchiveFormat.BZIP2);
    }

    /**
     * 解压字节数组（XZ）
     *
     * @param compressedData 压缩数据
     * @return 解压后字节数组
     */
    public static byte[] unxzBytes(byte[] compressedData) {
        return decompressBytes(compressedData, ArchiveFormat.XZ);
    }

    /**
     * 字节数组解压通用实现
     *
     * @param compressedData 压缩数据
     * @param format         压缩格式
     * @return 解压后字节数组
     */
    private static byte[] decompressBytes(byte[] compressedData, ArchiveFormat format) {
        if (compressedData == null || compressedData.length == 0) {
            return new byte[0];
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             InputStream cis = createCompressorInputStream(bis, format);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = cis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("decompressBytes failed: format={}", format, e);
            return new byte[0];
        }
    }

    // ==================== 列出归档内文件 ====================

    /**
     * 列出归档文件内的所有条目名称
     * <p>
     * 支持 ZIP / TAR / 7Z / TAR.GZ / TAR.BZ2 / TAR.XZ。
     * </p>
     *
     * @param archivePath 归档文件路径
     * @return 条目名称列表，失败返回空列表
     */
    public static List<String> listEntries(String archivePath) {
        if (StringUtils.isObjectBlank(archivePath)) {
            return Collections.emptyList();
        }
        String lowerPath = archivePath.toLowerCase();
        try {
            if (lowerPath.endsWith(".zip")) {
                return listZipEntries(archivePath);
            }
            if (lowerPath.endsWith(".tar")) {
                return listTarEntries(archivePath);
            }
            if (lowerPath.endsWith(".7z")) {
                return listSevenZEntries(archivePath);
            }
            if (lowerPath.endsWith(".tar.gz") || lowerPath.endsWith(".tgz")) {
                return listTarCompressedEntries(archivePath, ArchiveFormat.GZIP);
            }
            if (lowerPath.endsWith(".tar.bz2") || lowerPath.endsWith(".tbz2")) {
                return listTarCompressedEntries(archivePath, ArchiveFormat.BZIP2);
            }
            if (lowerPath.endsWith(".tar.xz") || lowerPath.endsWith(".txz")) {
                return listTarCompressedEntries(archivePath, ArchiveFormat.XZ);
            }
        } catch (IOException e) {
            log.error("listEntries failed: {}", archivePath, e);
        }
        return Collections.emptyList();
    }

    /**
     * 列出 ZIP 内条目
     */
    private static List<String> listZipEntries(String zipPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new BufferedInputStream(new FileInputStream(zipPath)))) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    /**
     * 列出 TAR 内条目
     */
    private static List<String> listTarEntries(String tarPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (TarArchiveInputStream tis = new TarArchiveInputStream(
                new BufferedInputStream(new FileInputStream(tarPath)))) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    /**
     * 列出 7Z 内条目
     */
    private static List<String> listSevenZEntries(String sevenZPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (SevenZFile sevenZFile = new SevenZFile(new File(sevenZPath))) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    /**
     * 列出 TAR + 压缩 组合内条目
     */
    private static List<String> listTarCompressedEntries(String compressedPath,
                                                          ArchiveFormat compressorFormat) throws IOException {
        List<String> entries = new ArrayList<>();
        try (InputStream fis = new BufferedInputStream(new FileInputStream(compressedPath));
             InputStream cis = createCompressorInputStream(fis, compressorFormat);
             TarArchiveInputStream tis = new TarArchiveInputStream(cis)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    // ==================== 辅助方法 ====================

    /**
     * 处理归档条目（提取文件/目录，含 Zip Slip 防护）
     *
     * @param ais       归档输入流
     * @param entry     归档条目
     * @param entryName 条目名称
     * @param destBase  目标根目录
     * @return true 表示继续处理，false 表示跳过
     */
    private static boolean handleArchiveEntry(ArchiveInputStream ais, ArchiveEntry entry,
                                              String entryName, Path destBase) throws IOException {
        Path target = destBase.resolve(entryName).normalize();
        if (!target.startsWith(destBase)) {
            log.warn("Skip suspicious entry (Zip Slip): {}", entryName);
            return false;
        }
        if (entry.isDirectory()) {
            Files.createDirectories(target);
        } else {
            Files.createDirectories(target.getParent());
            Files.copy(ais, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    /**
     * 创建压缩输出流
     *
     * @param os     原始输出流
     * @param format 压缩格式
     * @return 压缩输出流
     * @throws IOException 创建失败
     */
    private static OutputStream createCompressorOutputStream(OutputStream os, ArchiveFormat format) throws IOException {
        switch (format) {
            case GZIP:
            case TAR_GZ:
                return new GzipCompressorOutputStream(os);
            case BZIP2:
            case TAR_BZ2:
                return new BZip2CompressorOutputStream(os);
            case XZ:
            case TAR_XZ:
                return new XZCompressorOutputStream(os);
            default:
                throw new IllegalArgumentException("Unsupported compressor format: " + format);
        }
    }

    /**
     * 创建压缩输入流
     *
     * @param is     原始输入流
     * @param format 压缩格式
     * @return 压缩输入流
     * @throws IOException 创建失败
     */
    private static InputStream createCompressorInputStream(InputStream is, ArchiveFormat format) throws IOException {
        switch (format) {
            case GZIP:
            case TAR_GZ:
                return new GzipCompressorInputStream(is);
            case BZIP2:
            case TAR_BZ2:
                return new BZip2CompressorInputStream(is);
            case XZ:
            case TAR_XZ:
                return new XZCompressorInputStream(is);
            default:
                throw new IllegalArgumentException("Unsupported compressor format: " + format);
        }
    }

    /**
     * 确保目录存在
     *
     * @param dirPath 目录路径
     * @return 是否成功
     */
    private static boolean ensureDirectory(String dirPath) {
        try {
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            return true;
        } catch (IOException e) {
            log.error("ensureDirectory failed: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 确保父目录存在
     *
     * @param filePath 文件路径
     */
    private static void ensureParentDirectory(String filePath) {
        try {
            Path parent = Paths.get(filePath).getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            log.error("ensureParentDirectory failed: {}", filePath, e);
        }
    }
}
