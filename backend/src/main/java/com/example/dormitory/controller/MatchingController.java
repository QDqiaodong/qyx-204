package com.example.dormitory.controller;

import com.example.dormitory.dto.request.BindingRequestDTO;
import com.example.dormitory.dto.response.ApiResponse;
import com.example.dormitory.dto.response.MatchingCheckResultDTO;
import com.example.dormitory.dto.response.UnitMatchingDTO;
import com.example.dormitory.entity.MatchingCheckRecord;
import com.example.dormitory.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/bind")
    public ApiResponse<MatchingCheckResultDTO> bindWashbasin(@Valid @RequestBody BindingRequestDTO request) {
        return ApiResponse.success(matchingService.bindWashbasin(request));
    }

    @PostMapping("/unbind")
    public ApiResponse<MatchingCheckResultDTO> unbindWashbasin(@RequestParam Long unitId,
                                                               @RequestParam Long washbasinId,
                                                               @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success(matchingService.unbindWashbasin(unitId, washbasinId, operator));
    }

    @GetMapping("/check/{unitId}")
    public ApiResponse<MatchingCheckResultDTO> checkCapacity(@PathVariable Long unitId) {
        return ApiResponse.success(matchingService.checkCapacity(unitId));
    }

    @GetMapping("/unit/{unitId}")
    public ApiResponse<UnitMatchingDTO> getUnitMatchingInfo(@PathVariable Long unitId) {
        return ApiResponse.success(matchingService.getUnitMatchingInfo(unitId));
    }

    @GetMapping("/units")
    public ApiResponse<List<UnitMatchingDTO>> getAllUnitsMatchingInfo() {
        return ApiResponse.success(matchingService.getAllUnitsMatchingInfo());
    }

    @GetMapping("/records")
    public ApiResponse<List<MatchingCheckRecord>> getRecentCheckRecords() {
        return ApiResponse.success(matchingService.getRecentCheckRecords());
    }

    @GetMapping("/records/{unitId}")
    public ApiResponse<List<MatchingCheckRecord>> getCheckRecordsByUnitId(@PathVariable Long unitId) {
        return ApiResponse.success(matchingService.getCheckRecordsByUnitId(unitId));
    }
}