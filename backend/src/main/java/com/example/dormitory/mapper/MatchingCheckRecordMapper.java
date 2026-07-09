package com.example.dormitory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitory.entity.MatchingCheckRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MatchingCheckRecordMapper extends BaseMapper<MatchingCheckRecord> {

    @Select("SELECT r.* FROM matching_check_record r WHERE r.unit_id = #{unitId} ORDER BY r.check_time DESC")
    List<MatchingCheckRecord> selectByUnitId(@Param("unitId") Long unitId);

    @Select("SELECT r.* FROM matching_check_record r ORDER BY r.check_time DESC LIMIT #{limit}")
    List<MatchingCheckRecord> selectRecent(@Param("limit") Integer limit);

    @Select("SELECT r.* FROM ${tableName} r WHERE r.unit_id = #{unitId} ORDER BY r.check_time DESC")
    List<MatchingCheckRecord> selectByUnitIdFromTable(@Param("tableName") String tableName, @Param("unitId") Long unitId);

    @Select("SELECT * FROM (SELECT * FROM matching_check_record UNION ALL SELECT * FROM matching_check_record_1 UNION ALL SELECT * FROM matching_check_record_2 UNION ALL SELECT * FROM matching_check_record_3) AS t ORDER BY t.check_time DESC")
    List<MatchingCheckRecord> selectAllFromAllTables();

    @Select("SELECT r.* FROM ${tableName} r ORDER BY r.check_time DESC LIMIT #{limit}")
    List<MatchingCheckRecord> selectRecentFromTable(@Param("tableName") String tableName, @Param("limit") Integer limit);

    @Insert("INSERT INTO ${tableName} (unit_id, washbasin_id, check_type, unit_resident_count, total_capacity, check_result, check_message, operator, check_time, created_at) VALUES (#{record.unitId}, #{record.washbasinId}, #{record.checkType}, #{record.unitResidentCount}, #{record.totalCapacity}, #{record.checkResult}, #{record.checkMessage}, #{record.operator}, #{record.checkTime}, #{record.createdAt})")
    void insertIntoTable(@Param("tableName") String tableName, @Param("record") MatchingCheckRecord record);
}
