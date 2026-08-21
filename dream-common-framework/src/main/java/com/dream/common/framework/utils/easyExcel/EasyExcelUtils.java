package com.dream.common.framework.utils.easyExcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.WriteTable;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * EasyExcel 工具类（基于 EasyExcel 4.0.3，JDK 8）
 * <p>
 * 提供以下能力：
 * <ul>
 *   <li>无实体类导入：读取为 {@code List<Map>} 或带表头映射的数据</li>
 *   <li>实体类导入：支持指定 Sheet 名/序号、自定义监听器（批量+校验），返回 {@link ExcelMessage}</li>
 *   <li>多 Sheet 导入：一次读取所有 Sheet 或指定 Sheet 列表</li>
 *   <li>简单导出：实体类导出，支持自定义样式处理器</li>
 *   <li>多级标题导出：通过复杂表头 {@code List<List<String>>} 实现自动合并</li>
 *   <li>多 Sheet 导出：支持一次写入多个不同数据的 Sheet</li>
 *   <li>模板填充：支持动态列表数据和普通数据混合填充</li>
 *   <li>CSV 导出：通过 {@link ExcelTypeEnum#CSV} 实现</li>
 *   <li>大数据量导出：通过 SXSSF 内存限制 {@code inMemory(false)} 避免内存溢出</li>
 *   <li>动态表头导出：运行时构建表头</li>
 *   <li>输出方式：支持返回字节数组、写入 OutputStream 或直接写入 HttpServletResponse</li>
 * </ul>
 * </p>
 *
 * @author dream
 * @since 2026-08-01
 */
@Slf4j
public final class EasyExcelUtils {

    /** 默认 Sheet 名称 */
    private static final String DEFAULT_SHEET_NAME = "Sheet1";

    /** 默认最大错误数（不限制） */
    private static final int DEFAULT_MAX_ERROR_COUNT = 0;

    private EasyExcelUtils() {
    }

    // ==================== 导入（无实体类） ====================

    /**
     * 无实体类同步读取 Excel，将所有行解析为 {@code Map<列索引, 字符串值>}。
     *
     * @param inputStream   输入流
     * @param sheetNo       Sheet 编号（从 0 开始），为 null 则读取第一个 Sheet
     * @param headRowNumber 标题所在行号（1-based），0 表示无标题，所有行都作为数据
     * @return 读取结果，每行为一个 Map，key 为列索引（0-based），value 为单元格字符串
     */
    public static List<Map<Integer, String>> readNoModelSync(InputStream inputStream,
                                                             Integer sheetNo,
                                                             Integer headRowNumber) {
        return readNoModelSync(inputStream, sheetNo, null, headRowNumber);
    }

    /**
     * 无实体类同步读取 Excel（按 Sheet 名称）
     *
     * @param inputStream   输入流
     * @param sheetName     Sheet 名称（为 null 则回退到第一个 Sheet）
     * @param headRowNumber 标题所在行号（1-based），0 表示无标题
     * @return 读取结果
     */
    public static List<Map<Integer, String>> readNoModelSyncByName(InputStream inputStream,
                                                                    String sheetName,
                                                                    Integer headRowNumber) {
        return readNoModelSync(inputStream, null, sheetName, headRowNumber);
    }

    /**
     * 无实体类同步读取 Excel（支持按 Sheet 序号或名称匹配）
     *
     * @param inputStream   输入流
     * @param sheetNo       Sheet 编号（从 0 开始），为 null 则使用 sheetName
     * @param sheetName     Sheet 名称（为 null 则使用 sheetNo）
     * @param headRowNumber 标题所在行号（1-based），0 表示无标题
     * @return 读取结果
     */
    public static List<Map<Integer, String>> readNoModelSync(InputStream inputStream,
                                                             Integer sheetNo,
                                                             String sheetName,
                                                             Integer headRowNumber) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
        int headRow = (headRowNumber == null) ? 0 : headRowNumber;

        List<Map<Integer, String>> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            ExcelReader reader = EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    if (data != null) {
                        result.add(data);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    latch.countDown();
                }

                @Override
                public void onException(Exception exception, AnalysisContext context) {
                    log.error("Excel read error", exception);
                    latch.countDown();
                }
            }).build();

            ReadSheet readSheet = buildReadSheet(sheetNo, sheetName, headRow);
            reader.read(readSheet);
            latch.await();
            reader.finish();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Excel read interrupted", e);
        }

        log.debug("Read noModel Excel done, sheetNo:{}, sheetName:{}, headRowNumber:{}, total rows:{}",
                sheetNo, sheetName, headRow, result.size());
        return result;
    }

    /**
     * 无实体类同步读取 Excel，将标题行作为 Map 的 key，返回带表头映射的数据列表。
     *
     * @param inputStream   输入流
     * @param sheetNo       Sheet 编号（从 0 开始），为 null 则读取第一个 Sheet
     * @param headRowNumber 标题所在行号（1-based），必须 >= 1
     * @return 数据列表，每个 Map 的 key 为标题名称，value 为单元格字符串
     */
    public static List<Map<String, String>> readWithHeadSync(InputStream inputStream,
                                                             Integer sheetNo,
                                                             Integer headRowNumber) {
        return readWithHeadSync(inputStream, sheetNo, null, headRowNumber);
    }

    /**
     * 无实体类同步读取 Excel（按 Sheet 名称 + 表头映射）
     *
     * @param inputStream   输入流
     * @param sheetName     Sheet 名称
     * @param headRowNumber 标题所在行号（1-based），必须 >= 1
     * @return 数据列表
     */
    public static List<Map<String, String>> readWithHeadSyncByName(InputStream inputStream,
                                                                    String sheetName,
                                                                    Integer headRowNumber) {
        return readWithHeadSync(inputStream, null, sheetName, headRowNumber);
    }

    /**
     * 无实体类同步读取 Excel（支持按 Sheet 序号或名称 + 表头映射）
     *
     * @param inputStream   输入流
     * @param sheetNo       Sheet 编号（从 0 开始），为 null 则使用 sheetName
     * @param sheetName     Sheet 名称
     * @param headRowNumber 标题所在行号（1-based），必须 >= 1
     * @return 数据列表
     */
    public static List<Map<String, String>> readWithHeadSync(InputStream inputStream,
                                                             Integer sheetNo,
                                                             String sheetName,
                                                             Integer headRowNumber) {
        if (headRowNumber == null || headRowNumber < 1) {
            throw new IllegalArgumentException("headRowNumber must >= 1");
        }

        List<Map<Integer, String>> allRows = readNoModelSync(inputStream, sheetNo, sheetName, 0);
        if (allRows.isEmpty() || allRows.size() < headRowNumber) {
            return Collections.emptyList();
        }

        Map<Integer, String> headerRow = allRows.get(headRowNumber - 1);
        Map<Integer, String> cleanHeader = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            String title = (entry.getValue() == null) ? "" : entry.getValue().trim();
            cleanHeader.put(entry.getKey(), title);
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (int i = headRowNumber; i < allRows.size(); i++) {
            Map<Integer, String> rawRow = allRows.get(i);
            Map<String, String> mappedRow = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> col : cleanHeader.entrySet()) {
                mappedRow.put(col.getValue(), rawRow.getOrDefault(col.getKey(), ""));
            }
            result.add(mappedRow);
        }

        log.debug("Read with head Excel done, sheetNo:{}, sheetName:{}, headRow:{}, data rows:{}",
                sheetNo, sheetName, headRowNumber, result.size());
        return result;
    }

    /**
     * 读取 Excel 的所有 Sheet 名称列表
     *
     * @param inputStream 输入流
     * @return Sheet 名称列表
     */
    public static List<String> readSheetNames(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
        List<String> sheetNames = new ArrayList<>();
        try {
            ExcelReader reader = EasyExcel.read(inputStream).build();
            List<ReadSheet> sheets = reader.excelExecutor().sheetList();
            for (ReadSheet sheet : sheets) {
                sheetNames.add(sheet.getSheetName());
            }
            reader.finish();
        } catch (Exception e) {
            log.error("Read sheet names error", e);
            throw new RuntimeException("读取 Sheet 名称失败：" + e.getMessage(), e);
        }
        return sheetNames;
    }

    // ==================== 导入（实体类 + 自定义监听器） ====================

    /**
     * 使用自定义监听器进行实体类导入（适合大文件批量+校验）
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      自定义监听器
     * @param sheetNo       Sheet 编号（从 0 开始），为 null 则读第一个
     * @param headRowNumber 标题行号（1-based），为 null 则默认 1
     */
    public static <T> void readSync(InputStream inputStream,
                                    Class<T> headClazz,
                                    ReadListener<T> listener,
                                    Integer sheetNo,
                                    Integer headRowNumber) {
        readSync(inputStream, headClazz, listener, sheetNo, null, headRowNumber);
    }

    /**
     * 使用自定义监听器进行实体类导入（按 Sheet 名称）
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      自定义监听器
     * @param sheetName     Sheet 名称（为 null 则读第一个）
     * @param headRowNumber 标题行号（1-based），为 null 则默认 1
     */
    public static <T> void readSyncByName(InputStream inputStream,
                                          Class<T> headClazz,
                                          ReadListener<T> listener,
                                          String sheetName,
                                          Integer headRowNumber) {
        readSync(inputStream, headClazz, listener, null, sheetName, headRowNumber);
    }

    /**
     * 使用自定义监听器进行实体类导入（支持按 Sheet 序号或名称）
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      自定义监听器
     * @param sheetNo       Sheet 编号（从 0 开始），为 null 则使用 sheetName
     * @param sheetName     Sheet 名称
     * @param headRowNumber 标题行号（1-based），为 null 则默认 1
     */
    public static <T> void readSync(InputStream inputStream,
                                    Class<T> headClazz,
                                    ReadListener<T> listener,
                                    Integer sheetNo,
                                    String sheetName,
                                    Integer headRowNumber) {
        int headRow = (headRowNumber == null) ? 1 : headRowNumber;

        ExcelReader reader = EasyExcel.read(inputStream, headClazz, listener).build();
        ReadSheet readSheet = buildReadSheet(sheetNo, sheetName, headRow);
        reader.read(readSheet);
        reader.finish();
        log.debug("Read with entity listener done, sheetNo:{}, sheetName:{}, headRow:{}", sheetNo, sheetName, headRow);
    }

    /**
     * 读取 Excel 的所有 Sheet 到实体类列表（每个 Sheet 一个 List）
     *
     * @param inputStream 输入流
     * @param headClazz   实体类
     * @param headRowNumber 标题行号（1-based），为 null 则默认 1
     * @param <T>         数据类型
     * @return Sheet 编号 → 数据列表
     */
    public static <T> Map<Integer, List<T>> readAllSheetsSync(InputStream inputStream,
                                                               Class<T> headClazz,
                                                               Integer headRowNumber) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
        int headRow = (headRowNumber == null) ? 1 : headRowNumber;
        Map<Integer, List<T>> result = new LinkedHashMap<>();

        ExcelReader reader = EasyExcel.read(inputStream, headClazz, new ReadListener<T>() {
            private int currentSheetNo = -1;
            private List<T> currentList = new ArrayList<>();

            @Override
            public void invoke(T data, AnalysisContext context) {
                int sheetNo = context.readSheetHolder().getSheetNo();
                if (sheetNo != currentSheetNo) {
                    if (currentSheetNo >= 0) {
                        result.put(currentSheetNo, currentList);
                    }
                    currentSheetNo = sheetNo;
                    currentList = new ArrayList<>();
                }
                currentList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                if (currentSheetNo >= 0) {
                    result.put(currentSheetNo, currentList);
                }
            }

            @Override
            public void onException(Exception exception, AnalysisContext context) {
                log.error("Read all sheets error", exception);
            }
        }).build();

        List<ReadSheet> sheets = reader.excelExecutor().sheetList();
        for (ReadSheet sheet : sheets) {
            sheet.setHeadRowNumber(headRow);
            reader.read(sheet);
        }
        reader.finish();

        log.debug("Read all sheets done, total sheets:{}", result.size());
        return result;
    }

    /**
     * 使用抽象监听器 {@link AbstractExcelListener} 进行导入，并直接返回 {@link ExcelMessage} 结果。
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      AbstractExcelListener 的子类实例
     * @param sheetNo       Sheet 编号（从 0 开始），null 默认 0
     * @param headRowNumber 标题行号（1-based），null 默认 1
     * @param <T>           数据类型
     * @return 导入结果消息
     */
    public static <T> ExcelMessage readWithAbstractListener(InputStream inputStream,
                                                            Class<T> headClazz,
                                                            AbstractExcelListener<T> listener,
                                                            Integer sheetNo,
                                                            Integer headRowNumber) {
        return readWithAbstractListener(inputStream, headClazz, listener, sheetNo, null, headRowNumber);
    }

    /**
     * 使用抽象监听器进行导入（按 Sheet 名称）
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      AbstractExcelListener 的子类实例
     * @param sheetName     Sheet 名称
     * @param headRowNumber 标题行号（1-based），null 默认 1
     * @param <T>           数据类型
     * @return 导入结果消息
     */
    public static <T> ExcelMessage readWithAbstractListenerByName(InputStream inputStream,
                                                                   Class<T> headClazz,
                                                                   AbstractExcelListener<T> listener,
                                                                   String sheetName,
                                                                   Integer headRowNumber) {
        return readWithAbstractListener(inputStream, headClazz, listener, null, sheetName, headRowNumber);
    }

    /**
     * 使用抽象监听器进行导入（支持按 Sheet 序号或名称）
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      AbstractExcelListener 的子类实例
     * @param sheetNo       Sheet 编号（从 0 开始），为 null 则使用 sheetName
     * @param sheetName     Sheet 名称
     * @param headRowNumber 标题行号（1-based），null 默认 1
     * @param <T>           数据类型
     * @return 导入结果消息
     */
    public static <T> ExcelMessage readWithAbstractListener(InputStream inputStream,
                                                            Class<T> headClazz,
                                                            AbstractExcelListener<T> listener,
                                                            Integer sheetNo,
                                                            String sheetName,
                                                            Integer headRowNumber) {
        readSync(inputStream, headClazz, listener, sheetNo, sheetName, headRowNumber);
        return listener.getExcelMessage();
    }

    /**
     * 构造一个简易的导入监听器（无需继承 {@link AbstractExcelListener}）
     * <p>
     * 适合简单场景：直接收集所有数据到 List，不做批量入库。
     * </p>
     *
     * @param dataConsumer 数据消费者（每读到一行调用一次）
     * @param <T>          数据类型
     * @return 简易监听器
     */
    public static <T> ReadListener<T> simpleListener(Consumer<T> dataConsumer) {
        return new ReadListener<T>() {
            @Override
            public void invoke(T data, AnalysisContext context) {
                if (data != null) {
                    dataConsumer.accept(data);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 无需处理
            }
        };
    }

    // ==================== 简单导出（实体类） ====================

    /**
     * 导出数据到输出流（基于实体类）
     *
     * @param outputStream   输出流
     * @param data           数据列表
     * @param headClazz      实体类
     * @param sheetName      Sheet 名称（为 null 则使用默认值）
     * @param customHandlers 自定义处理器
     */
    public static <T> void write(OutputStream outputStream,
                                 List<T> data,
                                 Class<T> headClazz,
                                 String sheetName,
                                 List<WriteHandler> customHandlers) {
        write(outputStream, data, headClazz, sheetName, customHandlers, false);
    }

    /**
     * 导出数据到输出流（基于实体类，支持大数据量模式）
     *
     * @param outputStream   输出流
     * @param data           数据列表
     * @param headClazz      实体类
     * @param sheetName      Sheet 名称
     * @param customHandlers 自定义处理器
     * @param largeData      是否启用大数据量模式（SXSSF，避免 OOM）
     * @param <T>            数据类型
     */
    public static <T> void write(OutputStream outputStream,
                                 List<T> data,
                                 Class<T> headClazz,
                                 String sheetName,
                                 List<WriteHandler> customHandlers,
                                 boolean largeData) {
        ExcelWriterBuilder builder = EasyExcel.write(outputStream, headClazz);
        if (largeData) {
            builder.inMemory(false);
        }
        registerHandlers(builder, customHandlers);
        builder.sheet(sheetName != null ? sheetName : DEFAULT_SHEET_NAME).doWrite(data);
        log.debug("Write Excel with model done, sheet:{}, rows:{}, largeData:{}", sheetName, data.size(), largeData);
    }

    /**
     * 导出数据为字节数组
     *
     * @param data           数据列表
     * @param headClazz      实体类
     * @param sheetName      Sheet 名称
     * @param customHandlers 自定义处理器
     */
    public static <T> byte[] writeToBytes(List<T> data,
                                          Class<T> headClazz,
                                          String sheetName,
                                          List<WriteHandler> customHandlers) {
        return writeToBytes(data, headClazz, sheetName, customHandlers, false);
    }

    /**
     * 导出数据为字节数组（支持大数据量模式）
     *
     * @param data           数据列表
     * @param headClazz      实体类
     * @param sheetName      Sheet 名称
     * @param customHandlers 自定义处理器
     * @param largeData      是否启用大数据量模式
     */
    public static <T> byte[] writeToBytes(List<T> data,
                                          Class<T> headClazz,
                                          String sheetName,
                                          List<WriteHandler> customHandlers,
                                          boolean largeData) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        write(bos, data, headClazz, sheetName, customHandlers, largeData);
        return bos.toByteArray();
    }

    /**
     * 导出数据到 HttpServletResponse
     *
     * @param response       HttpServletResponse
     * @param fileName       下载文件名（不含扩展名）
     * @param data           数据列表
     * @param headClazz      实体类
     * @param sheetName      Sheet 名称
     * @param customHandlers 自定义处理器
     */
    public static <T> void writeToResponse(HttpServletResponse response,
                                           String fileName,
                                           List<T> data,
                                           Class<T> headClazz,
                                           String sheetName,
                                           List<WriteHandler> customHandlers) throws IOException {
        writeToResponse(response, fileName, data, headClazz, sheetName, customHandlers, false);
    }

    /**
     * 导出数据到 HttpServletResponse（支持大数据量模式）
     *
     * @param response       HttpServletResponse
     * @param fileName       下载文件名（不含扩展名）
     * @param data           数据列表
     * @param headClazz      实体类
     * @param sheetName      Sheet 名称
     * @param customHandlers 自定义处理器
     * @param largeData      是否启用大数据量模式
     */
    public static <T> void writeToResponse(HttpServletResponse response,
                                           String fileName,
                                           List<T> data,
                                           Class<T> headClazz,
                                           String sheetName,
                                           List<WriteHandler> customHandlers,
                                           boolean largeData) throws IOException {
        setExcelResponse(response, fileName);
        write(response.getOutputStream(), data, headClazz, sheetName, customHandlers, largeData);
    }

    // ==================== 复杂表头导出（无实体类） ====================

    /**
     * 导出复杂多级表头数据到输出流（无实体类）
     *
     * @param outputStream       输出流
     * @param head               复杂表头
     * @param data               数据
     * @param sheetName          Sheet 名称
     * @param relativeHeadRowIndex 表头相对起始行
     * @param customHandlers     自定义处理器
     */
    public static void writeComplex(OutputStream outputStream,
                                    List<List<String>> head,
                                    List<List<Object>> data,
                                    String sheetName,
                                    int relativeHeadRowIndex,
                                    List<WriteHandler> customHandlers) {
        ExcelWriterBuilder builder = EasyExcel.write(outputStream).head(head);
        registerHandlers(builder, customHandlers);

        if (relativeHeadRowIndex == 0) {
            builder.sheet(sheetName != null ? sheetName : DEFAULT_SHEET_NAME).doWrite(data);
        } else {
            try (ExcelWriter excelWriter = builder.build()) {
                WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
                WriteTable writeTable = EasyExcel.writerTable()
                        .relativeHeadRowIndex(relativeHeadRowIndex)
                        .build();
                excelWriter.write(data, writeSheet, writeTable);
            }
        }
        log.debug("Write complex Excel done, sheet:{}, head rows:{}, data rows:{}, start row:{}",
                sheetName, head.size(), data.size(), relativeHeadRowIndex);
    }

    /**
     * 导出复杂多级表头为字节数组
     */
    public static byte[] writeComplexToBytes(List<List<String>> head,
                                             List<List<Object>> data,
                                             String sheetName,
                                             int relativeHeadRowIndex,
                                             List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeComplex(bos, head, data, sheetName, relativeHeadRowIndex, customHandlers);
        return bos.toByteArray();
    }

    /**
     * 导出复杂多级表头到 HttpServletResponse
     */
    public static void writeComplexToResponse(HttpServletResponse response,
                                              String fileName,
                                              List<List<String>> head,
                                              List<List<Object>> data,
                                              String sheetName,
                                              int relativeHeadRowIndex,
                                              List<WriteHandler> customHandlers) throws IOException {
        setExcelResponse(response, fileName);
        writeComplex(response.getOutputStream(), head, data, sheetName, relativeHeadRowIndex, customHandlers);
    }

    // ==================== 动态表头导出 ====================

    /**
     * 动态表头导出（运行时构建表头，无实体类）
     *
     * @param outputStream   输出流
     * @param head            表头（每列一个 List，多元素表示多级标题）
     * @param data            数据
     * @param sheetName       Sheet 名称
     * @param customHandlers  自定义处理器
     */
    public static void writeDynamicHead(OutputStream outputStream,
                                        List<List<String>> head,
                                        List<List<Object>> data,
                                        String sheetName,
                                        List<WriteHandler> customHandlers) {
        ExcelWriterBuilder builder = EasyExcel.write(outputStream).head(head);
        registerHandlers(builder, customHandlers);
        builder.sheet(sheetName != null ? sheetName : DEFAULT_SHEET_NAME).doWrite(data);
        log.debug("Write dynamic head Excel done, sheet:{}, columns:{}, rows:{}",
                sheetName, head.size(), data.size());
    }

    /**
     * 动态表头导出为字节数组
     */
    public static byte[] writeDynamicHeadToBytes(List<List<String>> head,
                                                  List<List<Object>> data,
                                                  String sheetName,
                                                  List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeDynamicHead(bos, head, data, sheetName, customHandlers);
        return bos.toByteArray();
    }

    // ==================== 多 Sheet 导出 ====================

    /**
     * 多 Sheet 导出到输出流
     *
     * @param outputStream   输出流
     * @param sheets         Sheet 数据列表
     * @param customHandlers 自定义处理器
     */
    public static void writeMultiSheet(OutputStream outputStream,
                                       List<SheetData> sheets,
                                       List<WriteHandler> customHandlers) {
        if (sheets == null || sheets.isEmpty()) {
            log.warn("No sheets to export");
            return;
        }

        ExcelWriterBuilder builder = EasyExcel.write(outputStream);
        registerHandlers(builder, customHandlers);

        try (ExcelWriter excelWriter = builder.build()) {
            for (int i = 0; i < sheets.size(); i++) {
                SheetData sheetData = sheets.get(i);
                String sheetName = sheetData.getSheetName() != null
                        ? sheetData.getSheetName()
                        : (DEFAULT_SHEET_NAME + (i + 1));
                WriteSheet writeSheet = EasyExcel.writerSheet(i, sheetName).build();

                WriteTable writeTable;
                if (sheetData.getHead() != null && !sheetData.getHead().isEmpty()) {
                    writeTable = EasyExcel.writerTable()
                            .head(sheetData.getHead())
                            .relativeHeadRowIndex(sheetData.getRelativeHeadRowIndex())
                            .build();
                } else {
                    writeTable = EasyExcel.writerTable()
                            .relativeHeadRowIndex(sheetData.getRelativeHeadRowIndex())
                            .build();
                }
                excelWriter.write(sheetData.getData(), writeSheet, writeTable);
            }
        }
        log.debug("Multi-sheet Excel export done, total sheets: {}", sheets.size());
    }

    /**
     * 多 Sheet 导出为字节数组
     */
    public static byte[] writeMultiSheetToBytes(List<SheetData> sheets,
                                                List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeMultiSheet(bos, sheets, customHandlers);
        return bos.toByteArray();
    }

    /**
     * 多 Sheet 导出到 HttpServletResponse
     */
    public static void writeMultiSheetToResponse(HttpServletResponse response,
                                                 String fileName,
                                                 List<SheetData> sheets,
                                                 List<WriteHandler> customHandlers) throws IOException {
        setExcelResponse(response, fileName);
        writeMultiSheet(response.getOutputStream(), sheets, customHandlers);
    }

    // ==================== 模板填充 ====================

    /**
     * 基于模板填充数据到输出流（支持动态列表）
     *
     * @param templateInputStream 模板输入流
     * @param data                填充数据（key-value，value 为 List 时按列表填充）
     * @param outputStream        输出流
     * @param fillConfig          填充配置（为 null 使用默认）
     * @param customHandlers      自定义处理器
     */
    public static void fillTemplate(InputStream templateInputStream,
                                    Map<String, Object> data,
                                    OutputStream outputStream,
                                    FillConfig fillConfig,
                                    List<WriteHandler> customHandlers) {
        if (fillConfig == null) {
            fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
        }

        ExcelWriterBuilder builder = EasyExcel.write(outputStream)
                .withTemplate(templateInputStream);
        registerHandlers(builder, customHandlers);

        try (ExcelWriter excelWriter = builder.build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet().build();
            excelWriter.fill(data, fillConfig, writeSheet);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof List) {
                    excelWriter.fill(new FillWrapper(entry.getKey(), (List<?>) entry.getValue()), fillConfig, writeSheet);
                }
            }
        }
        log.debug("Template fill done, filled fields: {}", data.keySet());
    }

    /**
     * 基于模板填充数据为字节数组
     */
    public static byte[] fillTemplateToBytes(InputStream templateInputStream,
                                             Map<String, Object> data,
                                             FillConfig fillConfig,
                                             List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        fillTemplate(templateInputStream, data, bos, fillConfig, customHandlers);
        return bos.toByteArray();
    }

    /**
     * 基于模板填充数据到 HttpServletResponse
     */
    public static void fillTemplateToResponse(HttpServletResponse response,
                                              String fileName,
                                              InputStream templateInputStream,
                                              Map<String, Object> data,
                                              FillConfig fillConfig,
                                              List<WriteHandler> customHandlers) throws IOException {
        setExcelResponse(response, fileName);
        fillTemplate(templateInputStream, data, response.getOutputStream(), fillConfig, customHandlers);
    }

    // ==================== CSV 导出 ====================

    /**
     * 导出 CSV 到输出流
     *
     * @param outputStream   输出流
     * @param data           数据列表
     * @param headClazz      实体类
     * @param sheetName      Sheet 名称（CSV 实际无 Sheet，仅作日志）
     * @param customHandlers 自定义处理器
     */
    public static <T> void writeCsv(OutputStream outputStream,
                                    List<T> data,
                                    Class<T> headClazz,
                                    String sheetName,
                                    List<WriteHandler> customHandlers) {
        ExcelWriterBuilder builder = EasyExcel.write(outputStream, headClazz)
                .excelType(ExcelTypeEnum.CSV);
        registerHandlers(builder, customHandlers);
        builder.sheet(sheetName != null ? sheetName : DEFAULT_SHEET_NAME).doWrite(data);
        log.debug("Write CSV done, rows:{}", data.size());
    }

    /**
     * 导出 CSV 为字节数组
     */
    public static <T> byte[] writeCsvToBytes(List<T> data,
                                              Class<T> headClazz,
                                              String sheetName,
                                              List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeCsv(bos, data, headClazz, sheetName, customHandlers);
        return bos.toByteArray();
    }

    /**
     * 导出 CSV 到 HttpServletResponse
     *
     * @param response  HttpServletResponse
     * @param fileName  下载文件名（不含扩展名）
     * @param data      数据列表
     * @param headClazz 实体类
     */
    public static <T> void writeCsvToResponse(HttpServletResponse response,
                                              String fileName,
                                              List<T> data,
                                              Class<T> headClazz,
                                              List<WriteHandler> customHandlers) throws IOException {
        setCsvResponse(response, fileName);
        writeCsv(response.getOutputStream(), data, headClazz, DEFAULT_SHEET_NAME, customHandlers);
    }

    // ==================== 辅助方法 ====================

    /**
     * 默认表头样式：灰色背景 + 宋体加粗居中
     *
     * @return HorizontalCellStyleStrategy
     */
    public static HorizontalCellStyleStrategy defaultStyleStrategy() {
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontName("宋体");
        headWriteFont.setBold(true);
        headWriteFont.setFontHeightInPoints((short) 11);
        headWriteCellStyle.setWriteFont(headWriteFont);
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        headWriteCellStyle.setBorderRight(BorderStyle.THIN);
        headWriteCellStyle.setBorderTop(BorderStyle.THIN);
        headWriteCellStyle.setBorderBottom(BorderStyle.THIN);

        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        WriteFont contentWriteFont = new WriteFont();
        contentWriteFont.setFontName("宋体");
        contentWriteFont.setFontHeightInPoints((short) 10);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);

        return new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
    }

    /**
     * 设置 Excel 下载响应头
     *
     * @param response HttpServletResponse
     * @param fileName 文件名（不含扩展名）
     */
    public static void setExcelResponse(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName + ".xlsx");
    }

    /**
     * 设置 CSV 下载响应头
     *
     * @param response HttpServletResponse
     * @param fileName 文件名（不含扩展名）
     */
    public static void setCsvResponse(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName + ".csv");
    }

    /**
     * 注册默认样式 + 自定义处理器到 Builder
     *
     * @param builder        ExcelWriterBuilder
     * @param customHandlers 自定义处理器列表
     */
    private static void registerHandlers(ExcelWriterBuilder builder, List<WriteHandler> customHandlers) {
        builder.registerWriteHandler(defaultStyleStrategy())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        if (customHandlers != null && !customHandlers.isEmpty()) {
            for (WriteHandler handler : customHandlers) {
                builder.registerWriteHandler(handler);
            }
        }
    }

    /**
     * 构建 ReadSheet（支持按序号或名称）
     *
     * @param sheetNo       Sheet 编号
     * @param sheetName     Sheet 名称
     * @param headRowNumber 标题行号
     * @return ReadSheet
     */
    private static ReadSheet buildReadSheet(Integer sheetNo, String sheetName, int headRowNumber) {
        ReadSheet readSheet = new ReadSheet();
        if (sheetNo != null) {
            readSheet.setSheetNo(sheetNo);
        }
        if (sheetName != null && !sheetName.isEmpty()) {
            readSheet.setSheetName(sheetName);
        }
        readSheet.setHeadRowNumber(headRowNumber);
        return readSheet;
    }

    // ==================== 内部数据类 ====================

    /**
     * 多 Sheet 导出数据封装
     */
    @Data
    public static class SheetData {

        /** Sheet 名称 */
        private String sheetName;

        /** 复杂表头（每列一个 List，多元素表示多级标题） */
        private List<List<String>> head;

        /** 数据列表 */
        private List<?> data;

        /** 表头相对起始行 */
        private int relativeHeadRowIndex = 0;

        public SheetData() {
        }

        public SheetData(String sheetName, List<?> data) {
            this.sheetName = sheetName;
            this.data = data;
        }

        public SheetData(String sheetName, List<List<String>> head, List<?> data) {
            this.sheetName = sheetName;
            this.head = head;
            this.data = data;
        }

        public SheetData(String sheetName, List<List<String>> head, List<?> data, int relativeHeadRowIndex) {
            this.sheetName = sheetName;
            this.head = head;
            this.data = data;
            this.relativeHeadRowIndex = relativeHeadRowIndex;
        }
    }
}
