package com.dream.common.core.vo.sys;

/**
 * 机构信息 VO。
 *
 * <p>继承 {@link AbstractUnitVo} 的公共字段，仅补充机构特有字段。
 * 机构通常对应对外的法人单位。</p>
 *
 * @author dream
 * @since 2026-09-10
 */
public class OrgVo extends AbstractUnitVo {

    private static final long serialVersionUID = 1L;

    /** 法人代表 */
    private String legalPerson;

    public String getLegalPerson() {
        return legalPerson;
    }

    public void setLegalPerson(String legalPerson) {
        this.legalPerson = legalPerson;
    }
}