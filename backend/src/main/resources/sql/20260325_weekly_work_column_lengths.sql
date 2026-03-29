ALTER TABLE weekly_work
    ALTER COLUMN status TYPE varchar(50);

ALTER TABLE weekly_work
    ALTER COLUMN current_approval_node TYPE varchar(100);

ALTER TABLE weekly_work
    ALTER COLUMN last_return_target TYPE varchar(100);
