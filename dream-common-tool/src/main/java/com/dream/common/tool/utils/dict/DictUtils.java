package com.dream.common.tool.utils.dict;

import com.dream.common.core.annotation.Dict;
import com.dream.common.core.enums.StringPool;
import com.dream.common.tool.utils.ReflectionUtils;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典工具类
 * <p>
 * 提供字典解析、转换、字段填充、树形字典等能力。
 * <p>
 * <b>注意：本工具类不包含任何缓存，每次调用均实时解析或查询。</b>
 * 如需缓存，请在各业务服务的 DictProvider 实现中自行处理。
 *
 * <p><b>初始化：</b>
 * <pre>{@code
 * @Configuration
 * public class DictConfig {
 *     @Autowired
 *     private DictService dictService;
 *
 *     @PostConstruct
 *     public void init() {
 *         DictUtils.setProvider(new DatabaseDictProvider(dictService));
 *     }
 * }
 * }</pre>
 *
 * @author dream
 */
@Slf4j
public final class DictUtils {

    private DictUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 字段 ====================

    /** 字典数据提供者 */
    private static DictProvider dictProvider;

    // ==================== 初始化 ====================

    /**
     * 设置字典数据提供者
     */
    public static void setProvider(DictProvider provider) {
        dictProvider = provider;
        log.info("DictUtils 已初始化，使用 DictProvider: {}", provider.getClass().getSimpleName());
    }

    /**
     * 判断是否已初始化
     */
    public static boolean isInitialized() {
        return dictProvider != null;
    }

    // ==================== 解析 mapping ====================

    /**
     * 解析 mapping 字符串为 DictItem 列表
     */
    public static List<DictItem> parseMapping(String mapping) {
        if (StringUtils.isBlank(mapping)) {
            return Collections.emptyList();
        }

        List<DictItem> items = new ArrayList<>();
        String[] entries = mapping.split(StringPool.COMMA.getValue());

        for (String entry : entries) {
            String trimmed = entry.trim();
            if (StringUtils.isBlank(trimmed)) {
                continue;
            }

            String[] parts = trimmed.split(StringPool.COLON.getValue());
            if (parts.length < 2) {
                log.warn("无效的字典映射条目: {}", trimmed);
                continue;
            }

            String value = parts[0].trim();
            String label = parts[1].trim();
            String icon = parts.length > 2 ? trimToNull(parts[2]) : null;
            String color = parts.length > 3 ? trimToNull(parts[3]) : null;
            boolean disabled = parts.length > 4 && "1".equals(parts[4].trim());
            boolean stop = parts.length > 5 && "1".equals(parts[5].trim());
            String parentValue = parts.length > 6 ? trimToNull(parts[6]) : null;
            Integer sort = parts.length > 7 ? parseInt(parts[7]) : null;

            items.add(new DictItem(value, label, icon, color, disabled, stop, parentValue, sort, null));
        }

        return items;
    }

    // ==================== 获取字典数据 ====================

    /**
     * 获取字典项列表（优先 mapping，其次 type）
     */
    public static List<DictItem> getDictItems(Dict dict) {
        if (dict == null || !dict.enable()) {
            return Collections.emptyList();
        }

        // 1. 优先使用 mapping
        if (StringUtils.isNotBlank(dict.mapping())) {
            return parseMapping(dict.mapping());
        }

        // 2. 其次使用 type
        if (StringUtils.isNotBlank(dict.type())) {
            if (dictProvider == null) {
                log.warn("DictProvider 未初始化，请调用 DictUtils.setProvider() 进行初始化");
                return Collections.emptyList();
            }
            if (!dictProvider.supports(dict.type())) {
                return Collections.emptyList();
            }
            return dictProvider.getDictItems(dict.type());
        }

        return Collections.emptyList();
    }

    /**
     * 根据字典类型获取字典项列表
     */
    public static List<DictItem> getDictItemsByType(String dictType) {
        if (StringUtils.isBlank(dictType) || dictProvider == null) {
            return Collections.emptyList();
        }
        if (!dictProvider.supports(dictType)) {
            return Collections.emptyList();
        }
        return dictProvider.getDictItems(dictType);
    }

    // ==================== 字典值查询 ====================

    /**
     * 根据字典值获取标签
     */
    public static String getLabel(Dict dict, String value) {
        if (value == null || dict == null) {
            return value;
        }
        List<DictItem> items = getDictItems(dict);
        return items.stream()
                .filter(item -> value.equals(item.getValue()))
                .findFirst()
                .map(DictItem::getLabel)
                .orElse(value);
    }

