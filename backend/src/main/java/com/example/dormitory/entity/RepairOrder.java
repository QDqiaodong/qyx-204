package com.example.dormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 洗漱台送检单：一台洗漱台同时只允许存在一张未结单（PENDING 待接单）。
 * 未结唯一性由数据库生成列 open_flag + 唯一索引 uk_washbasin_open 兜底，
 * 并发开单时后交的一张必然因唯一键冲突失败，先写下的一张原样保留。
 */
@TableName("washbasin_repair_order")
public class RepairOrder {

    /** 待接单：已送检、尚未修复，期间该台不计入任何单元的配套洗漱台 */
    public static final String STATUS_PENDING = "PENDING";
    /** 已修复：送检结案，该台可以重新出现在配套一览中 */
    public static final String STATUS_REPAIRED = "REPAIRED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long washbasinId;

    /** 损坏部位（口头报修落成文字，必填） */
    private String damagePart;

    /** 经办值班员（必填） */
    private String dutyPerson;

    /** 实际开单操作人，缺省取经办值班员 */
    private String operator;

    private String status;

    /**
     * 未结标记：待接单为 1，已修复为 NULL。配合唯一索引 uk_washbasin_open
     * 保证同一洗漱台至多一行 open_flag=1；多个 NULL（历史已结单）互不冲突。
     * 由应用维护：开单置 1，登记修复时随状态一并置 NULL。
     */
    private Integer openFlag;

    /** 修复备注：修没修好、换了什么，登记修复时填写 */
    private String repairNote;

    private LocalDateTime repairedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWashbasinId() {
        return washbasinId;
    }

    public void setWashbasinId(Long washbasinId) {
        this.washbasinId = washbasinId;
    }

    public String getDamagePart() {
        return damagePart;
    }

    public void setDamagePart(String damagePart) {
        this.damagePart = damagePart;
    }

    public String getDutyPerson() {
        return dutyPerson;
    }

    public void setDutyPerson(String dutyPerson) {
        this.dutyPerson = dutyPerson;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOpenFlag() {
        return openFlag;
    }

    public void setOpenFlag(Integer openFlag) {
        this.openFlag = openFlag;
    }

    public String getRepairNote() {
        return repairNote;
    }

    public void setRepairNote(String repairNote) {
        this.repairNote = repairNote;
    }

    public LocalDateTime getRepairedAt() {
        return repairedAt;
    }

    public void setRepairedAt(LocalDateTime repairedAt) {
        this.repairedAt = repairedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
