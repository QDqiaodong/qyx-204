package com.example.dormitory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitory.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    @Select("SELECT * FROM washbasin_repair_order WHERE washbasin_id = #{washbasinId} AND status = 'PENDING' LIMIT 1")
    RepairOrder selectOpenByWashbasinId(@Param("washbasinId") Long washbasinId);

    @Select("SELECT * FROM washbasin_repair_order WHERE washbasin_id = #{washbasinId} AND status = 'PENDING' LIMIT 1 FOR UPDATE")
    RepairOrder selectOpenByWashbasinIdForUpdate(@Param("washbasinId") Long washbasinId);

    @Select("SELECT * FROM washbasin_repair_order WHERE id = #{id} FOR UPDATE")
    RepairOrder selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM washbasin_repair_order ORDER BY created_at DESC, id DESC")
    List<RepairOrder> selectAllOrders();

    /** 全部未结送检单，匹配一览一次取回，用于剔除配套并展示“送检中”痕迹 */
    @Select("SELECT * FROM washbasin_repair_order WHERE status = 'PENDING'")
    List<RepairOrder> selectPendingOrders();

    /** 登记修复：状态置已修复，同时把未结标记置 NULL，释放该台的再次开单与配套资格 */
    @Update("UPDATE washbasin_repair_order SET status = 'REPAIRED', open_flag = NULL, " +
            "repair_note = #{repairNote}, repaired_at = #{repairedAt}, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int markRepaired(@Param("id") Long id,
                     @Param("repairNote") String repairNote,
                     @Param("repairedAt") LocalDateTime repairedAt);
}
