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
import com.carbon.manage.entity.AiDiagLog;
import com.carbon.manage.service.AiDiagnosisService;

@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class AiDiagnosisController {

    private final AiDiagnosisService aiDiagnosisService;

    @GetMapping("/page")
    public Result<PageResult<AiDiagLog>> page(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String status) {
        return Result.ok(aiDiagnosisService.page(pageNum, pageSize, status));
    }

    @GetMapping("/{id}")
    public Result<AiDiagLog> detail(@PathVariable Long id) {
        return Result.ok(aiDiagnosisService.getById(id));
    }

    @PostMapping("/{id}/retry")
    public Result<Void> retry(@PathVariable Long id) {
        aiDiagnosisService.retry(id);
        return Result.ok();
    }

    /**
     * 手动对指定能耗数据触发诊断（答辩演示用，不要求该数据超标）。
     */
    @PostMapping("/trigger/{energyDataId}")
    public Result<Long> trigger(@PathVariable Long energyDataId) {
        return Result.ok(aiDiagnosisService.claimAndTrigger(energyDataId));
    }
}
