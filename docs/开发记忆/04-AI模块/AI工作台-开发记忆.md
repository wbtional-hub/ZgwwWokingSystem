# AI工作台

## 1. 功能目标

承接 Web 端 AI 主问答链路，支持创建会话、发送问题、查看引用，并把结果沉淀到台账、月报和日志中台。

## 2. 功能范围

- AI 工作台问答
- 会话管理与消息查看
- 知识引用展示
- 来源场景落库为 `AI_WORKBENCH`
- 下游承接到咨询台账、月度报表、日志中台

## 3. 菜单位置

- AI模块 -> AI工作台

## 4. 页面路径

- `/ai-workbench`

## 5. 前端文件

- `frontend/src/views/agent/AIWorkbenchView.vue`
- `frontend/src/api/agent.js`

## 6. 后端接口

- `POST /api/agent/session/create`
- `POST /api/agent/chat`
- `POST /api/agent/session/list`
- `GET /api/agent/session/{sessionId}/messages`

## 7. 后端服务/类

- `backend/src/main/java/com/example/lecturesystem/modules/agent/controller/AgentController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/service/impl/AgentServiceImpl.java`

## 8. 数据表与关键字段

- `ai_agent_session.source_scene`
- `ai_agent_session.skill_id`
- `ai_agent_session.base_id`
- `ai_agent_message.message_role`
- `ai_agent_message.cited_chunk_ids`

## 9. 权限规则

- 进入工作台依赖 AI 模块权限
- 发问依赖 `canUseAgent`
- 真实 AI 调用依赖 `canUseAi`
- Skill 与知识库继续受细粒度授权控制

## 10. 当前状态

- 已形成主链可用版本
- 与手机端政策咨询共用同一 Agent 主链和会话体系

## 11. 已实现内容

- 会话创建、问答、消息查看
- 知识命中与引用返回
- AI 不可用时知识库兜底
- 来源场景区分 `AI_WORKBENCH`
- 日志中台关键节点记录

## 12. 未实现内容

- 更细的工作台运营分析待补充
- 更丰富的会话质量度量待补充

## 13. 页面操作步骤
### 第一步：
进入 `/ai-workbench`，选择或创建会话。
### 第二步：
输入问题并发送。
### 第三步：
查看 AI 回复、知识引用和当前命中的 Skill。
### 第四步：
到咨询台账、月度报表和日志中台回查结果承接情况。

## 14. 常见问题

- 能进页面但不能发问，通常是 `canUseAgent` 未开通。
- 能发问但没有 AI 回复，通常是 Provider 不可用。
- 台账或月报没有数据，优先检查 `source_scene=AI_WORKBENCH` 是否写入。

## 15. 风险点

- 新增来源场景字段后，历史数据默认值兼容必须稳定。
- Skill 路由、知识命中和 Provider 选择现在集中在主链中，局部改动容易影响全局。

## 16. 防回归点

- 验证 AI工作台问答不回退。
- 验证 `source_scene=AI_WORKBENCH` 正常落库。
- 验证台账和月报仍能按来源场景查询。

## 17. 最近一次关键修改

- `2026-04-17`：补上来源场景落库、主链关键日志和与手机端共用的自动 Skill 路由能力。

## 18. 相关资料与来源文件

- `frontend/src/views/agent/AIWorkbenchView.vue`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/service/impl/AgentServiceImpl.java`
- `backend/src/main/resources/mapper/agent/AgentSessionMapper.xml`
- `docs/开发记忆/04-AI模块/手机端政策咨询-开发记忆.md`
- `docs/开发记忆/04-AI模块/咨询台账-开发记忆.md`

## 19. 关联功能

- AI工作台 -> 手机端政策咨询 -> AI结果回流 -> 咨询台账 -> 月度报表 -> 日志中台
- AI工作台 -> AI接入区 -> AI权限配置 -> 知识库中心 -> Skills中心

## 20. 2026-04-17 性能补记

- AI工作台与手机端政策咨询共用 `frontend/src/api/agent.js`，本轮已统一给 Agent 会话、聊天、统计、导出接口加单独超时，不影响普通接口
- Agent 主链保留原有 Skill / Knowledge / Provider 语义，只补充了 Provider 调用开始、调用耗时、timeoutLike 与知识库兜底成功日志
- `AI_WORKBENCH` 仍保持默认 `knowledgeTopN=5`，没有和手机端一起收缩，避免直接影响桌面端问答质量
