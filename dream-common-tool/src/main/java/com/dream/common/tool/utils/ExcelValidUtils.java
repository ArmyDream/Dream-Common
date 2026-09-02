package com.dream.common.tool.utils;

import com.dream.common.core.annotation.ExcelValid;
import com.dream.common.core.enums.StringPool;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 注解工具类
 * <p>
 * 封装 {@link com.dream.common.core.annotation} 中各注解的解析与辅助操作：
 * <ul>
 *   <li>{@link ExcelValid} —— Excel 校验：字段校验、字典转换、批量校验</li>
 * </ul>
 * <p>
 * 注意：本类依赖 {@link ReflectionUtils}  {@link AnnotationUtils} 进行反射操作，
 * 使用前请确保目标类有无参构造器（用于实例化场景）。
 *
 * @author dream
 */
@SuppressWarnings("all")
public final class ExcelValidUtils {

    private ExcelValidUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }


    // ==================== ExcelValid 操作 ====================

    /**
     * 获取类中所有带 {@link ExcelValid} 注解的字段
     *
     * @param clazz 目标类
     * @return 带注解的字段列表
     */
    public static List<Field> getExcelValidFields(Class<?> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }
        return ReflectionUtils.getFieldsWithAnnotation(clazz, ExcelValid.class);
    }

    /**
     * 获取字段上的 {@link ExcelValid} 注解
     *
     * @param field 字段对象
     * @return 注解实例；未标注返回 null
     */
    public static ExcelValid getExcelValid(Field field) {
        return ReflectionUtils.getFieldAnnotation(field, ExcelValid.class);
    }

    /**
     * 获取字段的 ExcelValid 显示名称
     * <p>
     * 优先使用 {@link ExcelValid#fieldName()}，留空时返回字段名。
     *
     * @param field 字段对象
     * @return 显示名称
     */
    public static String getExcelValidFieldName(Field field) {
        if (field == null) {
            return StringUtils.EMPTY;
        }
        ExcelValid annotation = getExcelValid(field);
        if (annotation == null) {
            return field.getName();
        }
        return StringUtils.isNotBlank(annotation.fieldName()) ? annotation.fieldName() : field.getName();
    }

    /**
     * 解析 {@link ExcelValid#readConverterExp()} 为 Map
     * <p>
     * 格式："0=男,1=女,2=未知" → {0: "男", 1: "女", 2: "未知"}
     * <pre>
     * parseConverterExp("0=男,1=女,2=未知")
     *   → {"0"="男", "1"="女", "2"="未知"}
     * </pre>
     *
     * @param readConverterExp 转换表达式
     * @return key → value 映射 Map；表达式为空返回空 Map
     */
    public static Map<String, String> parseConverterExp(String readConverterExp) {
        Map<String, String> map = new LinkedHashMap<>();
        if (StringUtils.isBlank(readConverterExp)) {
            return map;
        }
        String[] pairs = readConverterExp.split(StringPool.COMMA.getValue());
        for (String pair : pairs) {
            String trimmed = pair.trim();
            int eqIndex = trimmed.indexOf(StringPool.EQUALS.getValue());
            if (eqIndex > 0 && eqIndex < trimmed.length() - 1) {
                String key = trimmed.substring(0, eqIndex).trim();
                String value = trimmed.substring(eqIndex + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * 反向解析转换表达式（value → key）
     * <p>
     * 常用于 Excel 导入时将显示值转回代码值。
     * <pre>
     * parseConverterExpReverse("0=男,1=女")
     *   → {"男"="0", "女"="1"}
     * </pre>
     *
     * @param readConverterExp 转换表达式
     * @return value → key 映射 Map
     */
    public static Map<String, String> parseConverterExpReverse(String readConverterExp) {
        Map<String, String> forward = parseConverterExp(readConverterExp);
        Map<String, String> reverse = new LinkedHashMap<>();
        forward.forEach((k, v) -> reverse.put(v, k));
        return reverse;
    }

    /**
     * 根据转换表达式转换值（key → value，导出方向）
     *
     * @param value            原始值（如 "0"）
     * @param readConverterExp 转换表达式（如 "0=男,1=女"）
     * @return 转换后的值（如 "男"）；无匹配返回原值
     */
    public static String convertByExp(Object value, String readConverterExp) {
        if (value == null || StringUtils.isBlank(readConverterExp)) {
            return value == null ? StringUtils.EMPTY : value.toString();
        }
        Map<String, String> map = parseConverterExp(readConverterExp);
        return map.getOrDefault(value.toString(), value.toString());
    }

    /**
     * 根据转换表达式反向转换值（value → key，导入方向）
     *
     * @param displayValue     显示值（如 "男"）
     * @param readConverterExp 转换表达式（如 "0=男,1=女"）
     * @return 代码值（如 "0"）；无匹配返回原值
     */
    public static String convertByExpReverse(Object displayValue, String readConverterExp) {
        if (displayValue == null || StringUtils.isBlank(readConverterExp)) {
            return displayValue == null ? StringUtils.EMPTY : displayValue.toString();
        }
        Map<String, String> map = parseConverterExpReverse(readConverterExp);
        return map.getOrDefault(displayValue.toString(), displayValue.toString());
    }

    /**
     * 对单个值执行 {@link ExcelValid} 校验
     * <p>
     * 校验顺序：required → minLength/maxLength → pattern → min/max → dateFormat
     * <pre>
     * validate(null, @ExcelValid(required=true, fieldName="手机号"))
     *   → ["手机号不能为空"]
     * validate("abc", @ExcelValid(minLength=5, fieldName="密码"))
     *   → ["密码长度不能少于5个字符"]
     * </pre>
     *
     * @param value      待校验值（可以是 String/Number 等）
     * @param annotation ExcelValid 注解实例
     * @return 错误信息列表；校验通过返回空列表
     */
    public static List<String> validate(Object value, ExcelValid annotation) {
        List<String> errors = new ArrayList<>();
        if (annotation == null) {
            return errors;
        }
        String fieldName = StringUtils.isNotBlank(annotation.fieldName()) ? annotation.fieldName() : "字段";
        String msg = annotation.message();
        String strValue = (value != null) ? value.toString() : null;

        // 1. 必填校验
        if (annotation.required() && StringUtils.isBlank(strValue)) {
            errors.add(AnnotationUtils.formatMsg(msg, fieldName + "不能为空"));
            return errors; // 必填失败直接返回
        }
        // 值为空且非必填，跳过后续校验
        if (StringUtils.isBlank(strValue)) {
            return errors;
        }

        // 2. 字符串长度校验
        if (annotation.minLength() > 0 && strValue.length() < annotation.minLength()) {
            errors.add(AnnotationUtils.formatMsg(msg, fieldName + "长度不能少于" + annotation.minLength() + "个字符"));
        }
        if (annotation.maxLength() > 0 && strValue.length() > annotation.maxLength()) {
            errors.add(AnnotationUtils.formatMsg(msg, fieldName + "长度不能超过" + annotation.maxLength() + "个字符"));
        }

        // 3. 正则格式校验
        String pattern = annotation.pattern();
        if (StringUtils.isNotBlank(pattern)) {
            try {
                if (!Pattern.matches(pattern, strValue)) {
                    String patternMsg = annotation.patternMessage();
                    if (StringUtils.isNotBlank(patternMsg)) {
                        errors.add(AnnotationUtils.formatMsg(patternMsg, fieldName + "格式不正确"));
                    } else {
                        errors.add(AnnotationUtils.formatMsg(msg, fieldName + "格式不正确"));
                    }
                }
            } catch (Exception e) {
                errors.add(fieldName + "的正则表达式无效：" + pattern);
            }
        }

        // 4. 数值范围校验
        if (value instanceof Number) {
            double numValue = ((Number) value).doubleValue();
            if (annotation.min() != Double.MIN_VALUE && numValue < annotation.min()) {
                errors.add(AnnotationUtils.formatMsg(msg, fieldName + "不能小于" + annotation.min()));
            }
            if (annotation.max() != Double.MAX_VALUE && numValue > annotation.max()) {
                errors.add(AnnotationUtils.formatMsg(msg, fieldName + "不能大于" + annotation.max()));
            }
        } else if (StringUtils.isNotBlank(strValue)) {
            // 尝试将字符串解析为数值进行范围校验
            try {
                double numValue = Double.parseDouble(strValue);
                if (annotation.min() != Double.MIN_VALUE && numValue < annotation.min()) {
                    errors.add(AnnotationUtils.formatMsg(msg, fieldName + "不能小于" + annotation.min()));
                }
                if (annotation.max() != Double.MAX_VALUE && numValue > annotation.max()) {
                    errors.add(AnnotationUtils.formatMsg(msg, fieldName + "不能大于" + annotation.max()));
                }
            } catch (NumberFormatException ignored) {
                // 非数值类型，跳过范围校验
            }
        }

        // 5. 日期格式校验
        String dateFormat = annotation.dateFormat();
        if (StringUtils.isNotBlank(dateFormat)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                sdf.setLenient(false);
                sdf.parse(strValue);
            } catch (ParseException e) {
                errors.add(AnnotationUtils.formatMsg(msg, fieldName + "日期格式不正确，期望格式：" + dateFormat));
            }
        }

        return errors;
    }

    /**
     * 批量校验对象中所有带 {@link ExcelValid} 注解的字段
     * <p>
     * 遍历对象所有带 @ExcelValid 的字段，逐一执行校验，汇总错误信息。
     * <pre>
     * List&lt;String&gt; errors = validateAll(userDTO);
     * if (!errors.isEmpty()) {
     *     throw new BusinessException(String.join("；", errors));
     * }
     * </pre>
     *
     * @param obj 待校验对象
     * @return 错误信息列表；全部通过返回空列表
     */
    public static List<String> validateAll(Object obj) {
        return validateAll(obj, (String[]) null);
    }

    /**
     * 批量校验对象（支持分组过滤）
     * <p>
     * 仅校验 {@link ExcelValid#groups()} 中包含指定 group 的字段。
     *
     * @param obj   待校验对象
     * @param group 分组标识（null 表示校验所有字段）
     * @return 错误信息列表
     */
    public static List<String> validateAll(Object obj, String... group) {
        List<String> errors = new ArrayList<>();
        if (obj == null) {
            errors.add("校验对象不能为 null");
            return errors;
        }
        Set<String> groupSet = (group != null && group.length > 0)
                ? new HashSet<>(Arrays.asList(group))
                : null;
        for (Field field : getExcelValidFields(obj.getClass())) {
            ExcelValid annotation = getExcelValid(field);
            if (annotation == null) {
                continue;
            }
            // 分组过滤：注解有分组配置时，检查是否包含当前分组
            String[] annotationGroups = annotation.groups();
            if (groupSet != null && annotationGroups.length > 0) {
                boolean match = false;
                for (String g : annotationGroups) {
                    if (groupSet.contains(g)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
            }
            // 取字段值
            Object value = ReflectionUtils.getFieldValue(obj, field.getName());
            List<String> fieldErrors = validate(value, annotation);
            errors.addAll(fieldErrors);
        }
        return errors;
    }

    /**
     * 根据 {@link ExcelValid#numberFormat()} 格式化数字
     * <pre>
     * formatNumber(1234567.8, "#,##0.00")  → "1,234,567.80"
     * formatNumber(3.14159, "0.00")        → "3.14"
     * </pre>
     *
     * @param value        数值
     * @param numberFormat 格式化模板
     * @return 格式化后的字符串；value 为 null 或模板为空返回原值字符串
     */
    public static String formatNumber(Object value, String numberFormat) {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isBlank(numberFormat)) {
            return value.toString();
        }
        try {
            DecimalFormat df = new DecimalFormat(numberFormat);
            if (value instanceof Number) {
                return df.format(value);
            }
            return df.format(Double.parseDouble(value.toString()));
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 构建 ExcelValid 字段映射（fieldName → Field）
     * <p>
     * 常用于 Excel 表头解析时，根据注解的 fieldName 找到对应的实体字段。
     *
     * @param clazz 目标类
     * @return fieldName → Field 映射；fieldName 为空时使用字段属性名
     */
    public static Map<String, Field> buildExcelValidFieldMap(Class<?> clazz) {
        Map<String, Field> map = new LinkedHashMap<>();
        if (clazz == null) {
            return map;
        }
        for (Field field : getExcelValidFields(clazz)) {
            String displayName = getExcelValidFieldName(field);
            map.put(displayName, field);
        }
        return map;
    }

}
