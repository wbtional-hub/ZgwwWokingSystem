package com.example.lecturesystem.modules.attendance.support;

import com.example.lecturesystem.modules.attendance.service.AttendanceReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AttendanceReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(AttendanceReminderScheduler.class);

    private final AttendanceReminderService attendanceReminderService;
    private final AttendanceReminderProperties attendanceReminderProperties;
    private final AtomicBoolean checkinDefaultCronLogged = new AtomicBoolean(false);
    private final AtomicBoolean checkoutDefaultCronLogged = new AtomicBoolean(false);

    public AttendanceReminderScheduler(AttendanceReminderService attendanceReminderService,
                                       AttendanceReminderProperties attendanceReminderProperties) {
        this.attendanceReminderService = attendanceReminderService;
        this.attendanceReminderProperties = attendanceReminderProperties;
    }

    @Scheduled(cron = "#{@attendanceReminderProperties.resolvedCheckinCron}")
    public void sendCheckInMissReminders() {
        logDefaultCronIfNecessary(
                AttendanceReminderType.CHECK_IN_MISS,
                attendanceReminderProperties.isDefaultCheckinCronApplied(),
                attendanceReminderProperties.getResolvedCheckinCron(),
                checkinDefaultCronLogged
        );
        try {
            attendanceReminderService.sendScheduledReminders(AttendanceReminderType.CHECK_IN_MISS);
        } catch (Exception ex) {
            log.error("attendance check-in reminder scheduler failed", ex);
        }
    }

    @Scheduled(cron = "#{@attendanceReminderProperties.resolvedCheckoutCron}")
    public void sendCheckOutMissReminders() {
        logDefaultCronIfNecessary(
                AttendanceReminderType.CHECK_OUT_MISS,
                attendanceReminderProperties.isDefaultCheckoutCronApplied(),
                attendanceReminderProperties.getResolvedCheckoutCron(),
                checkoutDefaultCronLogged
        );
        try {
            attendanceReminderService.sendScheduledReminders(AttendanceReminderType.CHECK_OUT_MISS);
        } catch (Exception ex) {
            log.error("attendance check-out reminder scheduler failed", ex);
        }
    }

    private void logDefaultCronIfNecessary(AttendanceReminderType reminderType,
                                           boolean defaultApplied,
                                           String cron,
                                           AtomicBoolean guard) {
        if (defaultApplied && guard.compareAndSet(false, true)) {
            log.warn("attendance reminder scheduler uses default cron: type={}, cron={}",
                    reminderType.getCode(), cron);
        }
    }
}
