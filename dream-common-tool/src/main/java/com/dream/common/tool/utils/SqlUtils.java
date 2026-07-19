package com.dream.common.tool.utils;

import com.dream.common.core.enums.DbType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL 工具类
 * <p>
 * 提供 SQL 相关能力：注入检测、标识符转义、动态条件构建、分页 SQL 生成等。
 * 与 {@link DbType} 枚举配合，支持多数据库方言适配。
 * </p>
 * <p>
 * <b>统一返回约定：</b>
 * <br>
 * 所有条件构建方法在参数无效时统一返回 <b>空字符串 ""</b>，由 {@link #buildWhere(String...)} 统一过滤，
 * 避免生成 "1=1" 或 "1=0" 等影响索引使用的恒等条件。
 * </p>
 * <p>
 * <b>安全说明：</b>
 * <br>
 * 本工具类提供的 SQL 拼接能力仅为辅助开发，<strong>强烈建议</strong>在生产环境中使用
 * 预编译（PreparedStatement）或 MyBatis 等 ORM 框架，本工具类不替代预编译的安全保障。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>
 * // 1. 条件构建（空条件自动忽略）
 * List&lt;String&gt; conds = new ArrayList&lt;&gt;();
 * conds.add(SqlUtils.buildEqualClause("status", 1));          // "status = 1"
 * conds.add(SqlUtils.buildInClause("type", emptyList));       // ""（空集合）
 * conds.add(SqlUtils.buildLikeClause("name", null, null));    // ""（keyword为空）
 * String where = SqlUtils.buildWhere(conds);  // 自动过滤空条件
 *
 * // 2. 分页 SQL
 * String sql = SqlUtils.buildPaginationSql("SELECT * FROM users", 1, 10, DbType.MYSQL);
 * </pre>
 *
 * @author dream
 * @see DbType
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SqlUtils {

    // ==================== 常量 ====================

    /**
     * SQL 注入高危关键词正则（忽略大小写）
     */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\b(OR|AND)\\b\\s+\\d+\\s*=\\s*\\d+" +
                    "|\\b(UNION)\\s+(ALL\\s+)?SELECT" +
                    "|;\\s*(DROP|ALTER|TRUNCATE|DELETE|INSERT|UPDATE|CREATE|EXEC)\\b" +
                    "|--\\s*$" +
                    "|/\\*.*\\*/" +
                    "|\\b(SLEEP|BENCHMARK|WAITFOR)\\s*\\(" +
                    "|\\b(CHAR|CONCAT|CONVERT|CAST)\\s*\\(" +
                    "|\\bINTO\\s+OUTFILE" +
                    "|\\bLOAD_FILE\\s*\\(" +
                    ")"
    );

    /**
     * 合法标识符正则：字母、下划线、中文开头，仅含字母、数字、下划线、中文
     */
    private static final Pattern VALID_IDENTIFIER_PATTERN =
            Pattern.compile("^[a-zA-Z_\\u4e00-\\u9fa5][a-zA-Z0-9_\\u4e00-\\u9fa5]*$");

    /**
     * 排序方向常量
     */
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";

    /**
     * 限制最大分页大小（防止恶意超大分页）
     */
    private static final int MAX_PAGE_SIZE = 10000;

    // ==================== 私有构造器 ====================

    private SqlUtils() {
        throw new UnsupportedOperationException("SqlUtils is a utility class");
    }

    // ==================== 枚举定义 ====================

    /**
     * LIKE 模糊匹配类型
     */
    @Getter
    public enum LikeType {
        /** 全模糊：%value% */
        FULL("%%%s%%"),
        /** 左模糊：%value */
        LEFT("%%%s"),
        /** 右模糊：value% */
        RIGHT("%s%%");

        private final String pattern;

        LikeType(String pattern) {
            this.pattern = pattern;
        }
    }

    /**
     * 排序方向枚举
     */
    @Getter
    public enum SortDirection {
        ASC("ASC"),
        DESC("DESC");

        private final String value;

        SortDirection(String value) {
            this.value = value;
        }

        public static SortDirection fromBoolean(boolean isAsc) {
            return isAsc ? ASC : DESC;
        }
    }

    // ==================== DTO 类 ====================

    /**
     * 排序项（列名 + 方向）
     */
    @Data
    @Builder
    public static class OrderItem {
        private final String column;
        private final SortDirection direction;

        public static OrderItem asc(String column) {
            return new OrderItem(column, SortDirection.ASC);
        }

        public static OrderItem desc(String column) {
            return new OrderItem(column, SortDirection.DESC);
        }

        public static OrderItem of(String column, boolean isAsc) {
            return new OrderItem(column, SortDirection.fromBoolean(isAsc));
        }
    }

    /**
     * 分页结果（包含 SQL 和总行数查询 SQL）
     */
    @Data
    @Builder
    public static class PageSql {
        private final String sql;
        private final String countSql;

        public PageSql(String sql, String countSql) {
            this.sql = sql;
            this.countSql = countSql;
        }
    }

    // ==================== SQL 注入检测 ====================

    /**
     * 检测字符串是否存在 SQL 注入风险
     *
     * @param input 待检测字符串
     * @return true=存在注入风险；input 为空时返回 false
     */
    public static boolean hasInjectionRisk(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }
        boolean risk = SQL_INJECTION_PATTERN.matcher(input).find();
        if (risk && log.isWarnEnabled()) {
            log.warn("SQL injection risk detected: {}", input);
        }
        return risk;
    }

    /**
     * 校验标识符是否合法（表名/列名）
     *
     * @param identifier 标识符
     * @return true=合法；identifier 为空返回 false
     */
    public static boolean isValidIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return false;
        }
        return VALID_IDENTIFIER_PATTERN.matcher(identifier).matches();
    }

    /**
     * 校验标识符，不合法时抛出异常
     *
     * @param identifier 标识符
     * @throws IllegalArgumentException 标识符不合法时抛出
     */
    public static void validateIdentifier(String identifier) {
        if (!isValidIdentifier(identifier)) {
            throw new IllegalArgumentException("非法 SQL 标识符: " + identifier);
        }
    }

    // ==================== 标识符转义 ====================

    /**
     * 转义 SQL 标识符（表名/列名），防止与数据库关键字冲突
     *
     * @param identifier 标识符
     * @param dbType     数据库类型（为 null 时使用 MySQL）
     * @return 转义后的标识符；identifier 为空时返回空字符串
     */
    public static String escapeIdentifier(String identifier, DbType dbType) {
        if (StringUtils.isBlank(identifier)) {
            return "";
        }

        DbType type = dbType != null ? dbType : DbType.MYSQL;

        if (type.isMysqlCompatible()) {
            return "`" + identifier.replace("`", "``") + "`";
        }

        if (type == DbType.SQL_SERVER) {
            return "[" + identifier.replace("]", "]]") + "]";
        }

        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * 转义标识符（默认 MySQL）
     *
     * @param identifier 标识符
     * @return 转义后的标识符；identifier 为空返回空字符串
     */
    public static String escapeIdentifier(String identifier) {
        return escapeIdentifier(identifier, DbType.MYSQL);
    }

    /**
     * 批量转义标识符（用逗号连接）
     *
     * @param identifiers 标识符集合
     * @param dbType      数据库类型
     * @return 逗号分隔的转义后标识符；集合为空返回空字符串
     */
    public static String escapeIdentifiers(Collection<String> identifiers, DbType dbType) {
        if (StringUtils.isObjectBlank(identifiers)) {
            return "";
        }
        return identifiers.stream()
                .map(id -> escapeIdentifier(id, dbType))
                .collect(Collectors.joining(", "));
    }

    // ==================== 条件构建（统一返回空字符串） ====================

    /**
     * 构建 IN 子句（Collection 入参）
     * <p>
     * 当 values 为 null 或空集合时，返回空字符串。
     * </p>
     *
     * @param column 列名（不能为空）
     * @param values 值集合
     * @return IN 子句，如 "status IN (1, 2, 3)"；参数无效返回空字符串
     */
    public static String buildInClause(String column, Collection<?> values) {
        if (StringUtils.isBlank(column) || StringUtils.isObjectBlank(values)) {
            return "";
        }
        String valueStr = values.stream()
                .map(SqlUtils::formatValue)
                .collect(Collectors.joining(", "));
        return column + " IN (" + valueStr + ")";
    }

    /**
     * 构建 IN 子句（逗号分隔字符串入参）
     * <p>
     * 将逗号分隔的字符串拆分为数组后调用 {@link #buildInClause(String, Collection)}。
     * 若 values 为空字符串或只包含空白，返回空字符串。
     * </p>
     *
     * @param column 列名
     * @param values 逗号分隔的字符串，如 "1,2,3"
     * @return IN 子句；参数无效返回空字符串
     */
    public static String buildInClause(String column, String values) {
        if (StringUtils.isBlank(column) || StringUtils.isBlank(values)) {
            return "";
        }
        String[] arr = values.split(",");
        List<String> list = Arrays.stream(arr)
                .filter(s -> !StringUtils.isBlank(s))
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            return "";
        }
        return buildInClause(column, list);
    }

    /**
     * 构建 IN 子句（数组入参）
     *
     * @param column 列名
     * @param values 值数组
     * @param <T>    数组元素类型
     * @return IN 子句；参数无效返回空字符串
     */
    public static <T> String buildInClause(String column, T[] values) {
        if (StringUtils.isBlank(column) || StringUtils.isObjectBlank(values)) {
            return "";
        }
        return buildInClause(column, Arrays.asList(values));
    }

    /**
     * 构建 NOT IN 子句（Collection 入参）
     *
     * @param column 列名
     * @param values 值集合
     * @return NOT IN 子句；参数无效返回空字符串
     */
    public static String buildNotInClause(String column, Collection<?> values) {
        if (StringUtils.isBlank(column) || StringUtils.isObjectBlank(values)) {
            return "";
        }
        String valueStr = values.stream()
                .map(SqlUtils::formatValue)
                .collect(Collectors.joining(", "));
        return column + " NOT IN (" + valueStr + ")";
    }

    /**
     * 构建 BETWEEN 子句
     * <p>
     * 智能处理 null 值：
     * <ul>
     *   <li>start 和 end 都不为 null：BETWEEN start AND end</li>
     *   <li>仅 start 不为 null：>= start</li>
     *   <li>仅 end 不为 null：<= end</li>
     *   <li>都为 null：返回空字符串</li>
     * </ul>
     * </p>
     *
     * @param column 列名
     * @param start  起始值（可为 null）
     * @param end    结束值（可为 null）
     * @return BETWEEN 子句；都为空返回空字符串
     */
    public static String buildBetweenClause(String column, Object start, Object end) {
        if (StringUtils.isBlank(column)) {
            return "";
        }

        if (start != null && end != null) {
            return column + " BETWEEN " + formatValue(start) + " AND " + formatValue(end);
        }

        if (start != null) {
            return column + " >= " + formatValue(start);
        }

        if (end != null) {
            return column + " <= " + formatValue(end);
        }

        return "";
    }

    /**
     * 构建 LIKE 条件
     * <p>
     * keyword 为空时返回空字符串。
     * </p>
     *
     * @param column   列名
     * @param keyword  关键词
     * @param likeType 模糊类型（为 null 时使用 FULL）
     * @return LIKE 条件；keyword 为空返回空字符串
     */
    public static String buildLikeClause(String column, String keyword, LikeType likeType) {
        if (StringUtils.isBlank(column) || StringUtils.isBlank(keyword)) {
            return "";
        }
        LikeType type = likeType != null ? likeType : LikeType.FULL;
        String escaped = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String pattern = String.format(type.getPattern(), escaped);
        return column + " LIKE '" + pattern + "' ESCAPE '\\\\'";
    }

    /**
     * 构建等值条件
     * <p>
     * value 为 null 时返回 "column IS NULL"。
     * </p>
     *
     * @param column 列名
     * @param value  值（可为 null）
     * @return 等值条件；column 为空返回空字符串
     */
    public static String buildEqualClause(String column, Object value) {
        if (StringUtils.isBlank(column)) {
            return "";
        }
        if (value == null) {
            return column + " IS NULL";
        }
        return column + " = " + formatValue(value);
    }

    /**
     * 构建不等值条件
     *
     * @param column 列名
     * @param value  值（可为 null）
     * @return 不等值条件；column 为空返回空字符串
     */
    public static String buildNotEqualClause(String column, Object value) {
        if (StringUtils.isBlank(column)) {
            return "";
        }
        if (value == null) {
            return column + " IS NOT NULL";
        }
        return column + " != " + formatValue(value);
    }

    /**
     * 构建 IS NULL / IS NOT NULL 条件
     *
     * @param column    列名
     * @param isNotNull true=IS NOT NULL，false=IS NULL
     * @return 条件片段；column 为空返回空字符串
     */
    public static String buildNullCheck(String column, boolean isNotNull) {
        if (StringUtils.isBlank(column)) {
            return "";
        }
        return column + (isNotNull ? " IS NOT NULL" : " IS NULL");
    }

    // ==================== ORDER BY 构建 ====================

    /**
     * 构建 ORDER BY 子句（使用 OrderItem 列表）
     *
     * @param items 排序项列表
     * @return ORDER BY 子句；items 为空返回空字符串
     */
    public static String buildOrderBy(Collection<OrderItem> items) {
        if (StringUtils.isObjectBlank(items)) {
            return "";
        }
        return items.stream()
                .filter(item -> !StringUtils.isBlank(item.getColumn()))
                .map(item -> item.getColumn() + " " + item.getDirection().getValue())
                .collect(Collectors.joining(", "));
    }

    /**
     * 构建 ORDER BY 子句（简化入参）
     * <p>
     * 示例：{@code buildOrderBy("name", true, "age", false)} → "name ASC, age DESC"
     * </p>
     *
     * @param columnAndAsc 列名与排序方向交替传入（列名, boolean, 列名, boolean...）
     * @return ORDER BY 子句；参数无效返回空字符串
     */
    public static String buildOrderBy(Object... columnAndAsc) {
        if (columnAndAsc == null || columnAndAsc.length == 0) {
            return "";
        }
        if (columnAndAsc.length % 2 != 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnAndAsc.length; i += 2) {
            String column = String.valueOf(columnAndAsc[i]);
            if (StringUtils.isBlank(column)) {
                continue;
            }
            boolean isAsc = Boolean.TRUE.equals(columnAndAsc[i + 1]);
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(column).append(" ").append(isAsc ? ASC : DESC);
        }
        return sb.toString();
    }

    // ==================== 分页 SQL 生成 ====================

    /**
     * 生成分页 SQL（自动适配数据库方言）
     * <p>
     * <b>注意：</b>SQL Server / Oracle 的 OFFSET...FETCH 语法必须配合 ORDER BY。
     * </p>
     *
     * @param baseSql  基础 SQL（不含分页）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @param dbType   数据库类型（为 null 时使用 MySQL）
     * @return 分页 SQL；baseSql 为空返回空字符串
     */
    public static String buildPaginationSql(String baseSql, int pageNum, int pageSize, DbType dbType) {
        if (StringUtils.isBlank(baseSql)) {
            return "";
        }

        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            log.warn("pageSize={} exceeds max limit {}, truncated to {}", pageSize, MAX_PAGE_SIZE, MAX_PAGE_SIZE);
            pageSize = MAX_PAGE_SIZE;
        }

        int offset = (pageNum - 1) * pageSize;
        DbType type = dbType != null ? dbType : DbType.MYSQL;

        // MySQL / PostgreSQL / SQLite / H2 / HSQL / Derby / DB2
        if (type.isMysqlCompatible() || type == DbType.SQLITE || type == DbType.H2
                || type == DbType.HSQL || type == DbType.DERBY || type == DbType.DB2) {
            return baseSql + " LIMIT " + pageSize + " OFFSET " + offset;
        }

        // Oracle / 达梦 / 人大金仓（12c+）
        if (type == DbType.ORACLE || type == DbType.DM || type == DbType.KINGBASE) {
            return baseSql + " OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
        }

        // SQL Server 2012+
        if (type == DbType.SQL_SERVER) {
            return baseSql + " OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
        }

        log.warn("未识别的数据库类型: {}，使用 MySQL 风格分页", type);
        return baseSql + " LIMIT " + pageSize + " OFFSET " + offset;
    }

    /**
     * 生成分页 SQL + 计数 SQL
     *
     * @param baseSql  基础 SQL
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param dbType   数据库类型
     * @return PageSql 对象；baseSql 为空返回 null
     */
    public static PageSql buildPageSql(String baseSql, int pageNum, int pageSize, DbType dbType) {
        if (StringUtils.isBlank(baseSql)) {
            return null;
        }
        return new PageSql(
                buildPaginationSql(baseSql, pageNum, pageSize, dbType),
                buildCountSql(baseSql)
        );
    }

    /**
     * 生成 COUNT SQL
     *
     * @param baseSql 基础 SQL
     * @return 计数 SQL；baseSql 为空返回空字符串
     */
    public static String buildCountSql(String baseSql) {
        if (StringUtils.isBlank(baseSql)) {
            return "";
        }
        return "SELECT COUNT(*) FROM (" + baseSql + ") _t";
    }

    // ==================== 动态 WHERE 构建（核心，自动过滤空字符串） ====================

    /**
     * 动态 WHERE 条件构建器
     * <p>
     * 自动拼接多个条件，忽略空字符串条件，用 AND 连接。
     * <b>无有效条件时返回空字符串</b>，避免生成 "1=1" 影响索引使用。
     * </p>
     *
     * @param conditions 条件片段（空字符串会被自动忽略）
     * @return WHERE 子句（含 WHERE 关键字），无条件时返回空字符串
     */
    public static String buildWhere(String... conditions) {
        if (conditions == null || conditions.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String c : conditions) {
            if (StringUtils.isBlank(c)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(c.trim());
        }

        return sb.length() > 0 ? "WHERE " + sb.toString() : "";
    }

    /**
     * 动态 WHERE 条件构建器（Collection 入参）
     *
     * @param conditions 条件片段集合
     * @return WHERE 子句
     */
    public static String buildWhere(Collection<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        return buildWhere(conditions.toArray(new String[0]));
    }

    // ==================== 值格式化 ====================

    /**
     * 格式化 SQL 值（自动判断类型并加引号）
     * <p>
     * 规则：
     * <ul>
     *   <li>null → "NULL"</li>
     *   <li>Number → 直接 toString</li>
     *   <li>Boolean → 直接 toString</li>
     *   <li>Enum → 返回枚举名称（name()）</li>
     *   <li>其他 → 单引号包裹，内部单引号转义为 ''</li>
     * </ul>
     * </p>
     *
     * @param value 值
     * @return 格式化后的 SQL 值字符串
     */
    public static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof Enum<?>) {
            return "'" + ((Enum<?>) value).name() + "'";
        }

        return "'" + value.toString().replace("'", "''") + "'";
    }

    /**
     * 转义 SQL 字符串中的单引号
     *
     * @param value 原始字符串
     * @return 转义后的字符串；value 为 null 返回 null
     */
    public static String escapeQuote(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("'", "''");
    }

    // ==================== CASE WHEN / 批量值 ====================

    /**
     * 生成 CASE WHEN 表达式
     *
     * @param column       列名
     * @param valueWhenMap 值映射（key=匹配值, value=返回值）
     * @param defaultValue 默认值（可为 null）
     * @return CASE WHEN 表达式；column 为空或 map 为空返回空字符串
     */
    public static String buildCaseWhen(String column, Map<Object, Object> valueWhenMap, Object defaultValue) {
        if (StringUtils.isBlank(column) || valueWhenMap == null || valueWhenMap.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("CASE ");
        for (Map.Entry<Object, Object> entry : valueWhenMap.entrySet()) {
            sb.append("WHEN ").append(column).append(" = ").append(formatValue(entry.getKey()));
            sb.append(" THEN ").append(formatValue(entry.getValue())).append(" ");
        }
        if (defaultValue != null) {
            sb.append("ELSE ").append(formatValue(defaultValue)).append(" ");
        }
        sb.append("END");
        return sb.toString();
    }

    /**
     * 生成批量 INSERT 的值行
     * <p>
     * 如：(1, 'a'), (2, 'b'), (3, 'c')
     * </p>
     *
     * @param rows 多行数据，每行为一组值
     * @return 值行字符串；rows 为空返回空字符串
     */
    public static String buildBatchValues(Collection<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        return rows.stream()
                .map(row -> {
                    String values = Arrays.stream(row)
                            .map(SqlUtils::formatValue)
                            .collect(Collectors.joining(", "));
                    return "(" + values + ")";
                })
                .collect(Collectors.joining(", "));
    }
}