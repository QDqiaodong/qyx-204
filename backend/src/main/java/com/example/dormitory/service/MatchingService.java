package com.example.dormitory.service;

import com.example.dormitory.dto.request.BindingRequestDTO;
import com.example.dormitory.dto.response.MatchingCheckResultDTO;
import com.example.dormitory.dto.response.UnitMatchingDTO;
import com.example.dormitory.entity.MatchingCheckRecord;

import java.util.List;

public interface MatchingService {

    MatchingCheckResultDTO bindWashbasin(BindingRequestDTO request);

    MatchingCheckResultDTO bindWashbasinInTx(BindingRequestDTO request);

    MatchingCheckResultDTO unbindWashbasin(Long unitId, Long washbasinId, String operator);

    MatchingCheckResultDTO unbindWashbasinInTx(Long unitId, Long washbasinId, String operator);

    MatchingCheckResultDTO checkCapacity(Long unitId);

    UnitMatchingDTO getUnitMatchingInfo(Long unitId);

    List<UnitMatchingDTO> getAllUnitsMatchingInfo();

    List<MatchingCheckRecord> getCheckRecordsByUnitId(Long unitId);

    List<MatchingCheckRecord> getRecentCheckRecords();

    void saveCheckRecord(Long unitId, Long washbasinId, String checkType,
                         Integer residentCount, Integer totalCapacity,
                         String checkResult, String checkMessage, String operator);
}