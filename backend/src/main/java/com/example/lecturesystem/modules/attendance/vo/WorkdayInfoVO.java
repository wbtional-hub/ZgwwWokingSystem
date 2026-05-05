package com.example.lecturesystem.modules.attendance.vo;

public class WorkdayInfoVO {
    private Boolean workday;
    private Boolean holiday;
    private Boolean adjustedWorkday;
    private String dayType;
    private String name;
    private String notice;

    public static WorkdayInfoVO holiday(String dayType, String name) {
        WorkdayInfoVO vo = new WorkdayInfoVO();
        vo.setWorkday(false);
        vo.setHoliday(true);
        vo.setAdjustedWorkday(false);
        vo.setDayType(dayType);
        vo.setName(name);
        vo.setNotice("今日为" + resolveName(name, "法定节假日") + "，无需正常打卡；如需值班/加班/外勤，请提交补打卡留痕。");
        return vo;
    }

    public static WorkdayInfoVO weekdayRest(String name) {
        WorkdayInfoVO vo = new WorkdayInfoVO();
        vo.setWorkday(false);
        vo.setHoliday(false);
        vo.setAdjustedWorkday(false);
        vo.setDayType("WEEKDAY_REST");
        vo.setName(name);
        vo.setNotice("今日为调整休息日，无需正常打卡；如需值班/加班/外勤，请提交补打卡留痕。");
        return vo;
    }

    public static WorkdayInfoVO adjustedWorkday(String dayType, String name) {
        WorkdayInfoVO vo = new WorkdayInfoVO();
        vo.setWorkday(true);
        vo.setHoliday(false);
        vo.setAdjustedWorkday(true);
        vo.setDayType(dayType);
        vo.setName(name);
        vo.setNotice("今日为补班工作日，请按工作日打卡");
        return vo;
    }

    public static WorkdayInfoVO weekday() {
        WorkdayInfoVO vo = new WorkdayInfoVO();
        vo.setWorkday(true);
        vo.setHoliday(false);
        vo.setAdjustedWorkday(false);
        vo.setDayType("WEEKDAY");
        vo.setNotice("今日为工作日，请按规则打卡");
        return vo;
    }

    public static WorkdayInfoVO weekend() {
        WorkdayInfoVO vo = new WorkdayInfoVO();
        vo.setWorkday(false);
        vo.setHoliday(false);
        vo.setAdjustedWorkday(false);
        vo.setDayType("WEEKEND");
        vo.setNotice("今日为周末，无需正常打卡；如需值班/加班/外勤，请提交补打卡留痕。");
        return vo;
    }

    private static String resolveName(String name, String fallback) {
        return name == null || name.trim().isEmpty() ? fallback : name.trim();
    }

    public Boolean getWorkday() {
        return workday;
    }

    public void setWorkday(Boolean workday) {
        this.workday = workday;
    }

    public Boolean getHoliday() {
        return holiday;
    }

    public void setHoliday(Boolean holiday) {
        this.holiday = holiday;
    }

    public Boolean getAdjustedWorkday() {
        return adjustedWorkday;
    }

    public void setAdjustedWorkday(Boolean adjustedWorkday) {
        this.adjustedWorkday = adjustedWorkday;
    }

    public String getDayType() {
        return dayType;
    }

    public void setDayType(String dayType) {
        this.dayType = dayType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
