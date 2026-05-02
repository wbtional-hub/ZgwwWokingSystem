政策智能体 Phase1 - 缺失 intent 小修补

执行顺序：
1. 确认上一轮 03_policy_phase1_verify.sql 已通过。
2. 执行 02b_policy_phase1_missing_intent.sql。
3. 执行 03b_policy_phase1_missing_intent_verify.sql。
4. 如有异常，只执行 99b_policy_phase1_missing_intent_rollback.sql。

本脚本只补：
- double_hundred / material
- double_hundred / department
- xiamen_finance / material
- xiamen_finance / benefit
- postdoc / station
- postdoc / city_province_boundary

不修改已有 fallback 短语，不动 Java/Vue/菜单/权限/日志/考勤/周报等模块。
