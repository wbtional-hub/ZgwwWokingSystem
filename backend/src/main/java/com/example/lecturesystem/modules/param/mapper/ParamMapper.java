package com.example.lecturesystem.modules.param.mapper;

import com.example.lecturesystem.modules.param.dto.ParamQueryRequest;
import com.example.lecturesystem.modules.param.entity.ParamEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ParamMapper {
    List<ParamEntity> queryList(@Param("request") ParamQueryRequest request);

    ParamEntity findById(@Param("id") Long id);

    ParamEntity findByCode(@Param("paramCode") String paramCode);

    int insert(ParamEntity entity);

    int update(ParamEntity entity);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateUser") String updateUser,
                     @Param("updateTime") LocalDateTime updateTime);

    int logicalDelete(@Param("id") Long id,
                      @Param("updateUser") String updateUser,
                      @Param("updateTime") LocalDateTime updateTime);
}
