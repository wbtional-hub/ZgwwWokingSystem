package com.example.lecturesystem.modules.attendance.support;

public final class AttendanceApplyType {
    public static final String MAKEUP = "MAKEUP";
    public static final String EVIDENCE = "EVIDENCE";

    private AttendanceApplyType() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return MAKEUP;
        }
        String normalized = value.trim().toUpperCase();
        if (EVIDENCE.equals(normalized)) {
            return EVIDENCE;
        }
        return MAKEUP;
    }
}
