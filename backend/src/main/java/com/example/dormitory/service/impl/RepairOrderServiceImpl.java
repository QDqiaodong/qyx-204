package com.example.dormitory.service.impl;

import com.example.dormitory.dto.request.RepairCompleteDTO;
import com.example.dormitory.dto.request.RepairOrderCreateDTO;
import com.example.dormitory.entity.MatchingCheckRecord;
import com.example.dormitory.entity.RepairOrder;
import com.example.dormitory.entity.Washbasin;
import com.example.dormitory.mapper.MatchingCheckRecordMapper;
import com.example.dormitory.mapper.RepairOrderMapper;
import com.example.dormitory.mapper.WashbasinMapper;
import com.example.dormitory.service.RepairOrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepairOrderServiceImpl implements RepairOrderService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RepairOrderMapper repairOrderMapper;
    private final WashbasinMapper washbasinMapper;
    private final MatchingCheckRecordMapper checkRecordMapper;

    public RepairOrderServiceImpl(RepairOrderMapper repairOrderMapper,
                                  WashbasinMapper washbasinMapper,
                                  MatchingCheckRecordMapper checkRecordMapper) {
        this.repairOrderMapper = repairOrderMapper;
        this.washbasinMapper = washbasinMapper;
        this.checkRecordMapper = checkRecordMapper;
    }

    @Override
    @Transactional
    public RepairOrder create(RepairOrderCreateDTO dto) {
        Washbasin washbasin = washbasinMapper.selectById(dto.getWashbasinId());
        if (washbasin == null) {
            throw new RuntimeException("洗漱台不存在");
        }

        // 先锁洗漱台行，同一台的并发开单在此串行；再查未结单给出可读的拦截信息
        washbasinMapper.selectByIdForUpdate(dto.getWashbasinId());
        RepairOrder openOrder = repairOrderMapper.selectOpenByWashbasinIdForUpdate(dto.getWashbasinId());
        if (openOrder != null) {
            throw new RuntimeException("该洗漱台已存在未结的送检单（单号" + openOrder.getId()
                    + "，自" + formatTime(openOrder.getCreatedAt()) + "起处于待接单，损坏部位："
                    + openOrder.getDamagePart() + "，经办值班员：" + openOrder.getDutyPerson()
                    + "），登记修复后才能再开");
        }

        RepairOrder order = new RepairOrder();
        order.setWashbasinId(dto.getWashbasinId());
        order.setDamagePart(dto.getDamagePart().trim());
        order.setDutyPerson(dto.getDutyPerson().trim());
        order.setOperator(resolveOperator(dto.getOperator(), order.getDutyPerson()));
        order.setStatus(RepairOrder.STATUS_PENDING);
        order.setOpenFlag(1);

        try {
            repairOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_washbasin_open 兜底：并发下后交的一张必然失败，先写下的待接单原样保留
            RepairOrder winner = repairOrderMapper.selectOpenByWashbasinId(dto.getWashbasinId());
            String since = winner != null && winner.getCreatedAt() != null ? formatTime(winner.getCreatedAt()) : "此前";
            throw new RuntimeException("该洗漱台已有一张先写下的未结送检单（单号"
                    + (winner != null ? winner.getId() : "?") + "，自" + since
                    + "起待接单），本次开单失败");
        }

        String message = "洗漱台送检：损坏部位【" + order.getDamagePart() + "】，经办值班员"
                + order.getDutyPerson() + "，当前待接单，期间不计入配套洗漱台";
        saveRepairTrace(washbasin, "REPAIR_OPEN", message, order.getOperator());

        return order;
    }

    @Override
    @Transactional
    public RepairOrder complete(Long id, RepairCompleteDTO dto) {
        RepairOrder order = repairOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new RuntimeException("送检单不存在");
        }
        if (!RepairOrder.STATUS_PENDING.equals(order.getStatus())) {
            throw new RuntimeException("该送检单已修复，请勿重复登记");
        }

        // 只流转状态并追加修复结果，损坏部位、经办值班员等原单字段一律不动；
        // open_flag 随状态置 NULL，该台恢复“可再开单 / 可回配套”资格
        String note = dto.getRepairNote() == null ? "" : dto.getRepairNote().trim();
        LocalDateTime repairedAt = LocalDateTime.now();
        int affected = repairOrderMapper.markRepaired(order.getId(), note, repairedAt);
        if (affected == 0) {
            throw new RuntimeException("送检单状态已变化，修复登记失败");
        }
        order.setStatus(RepairOrder.STATUS_REPAIRED);
        order.setRepairedAt(repairedAt);
        order.setRepairNote(note);
        order.setOpenFlag(null);

        Washbasin washbasin = washbasinMapper.selectById(order.getWashbasinId());
        String operator = resolveOperator(dto.getOperator(),
                order.getDutyPerson() != null ? order.getDutyPerson() : null);
        String message = "洗漱台修复：原损坏部位【" + order.getDamagePart() + "】，经办值班员"
                + order.getDutyPerson() + "，自" + formatTime(order.getCreatedAt())
                + "起送检待接单，现已恢复配套"
                + (note.isEmpty() ? "" : "；修复备注：" + note);
        saveRepairTrace(washbasin, "REPAIR_DONE", message, operator);

        return order;
    }

    @Override
    public List<RepairOrder> list(Long washbasinId, String status, Long buildingId) {
        List<RepairOrder> orders = repairOrderMapper.selectAllOrders();
        return orders.stream()
                .filter(o -> washbasinId == null || washbasinId.equals(o.getWashbasinId()))
                .filter(o -> status == null || status.isEmpty() || status.equals(o.getStatus()))
                .filter(o -> {
                    if (buildingId == null) {
                        return true;
                    }
                    Washbasin w = washbasinMapper.selectById(o.getWashbasinId());
                    return w != null && buildingId.equals(w.getBuildingId());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<RepairOrder> listOpen() {
        return repairOrderMapper.selectPendingOrders();
    }

    @Override
    public RepairOrder getOpenByWashbasin(Long washbasinId) {
        return repairOrderMapper.selectOpenByWashbasinId(washbasinId);
    }

    private String resolveOperator(String operator, String fallback) {
        if (operator != null && !operator.trim().isEmpty()) {
            return operator.trim();
        }
        return fallback != null ? fallback : "system";
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FMT.format(time);
    }

    /**
     * 送检痕迹按洗漱台所属楼栋落到既有分表，与绑定/定额痕迹同一套只追加表，
     * 历史记录不改写、不删除。
     */
    private void saveRepairTrace(Washbasin washbasin, String checkType, String message, String operator) {
        String tableName = "matching_check_record";
        if (washbasin != null && washbasin.getBuildingId() != null) {
            tableName = "matching_check_record_" + washbasin.getBuildingId();
        }

        MatchingCheckRecord record = new MatchingCheckRecord();
        record.setUnitId(0L);
        record.setWashbasinId(washbasin != null ? washbasin.getId() : null);
        record.setCheckType(checkType);
        record.setUnitResidentCount(0);
        record.setTotalCapacity(washbasin != null && washbasin.getCapacity() != null ? washbasin.getCapacity() : 0);
        record.setCheckResult("INFO");
        record.setCheckMessage(message);
        record.setOperator(operator);
        record.setCheckTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        checkRecordMapper.insertIntoTable(tableName, record);
    }
}
