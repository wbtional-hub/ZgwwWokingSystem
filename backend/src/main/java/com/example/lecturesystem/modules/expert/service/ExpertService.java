package com.example.lecturesystem.modules.expert.service;

import com.example.lecturesystem.modules.expert.dto.ExpertQueryRequest;
import com.example.lecturesystem.modules.expert.dto.SaveSkillOwnerRequest;

public interface ExpertService {
    Object list(ExpertQueryRequest request);
    void saveSkillOwner(SaveSkillOwnerRequest request);
}