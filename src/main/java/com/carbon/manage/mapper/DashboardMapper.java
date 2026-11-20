package com.carbon.manage.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DashboardMapper {

    BigDecimal sumTodayEmission();

    long countTodayExceeded();

    long countActiveOrders();

    long countTodayDiagnosis();

    List<Map<String, Object>> selectEmissionTrend();

    List<Map<String, Object>> selectEnergyPie();

    List<Map<String, Object>> selectOrderStatus();
}
