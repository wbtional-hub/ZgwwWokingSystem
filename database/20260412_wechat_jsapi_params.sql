INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_ENABLED', '微信 JS-SDK 总开关', 'false', 1, '控制移动端是否启用微信 JS-SDK 配置下发', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_ENABLED' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_SIGNATURE_ENABLED', '微信 JS-SDK 签名开关', 'true', 1, '控制服务端是否生成微信 JS-SDK 签名', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_SIGNATURE_ENABLED' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_LOCATION_ENABLED', '微信定位开关', 'true', 1, '控制微信 JS-SDK 定位能力是否启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_LOCATION_ENABLED' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_ALLOWED_DOMAINS', '微信签名域名白名单', '', 1, '逗号或换行分隔，可填写 xmzgww.com 或完整域名', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_ALLOWED_DOMAINS' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_LOCATION_PRIORITY', '微信定位优先级', 'WECHAT_FIRST', 1, '支持 WECHAT_FIRST、BROWSER_FIRST、WECHAT_ONLY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_LOCATION_PRIORITY' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_LOCATION_TYPE', '微信定位坐标类型', 'gcj02', 1, '默认 gcj02，可选 wgs84', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_LOCATION_TYPE' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_DEBUG', '微信 JS-SDK Debug', 'false', 1, '控制 wx.config 是否开启 debug', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_DEBUG' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_NON_WECHAT_FALLBACK', '非微信浏览器定位兜底', 'BROWSER', 1, '支持 NONE、BROWSER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_NON_WECHAT_FALLBACK' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_ACCURACY_THRESHOLD', '微信定位精度阈值', '80', 1, '前端用于判断微信定位精度是否可接受，单位米', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_ACCURACY_THRESHOLD' AND is_deleted = FALSE);

INSERT INTO sys_param (param_code, param_name, param_value, status, remark, create_time, update_time, create_user, update_user, is_deleted)
SELECT 'WECHAT_JSAPI_TIMEOUT_MS', '微信定位超时毫秒数', '8000', 1, '前端等待微信定位的超时时间，单位毫秒', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', FALSE
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE param_code = 'WECHAT_JSAPI_TIMEOUT_MS' AND is_deleted = FALSE);
