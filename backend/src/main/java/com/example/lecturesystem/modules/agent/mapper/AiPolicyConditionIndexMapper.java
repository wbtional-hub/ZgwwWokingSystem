package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyConditionIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyConditionIndexMapper {

    List<AiPolicyConditionIndex> selectByConditionCodes(@Param("baseId") Long baseId,
                                                        @Param("conditionCodes") List<String> conditionCodes);
}