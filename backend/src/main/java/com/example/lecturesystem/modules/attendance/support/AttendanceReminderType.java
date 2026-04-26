package com.example.lecturesystem.modules.attendance.support;

import java.util.Locale;

public enum AttendanceReminderType {
    CHECK_IN_MISS(
            "CHECK_IN_MISS",
            "checkin",
            "考勤提醒",
            "您今日上班打卡尚未完成，请尽快进入系统处理。"
    ),
    CHECK_OUT_MISS(
            "CHECK_OUT_MISS",
            "checkout",
            "考勤提醒",
            "您今日下班打卡尚未完成，请及时进入系统确认。"
    );

    private final String code;
    private final String entryType;
    private final String title;
    private final String content;

    AttendanceReminderType(String code, String entryType, String title, String content) {
        this.code = code;
        this.entryType = entryType;
        this.title = title;
        this.content = content;
    }

    public String getCode() {
        return code;
    }

    public String getEntryType() {
        return entryType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean matchesAction(String action) {
        if (action == null || action.isBlank()) {
            return false;
        }
        String normalized = action.trim().toUpperCase(Locale.ROOT);
        if (this == CHECK_IN_MISS) {
            return "AM_ON".equals(normalized);
        }
        return "PM_OFF".equals(normalized);
    }

    public static AttendanceReminderType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("提醒类型不能为空");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (AttendanceReminderType value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        throw new IllegalArgumentException("不支持的提醒类型: " + code);
    }
}
