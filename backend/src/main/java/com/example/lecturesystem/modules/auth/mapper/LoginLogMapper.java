package com.example.lecturesystem.modules.auth.mapper;

import com.example.lecturesystem.modules.auth.entity.LoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper {
    int insert(LoginLogEntity entity);
}
