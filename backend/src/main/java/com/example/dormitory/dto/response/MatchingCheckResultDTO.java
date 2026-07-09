package com.example.dormitory.dto.response;

public class MatchingCheckResultDTO {

    private Boolean success;

    private String checkResult;

    private String checkMessage;

    private Integer unitResidentCount;

    private Integer totalCapacity;

    private Integer remainingCapacity;

    private Double usageRate;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getCheckMessage() {
        return checkMessage;
    }

    public void setCheckMessage(String checkMessage) {
        this.checkMessage = checkMessage;
    }

    public Integer getUnitResidentCount() {
        return unitResidentCount;
    }

    public void setUnitResidentCount(Integer unitResidentCount) {
        this.unitResidentCount = unitResidentCount;
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

    public static MatchingCheckResultDTO pass(Integer residentCount, Integer capacity) {
        MatchingCheckResultDTO result = new MatchingCheckResultDTO();
        result.setSuccess(true);
        result.setCheckResult("PASS");
        result.setUnitResidentCount(residentCount);
        result.setTotalCapacity(capacity);
        result.setRemainingCapacity(capacity - residentCount);
        result.setUsageRate(capacity > 0 ? (double) residentCount / capacity * 100 : 0);
        result.setCheckMessage("容量匹配校验通过，单元居住人数: " + residentCount + "，总容纳容量: " + capacity);
        return result;
    }

    public static MatchingCheckResultDTO warn(Integer residentCount, Integer capacity) {
        MatchingCheckResultDTO result = new MatchingCheckResultDTO();
        result.setSuccess(true);
        result.setCheckResult("WARN");
        result.setUnitResidentCount(residentCount);
        result.setTotalCapacity(capacity);
        result.setRemainingCapacity(capacity - residentCount);
        result.setUsageRate(capacity > 0 ? (double) residentCount / capacity * 100 : 0);
        result.setCheckMessage("容量匹配预警，单元居住人数接近上限，当前使用率: " + String.format("%.1f", result.getUsageRate()) + "%");
        return result;
    }

    public static MatchingCheckResultDTO fail(Integer residentCount, Integer capacity) {
        MatchingCheckResultDTO result = new MatchingCheckResultDTO();
        result.setSuccess(false);
        result.setCheckResult("FAIL");
        result.setUnitResidentCount(residentCount);
        result.setTotalCapacity(capacity);
        result.setRemainingCapacity(capacity - residentCount);
        result.setUsageRate(capacity > 0 ? (double) residentCount / capacity * 100 : 0);
        result.setCheckMessage("容量匹配校验失败，单元居住人数: " + residentCount + "超过总容纳容量: " + capacity + "，超出人数: " + (residentCount - capacity));
        return result;
    }
}
