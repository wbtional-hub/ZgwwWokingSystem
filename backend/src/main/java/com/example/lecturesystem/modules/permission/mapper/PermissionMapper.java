package com.example.lecturesystem.modules.permission.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {
    boolean existsUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    List<Long> queryUserIdsByTreePathPrefix(@Param("treePathPrefix") String treePathPrefix);

    int insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
