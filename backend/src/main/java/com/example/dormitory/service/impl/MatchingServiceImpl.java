package com.example.dormitory.service.impl;

import com.example.dormitory.config.RedisLock;
import com.example.dormitory.dto.request.BindingRequestDTO;
import com.example.dormitory.dto.response.MatchingCheckResultDTO;
import com.example.dormitory.dto.response.UnitMatchingDTO;
import com.example.dormitory.entity.Building;
import com.example.dormitory.entity.LivingUnit;
import com.example.dormitory.entity.MatchingCheckRecord;
import com.example.dormitory.entity.ShiftQuotaOrder;
import com.example.dormitory.entity.UnitWashbasinBinding;
import com.example.dormitory.entity.Washbasin;
import com.example.dormitory.mapper.BuildingMapper;
import com.example.dormitory.mapper.LivingUnitMapper;
import com.example.dormitory.mapper.MatchingCheckRecordMapper;
import com.example.dormitory.mapper.ShiftQuotaOrderMapper;
import com.example.dormitory.mapper.UnitWashbasinBindingMapper;
import com.example.dormitory.mapper.WashbasinMapper;
import com.example.dormitory.service.MatchingService;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
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
    private final ShiftQuotaOrderMapper shiftQuotaOrderMapper;
    private final RedisLock redisLock;

    /**
     * 自注入代理，保证 bind/unbind 的事务边界在分布式锁之内，
     * 即“拿到锁 -> 开事务 -> 提交 -> 放锁”，避免锁在事务提交前释放造成的空窗。
     */
    private final MatchingService self;

    private static final double WARN_THRESHOLD = 0.85;

    private static final String LOCK_WASHBASIN_PREFIX = "lock:matching:washbasin:";

    public MatchingServiceImpl(LivingUnitMapper livingUnitMapper,
                               WashbasinMapper washbasinMapper,
                               UnitWashbasinBindingMapper bindingMapper,
                               MatchingCheckRecordMapper checkRecordMapper,
                               BuildingMapper buildingMapper,
                               ShiftQuotaOrderMapper shiftQuotaOrderMapper,
                               RedisLock redisLock,
                               @Lazy MatchingService self) {
        this.livingUnitMapper = livingUnitMapper;
        this.washbasinMapper = washbasinMapper;
        this.bindingMapper = bindingMapper;
        this.checkRecordMapper = checkRecordMapper;
        this.buildingMapper = buildingMapper;
        this.shiftQuotaOrderMapper = shiftQuotaOrderMapper;
        this.redisLock = redisLock;
        this.self = self;
    }

    @Override
    public MatchingCheckResultDTO bindWashbasin(BindingRequestDTO request) {
        String lockKey = LOCK_WASHBASIN_PREFIX + request.getWashbasinId();
        String token = redisLock.tryLock(lockKey);
        try {
            return self.bindWashbasinInTx(request);
        } finally {
            redisLock.unlock(lockKey, token);
        }
    }

    @Override
    @Transactional
    public MatchingCheckResultDTO bindWashbasinInTx(BindingRequestDTO request) {
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

        // 锁定洗漱台行：同台的并发绑定/解绑在此串行，杜绝“挂上去瞬间被拆请求误伤”
        washbasinMapper.selectByIdForUpdate(request.getWashbasinId());

        UnitWashbasinBinding existing =
                bindingMapper.selectByUnitAndWashbasinForUpdate(request.getUnitId(), request.getWashbasinId());
        if (existing != null && existing.getStatus() == 1) {
            throw new RuntimeException("该洗漱台已绑定到该单元");
        }

        if (existing == null) {
            UnitWashbasinBinding binding = new UnitWashbasinBinding();
            binding.setUnitId(request.getUnitId());
            binding.setWashbasinId(request.getWashbasinId());
            binding.setBindingTime(LocalDateTime.now());
            binding.setStatus(1);
            try {
                bindingMapper.insert(binding);
            } catch (DuplicateKeyException e) {
                // 并发下可能已被另一事务写入，按“复活失效关系”处理
                existing = bindingMapper.selectByUnitAndWashbasin(request.getUnitId(), request.getWashbasinId());
                if (existing == null || existing.getStatus() == 1) {
                    throw new RuntimeException("该洗漱台已绑定到该单元");
                }
                bindingMapper.reactivate(existing.getId());
            }
        } else {
            // 该单元此前拆过同台：复活原失效行，而不是新增，避免唯一键冲突
            bindingMapper.reactivate(existing.getId());
        }

        // 容量一律以变更后该单元仍有效的绑定重查，绝不沿用历史快照
        Integer totalCapacity = bindingMapper.sumCapacityByUnitId(request.getUnitId());
        if (totalCapacity == null) {
            totalCapacity = 0;
        }

        MatchingCheckResultDTO checkResult = performCapacityCheck(unit.getResidentCount(), totalCapacity);

        saveCheckRecord(request.getUnitId(), request.getWashbasinId(), "BIND",
                unit.getResidentCount(), totalCapacity,
                checkResult.getCheckResult(), checkResult.getCheckMessage(),
                request.getOperator());

        return checkResult;
    }

    @Override
    public MatchingCheckResultDTO unbindWashbasin(Long unitId, Long washbasinId, String operator) {
        String lockKey = LOCK_WASHBASIN_PREFIX + washbasinId;
        String token = redisLock.tryLock(lockKey);
        try {
            return self.unbindWashbasinInTx(unitId, washbasinId, operator);
        } finally {
            redisLock.unlock(lockKey, token);
        }
    }

    @Override
    @Transactional
    public MatchingCheckResultDTO unbindWashbasinInTx(Long unitId, Long washbasinId, String operator) {
        LivingUnit unit = livingUnitMapper.selectById(unitId);
        if (unit == null) {
            throw new RuntimeException("居住单元不存在");
        }

        // 锁定洗漱台行：同台并发操作（别人正在把同台挂到第三个单元）在此串行，
        // 下面只按 (unitId, washbasinId) 精确失效，绝不波及其他单元的关系
        Washbasin washbasin = washbasinMapper.selectByIdForUpdate(washbasinId);
        if (washbasin == null) {
            throw new RuntimeException("洗漱台不存在");
        }

        UnitWashbasinBinding binding =
                bindingMapper.selectByUnitAndWashbasinForUpdate(unitId, washbasinId);
        if (binding == null || binding.getStatus() != 1) {
            throw new RuntimeException("该绑定关系不存在");
        }

        // 只拆当前单元与这台洗漱台的一条关系，其他单元挂同台的关系原样保留
        int affected = bindingMapper.invalidate(unitId, washbasinId);
        if (affected == 0) {
            throw new RuntimeException("该绑定关系不存在");
        }

        // 按拆完后本单元仍有效绑定重新汇总，已拆掉的容量不再计入
        Integer remainingCapacity = bindingMapper.sumCapacityByUnitId(unitId);
        if (remainingCapacity == null) {
            remainingCapacity = 0;
        }

        MatchingCheckResultDTO checkResult = performCapacityCheck(unit.getResidentCount(), remainingCapacity);

        // 仅追加一条 UNBIND 痕迹，历史 BIND 等记录不改、不删
        saveCheckRecord(unitId, washbasinId, "UNBIND",
                unit.getResidentCount(), remainingCapacity,
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
        dto.setBuildingId(unit.getBuildingId());
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

        applyShiftQuotaStatus(dto, unit.getBuildingId());

        return dto;
    }

    /**
     * 楼栋当班定额校验：该楼栋各单元居住人数合计一旦超过未结定额单的定额可洗人数，
     * 该楼栋所有单元在匹配一览中一律标为"超定额"，不得显示为匹配。
     */
    private void applyShiftQuotaStatus(UnitMatchingDTO dto, Long buildingId) {
        dto.setQuotaExceeded(false);
        if (buildingId == null) {
            return;
        }
        ShiftQuotaOrder openOrder = shiftQuotaOrderMapper.selectOpenByBuildingId(buildingId);
        if (openOrder == null) {
            return;
        }
        Integer residentTotal = livingUnitMapper.sumResidentCountByBuildingId(buildingId);
        int total = residentTotal != null ? residentTotal : 0;
        dto.setBuildingResidentTotal(total);
        dto.setShiftQuotaCapacity(openOrder.getQuotaCapacity());
        if (total > openOrder.getQuotaCapacity()) {
            dto.setMatchingStatus("超定额");
            dto.setQuotaExceeded(true);
        }
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