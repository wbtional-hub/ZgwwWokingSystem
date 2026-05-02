政策智能体三类样板政策专题卡补齐 SQL（GUI兼容版）
====================================================

这版是为 Navicat / DBeaver / DataGrip / pgAdmin 等数据库工具准备的。
修复点：
1. 去掉 psql 专用的 \echo，避免“语法错误 在 \ 或附近”。
2. 去掉 01 核查脚本里的 DO $$ 动态块，避免 SQL 编辑器按分号拆开后出现 “Unterminated dollar quote”。
3. 01 和 04 里读取可选字段时使用 to_jsonb(row)->>'字段名'，尽量避免字段不存在导致核查失败。

执行顺序：
1. 先执行 01_policy_phase1_check.sql
2. 把执行结果发给 ChatGPT 判断
3. 确认 region_scope = XM，且 created_at / updated_at 字段存在后，再执行 02_policy_phase1_insert.sql
4. 执行 03_policy_phase1_verify.sql 验证
5. 04_policy_phase1_evidence_candidates.sql 是证据候选查询，只读，可后续再执行
6. 如需回滚，执行 99_policy_phase1_rollback.sql

重要：
- 当前 02 脚本默认使用 created_at / updated_at 字段。
- 如果你的表实际是 create_time / update_time，请不要执行 02，先把 01 结果发回来，我会再生成对应字段版本。
- 02 脚本只 insert，不 update / delete / truncate / drop / alter。
- 回滚脚本只删除带 POLICY_STANDARDIZATION_PHASE1 标记的数据。
