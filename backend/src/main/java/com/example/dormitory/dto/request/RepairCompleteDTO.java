package com.example.dormitory.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 送检单修复登记：只记录修复结果，不允许改动当初开单的损坏部位、经办值班员。
 */
public class RepairCompleteDTO {

    @Size(max = 255, message = "修复备注过长")
    private String repairNote;

    private String operator;

    public String getRepairNote() {
        return repairNote;
    }

    public void setRepairNote(String repairNote) {
        this.repairNote = repairNote;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
