package com.example.dormitory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class QuotaOrderCreateDTO {

    @NotNull(message = "楼栋不能为空")
    private Long buildingId;

    private LocalDate quotaDate;

    @NotBlank(message = "值班人不能为空")
    private String dutyPerson;

    @NotNull(message = "定额可洗人数不能为空")
    @Min(value = 0, message = "定额可洗人数不能为负数")
    private Integer quotaCapacity;

    @NotNull(message = "当班开始时间不能为空")
    private LocalDateTime shiftStart;

    @NotNull(message = "当班结束时间不能为空")
    private LocalDateTime shiftEnd;

    private String reopenReason;

    private String operator;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public LocalDate getQuotaDate() {
        return quotaDate;
    }

    public void setQuotaDate(LocalDate quotaDate) {
        this.quotaDate = quotaDate;
    }

    public String getDutyPerson() {
        return dutyPerson;
    }

    public void setDutyPerson(String dutyPerson) {
        this.dutyPerson = dutyPerson;
    }

    public Integer getQuotaCapacity() {
        return quotaCapacity;
    }

    public void setQuotaCapacity(Integer quotaCapacity) {
        this.quotaCapacity = quotaCapacity;
    }

    public LocalDateTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalDateTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalDateTime getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(LocalDateTime shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getReopenReason() {
        return reopenReason;
    }

    public void setReopenReason(String reopenReason) {
        this.reopenReason = reopenReason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
