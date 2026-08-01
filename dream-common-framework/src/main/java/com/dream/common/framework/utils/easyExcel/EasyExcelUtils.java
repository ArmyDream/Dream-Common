package com.dream.common.framework.utils.easyExcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
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

/**
 * EasyExcel 工具类 (基于 EasyExcel 4.0.3, JDK 8)
 * <p>
 * 提供以下能力：
 * <ul>
 *     <li>无实体类导入：读取为 List&lt;Map&gt; 或带表头映射的数据</li>
 *     <li>实体类导入：支持指定起始行、自定义监听器（批量+校验），返回 {@link ExcelMessage}</li>
 *     <li>多级标题导出：通过复杂表头 {@code List<List<String>>} 实现自动合并</li>
 *     <li>样式控制：内置默认样式，可注入自定义 WriteHandler</li>
 *     <li>多 Sheet 导出：支持一次写入多个不同数据的 Sheet</li>
 *     <li>模板填充：支持动态列表数据和普通数据混合填充</li>
 *     <li>输出方式：支持返回字节数组或直接写入 HttpServletResponse</li>
 * </ul>
 *
 * @author dream
 * @since 2026-08-01
 */
@Slf4j
public final class EasyExcelUtils {

    private EasyExcelUtils() {
    }

    // ==================== 导入（无实体类） ====================

    /**
     * 无实体类同步读取 Excel，将所有行解析为 Map&lt;列索引, 字符串值&gt;。
     * <p>
     * 当 {@code headRowNumber = 0} 时，每一行都是数据，不进行标题识别；
     * 当 {@code headRowNumber > 0} 时，标题行会被跳过（不包含在结果中）。
     *
     * @param inputStream   输入流
     * @param sheetNo       sheet 编号（从 0 开始），为 null 则读取第一个 sheet
     * @param headRowNumber 标题所在行号（1-based），0 表示无标题，所有行都作为数据
     * @return 读取结果，每行为一个 Map，key 为列索引 (0-based)，value 为单元格字符串
     */
    public static List<Map<Integer, String>> readNoModelSync(InputStream inputStream,
                                                             Integer sheetNo,
                                                             Integer headRowNumber) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
        int sheetIndex = (sheetNo == null) ? 0 : sheetNo;
        int headRow = (headRowNumber == null) ? 0 : headRowNumber;

