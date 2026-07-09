package com.example.dormitory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormitory.dto.request.WashbasinCreateDTO;
import com.example.dormitory.dto.request.WashbasinUpdateDTO;
import com.example.dormitory.entity.Washbasin;
import com.example.dormitory.mapper.WashbasinMapper;
import com.example.dormitory.service.WashbasinService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WashbasinServiceImpl implements WashbasinService {

    private final WashbasinMapper washbasinMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "washbasin:capacity:";

    public WashbasinServiceImpl(WashbasinMapper washbasinMapper, RedisTemplate<String, Object> redisTemplate) {
        this.washbasinMapper = washbasinMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public Washbasin create(WashbasinCreateDTO dto) {
        Washbasin washbasin = new Washbasin();
        washbasin.setWashbasinCode(dto.getWashbasinCode());
        washbasin.setCapacity(dto.getCapacity());
        washbasin.setBuildingId(dto.getBuildingId());
        washbasin.setLocation(dto.getLocation());
        washbasin.setStatus(dto.getStatus());
        washbasinMapper.insert(washbasin);
        updateRedisCache(dto.getBuildingId());
        return washbasin;
    }

    @Override
    @Transactional
    public Washbasin update(Long id, WashbasinUpdateDTO dto) {
        Washbasin washbasin = washbasinMapper.selectById(id);
        if (washbasin == null) {
            throw new RuntimeException("洗漱台不存在");
        }
        Long oldBuildingId = washbasin.getBuildingId();

        if (dto.getWashbasinCode() != null) {
            washbasin.setWashbasinCode(dto.getWashbasinCode());
        }
        if (dto.getCapacity() != null) {
            washbasin.setCapacity(dto.getCapacity());
        }
        if (dto.getBuildingId() != null) {
            washbasin.setBuildingId(dto.getBuildingId());
        }
        if (dto.getLocation() != null) {
            washbasin.setLocation(dto.getLocation());
        }
        if (dto.getStatus() != null) {
            washbasin.setStatus(dto.getStatus());
        }

        washbasinMapper.updateById(washbasin);
        updateRedisCache(oldBuildingId);
        updateRedisCache(washbasin.getBuildingId());
        return washbasin;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Washbasin washbasin = washbasinMapper.selectById(id);
        if (washbasin != null) {
            washbasinMapper.deleteById(id);
            updateRedisCache(washbasin.getBuildingId());
        }
    }

    @Override
    public Washbasin getById(Long id) {
        return washbasinMapper.selectById(id);
    }

    @Override
    public List<Washbasin> getAll() {
        return washbasinMapper.selectList(new LambdaQueryWrapper<Washbasin>().eq(Washbasin::getStatus, 1));
    }

    @Override
    public List<Washbasin> getByBuildingId(Long buildingId) {
        return washbasinMapper.selectByBuildingId(buildingId);
    }

    @Override
    public Integer getTotalCapacityByBuildingId(Long buildingId) {
        String key = REDIS_KEY_PREFIX + buildingId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (Integer) cached;
        }
        Integer capacity = washbasinMapper.sumCapacityByBuildingId(buildingId);
        if (capacity != null) {
            redisTemplate.opsForValue().set(key, capacity);
        }
        return capacity != null ? capacity : 0;
    }

    @Override
    public void refreshRedisCache() {
        List<Washbasin> all = washbasinMapper.selectList(new LambdaQueryWrapper<Washbasin>().eq(Washbasin::getStatus, 1));
        all.stream()
                .collect(Collectors.groupingBy(Washbasin::getBuildingId))
                .forEach((buildingId, washbasins) -> {
                    int total = washbasins.stream().mapToInt(Washbasin::getCapacity).sum();
                    redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + buildingId, total);
                });
    }

    private void updateRedisCache(Long buildingId) {
        if (buildingId != null) {
            Integer capacity = washbasinMapper.sumCapacityByBuildingId(buildingId);
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + buildingId, capacity != null ? capacity : 0);
        }
    }
}