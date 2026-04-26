ALTER TABLE attendance_patch_apply
    ADD COLUMN IF NOT EXISTS attachments_json TEXT;

UPDATE attendance_patch_apply
SET attachments_json = NULL
WHERE attachments_json = '';