        List<Map<Integer, String>> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
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
            }).sheet(sheetIndex).headRowNumber(headRow).doRead();

            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Excel read interrupted", e);
        }

        log.debug("Read noModel Excel done, sheet:{}, headRowNumber:{}, total rows:{}",
                sheetIndex, headRow, result.size());
        return result;
    }

    /**
     * 无实体类同步读取 Excel，将标题行作为 Map 的 key，返回带表头映射的数据列表。
     * <p>
     * 要求 {@code headRowNumber >= 1}，该行会被提取为字段名称（去前后空格），
     * 数据从标题行的下一行开始读取，并以标题名称映射。
     *
     * @param inputStream   输入流
     * @param sheetNo       sheet 编号（从 0 开始），为 null 则读取第一个 sheet
     * @param headRowNumber 标题所在行号（1-based），必须 >=1
     * @return 数据列表，每个 Map 的 key 为标题名称，value 为单元格字符串。若无数据返回空列表
     */
    public static List<Map<String, String>> readWithHeadSync(InputStream inputStream,
                                                             Integer sheetNo,
                                                             Integer headRowNumber) {
        if (headRowNumber == null || headRowNumber < 1) {
            throw new IllegalArgumentException("headRowNumber must >= 1");
        }

        List<Map<Integer, String>> allRows = readNoModelSync(inputStream, sheetNo, 0);
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

        log.debug("Read with head Excel done, sheet:{}, headRow:{}, data rows:{}",
                sheetNo, headRowNumber, result.size());
        return result;
    }

    // ==================== 导入（实体类 + 自定义监听器） ====================

    /**
     * 使用自定义监听器进行实体类导入（适合大文件批量+校验）
     * <p>
     * 调用方需实现 {@link AbstractExcelListener}，或使用普通 {@link ReadListener}。
     * 推荐直接使用 {@link #readWithAbstractListener} 获取封装好的结果。
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      自定义监听器
     * @param sheetNo       sheet 编号（从 0 开始），为 null 则读第一个
     * @param headRowNumber 标题行号（1-based），为 null 则默认1
     */
    public static <T> void readSync(InputStream inputStream,
                                    Class<T> headClazz,
                                    ReadListener<T> listener,
                                    Integer sheetNo,
                                    Integer headRowNumber) {
        int sheetIndex = (sheetNo == null) ? 0 : sheetNo;
        int headRow = (headRowNumber == null) ? 1 : headRowNumber;

        EasyExcel.read(inputStream, headClazz, listener)
                .sheet(sheetIndex)
                .headRowNumber(headRow)
                .doRead();
        log.debug("Read with entity listener done, sheet:{}, headRow:{}", sheetIndex, headRow);
    }

    /**
     * 使用抽象监听器 {@link AbstractExcelListener} 进行导入，并直接返回 {@link ExcelMessage} 结果。
     * <p>
     * 示例：
     * <pre>{@code
     * ExcelMessage result = EasyExcelUtils.readWithAbstractListener(
     *     inputStream, UserDTO.class, new AbstractExcelListener<UserDTO>() {
     *         protected void batchInsert(List<UserDTO> list, ExcelMessage message) {
     *             userService.saveBatch(list);
     *             message.setSuccessCount(message.getSuccessCount() + list.size()); // 手工增加或直接调用 add
     *         }
     *     }, 0, 1);
     * }</pre>
     *
     * @param inputStream   输入流
     * @param headClazz     实体类
     * @param listener      AbstractExcelListener 的子类实例
     * @param sheetNo       sheet 编号（从 0 开始），null 默认 0
     * @param headRowNumber 标题行号（1-based），null 默认 1
     * @param <T>           数据类型
     * @return 导入结果消息
     */
    public static <T> ExcelMessage readWithAbstractListener(InputStream inputStream,
                                                            Class<T> headClazz,
                                                            AbstractExcelListener<T> listener,
                                                            Integer sheetNo,
                                                            Integer headRowNumber) {
        readSync(inputStream, headClazz, listener, sheetNo, headRowNumber);
        return listener.getExcelMessage();
    }

    // ==================== 简单导出（实体类） ====================

    /**
     * 导出数据到输出流（基于实体类）。
     */
    public static <T> void write(OutputStream outputStream,
                                 List<T> data,
                                 Class<T> headClazz,
                                 String sheetName,
                                 List<WriteHandler> customHandlers) {
        ExcelWriterBuilder builder = EasyExcel.write(outputStream, headClazz);
        registerHandlers(builder, customHandlers);
        builder.sheet(sheetName).doWrite(data);
        log.debug("Write Excel with model done, sheet:{}, rows:{}", sheetName, data.size());
    }

    public static <T> byte[] writeToBytes(List<T> data,
                                          Class<T> headClazz,
                                          String sheetName,
                                          List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        write(bos, data, headClazz, sheetName, customHandlers);
        return bos.toByteArray();
    }

    public static <T> void writeToResponse(HttpServletResponse response,
                                           String fileName,
                                           List<T> data,
                                           Class<T> headClazz,
                                           String sheetName,
                                           List<WriteHandler> customHandlers) throws IOException {
        setExcelResponse(response, fileName);
        write(response.getOutputStream(), data, headClazz, sheetName, customHandlers);
    }

    // ==================== 复杂表头导出（无实体类） ====================

    /**
     * 导出复杂多级表头数据到输出流（无实体类）。
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
            builder.sheet(sheetName).doWrite(data);
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

    public static byte[] writeComplexToBytes(List<List<String>> head,
                                             List<List<Object>> data,
                                             String sheetName,
                                             int relativeHeadRowIndex,
                                             List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeComplex(bos, head, data, sheetName, relativeHeadRowIndex, customHandlers);
        return bos.toByteArray();
    }

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

    // ==================== 多 Sheet 导出 ====================

    /**
     * 多 Sheet 导出到输出流。
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
                WriteSheet writeSheet = EasyExcel.writerSheet(i, sheetData.getSheetName()).build();

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

    public static byte[] writeMultiSheetToBytes(List<SheetData> sheets,
                                                List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeMultiSheet(bos, sheets, customHandlers);
        return bos.toByteArray();
    }

    public static void writeMultiSheetToResponse(HttpServletResponse response,
                                                 String fileName,
                                                 List<SheetData> sheets,
                                                 List<WriteHandler> customHandlers) throws IOException {
        setExcelResponse(response, fileName);
        writeMultiSheet(response.getOutputStream(), sheets, customHandlers);
    }

    // ==================== 模板填充 ====================

    /**
     * 基于模板填充数据到输出流（支持动态列表）。
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

    public static byte[] fillTemplateToBytes(InputStream templateInputStream,
                                             Map<String, Object> data,
                                             FillConfig fillConfig,
                                             List<WriteHandler> customHandlers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        fillTemplate(templateInputStream, data, bos, fillConfig, customHandlers);
        return bos.toByteArray();
    }

    public static void fillTemplateToResponse(HttpServletResponse response,
                                              String fileName,
                                              InputStream templateInputStream,
                                              Map<String, Object> data,
                                              FillConfig fillConfig,
                                              List<WriteHandler> customHandlers) throws IOException {
        setExcelResponse(response, fileName);
        fillTemplate(templateInputStream, data, response.getOutputStream(), fillConfig, customHandlers);
    }

    // ==================== 辅助方法 ====================

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

    public static void setExcelResponse(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName + ".xlsx");
    }

    private static void registerHandlers(ExcelWriterBuilder builder, List<WriteHandler> customHandlers) {
        builder.registerWriteHandler(defaultStyleStrategy())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        if (customHandlers != null && !customHandlers.isEmpty()) {
            for (WriteHandler handler : customHandlers) {
                builder.registerWriteHandler(handler);
            }
        }
    }

    // ==================== 内部数据类 ====================

    @Data
    public static class SheetData {
        private String sheetName;
        private List<List<String>> head;
        private List<?> data;
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