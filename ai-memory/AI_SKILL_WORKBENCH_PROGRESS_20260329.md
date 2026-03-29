# Skills中心与AI工作台开发进度记忆 - 2026-03-29 18:22:28

## 本轮新增能力
本轮完成第三层首版落地：
- Skills 技能中心
- 技能版本与知识库绑定
- 技能验证
- AI 工作台
- AI 与知识库权限在技能/工作台中的接入

## 已完成的后端模块
### skill
目录：`backend/src/main/java/com/example/lecturesystem/modules/skill`

已实现：
- `SkillController`
- `SkillService` / `SkillServiceImpl`
- `SkillMapper`
- `SkillVersionMapper`
- `SkillKbBindingMapper`
- `SkillTestCaseMapper`
- `SkillValidationRunMapper`
- `SkillValidationResultMapper`
- `SkillEntity`
- `SkillVersionEntity`
- `SkillKbBindingEntity`
- `SkillTestCaseEntity`
- `SkillValidationRunEntity`
- `SkillValidationResultEntity`
- `SkillQueryRequest`
- `SaveSkillRequest`
- `SaveSkillVersionRequest`
- `PublishSkillVersionRequest`
- `SaveSkillBindingRequest`
- `SkillTestCaseQueryRequest`
- `SaveSkillTestCaseRequest`
- `RunSkillValidationRequest`
- `SkillListItemVO`
- `SkillVersionDetailVO`
- `SkillTestCaseListItemVO`
- `SkillValidationDetailVO`
- `SkillValidationResultVO`

功能：
- 技能列表查询
- 技能新增/编辑
- 技能版本新增/编辑
- 技能版本发布
- 技能绑定知识库
- 验证题录入与查询
- 技能验证执行
- 验证详情查询

### agent
目录：`backend/src/main/java/com/example/lecturesystem/modules/agent`

已实现：
- `AgentController`
- `AgentService` / `AgentServiceImpl`
- `AgentSessionMapper`
- `AgentMessageMapper`
- `AgentSessionEntity`
- `AgentMessageEntity`
- `CreateAgentSessionRequest`
- `AgentChatRequest`
- `AgentSessionVO`
- `AgentMessageVO`
- `AgentChatResultVO`
- `OpenAiCompatibleChatClient`
- `KnowledgeCitationContext`

功能：
- 创建工作台会话
- 按技能和知识库发起 AI 问答
- 保存用户消息与 AI 消息
- 返回引用的 chunk id 和命中文档标题
- 查询会话历史消息

## 本轮关键实现逻辑
### 技能验证逻辑
- 读取技能版本默认 AI 接入与模型
- 读取绑定知识库
- 从知识库检索相关 chunk
- 拼接系统提示词、任务提示词、知识上下文和问题
- 调用 OpenAI-compatible `/chat/completions`
- 依据答案完整性、引用情况、预期要点命中情况进行评分
- 写入 `ai_skill_validation_run` 与 `ai_skill_validation_result`

### AI工作台逻辑
- 选择已发布技能
- 自动取已发布版本
- 自动取绑定知识库
- 校验用户是否有 `canUseAi + canUseAgent + canAnalyzeKnowledgeBase`
- 检索知识片段
- 调用 AI 分析
- 返回答案与引用依据
- 保存会话与消息日志

## 已完成的前端页面
### Skills中心
文件：`frontend/src/views/skill/SkillListView.vue`

功能：
- 查询技能列表
- 保存技能基础信息
- 保存技能版本
- 绑定知识库
- 维护验证题
- 运行验证并查看验证结果

### AI工作台
文件：`frontend/src/views/agent/AIWorkbenchView.vue`

功能：
- 选择已发布技能
- 创建工作台会话
- 输入问题并调用 AI
- 查看最新回答与引用文档
- 查看会话历史消息

### 路由与菜单
已接入：
- `/skills`
- `/ai-workbench`

说明：
- 路由对登录用户开放
- 是否真正可训练技能、可使用工作台，由后端权限控制

## 本轮权限接入结果
- `canTrainSkill` 已接入技能新增/编辑/版本保存/验证题保存/执行验证
- `canPublishSkill` 已接入技能版本发布
- `canUseAi + canUseAgent` 已接入 AI 工作台
- `canAnalyzeKnowledgeBase` 已接入技能验证和 AI 工作台知识分析

## 关键文件入口
- `backend/src/main/java/com/example/lecturesystem/modules/skill/controller/SkillController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/skill/service/impl/SkillServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/controller/AgentController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/service/impl/AgentServiceImpl.java`
- `frontend/src/views/skill/SkillListView.vue`
- `frontend/src/views/agent/AIWorkbenchView.vue`
- `frontend/src/api/skill.js`
- `frontend/src/api/agent.js`

## 本轮验证结果
### 后端
命令：`mvn -q -DskipTests compile`
结果：通过

### 前端
命令：`npm.cmd run build`
结果：通过

## 当前边界
- 技能页面当前以“单个技能的已发布版本 + 当前编辑版本”为主，暂未做完整版本历史页
- 权限页目前已支持 AI 权限和知识库权限，技能权限表 `ai_user_skill_permission` 尚未接入页面配置
- AI 工作台目前默认走 OpenAI-compatible `/chat/completions`
- 会话标题目前只在内存逻辑里更新，未额外落库更新标题接口
- 目前未实现专家台账与技能归属人管理

## 下一阶段建议
1. 接入 `ai_user_skill_permission` 到权限页面与后端校验
2. 开发 `expert` 模块：技能归属人、专家等级、专家台账
3. 优化普通用户知识库页与工作台页的体验，把管理员能力与使用者能力分离
4. 增加技能版本历史页、会话列表页、引用片段详情页