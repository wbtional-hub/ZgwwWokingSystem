package com.example.lecturesystem.modules.expert.mapper;

import com.example.lecturesystem.modules.expert.dto.ExpertQueryRequest;
import com.example.lecturesystem.modules.expert.entity.SkillOwnerEntity;
import com.example.lecturesystem.modules.expert.vo.SkillOwnerListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillOwnerMapper {
    List<SkillOwnerListItemVO> queryList(@Param("request") ExpertQueryRequest request);
    SkillOwnerEntity findByUserIdAndSkillId(@Param("userId") Long userId, @Param("skillId") Long skillId);
    int insert(SkillOwnerEntity entity);
    int update(SkillOwnerEntity entity);
}