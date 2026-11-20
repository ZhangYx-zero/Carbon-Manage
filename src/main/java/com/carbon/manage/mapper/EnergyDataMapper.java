package com.carbon.manage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.EnergyData;

@Mapper
public interface EnergyDataMapper {

    int insert(EnergyData data);

    EnergyData selectById(@Param("id") Long id);

    EnergyData selectDetailById(@Param("id") Long id);

    List<EnergyData> selectPage(@Param("enterpriseId") Long enterpriseId,
                                @Param("energyType") String energyType,
                                @Param("exceeded") Boolean exceeded,
                                @Param("offset") int offset,
                                @Param("size") int size);

    long countPage(@Param("enterpriseId") Long enterpriseId,
                   @Param("energyType") String energyType,
                   @Param("exceeded") Boolean exceeded);
}
