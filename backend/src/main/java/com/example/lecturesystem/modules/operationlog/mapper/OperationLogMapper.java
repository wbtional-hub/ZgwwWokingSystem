package com.example.lecturesystem.modules.operationlog.mapper;

import com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest;
import com.example.lecturesystem.modules.operationlog.entity.OperationLogEntity;
import com.example.lecturesystem.modules.operationlog.vo.OperationLogListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {
    int insert(OperationLogEntity entity);

    List<OperationLogListItemVO> queryList(@Param("request") OperationLogQueryRequest request,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
}
