package com.example.lecturesystem.modules.skill.mapper;

import com.example.lecturesystem.modules.skill.dto.SkillQueryRequest;
import com.example.lecturesystem.modules.skill.entity.SkillEntity;
import com.example.lecturesystem.modules.skill.vo.SkillListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SkillMapper {
    List<SkillListItemVO> queryList(@Param("request") SkillQueryRequest request);
    List<SkillListItemVO> queryPublishedList(@Param("request") SkillQueryRequest request);
    SkillEntity findById(@Param("id") Long id);
    SkillEntity findByCode(@Param("skillCode") String skillCode);
    int insert(SkillEntity entity);
    int update(SkillEntity entity);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateUser") String updateUser, @Param("updateTime") LocalDateTime updateTime);
}