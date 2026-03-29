ALTER TABLE weekly_work
    ADD COLUMN IF NOT EXISTS current_handler_user_id bigint;

ALTER TABLE weekly_work
    ADD COLUMN IF NOT EXISTS current_handler_user_name varchar(100);

ALTER TABLE weekly_work
    ADD COLUMN IF NOT EXISTS current_flow_order integer;

ALTER TABLE weekly_work
    ADD COLUMN IF NOT EXISTS final_approver_user_id bigint;

ALTER TABLE weekly_work
    ADD COLUMN IF NOT EXISTS approved_time timestamp;

ALTER TABLE weekly_work
    ADD COLUMN IF NOT EXISTS last_review_by_name varchar(100);
