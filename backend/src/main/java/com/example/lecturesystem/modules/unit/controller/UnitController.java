package com.example.lecturesystem.modules.unit.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.unit.dto.CreateUnitRequest;
import com.example.lecturesystem.modules.unit.dto.ToggleUnitStatusRequest;
import com.example.lecturesystem.modules.unit.dto.UpdateUnitRequest;
import com.example.lecturesystem.modules.unit.service.UnitService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/unit")
public class UnitController {
    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @PostMapping("/create")
    public ApiResponse<?> create(@Validated @RequestBody CreateUnitRequest request) {
        return ApiResponse.success(unitService.createUnit(request));
    }

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.success(unitService.listUnits());
    }

    @PostMapping("/update")
    public ApiResponse<?> update(@Validated @RequestBody UpdateUnitRequest request) {
        unitService.updateUnit(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/toggle-status")
    public ApiResponse<?> toggleStatus(@Validated @RequestBody ToggleUnitStatusRequest request) {
        unitService.toggleUnitStatus(request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        unitService.deleteUnit(id);
        return ApiResponse.success(null);
    }
}
