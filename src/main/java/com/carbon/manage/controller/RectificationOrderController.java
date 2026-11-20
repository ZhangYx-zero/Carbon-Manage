package com.carbon.manage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carbon.manage.common.PageResult;
import com.carbon.manage.common.Result;
import com.carbon.manage.dto.OrderActionDTO;
import com.carbon.manage.entity.RectificationOrder;
import com.carbon.manage.service.RectificationOrderService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RectificationOrderController {

    private final RectificationOrderService orderService;

    @GetMapping("/page")
    public Result<PageResult<RectificationOrder>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Long enterpriseId) {
        return Result.ok(orderService.page(pageNum, pageSize, status, enterpriseId));
    }

    @GetMapping("/{id}")
    public Result<RectificationOrder> detail(@PathVariable Long id) {
        return Result.ok(orderService.getById(id));
    }

    @PostMapping("/{id}/dispatch")
    public Result<RectificationOrder> dispatch(@PathVariable Long id,
                                               @RequestBody OrderActionDTO dto) {
        return Result.ok(orderService.dispatch(id, dto));
    }

    @PostMapping("/{id}/submit")
    public Result<RectificationOrder> submit(@PathVariable Long id,
                                             @RequestBody OrderActionDTO dto) {
        return Result.ok(orderService.submit(id, dto));
    }

    @PostMapping("/{id}/audit/pass")
    public Result<RectificationOrder> auditPass(@PathVariable Long id,
                                                @RequestBody OrderActionDTO dto) {
        return Result.ok(orderService.auditPass(id, dto));
    }

    @PostMapping("/{id}/audit/reject")
    public Result<RectificationOrder> auditReject(@PathVariable Long id,
                                                  @RequestBody OrderActionDTO dto) {
        return Result.ok(orderService.auditReject(id, dto));
    }
}
