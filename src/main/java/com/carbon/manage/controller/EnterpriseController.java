package com.carbon.manage.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carbon.manage.common.Result;
import com.carbon.manage.entity.Enterprise;
import com.carbon.manage.service.EnterpriseService;

@RestController
@RequestMapping("/api/enterprises")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @GetMapping
    public Result<List<Enterprise>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(enterpriseService.list(keyword));
    }

    @GetMapping("/list")
    public Result<List<Enterprise>> listEnabled() {
        return Result.ok(enterpriseService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<Enterprise> detail(@PathVariable Long id) {
        return Result.ok(enterpriseService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody Enterprise enterprise) {
        return Result.ok(enterpriseService.create(enterprise));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Enterprise enterprise) {
        enterpriseService.update(id, enterprise);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        enterpriseService.delete(id);
        return Result.ok();
    }
}
