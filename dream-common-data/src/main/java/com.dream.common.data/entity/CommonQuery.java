package com.dream.common.data.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用查询条件，封装分页、排序等通用查询参数。
 * <p>
 * 提供页码、每页条数、排序字段集合、是否查询总条数、是否开启驼峰转下划线等配置。
 * 支持链式调用 {@link #addOrder(String, boolean)} 添加排序字段。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * CommonQuery query = new CommonQuery();
 * query.setPageNum(1).setPageSize(10)
 *      .addOrder("create_time", false);
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Data
public class CommonQuery {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 20;

    /**
     * 排序字段列表
     */
    private List<OrderField> orderFields = new ArrayList<>();

    /**
     * 是否查询总条数
     */
    private Boolean searchCount = true;

    /**
     * 是否开启驼峰转换
     */
    private Boolean camelToUnderscore = true;

    /**
     * 添加排序字段（不带表别名）。
     *
     * @param column 排序字段名
     * @param isAsc  是否升序，true-升序，false-降序
     * @return 当前查询对象，便于链式调用
     */
    public CommonQuery addOrder(String column, boolean isAsc) {
        this.orderFields.add(new OrderField(column, isAsc));
        return this;
    }

    /**
     * 添加排序字段（带表别名，适用于多表关联查询）。
     *
     * @param column     排序字段名
     * @param isAsc      是否升序，true-升序，false-降序
     * @param tableAlias 表别名
     * @return 当前查询对象，便于链式调用
     */
    public CommonQuery addOrder(String column, boolean isAsc, String tableAlias) {
        this.orderFields.add(new OrderField(column, isAsc, tableAlias));
        return this;
    }

    /**
     * 排序字段内部类，描述单个排序项的字段名、排序方向与表别名。
     *
     * @author dream
     * @since 2026-08-21
     */
    @Data
    public static class OrderField {
        /**
         * 排序字段
         */
        private String column;

        /**
         * 是否升序
         */
        private boolean asc = true;

        /**
         * 表别名
         */
        private String tableAlias;

        /**
         * 默认构造方法
         */
        public OrderField() {
        }

        /**
         * 构造排序项（不带表别名）。
         *
         * @param column 排序字段名
         * @param isAsc  是否升序
         */
        public OrderField(String column, boolean isAsc) {
            this.column = column;
            this.asc = isAsc;
        }

        /**
         * 构造排序项（带表别名）。
         *
         * @param column     排序字段名
         * @param isAsc      是否升序
         * @param tableAlias 表别名
         */
        public OrderField(String column, boolean isAsc, String tableAlias) {
            this.column = column;
            this.asc = isAsc;
            this.tableAlias = tableAlias;
        }
    }
}
