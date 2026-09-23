package com.dream.common.core.vo.sys;

import java.io.Serializable;

/**
 * 组织单元公共字段基类。
 *
 * <p>抽象 {@link OrgVo}（机构）与 {@link DeptVo}（部门）的共有字段。
 * 两者结构高度相似（都有名称、编码、层级、地址、联系人等），
 * 通过继承避免字段重复。</p>
 *
 * <h2>设计意图</h2>
 * <p>机构与部门在业务语义上是"组织单元"的两个层级，共享大部分元数据。
 * 差异主要在于业务用途（机构对外、部门对内）和少量特有字段
 * （如机构的法人代表）。</p>
 *
 * @author dream
 * @since 2026-09-10
 */
public abstract class AbstractUnitVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private String id;

    /** 名称 */
    private String name;

    /** 简称 */
    private String shortName;

    /**
     * 行政区划编码（当前层级）。
     *
     * <p>表示组织单元所在的<b>最细粒度</b>行政区划编码，由组织单元的层级决定：
     * <ul>
     *   <li>省级单元 → 填省级编码（如 {@code 42}）</li>
     *   <li>市级单元 → 填市级编码（如 {@code 4201}）</li>
     *   <li>区县级单元 → 填区县级编码（如 {@code 420111}）</li>
     * </ul>
     *
     * <p>与 provinceCode / cityCode / areaCode 的区别：
     * 前者是"当前层级的编码"，后者是"各层级的拆分"。业务方如果只想拿到
     * 当前区划编码，直接用本字段，无需判断单元层级。</p>
     */
    private String divisionCode;

    /**
     * 组织单元类型。
     *
     * <p>取值来源由业务定义<p/>
     */
    private String unitType;

    /** 父级 ID */
    private String parentId;

    /** 父级名称 */
    private String parentName;

    /** 机构、部门 层级 */
    private String level;

    /** 省编码 */
    private String provinceCode;

    /** 省名称 */
    private String provinceName;

    /** 市编码 */
    private String cityCode;

    /** 市名称 */
    private String cityName;

    /** 区县编码 */
    private String areaCode;

    /** 区县名称 */
    private String areaName;

    /** 详细地址 */
    private String address;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 联系人 */
    private String contact;

    /** 联系人电话 */
    private String contactPhone;

    /** 正职领导 ID */
    private String leader;

    /** 正职领导姓名 */
    private String leaderName;

    /** 副职领导 ID */
    private String deputyLeader;

    /** 副职领导姓名 */
    private String deputyLeaderName;

    /** 备注 */
    private String remark;

    // ==================== Getter / Setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDivisionCode() {
        return divisionCode;
    }

    public void setDivisionCode(String divisionCode) {
        this.divisionCode = divisionCode;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCreditCode() {
        return creditCode;
    }

    public void setCreditCode(String creditCode) {
        this.creditCode = creditCode;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public String getDeputyLeader() {
        return deputyLeader;
    }

    public void setDeputyLeader(String deputyLeader) {
        this.deputyLeader = deputyLeader;
    }

    public String getDeputyLeaderName() {
        return deputyLeaderName;
    }

    public void setDeputyLeaderName(String deputyLeaderName) {
        this.deputyLeaderName = deputyLeaderName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
