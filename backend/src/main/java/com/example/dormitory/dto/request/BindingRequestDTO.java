package com.example.dormitory.dto.request;

import jakarta.validation.constraints.NotNull;

public class BindingRequestDTO {

    @NotNull(message = "居住单元ID不能为空")
    private Long unitId;

    @NotNull(message = "洗漱台ID不能为空")
    private Long washbasinId;

    private String operator = "system";

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

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
