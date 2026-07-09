package com.example.dormitory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitory.entity.LivingUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LivingUnitMapper extends BaseMapper<LivingUnit> {

    @Select("SELECT u.* FROM living_unit u WHERE u.building_id = #{buildingId} AND u.status = 1")
    List<LivingUnit> selectByBuildingId(@Param("buildingId") Long buildingId);
}