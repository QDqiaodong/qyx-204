package com.example.dormitory.service.impl;

import com.example.dormitory.dto.request.QuotaAppendDTO;
import com.example.dormitory.dto.request.QuotaOrderCreateDTO;
import com.example.dormitory.dto.response.BuildingQuotaStatusDTO;
import com.example.dormitory.entity.Building;
import com.example.dormitory.entity.MatchingCheckRecord;
import com.example.dormitory.entity.ShiftQuotaOrder;
import com.example.dormitory.mapper.BuildingMapper;
import com.example.dormitory.mapper.LivingUnitMapper;
import com.example.dormitory.mapper.MatchingCheckRecordMapper;
import com.example.dormitory.mapper.ShiftQuotaOrderMapper;
import com.example.dormitory.service.ShiftQuotaService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShiftQuotaServiceImpl implements ShiftQuotaService {

    private final ShiftQuotaOrderMapper quotaOrderMapper;
    private final BuildingMapper buildingMapper;
    private final LivingUnitMapper livingUnitMapper;
    private final MatchingCheckRecordMapper checkRecordMapper;

    public ShiftQuotaServiceImpl(ShiftQuotaOrderMapper quotaOrderMapper,
                                 BuildingMapper buildingMapper,
                                 LivingUnitMapper livingUnitMapper,
                                 MatchingCheckRecordMapper checkRecordMapper) {
        this.quotaOrderMapper = quotaOrderMapper;
        this.buildingMapper = buildingMapper;
        this.livingUnitMapper = livingUnitMapper;
        this.checkRecordMapper = checkRecordMapper;
    }

    @Override
    @Transactional
    public ShiftQuotaOrder create(QuotaOrderCreateDTO dto) {
        Building building = buildingMapper.selectById(dto.getBuildingId());
        if (building == null) {
            throw new RuntimeException("楼栋不存在");
        }

        LocalDate quotaDate = dto.getQuotaDate() != null ? dto.getQuotaDate() : LocalDate.now();

        if (dto.getShiftEnd() == null || dto.getShiftStart() == null
                || !dto.getShiftEnd().isAfter(dto.getShiftStart())) {
            throw new RuntimeException("当班结束时间必须晚于当班开始时间");
        }

        ShiftQuotaOrder openOrder = quotaOrderMapper.selectOpenByBuildingId(dto.getBuildingId());
        if (openOrder != null) {
            throw new RuntimeException("该楼栋已存在未结的当班定额单（" + openOrder.getQuotaDate()
                    + "，值班人：" + openOrder.getDutyPerson() + "），每个楼栋每个自然日只允许一张未结单，请先结案");
        }

        Integer closedCount = quotaOrderMapper.countClosedByBuildingAndDate(dto.getBuildingId(), quotaDate);
        boolean sameDayReopen = closedCount != null && closedCount > 0;
        if (sameDayReopen && (dto.getReopenReason() == null || dto.getReopenReason().trim().isEmpty())) {
            throw new RuntimeException("该楼栋今日已有结案的定额单，同日补开必须填写补开原因");
        }

        ShiftQuotaOrder order = new ShiftQuotaOrder();
        order.setBuildingId(dto.getBuildingId());
        order.setQuotaDate(quotaDate);
        order.setDutyPerson(dto.getDutyPerson().trim());
        order.setQuotaCapacity(dto.getQuotaCapacity());
        order.setShiftStart(dto.getShiftStart());
        order.setShiftEnd(dto.getShiftEnd());
        order.setStatus(ShiftQuotaOrder.STATUS_OPEN);
        order.setReopenReason(sameDayReopen ? dto.getReopenReason().trim() : null);

        try {
            quotaOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("该楼栋已存在未结的当班定额单，不能重复开单");
        }

        String operator = resolveOperator(dto.getOperator(), order.getDutyPerson());
        String message = "开具当班定额单：定额可洗人数" + order.getQuotaCapacity() + "人，值班人" + order.getDutyPerson()
                + "，当班" + order.getShiftStart() + "至" + order.getShiftEnd();
        if (sameDayReopen) {
            message += "，同日补开原因：" + order.getReopenReason();
        }
        saveQuotaTrace(order.getBuildingId(), "QUOTA_OPEN", order.getQuotaCapacity(), message, operator);

        return order;
    }

    @Override
    @Transactional
    public ShiftQuotaOrder append(Long id, QuotaAppendDTO dto) {
        ShiftQuotaOrder order = quotaOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new RuntimeException("定额单不存在");
        }
        if (!ShiftQuotaOrder.STATUS_OPEN.equals(order.getStatus())) {
            throw new RuntimeException("定额单已结案，不能追加定额");
        }

        int oldCapacity = order.getQuotaCapacity();
        int newCapacity = oldCapacity + dto.getAdditionalCapacity();
        order.setQuotaCapacity(newCapacity);
        quotaOrderMapper.updateById(order);

        String operator = resolveOperator(dto.getOperator(), order.getDutyPerson());
        String message = "定额追加：" + oldCapacity + "人 → " + newCapacity + "人（+" + dto.getAdditionalCapacity() + "人）";
        saveQuotaTrace(order.getBuildingId(), "QUOTA_APPEND", newCapacity, message, operator);

        return order;
    }

    @Override
    @Transactional
    public ShiftQuotaOrder close(Long id, String operator) {
        ShiftQuotaOrder order = quotaOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new RuntimeException("定额单不存在");
        }
        if (!ShiftQuotaOrder.STATUS_OPEN.equals(order.getStatus())) {
            throw new RuntimeException("定额单已结案，请勿重复结案");
        }

        order.setStatus(ShiftQuotaOrder.STATUS_CLOSED);
        order.setClosedAt(LocalDateTime.now());
        quotaOrderMapper.updateById(order);

        String resolvedOperator = resolveOperator(operator, order.getDutyPerson());
        String message = "定额单结案：定额可洗人数" + order.getQuotaCapacity() + "人，值班人" + order.getDutyPerson();
        saveQuotaTrace(order.getBuildingId(), "QUOTA_CLOSE", order.getQuotaCapacity(), message, resolvedOperator);

        return order;
    }

    @Override
    public List<ShiftQuotaOrder> list(Long buildingId, String status, LocalDate quotaDate) {
        List<ShiftQuotaOrder> orders = quotaOrderMapper.selectAllOrders();
        return orders.stream()
                .filter(o -> buildingId == null || buildingId.equals(o.getBuildingId()))
                .filter(o -> status == null || status.isEmpty() || status.equals(o.getStatus()))
                .filter(o -> quotaDate == null || quotaDate.equals(o.getQuotaDate()))
                .collect(Collectors.toList());
    }

    @Override
    public ShiftQuotaOrder getCurrentOpen(Long buildingId) {
        return quotaOrderMapper.selectOpenByBuildingId(buildingId);
    }

    @Override
    public List<BuildingQuotaStatusDTO> getBuildingStatuses() {
        List<Building> buildings = buildingMapper.selectList(null);
        List<BuildingQuotaStatusDTO> result = new ArrayList<>();
        for (Building building : buildings) {
            BuildingQuotaStatusDTO dto = new BuildingQuotaStatusDTO();
            dto.setBuildingId(building.getId());
            dto.setBuildingName(building.getBuildingName());
            Integer residentTotal = livingUnitMapper.sumResidentCountByBuildingId(building.getId());
            dto.setResidentTotal(residentTotal == null ? 0 : residentTotal);

            ShiftQuotaOrder openOrder = quotaOrderMapper.selectOpenByBuildingId(building.getId());
            if (openOrder != null) {
                dto.setHasOpenOrder(true);
                dto.setQuotaOrderId(openOrder.getId());
                dto.setQuotaCapacity(openOrder.getQuotaCapacity());
                dto.setDutyPerson(openOrder.getDutyPerson());
                dto.setOverQuota(dto.getResidentTotal() > openOrder.getQuotaCapacity());
            } else {
                dto.setHasOpenOrder(false);
                dto.setOverQuota(false);
            }
            result.add(dto);
        }
        return result;
    }

    private String resolveOperator(String operator, String fallback) {
        if (operator != null && !operator.trim().isEmpty()) {
            return operator.trim();
        }
        return fallback != null ? fallback : "system";
    }

    private void saveQuotaTrace(Long buildingId, String checkType, Integer quotaCapacity,
                                String message, String operator) {
        Integer residentTotal = livingUnitMapper.sumResidentCountByBuildingId(buildingId);

        MatchingCheckRecord record = new MatchingCheckRecord();
        record.setUnitId(0L);
        record.setWashbasinId(null);
        record.setCheckType(checkType);
        record.setUnitResidentCount(residentTotal == null ? 0 : residentTotal);
        record.setTotalCapacity(quotaCapacity);
        record.setCheckResult("INFO");
        record.setCheckMessage(message);
        record.setOperator(operator);
        record.setCheckTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        checkRecordMapper.insertIntoTable("matching_check_record_" + buildingId, record);
    }
}
