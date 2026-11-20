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
import org.springframework.web.bind.annotation.RestController;

import com.carbon.manage.common.Result;
import com.carbon.manage.entity.EmissionFactor;
import com.carbon.manage.service.EmissionFactorService;

@RestController
@RequestMapping("/api/factors")
@RequiredArgsConstructor
public class EmissionFactorController {

    private final EmissionFactorService factorService;

    @GetMapping
    public Result<List<EmissionFactor>> list() {
        return Result.ok(factorService.list());
    }

    @GetMapping("/list")
    public Result<List<EmissionFactor>> listEnabled() {
        return Result.ok(factorService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<EmissionFactor> detail(@PathVariable Long id) {
        return Result.ok(factorService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody EmissionFactor factor) {
        return Result.ok(factorService.create(factor));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody EmissionFactor factor) {
        factorService.update(id, factor);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        factorService.delete(id);
        return Result.ok();
    }
}
