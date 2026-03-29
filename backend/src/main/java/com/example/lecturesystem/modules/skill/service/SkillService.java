package com.example.lecturesystem.modules.skill.service;

import com.example.lecturesystem.modules.skill.dto.*;

public interface SkillService {
    Object list(SkillQueryRequest request);
    Long saveSkill(SaveSkillRequest request);
    Long saveSkillVersion(SaveSkillVersionRequest request);
    void publishSkillVersion(PublishSkillVersionRequest request);
    void saveSkillBinding(SaveSkillBindingRequest request);
    Object listTestCases(SkillTestCaseQueryRequest request);
    Long saveTestCase(SaveSkillTestCaseRequest request);
    Object runValidation(RunSkillValidationRequest request);
    Object getValidationDetail(Long runId);
    Object getPublishedVersion(Long skillId);
}