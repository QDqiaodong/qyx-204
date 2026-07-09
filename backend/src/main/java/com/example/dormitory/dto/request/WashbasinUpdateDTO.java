package com.example.dormitory.dto.request;

import jakarta.validation.constraints.Positive;

public class WashbasinUpdateDTO {

    private String washbasinCode;

    @Positive(message = "可容纳人数必须大于0")
    private Integer capacity;

    private Long buildingId;

    private String location;

    private Integer status;

    public String getWashbasinCode() {
        return washbasinCode;
    }

    public void setWashbasinCode(String washbasinCode) {
        this.washbasinCode = washbasinCode;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
