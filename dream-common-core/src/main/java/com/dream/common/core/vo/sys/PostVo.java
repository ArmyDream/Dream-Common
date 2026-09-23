package com.dream.common.core.vo.sys;

import java.io.Serializable;

/**
 * 岗位信息 VO。
 *
 * @author dream
 * @since 2026-09-10
 */
public class PostVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 岗位 ID */
    private String postId;

    /** 岗位编码 */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 备注 */
    private String remark;

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}