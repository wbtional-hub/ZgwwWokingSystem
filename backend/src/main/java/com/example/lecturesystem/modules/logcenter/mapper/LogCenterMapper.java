package com.example.lecturesystem.modules.logcenter.mapper;

import com.example.lecturesystem.modules.logcenter.dto.LogCenterQueryRequest;
import com.example.lecturesystem.modules.logcenter.entity.LogCenterEntity;
import com.example.lecturesystem.modules.logcenter.vo.LogCenterDetailVO;
import com.example.lecturesystem.modules.logcenter.vo.LogCenterListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogCenterMapper {
    int insert(LogCenterEntity entity);

    long countByQuery(@Param("request") LogCenterQueryRequest request,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    List<LogCenterListItemVO> queryPage(@Param("request") LogCenterQueryRequest request,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    LogCenterDetailVO findDetailById(@Param("id") Long id);
}
