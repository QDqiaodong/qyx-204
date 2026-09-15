package com.example.dormitory.dto.response;

import java.util.List;

public class UnitMatchingDTO {

    private Long unitId;

    private String unitCode;

    private String buildingName;

    private Integer floor;

    private Integer roomCount;

    private Integer residentCount;

    private List<WashbasinInfo> washbasins;

    /**
     * 绑定痕迹仍在、但因存在未结送检（待接单）而暂不计入配套的洗漱台。
     * 绑定关系不改不删，只是送检期间从配套容量中剔除，修复后自动回到 washbasins。
     */
    private List<WashbasinInfo> repairingWashbasins;

    /** 因送检而暂缺的容纳容量（仅展示用，不计入 totalCapacity） */
    private Integer repairingCapacity;

    private Integer totalCapacity;

    private Integer remainingCapacity;

    private Double usageRate;

    private String matchingStatus;

    private Long buildingId;

    private Integer buildingResidentTotal;

    private Integer shiftQuotaCapacity;

    private Boolean quotaExceeded;

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    public Integer getResidentCount() {
        return residentCount;
    }

    public void setResidentCount(Integer residentCount) {
        this.residentCount = residentCount;
    }

    public List<WashbasinInfo> getWashbasins() {
        return washbasins;
    }

    public void setWashbasins(List<WashbasinInfo> washbasins) {
        this.washbasins = washbasins;
    }

    public List<WashbasinInfo> getRepairingWashbasins() {
        return repairingWashbasins;
    }

    public void setRepairingWashbasins(List<WashbasinInfo> repairingWashbasins) {
        this.repairingWashbasins = repairingWashbasins;
    }

    public Integer getRepairingCapacity() {
        return repairingCapacity;
    }

    public void setRepairingCapacity(Integer repairingCapacity) {
        this.repairingCapacity = repairingCapacity;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Integer getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(Integer remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public Double getUsageRate() {
        return usageRate;
    }

    public void setUsageRate(Double usageRate) {
        this.usageRate = usageRate;
    }

    public String getMatchingStatus() {
        return matchingStatus;
    }

    public void setMatchingStatus(String matchingStatus) {
        this.matchingStatus = matchingStatus;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Integer getBuildingResidentTotal() {
        return buildingResidentTotal;
    }

    public void setBuildingResidentTotal(Integer buildingResidentTotal) {
        this.buildingResidentTotal = buildingResidentTotal;
    }

    public Integer getShiftQuotaCapacity() {
        return shiftQuotaCapacity;
    }

    public void setShiftQuotaCapacity(Integer shiftQuotaCapacity) {
        this.shiftQuotaCapacity = shiftQuotaCapacity;
    }

    public Boolean getQuotaExceeded() {
        return quotaExceeded;
    }

    public void setQuotaExceeded(Boolean quotaExceeded) {
        this.quotaExceeded = quotaExceeded;
    }

    public static class WashbasinInfo {
        private Long id;
        private String washbasinCode;
        private Integer capacity;
        private String location;

        /** 是否处于未结送检（待接单）；true 时仅出现在送检中名单，不计入配套容量 */
        private Boolean repairing;
        /** 送检单号 */
        private Long repairOrderId;
        /** 损坏部位 */
        private String damagePart;
        /** 经办值班员 */
        private String dutyPerson;
        /** 送检开始时间（上一张未结单从什么时候开始还没结） */
        private java.time.LocalDateTime repairStartedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

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

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public Boolean getRepairing() {
            return repairing;
        }

        public void setRepairing(Boolean repairing) {
            this.repairing = repairing;
        }

        public Long getRepairOrderId() {
            return repairOrderId;
        }

        public void setRepairOrderId(Long repairOrderId) {
            this.repairOrderId = repairOrderId;
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

        public java.time.LocalDateTime getRepairStartedAt() {
            return repairStartedAt;
        }

        public void setRepairStartedAt(java.time.LocalDateTime repairStartedAt) {
            this.repairStartedAt = repairStartedAt;
        }
    }
}
