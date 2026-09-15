package com.example.dormitory.service;

import com.example.dormitory.dto.request.QuotaAppendDTO;
import com.example.dormitory.dto.request.QuotaOrderCreateDTO;
import com.example.dormitory.dto.response.BuildingQuotaStatusDTO;
import com.example.dormitory.entity.ShiftQuotaOrder;

import java.time.LocalDate;
import java.util.List;

public interface ShiftQuotaService {

    ShiftQuotaOrder create(QuotaOrderCreateDTO dto);

    ShiftQuotaOrder append(Long id, QuotaAppendDTO dto);

    ShiftQuotaOrder close(Long id, String operator);

    List<ShiftQuotaOrder> list(Long buildingId, String status, LocalDate quotaDate);

    ShiftQuotaOrder getCurrentOpen(Long buildingId);

    List<BuildingQuotaStatusDTO> getBuildingStatuses();
}
