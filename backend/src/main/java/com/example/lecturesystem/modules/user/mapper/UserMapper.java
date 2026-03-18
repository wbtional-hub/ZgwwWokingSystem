package com.example.lecturesystem.modules.user.mapper;

import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.vo.UserDetailVO;
import com.example.lecturesystem.modules.user.vo.UserListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    UserEntity findById(@Param("id") Long id);

    UserEntity findByUsername(@Param("username") String username);

    long countPageByUserId(@Param("userId") Long userId, @Param("request") UserQueryRequest request);

    List<UserListItemVO> queryPageByUserId(@Param("userId") Long userId, @Param("request") UserQueryRequest request);

    long countPageByTreePath(@Param("treePathPrefix") String treePathPrefix, @Param("request") UserQueryRequest request);

    List<UserListItemVO> queryPageByTreePath(@Param("treePathPrefix") String treePathPrefix, @Param("request") UserQueryRequest request);

    int insertUser(UserEntity entity);

    long countPage(UserQueryRequest request);

    List<UserListItemVO> queryPage(UserQueryRequest request);

    UserDetailVO detailById(@Param("id") Long id);

    UserDetailVO detailByIdAndTreePath(@Param("id") Long id, @Param("treePathPrefix") String treePathPrefix);

    int updateUser(UserEntity entity);

    int logicalDelete(@Param("id") Long id,
                      @Param("updateUser") String updateUser,
                      @Param("updateTime") LocalDateTime updateTime);

    int updatePassword(@Param("id") Long id,
                       @Param("passwordHash") String passwordHash,
                       @Param("updateUser") String updateUser,
                       @Param("updateTime") LocalDateTime updateTime);
}
