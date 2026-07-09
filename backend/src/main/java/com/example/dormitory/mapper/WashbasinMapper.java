package com.example.dormitory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitory.entity.Washbasin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WashbasinMapper extends BaseMapper<Washbasin> {

    @Select("SELECT SUM(capacity) FROM washbasin WHERE building_id = #{buildingId} AND status = 1")
    Integer sumCapacityByBuildingId(@Param("buildingId") Long buildingId);

    @Select("SELECT w.* FROM washbasin w WHERE w.building_id = #{buildingId} AND w.status = 1")
    List<Washbasin> selectByBuildingId(@Param("buildingId") Long buildingId);
}