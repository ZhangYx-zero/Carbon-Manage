package com.carbon.manage.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.carbon.manage.dto.DashboardVO;
import com.carbon.manage.mapper.DashboardMapper;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardVO stats() {
        DashboardVO vo = new DashboardVO();
        BigDecimal todayEmission = dashboardMapper.sumTodayEmission();
        vo.setTodayEmission(todayEmission == null ? BigDecimal.ZERO : todayEmission);
        vo.setTodayExceeded(dashboardMapper.countTodayExceeded());
        vo.setActiveOrders(dashboardMapper.countActiveOrders());
        vo.setTodayDiagnosis(dashboardMapper.countTodayDiagnosis());
        vo.setEmissionTrend(dashboardMapper.selectEmissionTrend());
        vo.setEnergyPie(dashboardMapper.selectEnergyPie());
        vo.setOrderStatus(dashboardMapper.selectOrderStatus());
        return vo;
    }
}
