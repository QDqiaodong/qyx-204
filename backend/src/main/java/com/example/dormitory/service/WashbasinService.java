package com.example.dormitory.service;

import com.example.dormitory.dto.request.WashbasinCreateDTO;
import com.example.dormitory.dto.request.WashbasinUpdateDTO;
import com.example.dormitory.entity.Washbasin;

import java.util.List;

public interface WashbasinService {

    Washbasin create(WashbasinCreateDTO dto);

    Washbasin update(Long id, WashbasinUpdateDTO dto);

    void delete(Long id);

    Washbasin getById(Long id);

    List<Washbasin> getAll();

    List<Washbasin> getByBuildingId(Long buildingId);

    Integer getTotalCapacityByBuildingId(Long buildingId);

    void refreshRedisCache();
}