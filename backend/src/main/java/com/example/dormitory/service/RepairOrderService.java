package com.example.dormitory.service;

import com.example.dormitory.dto.request.RepairCompleteDTO;
import com.example.dormitory.dto.request.RepairOrderCreateDTO;
import com.example.dormitory.entity.RepairOrder;

import java.util.List;

public interface RepairOrderService {

    /**
     * 开送检单：同一台洗漱台只要还有未结送检（待接单），不能再开第二张；
     * 并发开单只允许一张落库，后交的一张失败并指明先写下的那张。
     */
    RepairOrder create(RepairOrderCreateDTO dto);

    /**
     * 修复登记：待接单 -> 已修复，只追加一条修复痕迹，不改原单内容。
     * 已修复后该台才能重新计入配套洗漱台。
     */
    RepairOrder complete(Long id, RepairCompleteDTO dto);

    List<RepairOrder> list(Long washbasinId, String status, Long buildingId);

    /** 全部未结送检单（一次取回，供一览/开单弹窗批量判断哪些台在送检中） */
    List<RepairOrder> listOpen();

    RepairOrder getOpenByWashbasin(Long washbasinId);
}
