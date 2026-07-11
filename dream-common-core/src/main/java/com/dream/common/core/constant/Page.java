package com.dream.common.core.constant;

/**
 * 分页常量
 */
public final class Page {
    private Page() {
    }

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;
    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 默认大数据分页大小
     */
    public static final int DEFAULT_PAGE_SIZE_LARGE = 500;

    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * 批量新增或更新最大操作量
     */
    public static final int BATCH_MAX_SIZE = 10000;

    /**
     * 默认批次提交数量
     */
    public static final int BATCH_HANDLE_SIZE = 500;

}