package com.example.dormitory.service.impl;

import com.example.dormitory.dto.request.BindingRequestDTO;
import com.example.dormitory.dto.response.MatchingCheckResultDTO;
import com.example.dormitory.dto.response.UnitMatchingDTO;
import com.example.dormitory.entity.Building;
import com.example.dormitory.entity.LivingUnit;
import com.example.dormitory.entity.MatchingCheckRecord;
import com.example.dormitory.entity.UnitWashbasinBinding;
import com.example.dormitory.entity.Washbasin;
import com.example.dormitory.mapper.BuildingMapper;
import com.example.dormitory.mapper.LivingUnitMapper;
import com.example.dormitory.mapper.MatchingCheckRecordMapper;
import com.example.dormitory.mapper.UnitWashbasinBindingMapper;
import com.example.dormitory.mapper.WashbasinMapper;
import com.example.dormitory.service.MatchingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchingServiceImpl implements MatchingService {

    private final LivingUnitMapper livingUnitMapper;
    private final WashbasinMapper washbasinMapper;
    private final UnitWashbasinBindingMapper bindingMapper;
    private final MatchingCheckRecordMapper checkRecordMapper;
    private final BuildingMapper buildingMapper;

    private static final double WARN_THRESHOLD = 0.85;

    public MatchingServiceImpl(LivingUnitMapper livingUnitMapper,
                               WashbasinMapper washbasinMapper,
                               UnitWashbasinBindingMapper bindingMapper,
                               MatchingCheckRecordMapper checkRecordMapper,
                               BuildingMapper buildingMapper) {
        this.livingUnitMapper = livingUnitMapper;
        this.washbasinMapper = washbasinMapper;
        this.bindingMapper = bindingMapper;
        this.checkRecordMapper = checkRecordMapper;
        this.buildingMapper = buildingMapper;
    }

    @Override
    @Transactional
    public MatchingCheckResultDTO bindWashbasin(BindingRequestDTO request) {
        LivingUnit unit = livingUnitMapper.selectById(request.getUnitId());
        Washbasin washbasin = washbasinMapper.selectById(request.getWashbasinId());

        if (unit == null) {
            throw new RuntimeException("居住单元不存在");
        }
        if (washbasin == null) {
            throw new RuntimeException("洗漱台不存在");
        }

        if (!unit.getBuildingId().equals(washbasin.getBuildingId())) {
            throw new RuntimeException("洗漱台与居住单元不属于同一楼栋");
        }

        List<UnitWashbasinBinding> existingBindings = bindingMapper.selectByUnitId(request.getUnitId());
        boolean alreadyBound = existingBindings.stream()
                .anyMatch(b -> b.getWashbasinId().equals(request.getWashbasinId()));

        if (alreadyBound) {
            throw new RuntimeException("该洗漱台已绑定到该单元");
        }

        Integer totalCapacity = bindingMapper.sumCapacityByUnitId(request.getUnitId());
        if (totalCapacity == null) {
            totalCapacity = 0;
        }
        totalCapacity += washbasin.getCapacity();

        MatchingCheckResultDTO checkResult = performCapacityCheck(unit.getResidentCount(), totalCapacity);

        UnitWashbasinBinding binding = new UnitWashbasinBinding();
        binding.setUnitId(request.getUnitId());
        binding.setWashbasinId(request.getWashbasinId());
        binding.setBindingTime(LocalDateTime.now());
        binding.setStatus(1);
        bindingMapper.insert(binding);

        saveCheckRecord(request.getUnitId(), request.getWashbasinId(), "BIND",
                unit.getResidentCount(), totalCapacity,
                checkResult.getCheckResult(), checkResult.getCheckMessage(),
                request.getOperator());

        return checkResult;
    }

    @Override
    @Transactional
    public MatchingCheckResultDTO unbindWashbasin(Long unitId, Long washbasinId, String operator) {
        LivingUnit unit = livingUnitMapper.selectById(unitId);
        if (unit == null) {
            throw new RuntimeException("居住单元不存在");
        }

        List<UnitWashbasinBinding> bindings = bindingMapper.selectByUnitId(unitId);
        UnitWashbasinBinding bindingToRemove = bindings.stream()
                .filter(b -> b.getWashbasinId().equals(washbasinId))
                .findFirst()
                .orElse(null);

        if (bindingToRemove == null) {
            throw new RuntimeException("该绑定关系不存在");
        }

        Integer totalCapacity = bindingMapper.sumCapacityByUnitId(unitId);
        if (totalCapacity == null) {
            totalCapacity = 0;
        }

        Washbasin washbasin = washbasinMapper.selectById(washbasinId);
        if (washbasin != null) {
            totalCapacity = Math.max(0, totalCapacity - washbasin.getCapacity());
        }

        bindingMapper.invalidateByWashbasinId(washbasinId);

        MatchingCheckResultDTO checkResult = performCapacityCheck(unit.getResidentCount(), totalCapacity);

        saveCheckRecord(unitId, washbasinId, "UNBIND",
                unit.getResidentCount(), totalCapacity,
                checkResult.getCheckResult(), checkResult.getCheckMessage(),
                operator);

        return checkResult;
    }

    @Override
    public MatchingCheckResultDTO checkCapacity(Long unitId) {
        LivingUnit unit = livingUnitMapper.selectById(unitId);
        if (unit == null) {
            throw new RuntimeException("居住单元不存在");
        }

        Integer totalCapacity = bindingMapper.sumCapacityByUnitId(unitId);
        if (totalCapacity == null) {
            totalCapacity = 0;
        }

        return performCapacityCheck(unit.getResidentCount(), totalCapacity);
    }

    @Override
    public UnitMatchingDTO getUnitMatchingInfo(Long unitId) {
        LivingUnit unit = livingUnitMapper.selectById(unitId);
        if (unit == null) {
            throw new RuntimeException("居住单元不存在");
        }

        Building building = buildingMapper.selectById(unit.getBuildingId());
        List<UnitWashbasinBinding> bindings = bindingMapper.selectByUnitId(unitId);

        List<UnitMatchingDTO.WashbasinInfo> washbasinInfos = new ArrayList<>();
        Integer totalCapacity = 0;

        for (UnitWashbasinBinding binding : bindings) {
            Washbasin washbasin = washbasinMapper.selectById(binding.getWashbasinId());
            if (washbasin != null && washbasin.getStatus() == 1) {
                UnitMatchingDTO.WashbasinInfo info = new UnitMatchingDTO.WashbasinInfo();
                info.setId(washbasin.getId());
                info.setWashbasinCode(washbasin.getWashbasinCode());
                info.setCapacity(washbasin.getCapacity());
                info.setLocation(washbasin.getLocation());
                washbasinInfos.add(info);
                totalCapacity += washbasin.getCapacity();
            }
        }

        UnitMatchingDTO dto = new UnitMatchingDTO();
        dto.setUnitId(unit.getId());
        dto.setUnitCode(unit.getUnitCode());
        dto.setBuildingName(building != null ? building.getBuildingName() : "");
        dto.setFloor(unit.getFloor());
        dto.setRoomCount(unit.getRoomCount());
        dto.setResidentCount(unit.getResidentCount());
        dto.setWashbasins(washbasinInfos);
        dto.setTotalCapacity(totalCapacity);
        dto.setRemainingCapacity(totalCapacity - unit.getResidentCount());
        dto.setUsageRate(totalCapacity > 0 ? (double) unit.getResidentCount() / totalCapacity * 100 : 0);

        if (totalCapacity == 0) {
            dto.setMatchingStatus("未绑定");
        } else if (unit.getResidentCount() > totalCapacity) {
            dto.setMatchingStatus("不匹配");
        } else if (dto.getUsageRate() >= WARN_THRESHOLD * 100) {
            dto.setMatchingStatus("预警");
        } else {
            dto.setMatchingStatus("匹配");
        }

        return dto;
    }

    @Override
    public List<UnitMatchingDTO> getAllUnitsMatchingInfo() {
        List<LivingUnit> units = livingUnitMapper.selectList(null);
        return units.stream()
                .map(unit -> getUnitMatchingInfo(unit.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchingCheckRecord> getCheckRecordsByUnitId(Long unitId) {
        LivingUnit unit = livingUnitMapper.selectById(unitId);
        if (unit == null) {
            return new ArrayList<>();
        }
        String tableName = getTableNameByBuildingId(unit.getBuildingId());
        return checkRecordMapper.selectByUnitIdFromTable(tableName, unitId);
    }

    @Override
    public List<MatchingCheckRecord> getRecentCheckRecords() {
        return checkRecordMapper.selectAllFromAllTables();
    }

    @Override
    public void saveCheckRecord(Long unitId, Long washbasinId, String checkType,
                                Integer residentCount, Integer totalCapacity,
                                String checkResult, String checkMessage, String operator) {
        LivingUnit unit = livingUnitMapper.selectById(unitId);
        String tableName = "matching_check_record";
        if (unit != null) {
            tableName = getTableNameByBuildingId(unit.getBuildingId());
        }

        MatchingCheckRecord record = new MatchingCheckRecord();
        record.setUnitId(unitId);
        record.setWashbasinId(washbasinId);
        record.setCheckType(checkType);
        record.setUnitResidentCount(residentCount);
        record.setTotalCapacity(totalCapacity);
        record.setCheckResult(checkResult);
        record.setCheckMessage(checkMessage);
        record.setOperator(operator);
        record.setCheckTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        checkRecordMapper.insertIntoTable(tableName, record);
    }

    private String getTableNameByBuildingId(Long buildingId) {
        if (buildingId == null) {
            return "matching_check_record";
        }
        return "matching_check_record_" + buildingId;
    }

    private MatchingCheckResultDTO performCapacityCheck(Integer residentCount, Integer totalCapacity) {
        if (totalCapacity == 0) {
            MatchingCheckResultDTO result = new MatchingCheckResultDTO();
            result.setSuccess(true);
            result.setCheckResult("WARN");
            result.setUnitResidentCount(residentCount);
            result.setTotalCapacity(totalCapacity);
            result.setRemainingCapacity(-residentCount);
            result.setUsageRate(0.0);
            result.setCheckMessage("未绑定任何洗漱台，请先绑定洗漱设施");
            return result;
        }

        if (residentCount > totalCapacity) {
            return MatchingCheckResultDTO.fail(residentCount, totalCapacity);
        }

        double usageRate = (double) residentCount / totalCapacity;
        if (usageRate >= WARN_THRESHOLD) {
            return MatchingCheckResultDTO.warn(residentCount, totalCapacity);
        }

        return MatchingCheckResultDTO.pass(residentCount, totalCapacity);
    }
}