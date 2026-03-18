package com.example.lecturesystem.modules.workscore.mapper;

import com.example.lecturesystem.modules.workscore.dto.WorkScoreQueryRequest;
import com.example.lecturesystem.modules.workscore.entity.WorkScoreEntity;
import com.example.lecturesystem.modules.workscore.vo.WorkScoreCandidateVO;
import com.example.lecturesystem.modules.workscore.vo.WorkScoreListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkScoreMapper {
    int upsert(WorkScoreEntity entity);

    WorkScoreListItemVO findDetailById(@Param("id") Long id,
                                       @Param("unitId") Long unitId);

    List<WorkScoreListItemVO> queryList(@Param("request") WorkScoreQueryRequest request);

    List<WorkScoreCandidateVO> queryCandidates(@Param("weekNo") String weekNo,
                                               @Param("weekStart") LocalDate weekStart,
                                               @Param("weekEnd") LocalDate weekEnd,
                                               @Param("unitId") Long unitId);

    List<WorkScoreEntity> queryWeekRanking(@Param("weekNo") String weekNo);
}
