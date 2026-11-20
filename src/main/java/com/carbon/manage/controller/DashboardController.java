package com.carbon.manage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carbon.manage.common.Result;
import com.carbon.manage.dto.DashboardVO;
import com.carbon.manage.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardVO> stats() {
        return Result.ok(dashboardService.stats());
    }
}
