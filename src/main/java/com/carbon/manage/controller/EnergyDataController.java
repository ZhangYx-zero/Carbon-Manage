package com.carbon.manage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carbon.manage.common.PageResult;
import com.carbon.manage.common.Result;
import com.carbon.manage.entity.EnergyData;
import com.carbon.manage.service.EnergyDataService;
import com.carbon.manage.task.EnergyCollectTask;

@RestController
@RequestMapping("/api/energy")
@RequiredArgsConstructor
public class EnergyDataController {

    private final EnergyDataService energyDataService;
    private final EnergyCollectTask energyCollectTask;

    @GetMapping("/page")
    public Result<PageResult<EnergyData>> page(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) Long enterpriseId,
                                               @RequestParam(required = false) String energyType,
                                               @RequestParam(required = false) Boolean exceeded) {
        return Result.ok(energyDataService.page(pageNum, pageSize, enterpriseId, energyType, exceeded));
    }

    @GetMapping("/{id}")
    public Result<EnergyData> detail(@PathVariable Long id) {
        return Result.ok(energyDataService.getById(id));
    }

    /**
     * 手动触发一轮采集，返回新增数据条数（答辩演示用）。
     */
    @PostMapping("/collect")
    public Result<Integer> collect() {
        return Result.ok(energyCollectTask.collectOnce());
    }
}
