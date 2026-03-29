package com.example.lecturesystem.modules.usermodule.mapper;

import com.example.lecturesystem.modules.usermodule.entity.UserModulePermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserModulePermissionMapper {
    List<String> findModuleCodesByUserId(@Param("userId") Long userId);

    int deleteByUserId(@Param("userId") Long userId);

    int batchInsert(@Param("list") List<UserModulePermissionEntity> list);
}
