package com.example.dormitory.dto.response;

public class BuildingQuotaStatusDTO {

    private Long buildingId;

    private String buildingName;

    private Integer residentTotal;

    private Boolean hasOpenOrder;

    private Long quotaOrderId;

    private Integer quotaCapacity;

    private String dutyPerson;

    private Boolean overQuota;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Integer getResidentTotal() {
        return residentTotal;
    }

    public void setResidentTotal(Integer residentTotal) {
        this.residentTotal = residentTotal;
    }

    public Boolean getHasOpenOrder() {
        return hasOpenOrder;
    }

    public void setHasOpenOrder(Boolean hasOpenOrder) {
        this.hasOpenOrder = hasOpenOrder;
    }

    public Long getQuotaOrderId() {
        return quotaOrderId;
    }

    public void setQuotaOrderId(Long quotaOrderId) {
        this.quotaOrderId = quotaOrderId;
    }

    public Integer getQuotaCapacity() {
        return quotaCapacity;
    }

    public void setQuotaCapacity(Integer quotaCapacity) {
        this.quotaCapacity = quotaCapacity;
    }

    public String getDutyPerson() {
        return dutyPerson;
    }

    public void setDutyPerson(String dutyPerson) {
        this.dutyPerson = dutyPerson;
    }

    public Boolean getOverQuota() {
        return overQuota;
    }

    public void setOverQuota(Boolean overQuota) {
        this.overQuota = overQuota;
    }
}
