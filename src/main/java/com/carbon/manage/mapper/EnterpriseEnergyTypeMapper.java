package com.carbon.manage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.EnterpriseEnergyType;

@Mapper
public interface EnterpriseEnergyTypeMapper {

    List<String> selectEnergyTypesByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    List<EnterpriseEnergyType> selectByEnterpriseIds(@Param("enterpriseIds") List<Long> enterpriseIds);

    int insert(@Param("enterpriseId") Long enterpriseId, @Param("energyType") String energyType);

    int insertBatch(@Param("enterpriseId") Long enterpriseId,
                    @Param("energyTypes") List<String> energyTypes);

    int deleteByEnterpriseId(@Param("enterpriseId") Long enterpriseId);
}
