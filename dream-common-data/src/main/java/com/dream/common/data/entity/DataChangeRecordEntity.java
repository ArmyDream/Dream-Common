package com.dream.common.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dream.common.data.annotation.QueryCondition;
import com.dream.common.data.enums.QueryType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据变更记录实体，用于记录业务数据的新增、修改、删除等操作明细。
 * <p>
 * 继承自 {@link BaseEntity}，对应数据库表 {@code sys_data_change_record}。
 * 记录粒度可到字段级别，保存修改前后的值，便于数据审计与回溯。
 * 通常配合 {@link com.dream.common.data.annotation.DataChangeRecord} 注解使用。
 * </p>
 * <p>
 * 继承关系：{@code DataChangeRecordEntity} extends {@link BaseEntity} extends {@link BaseIdEntity}。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 新增一条变更记录
 * DataChangeRecordEntity record = new DataChangeRecordEntity();
 * record.setTableName("sys_user");
 * record.setDataId("123");
 * record.setOperationType("update");
 * record.setFieldName("username");
 * record.setOldValue("old");
 * record.setNewValue("new");
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_change_record")
public class DataChangeRecordEntity extends BaseEntity {

    /**
     * 租户ID（本表通常加入租户拦截器 IGNORE_TABLES，由业务侧 Handler 手动注入）
     */
    private String tenantId;

    /**
     * 表名
     */
    @QueryCondition(type = QueryType.LIKE)
    private String tableName;

    /**
     * 表描述
     */
    @QueryCondition(type = QueryType.LIKE)
    private String tableDesc;

    /**
     * 数据ID
     */
    @QueryCondition
    private String dataId;

    /**
     * 操作类型（insert/update/delete，存储 {@code OperationType.getValue()}，与字典 sys_oper_type 对齐）
     */
    @QueryCondition
    private String operationType;

    /**
     * 字段名
     */
    @QueryCondition(type = QueryType.LIKE)
    private String fieldName;

    /**
     * 字段描述
     */
    @QueryCondition(type = QueryType.LIKE)
    private String fieldDesc;

    /**
     * 字段key(用于标识)
     */
    @QueryCondition(type = QueryType.LIKE)
    private String fieldKey;

    /**
     * 修改前值
     */
    @QueryCondition(type = QueryType.LIKE)
    private String oldValue;

    /**
     * 修改后值
     */
    @QueryCondition(type = QueryType.LIKE)
    private String newValue;

}
