package com.example.lecturesystem.modules.orgtree.mapper;

import com.example.lecturesystem.modules.orgtree.entity.OrgNodeEntity;
import com.example.lecturesystem.modules.orgtree.vo.OrgTreeNodeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrgTreeMapper {
    OrgNodeEntity findByUserId(@Param("userId") Long userId);

    List<OrgTreeNodeVO> querySubtreeNodes(@Param("unitId") Long unitId, @Param("treePathPrefix") String treePathPrefix);

    List<OrgTreeNodeVO> queryChildrenNodes(@Param("unitId") Long unitId, @Param("parentUserId") Long parentUserId);

    List<OrgTreeNodeVO> queryAncestorNodes(@Param("unitId") Long unitId, @Param("treePath") String treePath);

    int updateUserNodeInfo(@Param("userId") Long userId,
                           @Param("parentUserId") Long parentUserId,
                           @Param("unitId") Long unitId,
                           @Param("levelNo") Integer levelNo,
                           @Param("treePath") String treePath);

    int updateSubtreeAfterMove(@Param("userId") Long userId,
                               @Param("oldTreePath") String oldTreePath,
                               @Param("newTreePath") String newTreePath,
                               @Param("levelOffset") int levelOffset,
                               @Param("newParentUserId") Long newParentUserId);

    int updateSubtreeUnitId(@Param("treePathPrefix") String treePathPrefix,
                            @Param("unitId") Long unitId);
}
