package com.example.dormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("unit_washbasin_binding")
public class UnitWashbasinBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long unitId;

    private Long washbasinId;

    private LocalDateTime bindingTime;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getWashbasinId() {
        return washbasinId;
    }

    public void setWashbasinId(Long washbasinId) {
        this.washbasinId = washbasinId;
    }

    public LocalDateTime getBindingTime() {
        return bindingTime;
    }

    public void setBindingTime(LocalDateTime bindingTime) {
        this.bindingTime = bindingTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
