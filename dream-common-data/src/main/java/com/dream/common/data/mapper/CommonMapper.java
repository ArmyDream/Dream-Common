package com.dream.common.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 通用 Mapper 接口
 * <p>
 * 所有业务 Mapper 都应继承此接口，自动获得 MyBatis-Plus {@link BaseMapper} 的全部能力，
 * 同时提供大数据流式查询和编号生成等扩展方法。
 * </p>
 *
 * <h2>安全说明</h2>
 * <ul>
 *   <li>{@link #dynamicSelectOfLargeData} 使用 {@code ${sql}} 动态拼接，调用方必须自行确保 SQL 安全</li>
 *   <li>{@link #getMaxNumber} 对表名和字段名进行正则白名单校验，防止 SQL 注入</li>
 * </ul>
 *
 * @param <T> 实体类型
 * @author dream
 * @since 2026-08-21
 */
@Mapper
public interface CommonMapper<T> extends BaseMapper<T> {

    /**
     * 合法标识符正则：仅允许字母、数字、下划线，长度 1-64
     */
    Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,64}$");

    /**
     * 大数据流式查询，适用于大批量数据处理
     * <p>
     * 使用 {@code ResultSetType.FORWARD_ONLY} + {@code fetchSize} 实现游标式读取，
     * 避免一次性将全部数据加载到内存。适用于百万级数据导出、批处理等场景。
     * </p>
     *
     * <h3>安全警告</h3>
     * <p>
     * 本方法使用 {@code ${sql}} 直接拼接 SQL 语句，<b>不经过参数化处理</b>。
     * 调用方必须确保 SQL 语句中不包含用户可控的输入，或已进行充分的转义/校验。
     * </p>
     *
     * <h3>关于 fetchSize</h3>
     * <p>
     * 实际生效的抓取行数固定为 {@code @Options(fetchSize = 1000)}：该注解值是编译期常量，
     * 无法在运行时读取方法参数，因此下面的 {@code fetchSize} 形参<b>不会生效</b>，
     * 仅为保持既有方法签名而保留。需要其他抓取行数时，请在业务 Mapper 中自行声明
     * 带对应 {@code @Options(fetchSize = ...)} 的查询方法。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * mapper.dynamicSelectOfLargeData(
     *     "SELECT id, name FROM sys_user WHERE deleted = 0",
     *     resultContext -> {
     *         Map<String, Object> row = resultContext.getResultObject();
     *         // 逐行处理
     *     },
     *     null   // fetchSize 形参不生效，传 null 即可
     * );
     * }</pre>
     *
     * @param sql       动态 SQL 语句（调用方自行确保安全）
     * @param handler   结果处理器（每行数据回调一次）
     * @param fetchSize 预留参数，不生效；实际抓取行数固定为 1000，见上方说明
     */
    @Select("${sql}")
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(Map.class)
    void dynamicSelectOfLargeData(@Param("sql") String sql,
                                  ResultHandler<Map<String, Object>> handler,
                                  @Param("fetchSize") Integer fetchSize);

    /**
     * 获取表中指定字段的最大编号（格式：前缀 + 数字）
     * <p>
     * 常用于自动生成业务编号，如订单号 {@code ORD20260821001}。
     * </p>
     *
     * <h3>SQL 注入防护</h3>
     * <p>
     * 表名和字段名使用 {@code ${}} 动态拼接（MyBatis 不会对表名/列名参数化），
     * 但在拼接前通过 {@link #IDENTIFIER_PATTERN} 正则白名单校验，仅允许
     * 字母、数字、下划线，长度 1-64 字符，有效防止 SQL 注入。
     * </p>
     *
     * @param tableName 表名称（仅允许字母、数字、下划线）
     * @param field     字段名称（仅允许字母、数字、下划线）
     * @param prefix    编号前缀（例如：ORD20260821）
     * @return 最大编号字符串，无匹配数据时返回 null
     * @throws IllegalArgumentException 如果 tableName 或 field 不符合标识符规范
     */
    default String getMaxNumber(@Param("tableName") String tableName,
                                @Param("field") String field,
                                @Param("prefix") String prefix) {
        validateIdentifier(tableName, "tableName");
        validateIdentifier(field, "field");

        String sql = "SELECT MAX(" + field + ") FROM " + tableName
                + " WHERE " + field + " LIKE CONCAT(#{prefix}, '%')";

        return doGetMaxNumber(sql, prefix);
    }

    /**
     * 执行最大编号查询（内部方法，由 {@link #getMaxNumber} 调用）
     *
     * @param sql    已校验安全的 SQL 语句
     * @param prefix 编号前缀（参数化绑定）
     * @return 最大编号字符串
     */
    @Select("${sql}")
    String doGetMaxNumber(@Param("sql") String sql, @Param("prefix") String prefix);

    /**
     * 校验数据库标识符（表名、字段名等）是否合法
     *
     * @param identifier 标识符
     * @param paramName   参数名（用于异常消息）
     * @throws IllegalArgumentException 如果标识符不合法
     */
    default void validateIdentifier(String identifier, String paramName) {
        if (identifier == null || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "非法的数据库标识符 [" + paramName + "]: " + identifier
                            + "，仅允许字母、数字、下划线，长度 1-64");
        }
    }
}
