-- 考勤最小闭环数据库脚本
-- 目标：保证同一用户同一天同一打卡类型只存在一条记录

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_user_type_day
ON attendance_record (user_id, check_type, (DATE(check_time)));
