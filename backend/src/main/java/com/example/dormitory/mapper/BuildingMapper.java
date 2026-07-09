package com.example.dormitory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitory.entity.Building;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BuildingMapper extends BaseMapper<Building> {
}