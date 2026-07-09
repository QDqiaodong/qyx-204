package com.example.dormitory.controller;

import com.example.dormitory.dto.request.LivingUnitCreateDTO;
import com.example.dormitory.dto.request.LivingUnitUpdateDTO;
import com.example.dormitory.dto.response.ApiResponse;
import com.example.dormitory.entity.LivingUnit;
import com.example.dormitory.service.LivingUnitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/living-units")
public class LivingUnitController {

    private final LivingUnitService livingUnitService;

    public LivingUnitController(LivingUnitService livingUnitService) {
        this.livingUnitService = livingUnitService;
    }

    @GetMapping
    public ApiResponse<List<LivingUnit>> getAll() {
        return ApiResponse.success(livingUnitService.getAll());
    }

    @GetMapping("/building/{buildingId}")
    public ApiResponse<List<LivingUnit>> getByBuildingId(@PathVariable Long buildingId) {
        return ApiResponse.success(livingUnitService.getByBuildingId(buildingId));
    }

    @GetMapping("/{id}")
    public ApiResponse<LivingUnit> getById(@PathVariable Long id) {
        return ApiResponse.success(livingUnitService.getById(id));
    }

    @PostMapping
    public ApiResponse<LivingUnit> create(@Valid @RequestBody LivingUnitCreateDTO dto) {
        return ApiResponse.success(livingUnitService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<LivingUnit> update(@PathVariable Long id, @Valid @RequestBody LivingUnitUpdateDTO dto) {
        return ApiResponse.success(livingUnitService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        livingUnitService.delete(id);
        return ApiResponse.success(null);
    }
}