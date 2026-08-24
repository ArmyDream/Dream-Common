package com.dream.common.data.entity;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用分页查询类，在 {@link CommonQuery} 基础上扩展业务查询参数实体。
 * <p>
 * 继承自 {@link CommonQuery}，新增泛型字段 {@code params} 用于承载具体业务查询条件，
 * 并提供 {@link #toPage()} 方法将分页参数转换为 MyBatis-Plus 的 {@link Page} 对象。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * CommonPageQuery<UserQuery> pageQuery = new CommonPageQuery<>();
 * pageQuery.setPageNum(1).setPageSize(10);
 * pageQuery.setParams(new UserQuery().setUsername("dream"));
 * Page<User> page = pageQuery.toPage();
 * }</pre>
 *
 * @author dream
 * @param <T> 业务查询参数类型
 * @since 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommonPageQuery<T> extends CommonQuery {

    /**
     * 查询参数实体，承载具体业务查询条件
     */
    private T params;

    /**
     * 转换为 MyBatis-Plus 分页对象。
     *
     * @param <E> 分页数据元素类型
     * @return 根据当前页码、每页条数及是否查询总条数构建的分页对象
     */
    public <E> Page<E> toPage() {
        return new Page<>(this.getPageNum(), this.getPageSize(), this.getSearchCount());
    }
}
