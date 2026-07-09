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

    private Integer totalCapacity;

    private Integer remainingCapacity;

    private Double usageRate;

    private String matchingStatus;

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

    public static class WashbasinInfo {
        private Long id;
        private String washbasinCode;
        private Integer capacity;
        private String location;

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
    }
}
