package com.example.dormitory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class QuotaAppendDTO {

    @NotNull(message = "追加人数不能为空")
    @Min(value = 1, message = "追加人数必须大于0")
    private Integer additionalCapacity;

    private String operator;

    public Integer getAdditionalCapacity() {
        return additionalCapacity;
    }

    public void setAdditionalCapacity(Integer additionalCapacity) {
        this.additionalCapacity = additionalCapacity;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
