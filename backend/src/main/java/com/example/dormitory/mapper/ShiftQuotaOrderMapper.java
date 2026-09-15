package com.example.dormitory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitory.entity.ShiftQuotaOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ShiftQuotaOrderMapper extends BaseMapper<ShiftQuotaOrder> {

    @Select("SELECT * FROM shift_quota_order WHERE building_id = #{buildingId} AND status = 'OPEN' LIMIT 1")
    ShiftQuotaOrder selectOpenByBuildingId(@Param("buildingId") Long buildingId);

    @Select("SELECT * FROM shift_quota_order WHERE building_id = #{buildingId} AND status = 'OPEN' LIMIT 1 FOR UPDATE")
    ShiftQuotaOrder selectOpenByBuildingIdForUpdate(@Param("buildingId") Long buildingId);

    @Select("SELECT * FROM shift_quota_order WHERE id = #{id} FOR UPDATE")
    ShiftQuotaOrder selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM shift_quota_order WHERE building_id = #{buildingId} AND quota_date = #{quotaDate} AND status = 'CLOSED'")
    Integer countClosedByBuildingAndDate(@Param("buildingId") Long buildingId, @Param("quotaDate") LocalDate quotaDate);

    @Select("SELECT * FROM shift_quota_order ORDER BY created_at DESC, id DESC")
    List<ShiftQuotaOrder> selectAllOrders();
}
