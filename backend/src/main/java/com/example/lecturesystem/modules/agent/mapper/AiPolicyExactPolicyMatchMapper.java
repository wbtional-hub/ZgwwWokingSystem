package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.dto.AiPolicyExactPolicyChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyExactPolicyMatchMapper {

    List<AiPolicyExactPolicyChunk> selectExactPolicyChunks(@Param("baseId") Long baseId,
                                                           @Param("keyword") String keyword);
}