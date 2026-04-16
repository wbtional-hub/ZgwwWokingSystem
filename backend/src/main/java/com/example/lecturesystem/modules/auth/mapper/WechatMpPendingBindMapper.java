package com.example.lecturesystem.modules.auth.mapper;

import com.example.lecturesystem.modules.auth.entity.WechatMpPendingBindEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WechatMpPendingBindMapper {

    WechatMpPendingBindEntity findById(@Param("id") Long id);

    List<WechatMpPendingBindEntity> queryPendingList();

    WechatMpPendingBindEntity findPendingByIdentity(@Param("openId") String openId,
                                                    @Param("unionId") String unionId);

    int insertPendingBind(WechatMpPendingBindEntity entity);

    int touchPendingBind(@Param("id") Long id,
                         @Param("requestIp") String requestIp,
                         @Param("userAgent") String userAgent,
                         @Param("remark") String remark);

    int updateBindSuccess(@Param("id") Long id,
                          @Param("bindUserId") Long bindUserId,
                          @Param("bindUsername") String bindUsername,
                          @Param("remark") String remark);
}