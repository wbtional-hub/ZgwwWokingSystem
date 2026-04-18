# AI权限配置

## 1. 功能目标

- 按用户分配 AI 接入、知识库与技能三层权限。
- 为 AI 工作台、知识库、Skills、专家与运营台账提供前置授权控制。

## 2. 功能范围

- 选择授权用户。
- 配置用户 AI 接入权限。
- 配置用户知识库权限。
- 配置用户技能权限。
- 查看并回填已有授权记录。

## 3. 菜单位置

- 一级菜单：AI 权限配置
- 模块编码：`AI_PERMISSION`

## 4. 页面路径

- `/ai-permissions`

## 5. 前端文件

- `frontend/src/views/ai/AIPermissionConfigView.vue`
- `frontend/src/api/ai.js`
- `frontend/src/api/knowledge.js`
- `frontend/src/api/skill.js`
- `frontend/src/api/user.js`
- `frontend/src/router/index.js`
- `frontend/src/constants/modules.js`
- `frontend/src/config/pageHelp.js`

## 6. 后端接口

- `POST /api/ai/permission/user-ai/list`
- `POST /api/ai/permission/user-ai/save`
- `POST /api/ai/permission/user-knowledge/list`
- `GET /api/ai/permission/user-knowledge/grantable-users/{baseId}`
- `POST /api/ai/permission/user-knowledge/save`
- `POST /api/ai/permission/user-skill/list`
- `POST /api/ai/permission/user-skill/save`
- `POST /api/ai/permission/current`

## 7. 后端服务/类

- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/controller/AiPermissionController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/service/impl/AiPermissionServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/entity/UserAiPermissionEntity.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/entity/UserKnowledgePermissionEntity.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/entity/UserSkillPermissionEntity.java`

## 8. 数据表与关键字段

- `ai_user_ai_permission`
  - `user_id`
  - `provider_config_id`
  - `can_manage_provider`
  - `can_use_ai`
  - `can_train_skill`
  - `can_publish_skill`
  - `can_use_agent`
  - `can_run_analysis`
  - `status`
- `ai_user_knowledge_permission`
  - `user_id`
  - `base_id`
  - `can_view`
  - `can_upload`
  - `can_train_skill`
  - `can_analyze`
  - `status`
- `ai_user_skill_permission`
  - `user_id`
  - `skill_id`
  - `can_view`
  - `can_use`
  - `can_train`
  - `can_publish`
  - `status`

## 9. 权限规则

- 管理员页面。
- 建议按“先 AI 接入、再知识库、后技能”的顺序授予。
- 当前用户自己的 AI 摘要通过 `/api/ai/permission/current` 获取。
- 菜单可见不等于 AI 可用，仍需本页授权。

## 10. 当前状态

- 已落地，支持三层授权保存与结果回看。

## 11. 已实现内容

- 选择用户后自动加载该用户已授权数据。
- 支持保存 AI 接入权限。
- 支持保存知识库权限。
- 支持保存技能权限。
- 支持在结果区点选记录回填表单继续调整。
- 已在帮助说明中统一缺权排查路径。

## 12. 未实现内容

- 更细的授权冲突诊断待补充。
- 授权批量导入/导出待补充。
- 与菜单模块授权联合视图待补充。

## 13. 页面操作步骤
### 第一步：
进入 `/ai-permissions`，先选择目标用户。
### 第二步：
先在 AI 权限区选择接入配置并保存，确认基础 AI 能力已开通。
### 第三步：
再分别在知识库权限区和技能权限区配置目标资源并逐块保存。
### 第四步：
在下方授权结果区回看已保存记录，必要时点选回填后继续调整，并让用户到个人中心确认摘要。

## 14. 常见问题

- 用户看得到菜单但用不了 AI，通常是本页没有完成三层授权。
- 只给知识库或技能授权而没给 AI 接入权限，工作台仍可能不可用。
- 权限结果看起来“有记录”不等于“状态已启用”，要同时看 `status` 和各能力布尔位。

## 15. 风险点

- AI 接入、知识库、技能三层权限分开保存，容易出现只配一层的半开通状态。
- 授权用户切换后若旧表单未清空，容易误把权限保存到错误用户。
- 本页与个人中心、AI 工作台的缺权提示必须一致，否则用户会反复排查。

## 16. 防回归点

- 验证切换用户后授权列表和表单会跟随刷新。
- 验证 AI、知识库、技能三块保存分别成功。
- 验证 `current` 接口在个人中心与工作台侧能正确反映授权结果。
- 验证只授菜单、不授 AI 权限时用户仍无法真正使用 AI 能力。

## 17. 最近一次关键修改

- `2026-03-29`：完成 AI 接入与权限第一版落地，补齐用户 AI 权限、知识库权限相关能力。
- `2026-03-29`：帮助说明与引导顺序收口为“先选用户 -> 再配置三层权限 -> 最后核对结果”。
- `2026-03-30`：统一首页、个人中心、AI 权限配置页和 AI 工作台的缺权排查提示。

## 18. 相关资料与来源文件

- `frontend/src/views/ai/AIPermissionConfigView.vue`
- `frontend/src/api/ai.js`
- `frontend/src/api/knowledge.js`
- `frontend/src/api/skill.js`
- `frontend/src/api/user.js`
- `frontend/src/config/pageHelp.js`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/controller/AiPermissionController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/service/impl/AiPermissionServiceImpl.java`
- `backend/src/main/resources/mapper/aipermission/UserAiPermissionMapper.xml`
- `backend/src/main/resources/mapper/aipermission/UserKnowledgePermissionMapper.xml`
- `backend/src/main/resources/mapper/aipermission/UserSkillPermissionMapper.xml`
- `ai-memory/AI_ACCESS_PERMISSION_PROGRESS_20260329.md`
- `ai-memory/AI_SKILL_WORKBENCH_PROGRESS_20260329.md`
- `ai_center/progress.md`
- `ai_center/tasks.md`

## 19. 关联功能

- AI权限配置 ↔ AI接入区 ↔ 个人中心与强制改密 ↔ AI工作台
- AI权限配置 ↔ 知识库中心 ↔ Skills中心 ↔ 专家台账
- AI权限配置 ↔ 菜单模块授权 ↔ 参数管理 ↔ 日志中台
