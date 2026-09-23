package com.dream.common.core.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限类型枚举。
 *
 * <p>定义系统中常见的操作权限类型，用于 RBAC 权限标识的后缀部分
 * （如 {@code system:user:add} 中的 {@code add}）。</p>
 *
 * <h2>命名规范</h2>
 * <ul>
 *   <li>枚举常量名：全大写 + 下划线（Java 常量规范）</li>
 *   <li>{@code value} 字段：小驼峰（前端消费友好）</li>
 * </ul>
 *
 * <h2>分类</h2>
 * <ul>
 *   <li><b>基础操作</b>（{@link PermissionCategory#BASIC}）：列表、详情、增删改、复制、移动、回收、重命名</li>
 *   <li><b>数据操作</b>（{@link PermissionCategory#DATA}）：导入导出、模板、上传下载、打印预览、清洗转换</li>
 *   <li><b>审批流程</b>（{@link PermissionCategory#AUDIT}）：提交、审核、通过、驳回、转审、催办、会签、撤销、撤回</li>
 *   <li><b>权限管理</b>（{@link PermissionCategory#PERMISSION}）：授权、解权、分配、委托、代登录</li>
 *   <li><b>状态控制</b>（{@link PermissionCategory#STATUS}）：启停、锁定、发布、激活、冻结、暂停、置顶、推荐</li>
 *   <li><b>其他</b>（{@link PermissionCategory#OTHER}）：重置、刷新、同步、排序、配置、归档、备份、校验、统计等</li>
 * </ul>
 *
 * <h2>颜色语义</h2>
 * <ul>
 *   <li>{@code primary} —— 普通操作（列表、详情、复制、刷新）</li>
 *   <li>{@code info} —— 信息类操作（预览、打印、同步、备份、统计）</li>
 *   <li>{@code success} —— 正向操作（新增、保存、启用、通过、激活、恢复、生成）</li>
 *   <li>{@code warning} —— 需注意操作（编辑、移动、审核、导出、归档、迁移）</li>
 *   <li>{@code danger} —— 危险操作（删除、驳回、禁用、授权、清空、代登录）</li>
 *   <li>{@code default} —— 中性操作（排序、配置）</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 构建权限标识
 * String perm = "system:user:" + PermissionType.ADD.getValue();  // "system:user:add"
 *
 * // 按 value 查找
 * PermissionType type = BaseEnum.fromValue(PermissionType.class, "add");
 *
 * // 按分类查询
 * List<PermissionType> basicOps = PermissionType.listByCategory(PermissionCategory.BASIC);
 *
 * // 只读权限列表（用于只读角色）
 * List<PermissionType> readOnly = PermissionType.listReadOnly();
 *
 * // 危险操作列表（用于二次确认）
 * List<PermissionType> dangerous = PermissionType.listDangerous();
 *
 * // 前端下拉列表
 * List<BaseEnum.Option<String>> options = BaseEnum.options(PermissionType.class);
 * }</pre>
 *
 * @author dream
 * @since 2026-09-23
 * @see BaseEnum
 * @see PermissionCategory
 */
public enum PermissionType implements BaseEnum<String> {

    // ==================== 基础操作（CRUD） ====================

    /**
     * 列表查询。
     * <p>查询列表、分页、条件搜索等聚合视图。</p>
     */
    LIST("list", "列表", PermissionCategory.BASIC, "primary"),

    /**
     * 详情查看。
     * <p>查看单条记录详情。</p>
     */
    DETAIL("detail", "详情", PermissionCategory.BASIC, "primary"),

    /**
     * 新增。
     */
    ADD("add", "新增", PermissionCategory.BASIC, "success"),

    /**
     * 保存。
     * <p>保存草稿、临时数据。与 {@link #EDIT} 的区别：
     * 本操作通常用于未提交的临时保存。</p>
     */
    SAVE("save", "保存", PermissionCategory.BASIC, "success"),

    /**
     * 编辑。
     */
    EDIT("edit", "编辑", PermissionCategory.BASIC, "warning"),

    /**
     * 删除。
     * <p>逻辑删除或物理删除。</p>
     */
    DELETE("delete", "删除", PermissionCategory.BASIC, "danger"),

    /**
     * 复制。
     * <p>基于现有记录创建副本。</p>
     */
    COPY("copy", "复制", PermissionCategory.BASIC, "primary"),

    /**
     * 移动。
     * <p>移动记录到其他分类、目录。</p>
     */
    MOVE("move", "移动", PermissionCategory.BASIC, "warning"),

    /**
     * 重命名。
     */
    RENAME("rename", "重命名", PermissionCategory.BASIC, "warning"),

    /**
     * 回收站。
     * <p>查看回收站（已删除记录）。</p>
     */
    RECYCLE("recycle", "回收站", PermissionCategory.BASIC, "warning"),

    /**
     * 恢复。
     * <p>从回收站恢复已删除记录。</p>
     */
    RECOVER("recover", "恢复", PermissionCategory.BASIC, "success"),

    // ==================== 数据操作 ====================

    /**
     * 导入。
     * <p>从文件批量导入数据。</p>
     */
    IMPORT("import", "导入", PermissionCategory.DATA, "warning"),

    /**
     * 导入模板。
     * <p>下载导入模板文件。</p>
     */
    IMPORT_TEMPLATE("importTemplate", "导入模板", PermissionCategory.DATA, "info"),

    /**
     * 导出。
     */
    EXPORT("export", "导出", PermissionCategory.DATA, "warning"),

    /**
     * 导出模板。
     * <p>导出数据为模板格式。</p>
     */
    EXPORT_TEMPLATE("exportTemplate", "导出模板", PermissionCategory.DATA, "info"),

    /**
     * 上传。
     * <p>上传附件、图片、文件。</p>
     */
    UPLOAD("upload", "上传", PermissionCategory.DATA, "success"),

    /**
     * 下载。
     */
    DOWNLOAD("download", "下载", PermissionCategory.DATA, "primary"),

    /**
     * 打印。
     */
    PRINT("print", "打印", PermissionCategory.DATA, "info"),

    /**
     * 预览。
     * <p>在线预览文档、图片、PDF。</p>
     */
    PREVIEW("preview", "预览", PermissionCategory.DATA, "info"),

    /**
     * 数据清洗。
     * <p>去重、修复、标准化等数据清理操作。</p>
     */
    CLEAN("clean", "清洗", PermissionCategory.DATA, "warning"),

    /**
     * 数据转换。
     * <p>格式转换、编码转换、单位换算。</p>
     */
    CONVERT("convert", "转换", PermissionCategory.DATA, "warning"),

    /**
     * 合并。
     * <p>合并多条记录、文件。</p>
     */
    MERGE("merge", "合并", PermissionCategory.DATA, "warning"),

    /**
     * 拆分。
     * <p>拆分记录、文件。</p>
     */
    SPLIT("split", "拆分", PermissionCategory.DATA, "warning"),

    // ==================== 审批流程 ====================

    /**
     * 提交。
     * <p>提交申请，进入审批流程。</p>
     */
    SUBMIT("submit", "提交", PermissionCategory.AUDIT, "primary"),

    /**
     * 审核。
     * <p>查看待审列表、审核意见。与 {@link #APPROVE} 的区别：
     * {@code AUDIT} 是"进入审核环节"，{@code APPROVE} 是"通过"。</p>
     */
    AUDIT("audit", "审核", PermissionCategory.AUDIT, "warning"),

    /**
     * 通过。
     * <p>审批通过。</p>
     */
    APPROVE("approve", "通过", PermissionCategory.AUDIT, "success"),

    /**
     * 驳回。
     */
    REJECT("reject", "驳回", PermissionCategory.AUDIT, "danger"),

    /**
     * 转审。
     * <p>将审批任务转交给其他人。</p>
     */
    TRANSFER("transfer", "转审", PermissionCategory.AUDIT, "warning"),

    /**
     * 催办。
     * <p>催办待审任务。</p>
     */
    URGE("urge", "催办", PermissionCategory.AUDIT, "warning"),

    /**
     * 会签。
     * <p>多人会签，需全部通过。</p>
     */
    COUNTERSIGN("countersign", "会签", PermissionCategory.AUDIT, "warning"),

    /**
     * 撤销。
     * <p>撤销已进入审核的申请。</p>
     */
    CANCEL("cancel", "撤销", PermissionCategory.AUDIT, "warning"),

    /**
     * 撤回。
     * <p>撤回到申请人手中。与 {@link #CANCEL} 的区别：
     * {@code WITHDRAW} 通常指未审核时主动收回；{@code CANCEL} 通常指已进入流程后终止。</p>
     */
    WITHDRAW("withdraw", "撤回", PermissionCategory.AUDIT, "warning"),

    // ==================== 权限管理 ====================

    /**
     * 授权。
     * <p>授予用户/角色权限。</p>
     */
    GRANT("grant", "授权", PermissionCategory.PERMISSION, "danger"),

    /**
     * 解除授权。
     * <p>回收权限。</p>
     */
    REVOKE("revoke", "解除授权", PermissionCategory.PERMISSION, "danger"),

    /**
     * 分配。
     * <p>分配资源、数据给用户。</p>
     */
    ASSIGN("assign", "分配", PermissionCategory.PERMISSION, "warning"),

    /**
     * 委托。
     * <p>将权限临时委托给他人（如休假期间）。</p>
     */
    DELEGATE("delegate", "委托", PermissionCategory.PERMISSION, "warning"),

    /**
     * 代登录。
     * <p>管理员代用户登录（模拟登录），用于问题排查。危险操作，需审计。</p>
     */
    IMPERSONATE("impersonate", "代登录", PermissionCategory.PERMISSION, "danger"),

    // ==================== 状态控制 ====================

    /**
     * 启用。
     */
    ENABLE("enable", "启用", PermissionCategory.STATUS, "success"),

    /**
     * 停用。
     */
    DISABLE("disable", "停用", PermissionCategory.STATUS, "danger"),

    /**
     * 锁定。
     * <p>锁定账号、记录，禁止操作但保留数据。</p>
     */
    LOCK("lock", "锁定", PermissionCategory.STATUS, "warning"),

    /**
     * 解锁。
     */
    UNLOCK("unlock", "解锁", PermissionCategory.STATUS, "success"),

    /**
     * 发布。
     */
    PUBLISH("publish", "发布", PermissionCategory.STATUS, "success"),

    /**
     * 下架。
     */
    UNPUBLISH("unpublish", "下架", PermissionCategory.STATUS, "warning"),

    /**
     * 激活。
     * <p>激活未生效的账号、配置。</p>
     */
    ACTIVATE("activate", "激活", PermissionCategory.STATUS, "success"),

    /**
     * 冻结。
     * <p>冻结账号、资金。</p>
     */
    FREEZE("freeze", "冻结", PermissionCategory.STATUS, "danger"),

    /**
     * 暂停。
     * <p>暂停服务、任务、流程。与 {@link #FREEZE} 的区别：
     * 暂停是临时的、可恢复的；冻结是长期限制。</p>
     */
    SUSPEND("suspend", "暂停", PermissionCategory.STATUS, "warning"),

    /**
     * 置顶。
     */
    TOP("top", "置顶", PermissionCategory.STATUS, "primary"),

    /**
     * 取消置顶。
     */
    UNTOP("untop", "取消置顶", PermissionCategory.STATUS, "primary"),

    /**
     * 推荐。
     */
    RECOMMEND("recommend", "推荐", PermissionCategory.STATUS, "success"),

    /**
     * 取消推荐。
     */
    UNRECOMMEND("unrecommend", "取消推荐", PermissionCategory.STATUS, "default"),

    // ==================== 其他 ====================

    /**
     * 重置。
     * <p>重置密码、重置配置。</p>
     */
    RESET("reset", "重置", PermissionCategory.OTHER, "warning"),

    /**
     * 刷新。
     * <p>刷新缓存、刷新数据。</p>
     */
    REFRESH("refresh", "刷新", PermissionCategory.OTHER, "primary"),

    /**
     * 同步。
     * <p>同步外部数据。</p>
     */
    SYNC("sync", "同步", PermissionCategory.OTHER, "info"),

    /**
     * 重试。
     * <p>重试失败的任务。</p>
     */
    RETRY("retry", "重试", PermissionCategory.OTHER, "warning"),

    /**
     * 回滚。
     * <p>回滚到指定版本、状态。</p>
     */
    ROLLBACK("rollback", "回滚", PermissionCategory.OTHER, "danger"),

    /**
     * 排序。
     */
    SORT("sort", "排序", PermissionCategory.OTHER, "default"),

    /**
     * 配置。
     */
    CONFIG("config", "配置", PermissionCategory.OTHER, "default"),

    /**
     * 归档。
     */
    ARCHIVE("archive", "归档", PermissionCategory.OTHER, "warning"),

    /**
     * 备份。
     */
    BACKUP("backup", "备份", PermissionCategory.OTHER, "info"),

    /**
     * 清空。
     * <p>清空数据、清空缓存。危险操作，需二次确认。</p>
     */
    CLEAR("clear", "清空", PermissionCategory.OTHER, "danger"),

    /**
     * 校验。
     * <p>数据校验、规则校验。</p>
     */
    VALIDATE("validate", "校验", PermissionCategory.OTHER, "warning"),

    /**
     * 签名。
     * <p>电子签名、数字签名。</p>
     */
    SIGN("sign", "签名", PermissionCategory.OTHER, "warning"),

    /**
     * 验证。
     * <p>验证签名、验证码、真伪。</p>
     */
    VERIFY("verify", "验证", PermissionCategory.OTHER, "info"),

    /**
     * 统计。
     * <p>数据统计、报表生成。</p>
     */
    STATISTICS("statistics", "统计", PermissionCategory.OTHER, "info"),

    /**
     * 对比。
     * <p>版本对比、数据对比。</p>
     */
    COMPARE("compare", "对比", PermissionCategory.OTHER, "info"),

    /**
     * 迁移。
     * <p>数据迁移、结构升级。</p>
     */
    MIGRATE("migrate", "迁移", PermissionCategory.OTHER, "warning"),

    /**
     * 生成。
     * <p>生成编号、生成报表、生成代码。</p>
     */
    GENERATE("generate", "生成", PermissionCategory.OTHER, "success"),

    /**
     * 通知。
     * <p>发送站内信、系统通知。</p>
     */
    NOTIFY("notify", "通知", PermissionCategory.OTHER, "primary"),

    /**
     * 提醒。
     * <p>发送提醒（如到期提醒、待办提醒）。</p>
     */
    REMIND("remind", "提醒", PermissionCategory.OTHER, "primary"),

    /**
     * 订阅。
     * <p>订阅消息、订阅报表。</p>
     */
    SUBSCRIBE("subscribe", "订阅", PermissionCategory.OTHER, "primary"),

    /**
     * 取消订阅。
     */
    UNSUBSCRIBE("unsubscribe", "取消订阅", PermissionCategory.OTHER, "default"),
    ;

    /** 权限类型值（小驼峰，用于构建权限标识后缀） */
    private final String value;

    /** 中文标签（展示名） */
    private final String label;

    /** 权限分类 */
    private final PermissionCategory category;

    /** UI 颜色（前端 Tag 展示） */
    private final String color;

    /**
     * 构造器。
     *
     * @param value    权限类型值（小驼峰）
     * @param label    中文标签
     * @param category 权限分类
     * @param color    UI 颜色
     */
    PermissionType(String value, String label, PermissionCategory category, String color) {
        this.value = value;
        this.label = label;
        this.category = category;
        this.color = color;
    }

    // ==================== 核心契约实现 ====================

    /**
     * 获取权限类型值。
     *
     * @return 权限类型值（小驼峰）
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * 获取中文标签。
     *
     * @return 中文标签
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * 获取 UI 颜色。
     *
     * @return UI 颜色
     */
    @Override
    public String getColor() {
        return color;
    }

    // ==================== 扩展属性 ====================

    /**
     * 获取权限分类。
     *
     * @return 权限分类
     */
    public PermissionCategory getCategory() {
        return category;
    }

    // ==================== 分类判断 ====================

    /**
     * 是否为基础操作类权限。
     *
     * @return 是返回 {@code true}
     */
    public boolean isBasic() {
        return this.category == PermissionCategory.BASIC;
    }

    /**
     * 是否为数据操作类权限。
     *
     * @return 是返回 {@code true}
     */
    public boolean isData() {
        return this.category == PermissionCategory.DATA;
    }

    /**
     * 是否为审批流程类权限。
     *
     * @return 是返回 {@code true}
     */
    public boolean isAudit() {
        return this.category == PermissionCategory.AUDIT;
    }

    /**
     * 是否为权限管理类权限。
     *
     * @return 是返回 {@code true}
     */
    public boolean isPermission() {
        return this.category == PermissionCategory.PERMISSION;
    }

    /**
     * 是否为状态控制类权限。
     *
     * @return 是返回 {@code true}
     */
    public boolean isStatus() {
        return this.category == PermissionCategory.STATUS;
    }

    /**
     * 是否为其他类权限。
     *
     * @return 是返回 {@code true}
     */
    public boolean isOther() {
        return this.category == PermissionCategory.OTHER;
    }

    // ==================== 语义判断 ====================

    /**
     * 是否为只读操作。
     *
     * <p>只读操作不修改数据，可以安全地暴露给只读角色。</p>
     *
     * <pre>{@code
     * // 只读角色可授予的权限
     * List<PermissionType> readOnly = PermissionType.listReadOnly();
     * }</pre>
     *
     * @return 是返回 {@code true}
     */
    public boolean isReadOnly() {
        return READ_ONLY_TYPES.contains(this);
    }

    /**
     * 是否为写操作。
     *
     * <p>写操作的补集：{@code !isReadOnly()}。写操作会修改数据，
     * 需谨慎授权。</p>
     *
     * @return 是返回 {@code true}
     */
    public boolean isWrite() {
        return !isReadOnly();
    }

    /**
     * 是否为危险操作。
     *
     * <p>危险操作需要二次确认（如删除、禁用、驳回、清空、代登录）。</p>
     *
     * @return 是返回 {@code true}
     */
    public boolean isDangerous() {
        return "danger".equals(color);
    }

    /**
     * 是否需要审计日志。
     *
     * <p>危险操作与权限管理操作都需要记录审计日志。</p>
     *
     * @return 是返回 {@code true}
     */
    public boolean requiresAudit() {
        return isDangerous() || isPermission();
    }

    // ==================== 静态工具方法 ====================

    /**
     * 只读操作类型集合（缓存，避免每次遍历）。
     */
    private static final Set<PermissionType> READ_ONLY_TYPES = Collections.unmodifiableSet(
            Arrays.stream(values())
                    .filter(t -> t == LIST || t == DETAIL
                            || t == DOWNLOAD || t == PREVIEW || t == PRINT)
                    .collect(Collectors.toSet())
    );

    /**
     * 按分类获取权限类型列表。
     *
     * @param category 权限分类
     * @return 该分类下的所有权限类型；{@code category} 为 {@code null} 时返回空列表
     */
    public static List<PermissionType> listByCategory(PermissionCategory category) {
        if (category == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(values())
                .filter(t -> t.category == category)
                .collect(Collectors.toList());
    }

    /**
     * 按颜色获取权限类型列表。
     *
     * @param color UI 颜色
     * @return 该颜色下的所有权限类型；{@code color} 为空时返回空列表
     */
    public static List<PermissionType> listByColor(String color) {
        if (color == null || color.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(values())
                .filter(t -> color.equals(t.color))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有只读操作权限。
     *
     * <p>用于只读角色、访客角色的权限模板。</p>
     *
     * @return 只读操作列表
     */
    public static List<PermissionType> listReadOnly() {
        return Arrays.stream(values())
                .filter(PermissionType::isReadOnly)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有写操作权限。
     *
     * <p>写操作的补集。用于"写权限"统计、审计范围确定。</p>
     *
     * @return 写操作列表
     */
    public static List<PermissionType> listWrite() {
        return Arrays.stream(values())
                .filter(PermissionType::isWrite)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有危险操作权限。
     *
     * <p>用于前端二次确认弹窗、审计日志等场景。</p>
     *
     * @return 危险操作列表
     */
    public static List<PermissionType> listDangerous() {
        return Arrays.stream(values())
                .filter(PermissionType::isDangerous)
                .collect(Collectors.toList());
    }
}