package com.example.dormitory.controller;

import com.example.dormitory.dto.request.QuotaAppendDTO;
import com.example.dormitory.dto.request.QuotaOrderCreateDTO;
import com.example.dormitory.dto.response.ApiResponse;
import com.example.dormitory.dto.response.BuildingQuotaStatusDTO;
import com.example.dormitory.entity.ShiftQuotaOrder;
import com.example.dormitory.service.ShiftQuotaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/quota-orders")
public class ShiftQuotaController {

    private final ShiftQuotaService shiftQuotaService;

    public ShiftQuotaController(ShiftQuotaService shiftQuotaService) {
        this.shiftQuotaService = shiftQuotaService;
    }

    @PostMapping
    public ApiResponse<ShiftQuotaOrder> create(@Valid @RequestBody QuotaOrderCreateDTO dto) {
        return ApiResponse.success(shiftQuotaService.create(dto));
    }

    @PostMapping("/{id}/append")
    public ApiResponse<ShiftQuotaOrder> append(@PathVariable Long id, @Valid @RequestBody QuotaAppendDTO dto) {
        return ApiResponse.success(shiftQuotaService.append(id, dto));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ShiftQuotaOrder> close(@PathVariable Long id,
                                              @RequestParam(required = false) String operator) {
        return ApiResponse.success(shiftQuotaService.close(id, operator));
    }

    @GetMapping
    public ApiResponse<List<ShiftQuotaOrder>> list(@RequestParam(required = false) Long buildingId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate quotaDate) {
        return ApiResponse.success(shiftQuotaService.list(buildingId, status, quotaDate));
    }

    @GetMapping("/current")
    public ApiResponse<ShiftQuotaOrder> getCurrentOpen(@RequestParam Long buildingId) {
        return ApiResponse.success(shiftQuotaService.getCurrentOpen(buildingId));
    }

    @GetMapping("/building-status")
    public ApiResponse<List<BuildingQuotaStatusDTO>> getBuildingStatuses() {
        return ApiResponse.success(shiftQuotaService.getBuildingStatuses());
    }
}
