package com.example.dormitory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 开送检单：选中一台洗漱台，写清损坏部位和经办值班员。
 */
public class RepairOrderCreateDTO {

    @NotNull(message = "送检洗漱台不能为空")
    private Long washbasinId;

    @NotBlank(message = "损坏部位不能为空")
    @Size(max = 255, message = "损坏部位描述过长")
    private String damagePart;

    @NotBlank(message = "经办值班员不能为空")
    private String dutyPerson;

    private String operator;

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
}
