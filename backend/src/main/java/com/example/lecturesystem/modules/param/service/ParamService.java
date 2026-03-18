package com.example.lecturesystem.modules.param.service;

import com.example.lecturesystem.modules.param.dto.ParamQueryRequest;
import com.example.lecturesystem.modules.param.dto.SaveParamRequest;
import com.example.lecturesystem.modules.param.dto.ToggleParamStatusRequest;

public interface ParamService {
    Object listParams(ParamQueryRequest request);
    String getByCode(String code);
    Long saveParam(SaveParamRequest request);
    void deleteParam(Long id);
    void toggleStatus(ToggleParamStatusRequest request);
}
