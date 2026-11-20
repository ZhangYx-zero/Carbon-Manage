package com.carbon.manage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.EnergyTypeDict;

@Mapper
public interface EnergyTypeDictMapper {

    List<EnergyTypeDict> selectList();

    List<EnergyTypeDict> selectEnabled();

    EnergyTypeDict selectById(@Param("id") Long id);

    EnergyTypeDict selectByCode(@Param("typeCode") String typeCode);

    int countAll();

    int insert(EnergyTypeDict type);

    int update(EnergyTypeDict type);

    int deleteById(@Param("id") Long id);

    int countEnterpriseConfigs(@Param("typeCode") String typeCode);

    int countFactors(@Param("typeCode") String typeCode);

    int countEnergyData(@Param("typeCode") String typeCode);
}
