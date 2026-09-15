package com.example.dormitory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormitory.dto.request.LivingUnitCreateDTO;
import com.example.dormitory.dto.request.LivingUnitUpdateDTO;
import com.example.dormitory.entity.LivingUnit;
import com.example.dormitory.entity.ShiftQuotaOrder;
import com.example.dormitory.mapper.LivingUnitMapper;
import com.example.dormitory.mapper.ShiftQuotaOrderMapper;
import com.example.dormitory.service.LivingUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LivingUnitServiceImpl implements LivingUnitService {

    private final LivingUnitMapper livingUnitMapper;
    private final ShiftQuotaOrderMapper shiftQuotaOrderMapper;

    public LivingUnitServiceImpl(LivingUnitMapper livingUnitMapper,
                                 ShiftQuotaOrderMapper shiftQuotaOrderMapper) {
        this.livingUnitMapper = livingUnitMapper;
        this.shiftQuotaOrderMapper = shiftQuotaOrderMapper;
    }

    @Override
    @Transactional
    public LivingUnit create(LivingUnitCreateDTO dto) {
        // 先锁定该楼栋未结定额单（如存在），保证人数校验与定额追加互斥
        ShiftQuotaOrder openOrder = dto.getBuildingId() != null
                ? shiftQuotaOrderMapper.selectOpenByBuildingIdForUpdate(dto.getBuildingId())
                : null;

        int newCount = dto.getResidentCount() != null ? dto.getResidentCount() : 0;
        if (openOrder != null && newCount > 0) {
            int currentSum = safeSum(dto.getBuildingId());
            long newSum = (long) currentSum + newCount;
            if (newSum > openOrder.getQuotaCapacity()) {
                throwOverQuota(openOrder, newSum);
            }
        }

        LivingUnit unit = new LivingUnit();
        unit.setUnitCode(dto.getUnitCode());
        unit.setBuildingId(dto.getBuildingId());
        unit.setFloor(dto.getFloor());
        unit.setRoomCount(dto.getRoomCount());
        unit.setResidentCount(dto.getResidentCount());
        unit.setStatus(dto.getStatus());
        livingUnitMapper.insert(unit);
        return unit;
    }

    @Override
    @Transactional
    public LivingUnit update(Long id, LivingUnitUpdateDTO dto) {
        LivingUnit existing = livingUnitMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("居住单元不存在");
        }

        Long targetBuildingId = dto.getBuildingId() != null ? dto.getBuildingId() : existing.getBuildingId();

        // 锁顺序：先锁楼栋未结定额单，再锁单元行，避免与定额追加/结案死锁
        ShiftQuotaOrder openOrder = targetBuildingId != null
                ? shiftQuotaOrderMapper.selectOpenByBuildingIdForUpdate(targetBuildingId)
                : null;

        LivingUnit unit = livingUnitMapper.selectByIdForUpdate(id);
        if (unit == null) {
            throw new RuntimeException("居住单元不存在");
        }

        int oldCount = unit.getResidentCount() != null ? unit.getResidentCount() : 0;
        int newCount = dto.getResidentCount() != null ? dto.getResidentCount() : oldCount;

        if (openOrder != null && newCount > oldCount) {
            int currentSum = safeSum(targetBuildingId);
            long newSum;
            if (targetBuildingId.equals(unit.getBuildingId())) {
                newSum = (long) currentSum - oldCount + newCount;
            } else {
                newSum = (long) currentSum + newCount;
            }
            if (newSum > openOrder.getQuotaCapacity()) {
                throwOverQuota(openOrder, newSum);
            }
        }

        if (dto.getUnitCode() != null) {
            unit.setUnitCode(dto.getUnitCode());
        }
        if (dto.getBuildingId() != null) {
            unit.setBuildingId(dto.getBuildingId());
        }
        if (dto.getFloor() != null) {
            unit.setFloor(dto.getFloor());
        }
        if (dto.getRoomCount() != null) {
            unit.setRoomCount(dto.getRoomCount());
        }
        if (dto.getResidentCount() != null) {
            unit.setResidentCount(dto.getResidentCount());
        }
        if (dto.getStatus() != null) {
            unit.setStatus(dto.getStatus());
        }

        livingUnitMapper.updateById(unit);
        return unit;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        livingUnitMapper.deleteById(id);
    }

    @Override
    public LivingUnit getById(Long id) {
        return livingUnitMapper.selectById(id);
    }

    @Override
    public List<LivingUnit> getAll() {
        return livingUnitMapper.selectList(new LambdaQueryWrapper<LivingUnit>().eq(LivingUnit::getStatus, 1));
    }

    @Override
    public List<LivingUnit> getByBuildingId(Long buildingId) {
        return livingUnitMapper.selectByBuildingId(buildingId);
    }

    private int safeSum(Long buildingId) {
        Integer sum = livingUnitMapper.sumResidentCountByBuildingId(buildingId);
        return sum != null ? sum : 0;
    }

    private void throwOverQuota(ShiftQuotaOrder openOrder, long newSum) {
        throw new RuntimeException("超过当班用水定额：该楼栋当班定额可洗" + openOrder.getQuotaCapacity()
                + "人，调整后居住人数合计" + newSum + "人。请先追加定额或把人数改回去后再保存");
    }
}
