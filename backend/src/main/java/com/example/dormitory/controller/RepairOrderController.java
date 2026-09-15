package com.example.dormitory.controller;

import com.example.dormitory.dto.request.RepairCompleteDTO;
import com.example.dormitory.dto.request.RepairOrderCreateDTO;
import com.example.dormitory.dto.response.ApiResponse;
import com.example.dormitory.entity.RepairOrder;
import com.example.dormitory.service.RepairOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair-orders")
public class RepairOrderController {

    private final RepairOrderService repairOrderService;

    public RepairOrderController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    /** 开送检单：选中一台、损坏部位、经办值班员 */
    @PostMapping
    public ApiResponse<RepairOrder> create(@Valid @RequestBody RepairOrderCreateDTO dto) {
        return ApiResponse.success(repairOrderService.create(dto));
    }

    /** 修复登记：待接单 -> 已修复 */
    @PostMapping("/{id}/complete")
    public ApiResponse<RepairOrder> complete(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) RepairCompleteDTO dto) {
        return ApiResponse.success(repairOrderService.complete(id, dto != null ? dto : new RepairCompleteDTO()));
    }

    /** 送检台账：可按洗漱台 / 状态（PENDING待接单、REPAIRED已修复）/ 楼栋筛选 */
    @GetMapping
    public ApiResponse<List<RepairOrder>> list(@RequestParam(required = false) Long washbasinId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long buildingId) {
        return ApiResponse.success(repairOrderService.list(washbasinId, status, buildingId));
    }

    /** 查某台当前未结送检（带出上一张从什么时候开始还没结） */
    @GetMapping("/current")
    public ApiResponse<RepairOrder> getCurrentOpen(@RequestParam Long washbasinId) {
        return ApiResponse.success(repairOrderService.getOpenByWashbasin(washbasinId));
    }

    /** 全部未结送检单 */
    @GetMapping("/open")
    public ApiResponse<List<RepairOrder>> listOpen() {
        return ApiResponse.success(repairOrderService.listOpen());
    }
}
