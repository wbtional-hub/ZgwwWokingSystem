-- 周工作最小闭环数据库脚本
-- 目标：保证同一用户同一周次只能存在一条周工作记录

CREATE UNIQUE INDEX IF NOT EXISTS uk_weekly_work_user_week
ON weekly_work (user_id, week_no);
