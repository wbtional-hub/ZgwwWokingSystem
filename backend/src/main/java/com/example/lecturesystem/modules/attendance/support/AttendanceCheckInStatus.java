package com.example.lecturesystem.modules.attendance.support;

public final class AttendanceCheckInStatus {
    public static final String LOCATION_READY = "LOCATION_READY";
    public static final String LOCATION_NOT_BOUND = "LOCATION_NOT_BOUND";
    public static final String LOCATION_NOT_CONFIGURED = "LOCATION_NOT_CONFIGURED";
    public static final String LOCATION_DISABLED = "LOCATION_DISABLED";
    public static final String LOCATION_REQUIRED = "LOCATION_REQUIRED";
    public static final String OUT_OF_RANGE = "OUT_OF_RANGE";
    public static final String ALREADY_FINISHED = "ALREADY_FINISHED";
    public static final String CHECK_IN_SUCCESS = "CHECK_IN_SUCCESS";
    public static final String CHECK_OUT_SUCCESS = "CHECK_OUT_SUCCESS";

    private AttendanceCheckInStatus() {
    }
}
