package com.example.lecturesystem.modules.attendance.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AttendanceReminderSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AttendanceReminderSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public AttendanceReminderSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> statements = List.of(
                "CREATE TABLE IF NOT EXISTS attendance_reminder_log ("
                        + "id BIGSERIAL PRIMARY KEY,"
                        + "user_id BIGINT NOT NULL,"
                        + "openid VARCHAR(128) NOT NULL,"
                        + "attendance_date DATE NOT NULL,"
                        + "reminder_type VARCHAR(32) NOT NULL,"
                        + "template_id VARCHAR(128),"
                        + "template_payload TEXT,"
                        + "target_url TEXT,"
                        + "send_status VARCHAR(16) NOT NULL,"
                        + "wx_err_code VARCHAR(64),"
                        + "wx_err_msg TEXT,"
                        + "created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_reminder_log_user_date_type ON attendance_reminder_log(user_id, attendance_date, reminder_type)",
                "CREATE INDEX IF NOT EXISTS idx_attendance_reminder_log_user_date_type ON attendance_reminder_log(user_id, attendance_date, reminder_type)",
                "CREATE INDEX IF NOT EXISTS idx_attendance_reminder_log_date_type_status ON attendance_reminder_log(attendance_date, reminder_type, send_status)"
        );

        for (String statement : statements) {
            try {
                jdbcTemplate.execute(statement);
            } catch (Exception ex) {
                log.error("attendance reminder schema init failed: statement={}", statement, ex);
                throw ex;
            }
        }
    }
}
