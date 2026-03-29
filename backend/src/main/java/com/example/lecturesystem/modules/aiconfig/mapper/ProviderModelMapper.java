package com.example.lecturesystem.modules.aiconfig.mapper;

import com.example.lecturesystem.modules.aiconfig.entity.ProviderModelEntity;
import com.example.lecturesystem.modules.aiconfig.vo.ProviderModelVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProviderModelMapper {
    List<ProviderModelVO> queryByProviderId(@Param("providerConfigId") Long providerConfigId);
    int deleteByProviderId(@Param("providerConfigId") Long providerConfigId);
    int batchInsert(@Param("list") List<ProviderModelEntity> list);
}