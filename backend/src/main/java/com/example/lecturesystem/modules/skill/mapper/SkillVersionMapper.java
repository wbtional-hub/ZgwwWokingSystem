package com.example.lecturesystem.modules.skill.mapper;

import com.example.lecturesystem.modules.skill.entity.SkillVersionEntity;
import com.example.lecturesystem.modules.skill.vo.SkillVersionDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkillVersionMapper {
    SkillVersionEntity findById(@Param("id") Long id);
    SkillVersionEntity findBySkillIdAndVersionNo(@Param("skillId") Long skillId, @Param("versionNo") String versionNo);
    SkillVersionDetailVO queryDetail(@Param("id") Long id);
    SkillVersionDetailVO queryLatestPublishedBySkillId(@Param("skillId") Long skillId);
    int insert(SkillVersionEntity entity);
    int update(SkillVersionEntity entity);
    int markPublished(@Param("skillId") Long skillId, @Param("skillVersionId") Long skillVersionId);
    int updateValidation(@Param("id") Long id, @Param("validationStatus") String validationStatus, @Param("score") java.math.BigDecimal score);
}