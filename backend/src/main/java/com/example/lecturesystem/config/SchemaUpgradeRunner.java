package com.example.lecturesystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchemaUpgradeRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SchemaUpgradeRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaUpgradeRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> statements = List.of(
                "ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS wechat_no VARCHAR(64)",
                "ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS wechat_open_id VARCHAR(128)",
                "ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS wechat_union_id VARCHAR(128)",
                "ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN NOT NULL DEFAULT FALSE",
                "ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS login_fail_count INT NOT NULL DEFAULT 0",
                "ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS lock_until TIMESTAMP",
                "CREATE INDEX IF NOT EXISTS idx_sys_user_wechat_open_id ON sys_user(wechat_open_id)",
                "CREATE INDEX IF NOT EXISTS idx_sys_user_wechat_union_id ON sys_user(wechat_union_id)",
                "CREATE INDEX IF NOT EXISTS idx_sys_user_lock_until ON sys_user(lock_until)",
                "ALTER TABLE attendance_record ADD COLUMN IF NOT EXISTS location_source VARCHAR(32)",
                "ALTER TABLE attendance_record ADD COLUMN IF NOT EXISTS location_provider VARCHAR(32)",
                "CREATE TABLE IF NOT EXISTS sys_login_log ("
                        + "id BIGSERIAL PRIMARY KEY,"
                        + "user_id BIGINT,"
                        + "username VARCHAR(64),"
                        + "login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "login_ip VARCHAR(64),"
                        + "login_result VARCHAR(16) NOT NULL,"
                        + "fail_reason VARCHAR(255),"
                        + "user_agent VARCHAR(500)"
                        + ")",
                "CREATE INDEX IF NOT EXISTS idx_sys_login_log_user_time ON sys_login_log(user_id, login_time DESC)",
                "CREATE INDEX IF NOT EXISTS idx_sys_login_log_username_time ON sys_login_log(username, login_time DESC)",
                "CREATE TABLE IF NOT EXISTS ai_user_preference ("
                        + "user_id BIGINT PRIMARY KEY,"
                        + "habit_summary TEXT,"
                        + "preferred_answer_style VARCHAR(32),"
                        + "recent_topics VARCHAR(512),"
                        + "last_question VARCHAR(500),"
                        + "update_time TIMESTAMP"
                        + ")",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_ENABLED', '微信 JS-SDK 总开关', 'false', 1, '控制移动端是否启用微信 JS-SDK 配置下发', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_ENABLED' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_SIGNATURE_ENABLED', '微信 JS-SDK 签名开关', 'true', 1, '控制服务端是否生成微信 JS-SDK 签名', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_SIGNATURE_ENABLED' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_LOCATION_ENABLED', '微信定位开关', 'true', 1, '控制微信 JS-SDK 定位能力是否启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_LOCATION_ENABLED' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_ALLOWED_DOMAINS', '微信签名域名白名单', '', 1, '逗号或换行分隔，可填写 xmzgww.com 或完整域名', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_ALLOWED_DOMAINS' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_LOCATION_PRIORITY', '微信定位优先级', 'WECHAT_FIRST', 1, '支持 WECHAT_FIRST、BROWSER_FIRST、WECHAT_ONLY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_LOCATION_PRIORITY' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_LOCATION_TYPE', '微信定位坐标类型', 'gcj02', 1, '默认 gcj02，可选 wgs84', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_LOCATION_TYPE' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_DEBUG', '微信 JS-SDK Debug', 'false', 1, '控制 wx.config 是否开启 debug', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_DEBUG' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_NON_WECHAT_FALLBACK', '非微信浏览器定位兜底', 'BROWSER', 1, '支持 NONE、BROWSER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_NON_WECHAT_FALLBACK' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_ACCURACY_THRESHOLD', '微信定位精度阈值', '80', 1, '前端用于判断微信定位精度是否可接受，单位米', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_ACCURACY_THRESHOLD' AND is_deleted = FALSE)",
                "INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted) "
                        + "SELECT 'WECHAT_JSAPI_TIMEOUT_MS', '微信定位超时毫秒数', '8000', 1, '前端等待微信定位的超时时间，单位毫秒', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_TIMEOUT_MS' AND is_deleted = FALSE)"
        );

        for (String statement : statements) {
            try {
                jdbcTemplate.execute(statement);
            } catch (Exception ex) {
                log.error("Schema upgrade failed for statement: {}", statement, ex);
                throw ex;
            }
        }
    }
}
