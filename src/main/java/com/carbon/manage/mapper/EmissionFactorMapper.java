package com.carbon.manage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.EmissionFactor;

@Mapper
public interface EmissionFactorMapper {

    List<EmissionFactor> selectList();

    List<EmissionFactor> selectEnabled();

    EmissionFactor selectById(@Param("id") Long id);

    EmissionFactor selectByEnergyType(@Param("energyType") String energyType);

    int insert(EmissionFactor factor);

    int update(EmissionFactor factor);

    int deleteById(@Param("id") Long id);

    int countEnergyData(@Param("factorId") Long factorId);
}
