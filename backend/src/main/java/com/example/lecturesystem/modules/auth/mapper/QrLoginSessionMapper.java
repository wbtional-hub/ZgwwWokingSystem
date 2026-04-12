package com.example.lecturesystem.modules.auth.mapper;

import com.example.lecturesystem.modules.auth.entity.QrLoginSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QrLoginSessionMapper {
    int insertSession(QrLoginSession session);

    QrLoginSession selectByQrToken(@Param("qrToken") String qrToken);

    int updateStatus(@Param("qrToken") String qrToken,
                     @Param("status") String status);

    int markScanned(@Param("qrToken") String qrToken,
                    @Param("scannedTime") java.time.LocalDateTime scannedTime);

    int confirmSession(@Param("qrToken") String qrToken,
                       @Param("userId") Long userId,
                       @Param("status") String status,
                       @Param("confirmedTime") java.time.LocalDateTime confirmedTime);

    int consumeConfirmedSession(@Param("qrToken") String qrToken,
                                @Param("consumedTime") java.time.LocalDateTime consumedTime);

    int deleteExpiredSessions();
}
