package com.example.dormitory.controller;

import com.example.dormitory.dto.response.ApiResponse;
import com.example.dormitory.entity.Building;
import com.example.dormitory.mapper.BuildingMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingMapper buildingMapper;

    public BuildingController(BuildingMapper buildingMapper) {
        this.buildingMapper = buildingMapper;
    }

    @GetMapping
    public ApiResponse<List<Building>> getAll() {
        return ApiResponse.success(buildingMapper.selectList(null));
    }
}