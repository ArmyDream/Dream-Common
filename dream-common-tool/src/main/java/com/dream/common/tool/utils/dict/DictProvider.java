package com.dream.common.tool.utils.dict;

import java.util.List;

/**
 * 字典数据提供者接口
 * <p>
 * 由业务模块实现，用于从数据库/缓存/外部系统加载字典数据。
 * 微服务环境下，各服务可独立实现，自行决定是否使用缓存。
 *
 * @author dream
 */
public interface DictProvider {

    /**
     * 根据字典类型获取字典项列表
     *
     * @param dictType 字典类型标识
     * @return 字典项列表；未找到返回空列表
     */
    List<DictItem> getDictItems(String dictType);

    /**
     * 判断是否支持该字典类型
     */
    default boolean supports(String dictType) {
        return true;
    }
}