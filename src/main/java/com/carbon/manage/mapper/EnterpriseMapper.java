package com.carbon.manage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.Enterprise;

@Mapper
public interface EnterpriseMapper {

    List<Enterprise> selectList(@Param("keyword") String keyword);

    List<Enterprise> selectEnabled();

    Enterprise selectById(@Param("id") Long id);

    int insert(Enterprise enterprise);

    int update(Enterprise enterprise);

    int deleteById(@Param("id") Long id);

    int countEnergyData(@Param("enterpriseId") Long enterpriseId);
}
