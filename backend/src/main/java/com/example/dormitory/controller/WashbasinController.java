package com.example.dormitory.controller;

import com.example.dormitory.dto.request.WashbasinCreateDTO;
import com.example.dormitory.dto.request.WashbasinUpdateDTO;
import com.example.dormitory.dto.response.ApiResponse;
import com.example.dormitory.entity.Washbasin;
import com.example.dormitory.service.WashbasinService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/washbasins")
public class WashbasinController {

    private final WashbasinService washbasinService;

    public WashbasinController(WashbasinService washbasinService) {
        this.washbasinService = washbasinService;
    }

    @GetMapping
    public ApiResponse<List<Washbasin>> getAll() {
        return ApiResponse.success(washbasinService.getAll());
    }

    @GetMapping("/building/{buildingId}")
    public ApiResponse<List<Washbasin>> getByBuildingId(@PathVariable Long buildingId) {
        return ApiResponse.success(washbasinService.getByBuildingId(buildingId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Washbasin> getById(@PathVariable Long id) {
        return ApiResponse.success(washbasinService.getById(id));
    }

    @PostMapping
    public ApiResponse<Washbasin> create(@Valid @RequestBody WashbasinCreateDTO dto) {
        return ApiResponse.success(washbasinService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<Washbasin> update(@PathVariable Long id, @Valid @RequestBody WashbasinUpdateDTO dto) {
        return ApiResponse.success(washbasinService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        washbasinService.delete(id);
        return ApiResponse.success(null);
    }
}