    /**
     * 根据字典值获取完整 DictItem
     */
    public static DictItem getDictItem(Dict dict, String value) {
        if (value == null || dict == null) {
            return null;
        }
        List<DictItem> items = getDictItems(dict);
        return items.stream()
                .filter(item -> value.equals(item.getValue()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据标签获取字典值（反向转换）
     */
    public static String getValue(Dict dict, String label) {
        if (label == null || dict == null) {
            return label;
        }
        List<DictItem> items = getDictItems(dict);
        return items.stream()
                .filter(item -> label.equals(item.getLabel()))
                .findFirst()
                .map(DictItem::getValue)
                .orElse(label);
    }

    // ==================== 自动转换 ====================

    /**
     * 自动转换对象中所有带 @Dict 注解的字段
     */
    public static int convert(Object obj) {
        if (obj == null) {
            return 0;
        }

        int count = 0;
        List<Field> dictFields = getDictFields(obj.getClass());

        for (Field field : dictFields) {
            Dict dict = field.getAnnotation(Dict.class);
            if (dict == null || !dict.enable()) {
                continue;
            }

            List<DictItem> items = getDictItems(dict);
            if (items.isEmpty()) {
                continue;
            }

            Object valueObj = ReflectionUtils.getFieldValue(obj, field.getName());
            if (valueObj == null) {
                continue;
            }

            String value = valueObj.toString();
            String fieldName = field.getName();

            DictItem item = items.stream()
                    .filter(i -> value.equals(i.getValue()))
                    .findFirst()
                    .orElse(null);

            if (item == null) {
                continue;
            }

            // 填充各个目标字段
            count += setField(obj, getTargetField(dict, fieldName), item.getLabel());
            count += setField(obj, getColorField(dict, fieldName), item.hasColor() ? item.getColor() : null);
            count += setField(obj, getIconField(dict, fieldName), item.hasIcon() ? item.getIcon() : null);
            count += setField(obj, getDisabledField(dict, fieldName), item.isDisabled());
            count += setField(obj, getStopField(dict, fieldName), item.isStop());

            if (StringUtils.isNotBlank(dict.extraField())) {
                count += setField(obj, dict.extraField(), item.getExtra());
            }
        }

        return count;
    }

    /**
     * 批量转换集合
     */
    public static int convert(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Object obj : collection) {
            count += convert(obj);
        }
        return count;
    }

    // ==================== 目标字段名生成 ====================

    public static String getTargetField(Dict dict, String fieldName) {
        if (dict == null || StringUtils.isBlank(fieldName)) {
            return fieldName;
        }
        if (StringUtils.isNotBlank(dict.targetField())) {
            return dict.targetField();
        }
        return dict.hump() ? fieldName + "Label" : fieldName + "label";
    }

    public static String getColorField(Dict dict, String fieldName) {
        if (dict == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (StringUtils.isNotBlank(dict.colorField())) {
            return dict.colorField();
        }
        if (!dict.includeColor()) {
            return null;
        }
        return fieldName + "Color";
    }

    public static String getIconField(Dict dict, String fieldName) {
        if (dict == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (StringUtils.isNotBlank(dict.iconField())) {
            return dict.iconField();
        }
        if (!dict.includeIcon()) {
            return null;
        }
        return fieldName + "Icon";
    }

    public static String getDisabledField(Dict dict, String fieldName) {
        if (dict == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (StringUtils.isNotBlank(dict.disabledField())) {
            return dict.disabledField();
        }
        if (!dict.includeDisabled()) {
            return null;
        }
        return fieldName + "Disabled";
    }

    public static String getStopField(Dict dict, String fieldName) {
        if (dict == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (StringUtils.isNotBlank(dict.stopField())) {
            return dict.stopField();
        }
        if (!dict.includeStop()) {
            return null;
        }
        return fieldName + "Stop";
    }

    // ==================== 树形字典 ====================

    /**
     * 构建字典树（利用 DictItem 自身的 @Tree 注解）
     * <p>
     * DictItem 已标注 @Tree 注解，无需额外配置
     *
     * @param dict Dict 注解
     * @return 树形字典项列表
     */
    public static List<DictItem> buildTree(Dict dict) {
        return buildTree(dict, null);
    }

    /**
     * 构建字典树（指定根节点值）
     *
     * @param dict          Dict 注解
     * @param rootNodeValue 根节点父值（null、空、空白 都表示根节点）
     * @return 树形字典项列表
     */
    public static List<DictItem> buildTree(Dict dict, String rootNodeValue) {
        List<DictItem> items = getDictItems(dict);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        String effectiveRoot = StringUtils.isBlank(rootNodeValue) ? "" : rootNodeValue;

        // 构建父子映射
        Map<String, List<DictItem>> parentMap = items.stream()
                .filter(item -> item.getParentValue() != null)
                .collect(Collectors.groupingBy(
                        DictItem::getParentValue,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 找出根节点
        List<DictItem> roots = items.stream()
                .filter(item -> isRootNode(item.getParentValue(), effectiveRoot))
                .collect(Collectors.toList());

        // 排序根节点
        sortItems(roots);

        // 递归构建子树
        for (DictItem root : roots) {
            buildChildren(root, parentMap);
        }

        return roots;
    }

    /**
     * 递归构建子节点
     */
    private static void buildChildren(DictItem parent, Map<String, List<DictItem>> parentMap) {
        String parentValue = parent.getValue();
        List<DictItem> children = parentMap.getOrDefault(parentValue, new ArrayList<>());

        // 排序
        sortItems(children);

        // 递归
        for (DictItem child : children) {
            buildChildren(child, parentMap);
        }

        parent.setChildren(children);
    }

    /**
     * 排序字典项
     */
    private static void sortItems(List<DictItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        items.sort(Comparator.comparingInt(
                item -> item.getSort() != null ? item.getSort() : 0
        ));
    }

    /**
     * 判断是否为根节点
     */
    private static boolean isRootNode(String parentValue, String rootNodeValue) {
        if (parentValue == null) {
            return true;
        }
        String trimmed = parentValue.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return trimmed.equals(rootNodeValue);
    }

    // ==================== 辅助方法 ====================

    private static List<Field> getDictFields(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        List<Field> allFields = ReflectionUtils.getAllFields(clazz);
        return allFields.stream()
                .filter(f -> f.isAnnotationPresent(Dict.class))
                .collect(Collectors.toList());
    }

    private static int setField(Object obj, String fieldName, Object value) {
        if (StringUtils.isBlank(fieldName) || value == null) {
            return 0;
        }
        ReflectionUtils.setFieldValue(obj, fieldName, value);
        return 1;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer parseInt(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}