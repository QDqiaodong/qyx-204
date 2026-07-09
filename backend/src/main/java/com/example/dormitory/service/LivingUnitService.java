package com.example.dormitory.service;

import com.example.dormitory.dto.request.LivingUnitCreateDTO;
import com.example.dormitory.dto.request.LivingUnitUpdateDTO;
import com.example.dormitory.entity.LivingUnit;

import java.util.List;

public interface LivingUnitService {

    LivingUnit create(LivingUnitCreateDTO dto);

    LivingUnit update(Long id, LivingUnitUpdateDTO dto);

    void delete(Long id);

    LivingUnit getById(Long id);

    List<LivingUnit> getAll();

    List<LivingUnit> getByBuildingId(Long buildingId);
}