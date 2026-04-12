package com.example.lecturesystem.modules.auth.service;

import com.example.lecturesystem.modules.auth.vo.QrLoginSessionCreateVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginConfirmResultVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginMobileSessionVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginSessionStatusVO;

public interface QrLoginService {
    QrLoginSessionCreateVO createSession();

    QrLoginSessionStatusVO getSessionStatus(String qrToken);

    QrLoginMobileSessionVO getMobileSession(String qrToken);

    QrLoginConfirmResultVO confirmSession(String qrToken, Long userId, boolean approve);
}
