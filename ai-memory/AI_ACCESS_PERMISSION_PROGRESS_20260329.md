# AI 接入与权限开发进度记忆 - 2026-03-29

## 本轮新增能力
本轮已完成“AI接入区 + AI权限配置 + 知识库权限接入”的第一版代码落地。

## 已完成的后端模块
### aiconfig
目录：`backend/src/main/java/com/example/lecturesystem/modules/aiconfig`

已实现：
- `AiProviderConfigController`
- `AiProviderConfigService` / `AiProviderConfigServiceImpl`
- `ProviderConfigMapper` / `ProviderModelMapper`
- `ProviderConfigEntity` / `ProviderModelEntity`
- `ProviderConfigQueryRequest`
- `SaveProviderConfigRequest`
- `ToggleProviderConfigStatusRequest`
- `TestProviderConfigRequest`
- `ProviderConfigListItemVO`
- `ProviderModelVO`
- `ProviderTestResultVO`
- `AiTokenCipherSupport`
- `AiProviderConnectivityTester`

功能：
- AI接入列表查询
- AI接入新增/编辑
- 接入启停
- Token 加密存储
- 连通测试
- 模型列表同步到 `ai_provider_model`

### aipermission
目录：`backend/src/main/java/com/example/lecturesystem/modules/aipermission`

已实现：
- `AiPermissionController`
- `AiPermissionService` / `AiPermissionServiceImpl`
- `UserAiPermissionMapper`
- `UserKnowledgePermissionMapper`
- `UserAiPermissionEntity`
- `UserKnowledgePermissionEntity`
- `UserAiPermissionQueryRequest`
- `SaveUserAiPermissionRequest`
- `UserKnowledgePermissionQueryRequest`
- `SaveUserKnowledgePermissionRequest`
- `UserAiPermissionListItemVO`
- `UserKnowledgePermissionListItemVO`
- `CurrentAiPermissionVO`

功能：
- 用户 AI 权限查询
- 用户 AI 权限保存
- 用户知识库权限查询
- 用户知识库权限保存
- 当前登录用户权限摘要查询
- 权限判断方法：
  - `canManageProvider`
  - `canViewKnowledgeBase`
  - `canUploadKnowledgeBase`
  - `canAnalyzeKnowledgeBase`

## 已完成的知识库权限接入
已修改 `knowledge` 模块，使其不再只依赖管理员判断。

当前权限规则：
- 管理员：拥有全部知识库能力
- 普通用户：
  - `listBases` 只能看到已授权查看的知识库
  - `listDocuments` 需要有知识库查看权限
  - `getDocumentDetail` 需要有知识库查看权限
  - `listChunks` 需要有知识库查看权限
  - `importDocx` 需要有知识库上传权限
  - `search` 需要有知识库分析权限

## 已完成的前端页面
### AI 接入区
文件：`frontend/src/views/ai/AIProviderConfigView.vue`

功能：
- 查询 AI 接入
- 新增/编辑接入
- 配置 Token / Base URL / 默认模型
- 执行连通测试
- 查看同步回来的模型列表

### AI 权限配置页
文件：`frontend/src/views/ai/AIPermissionConfigView.vue`

功能：
- 选择用户
- 给用户分配 AI 接入权限
- 给用户分配知识库权限
- 查看该用户已有授权记录

### 菜单与路由
已接入：
- `/ai-provider`
- `/ai-permissions`
- `/knowledge`

其中：
- `AI接入区` 和 `AI权限配置` 仍为管理员页面
- `知识库中心` 已向登录用户开放，但是否能看到/使用具体知识库由后端授权控制

## 关键文件入口
- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig/controller/AiProviderConfigController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/controller/AiPermissionController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/impl/KnowledgeServiceImpl.java`
- `frontend/src/views/ai/AIProviderConfigView.vue`
- `frontend/src/views/ai/AIPermissionConfigView.vue`
- `frontend/src/api/ai.js`

## 本轮验证结果
### 后端
命令：`mvn -q -DskipTests compile`
结果：通过

### 前端
命令：`npm.cmd run build`
结果：通过

## 当前边界与后续建议
- 当前 AI 接入测试按 OpenAI 兼容 `/models` 接口实现，适合大多数 OpenAI-compatible 服务
- 外部真实网络连通性尚未在本地开发环境完成实测，运行时需要真实 Token 和可访问网络验证
- 知识库页面目前仍偏管理员工作台风格，后续建议拆出“普通用户工作台”页面
- 技能训练、技能发布、AI 工作台、专家台账还未开始实际编码
- 下一阶段优先建议：
  1. skill 模块
  2. AI 工作台
  3. 将 `can_use_agent / can_train_skill / can_publish_skill` 真正接入页面和接口控制