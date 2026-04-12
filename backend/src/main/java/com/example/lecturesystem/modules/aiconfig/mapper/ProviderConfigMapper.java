package com.example.lecturesystem.modules.aiconfig.mapper;

import com.example.lecturesystem.modules.aiconfig.dto.ProviderConfigQueryRequest;
import com.example.lecturesystem.modules.aiconfig.entity.ProviderConfigEntity;
import com.example.lecturesystem.modules.aiconfig.vo.ProviderConfigListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProviderConfigMapper {
    List<ProviderConfigListItemVO> queryList(@Param("request") ProviderConfigQueryRequest request);
    ProviderConfigEntity findById(@Param("id") Long id);
    ProviderConfigEntity findByCode(@Param("providerCode") String providerCode);
    ProviderConfigEntity findFirstEnabledSuccess();
    int insert(ProviderConfigEntity entity);
    int update(ProviderConfigEntity entity);
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateUser") String updateUser,
                     @Param("updateTime") LocalDateTime updateTime);
    int updateTestStatus(@Param("id") Long id,
                         @Param("connectStatus") String connectStatus,
                         @Param("defaultModel") String defaultModel,
                         @Param("updateUser") String updateUser,
                         @Param("updateTime") LocalDateTime updateTime);
}
