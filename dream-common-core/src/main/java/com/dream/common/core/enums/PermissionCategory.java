package com.dream.common.core.enums;

/**
 * 权限分类枚举。
 *
 * <p>用于对 {@link PermissionType} 进行分组，便于按类别查询权限、
 * 前端分组展示、统计分析。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 按分类查询
 * List<PermissionType> basicOps = PermissionType.listByCategory(PermissionCategory.BASIC);
 *
 * // 前端分组展示
 * Map<String, List<PermissionType>> grouped = Arrays.stream(PermissionCategory.values())
 *         .collect(Collectors.toMap(
 *                 PermissionCategory::getLabel,
 *                 PermissionType::listByCategory,
 *                 (a, b) -> a,
 *                 LinkedHashMap::new
 *         ));
 * }</pre>
 *
 * @author dream
 * @since 2026-09-23
 * @see PermissionType
 */
public enum PermissionCategory implements BaseEnum<String> {

    /** 基础操作：列表、详情、增删改、复制、移动、重命名、回收、恢复 */
    BASIC("basic", "基础操作"),

    /** 数据操作：导入导出、模板、上传下载、打印预览、清洗转换、合并拆分 */
    DATA("data", "数据操作"),

    /** 审批流程：提交、审核、通过、驳回、转审、催办、会签、撤销、撤回 */
    AUDIT("audit", "审批流程"),

    /** 权限管理：授权、解除授权、分配、委托、代登录 */
    PERMISSION("permission", "权限管理"),

    /** 状态控制：启用、停用、锁定、解锁、发布、下架、激活、冻结、暂停、置顶、推荐 */
    STATUS("status", "状态控制"),

    /** 其他：重置、刷新、同步、重试、回滚、排序、配置、归档、备份、清空、校验、签名、验证、统计、对比、迁移、生成、通知、提醒、订阅 */
    OTHER("other", "其他");

    private final String value;
    private final String label;

    PermissionCategory(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }
}