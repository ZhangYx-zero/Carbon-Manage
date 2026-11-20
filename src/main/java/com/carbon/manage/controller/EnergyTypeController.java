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
import com.carbon.manage.entity.EnergyTypeDict;
import com.carbon.manage.service.EnergyTypeService;

@RestController
@RequestMapping("/api/energy-types")
@RequiredArgsConstructor
public class EnergyTypeController {

    private final EnergyTypeService energyTypeService;

    @GetMapping
    public Result<List<EnergyTypeDict>> list() {
        return Result.ok(energyTypeService.list());
    }

    @GetMapping("/list")
    public Result<List<EnergyTypeDict>> listEnabled() {
        return Result.ok(energyTypeService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<EnergyTypeDict> detail(@PathVariable Long id) {
        return Result.ok(energyTypeService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody EnergyTypeDict type) {
        return Result.ok(energyTypeService.create(type));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody EnergyTypeDict type) {
        energyTypeService.update(id, type);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        energyTypeService.delete(id);
        return Result.ok();
    }
}
