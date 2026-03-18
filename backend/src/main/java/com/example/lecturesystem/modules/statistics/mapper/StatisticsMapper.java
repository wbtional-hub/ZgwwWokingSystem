package com.example.lecturesystem.modules.statistics.mapper;

import com.example.lecturesystem.modules.statistics.vo.StatisticsOrgRankVO;
import com.example.lecturesystem.modules.statistics.vo.StatisticsOverviewVO;
import com.example.lecturesystem.modules.statistics.vo.StatisticsTrendVO;
import com.example.lecturesystem.modules.workscore.vo.WorkScoreListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatisticsMapper {
    long countAllUsers();

    long countUsersByUnitName(@Param("unitName") String unitName,
                              @Param("treePathPrefix") String treePathPrefix);

    StatisticsOverviewVO queryOverview(@Param("weekNo") String weekNo,
                                       @Param("unitName") String unitName,
                                       @Param("treePathPrefix") String treePathPrefix);

    List<StatisticsOrgRankVO> queryOrgRank(@Param("weekNo") String weekNo,
                                           @Param("unitName") String unitName,
                                           @Param("treePathPrefix") String treePathPrefix);

    List<WorkScoreListItemVO> queryStatusList(@Param("weekNo") String weekNo,
                                              @Param("unitName") String unitName,
                                              @Param("status") String status,
                                              @Param("treePathPrefix") String treePathPrefix);

    List<StatisticsTrendVO> queryTrend(@Param("unitName") String unitName,
                                       @Param("treePathPrefix") String treePathPrefix);
}
