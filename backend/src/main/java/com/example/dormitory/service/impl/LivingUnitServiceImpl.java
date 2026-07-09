package com.example.dormitory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormitory.dto.request.LivingUnitCreateDTO;
import com.example.dormitory.dto.request.LivingUnitUpdateDTO;
import com.example.dormitory.entity.LivingUnit;
import com.example.dormitory.mapper.LivingUnitMapper;
import com.example.dormitory.service.LivingUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LivingUnitServiceImpl implements LivingUnitService {

    private final LivingUnitMapper livingUnitMapper;

    public LivingUnitServiceImpl(LivingUnitMapper livingUnitMapper) {
        this.livingUnitMapper = livingUnitMapper;
    }

    @Override
    @Transactional
    public LivingUnit create(LivingUnitCreateDTO dto) {
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
        LivingUnit unit = livingUnitMapper.selectById(id);
        if (unit == null) {
            throw new RuntimeException("居住单元不存在");
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
}