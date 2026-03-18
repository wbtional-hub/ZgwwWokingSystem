package com.example.lecturesystem.modules.unit.service;

import com.example.lecturesystem.modules.unit.dto.CreateUnitRequest;
import com.example.lecturesystem.modules.unit.dto.ToggleUnitStatusRequest;
import com.example.lecturesystem.modules.unit.dto.UpdateUnitRequest;

public interface UnitService {
    Long createUnit(CreateUnitRequest request);
    Object listUnits();
    void updateUnit(UpdateUnitRequest request);
    void deleteUnit(Long id);
    void toggleUnitStatus(ToggleUnitStatusRequest request);
}
