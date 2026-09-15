package com.example.dormitory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitory.entity.UnitWashbasinBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UnitWashbasinBindingMapper extends BaseMapper<UnitWashbasinBinding> {

    @Select("SELECT b.* FROM unit_washbasin_binding b WHERE b.unit_id = #{unitId} AND b.status = 1")
    List<UnitWashbasinBinding> selectByUnitId(@Param("unitId") Long unitId);

    @Select("SELECT b.* FROM unit_washbasin_binding b WHERE b.washbasin_id = #{washbasinId} AND b.status = 1")
    List<UnitWashbasinBinding> selectByWashbasinId(@Param("washbasinId") Long washbasinId);

    @Select("SELECT b.* FROM unit_washbasin_binding b WHERE b.unit_id = #{unitId} AND b.washbasin_id = #{washbasinId}")
    UnitWashbasinBinding selectByUnitAndWashbasin(@Param("unitId") Long unitId,
                                                  @Param("washbasinId") Long washbasinId);

    @Select("SELECT b.* FROM unit_washbasin_binding b WHERE b.unit_id = #{unitId} AND b.washbasin_id = #{washbasinId} FOR UPDATE")
    UnitWashbasinBinding selectByUnitAndWashbasinForUpdate(@Param("unitId") Long unitId,
                                                           @Param("washbasinId") Long washbasinId);

    @Select("SELECT SUM(w.capacity) FROM unit_washbasin_binding b " +
            "JOIN washbasin w ON b.washbasin_id = w.id " +
            "WHERE b.unit_id = #{unitId} AND b.status = 1 AND w.status = 1")
    Integer sumCapacityByUnitId(@Param("unitId") Long unitId);

    @Update("UPDATE unit_washbasin_binding SET status = 0, updated_at = NOW() " +
            "WHERE unit_id = #{unitId} AND washbasin_id = #{washbasinId} AND status = 1")
    int invalidate(@Param("unitId") Long unitId, @Param("washbasinId") Long washbasinId);

    @Update("UPDATE unit_washbasin_binding SET status = 1, binding_time = NOW(), updated_at = NOW() " +
            "WHERE id = #{id}")
    int reactivate(@Param("id") Long id);

    @Update("UPDATE unit_washbasin_binding SET status = 0 WHERE unit_id = #{unitId} AND status = 1")
    void invalidateByUnitId(@Param("unitId") Long unitId);
}