# AI结果回流

## 1. 功能目标

把 AI 工作台和手机端政策咨询产生的会话、消息、引用和统计结果稳定回流到后台可查询、可统计、可排查的链路中。

## 2. 功能范围

- 会话落库
- 消息落库
- 来源场景承接
- 台账承接
- 月报承接
- 日志中台承接

## 3. 菜单位置

- 当前为轻量承接页，不强制依赖独立重页面

## 4. 页面路径

- `/ai-result-flow`

## 5. 前端文件

- `frontend/src/views/agent/AIResultFlowView.vue`
- `frontend/src/views/agent/AIConsultationLedgerView.vue`
- `frontend/src/views/agent/AIConsultationMonthlyReportView.vue`

## 6. 后端接口

- `POST /api/agent/session/list`
- `POST /api/agent/session/stats`
- `POST /api/agent/session/monthly-report`
- `GET /api/agent/session/export`
- `GET /api/agent/session/export/excel`

## 7. 后端服务/类

- `backend/src/main/java/com/example/lecturesystem/modules/agent/service/impl/AgentServiceImpl.java`
- `backend/src/main/resources/mapper/agent/AgentSessionMapper.xml`

## 8. 数据表与关键字段

- `ai_agent_session.source_scene`
- `ai_agent_session.status`
- `ai_agent_message.message_role`
- `ai_agent_message.cited_chunk_ids`

## 9. 权限规则

- 数据查询仍受 AI 模块权限与会话可见范围控制

## 10. 当前状态

- 已形成服务闭环
- 当前以服务层 + 台账 + 月报 + 日志承接为主
- 独立重页面仍待后续增强

## 11. 已实现内容

- 来源场景已区分 `AI_WORKBENCH` 与 `MOBILE_POLICY_CONSULTANT`
- 会话与消息落库后可被台账和月报消费
- 关键承接节点已写入日志中台

## 12. 未实现内容

- 更重的独立回流分析页面待补充
- 业务结果回写到更多外部模块待补充

## 13. 页面操作步骤
### 第一步：
在 AI工作台或手机端政策咨询发起一次真实问答。
### 第二步：
到 `/ai-result-flow` 查看当前来源场景的统计概览。
### 第三步：
到咨询台账确认会话明细已出现。
### 第四步：
到月度报表和日志中台确认统计与日志承接已完成。

## 14. 常见问题

- 能问但没有台账，多数是会话未落库或来源场景未写入。
- 有台账但没有月报，多数是筛选口径或时间范围问题。
- 三者都没有，优先检查 Agent 主链是否中途异常。

## 15. 风险点

- 回流链路长，任一节点缺字段都会影响下游多处查询。
- `source_scene` 是这轮新加的关键字段，最容易成为断点。

## 16. 防回归点

- 验证新会话能进入 `ai_agent_session`
- 验证新消息能进入 `ai_agent_message`
- 验证台账和月报都能按来源场景查到数据
- 验证日志中台能看到 `RESULT_FLOW` 日志

## 17. 最近一次关键修改

- `2026-04-17`：新增来源场景落库与 AI 结果回流导航页，补齐主链承接日志。

## 18. 相关资料与来源文件

- `frontend/src/views/agent/AIResultFlowView.vue`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/service/impl/AgentServiceImpl.java`
- `backend/src/main/resources/mapper/agent/AgentSessionMapper.xml`

## 19. 关联功能

- AI结果回流 -> 咨询台账 -> 月度报表 -> 日志中台
- AI结果回流 <- AI工作台 <- 手机端政策咨询
