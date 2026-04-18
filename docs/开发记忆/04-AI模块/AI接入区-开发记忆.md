# AI接入区

## 1. 功能目标

- 统一维护外部 AI 模型接入配置。
- 提供接入保存、启停与连通性测试入口。

## 2. 功能范围

- 接入列表查询。
- 接入新增与编辑。
- 接入启停。
- 连通测试。
- 最近一次测试结果回看。

## 3. 菜单位置

- 一级菜单：AI 接入区
- 模块编码：`AI_PROVIDER`

## 4. 页面路径

- `/ai-provider`

## 5. 前端文件

- `frontend/src/views/ai/AIProviderConfigView.vue`
- `frontend/src/api/ai.js`
- `frontend/src/router/index.js`
- `frontend/src/constants/modules.js`
- `frontend/src/config/pageHelp.js`

## 6. 后端接口

- `POST /api/ai/provider/list`
- `POST /api/ai/provider/save`
- `POST /api/ai/provider/toggle-status`
- `POST /api/ai/provider/test`

## 7. 后端服务/类

- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig/controller/AiProviderConfigController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig/service/impl/AiProviderConfigServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig/support/AiProviderConnectivityTester.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig/support/AiTokenCipherSupport.java`

## 8. 数据表与关键字段

- `ai_provider_config`
  - `id`
  - `provider_code`
  - `provider_name`
  - `api_base_url`
  - `api_token_cipher`
  - `token_mask`
  - `default_model`
  - `connect_status`
  - `status`
  - `remark`
  - `update_time`
- `ai_provider_model`
  - `provider_config_id`
  - 模型明细字段：待补充

## 9. 权限规则

- 管理员页面。
- Token 属于敏感信息，页面只回显掩码。
- 只有启用且测试成功的接入才能作为可用 AI 接入候选。

## 10. 当前状态

- 已落地，具备“保存接入 -> 测试连通 -> 回看结果 -> 启停”的最小闭环。

## 11. 已实现内容

- 支持按关键词、状态、连通状态查询接入列表。
- 支持维护接入编码、名称、Base URL、Token、默认模型与备注。
- 支持启停切换。
- 支持调用 `/models` 链路做连通测试。
- 支持回看最近一次测试结果、识别模型数与模型列表。

## 12. 未实现内容

- 多供应商差异化健康检查策略待补充。
- Token 轮换与审计说明待补充。
- 更细的模型级别启停与优先级待补充。

## 13. 页面操作步骤
### 第一步：
进入 `/ai-provider`，先查询或新建目标接入。
### 第二步：
填写接入编码、接入名称、API Base URL、API Token、默认模型与备注并保存。
### 第三步：
在接入列表中对目标接入执行“测试连通”，确认返回状态、消息与识别模型数。
### 第四步：
回看最近一次测试结果后，再决定是否启用该接入，并通知权限配置与工作台链路继续联调。

## 14. 常见问题

- 新增接入时未填写 Token 会直接被前端拦截。
- “能保存但不能用”通常不是保存问题，而是连通测试失败或未启用。
- 接入正常后用户仍不能使用 AI，还要检查 AI 权限配置与菜单模块授权。

## 15. 风险点

- `api_base_url`、`api_token_cipher`、`default_model` 任一配置错误都会导致后续 AI 能力整体不可用。
- 误启用测试失败的接入，会把问题向下游扩散到工作台、Skills 与月度报表。
- 页面帮助配置曾出现语法错误并导致构建失败，说明该页的引导区也属于回归敏感点。

## 16. 防回归点

- 验证保存接入后列表能正确回显 `provider_code`、`provider_name`、`connect_status`。
- 验证连通测试能返回状态、说明、模型数量和模型列表。
- 验证停用后的接入不会继续被作为可用接入候选。
- 验证帮助说明与测试结果区锚点不引入前端构建问题。

## 17. 最近一次关键修改

- `2026-03-29`：完成 AI 接入区第一版代码落地，支持查询、保存与测试。
- `2026-03-29`：帮助说明顺序收口为“先配接入 -> 再测连通 -> 最后回看测试结果”。
- `2026-03-29`：修复 `pageHelp.js` 中 AI 接入区帮助文案缺尾导致的前端构建失败。

## 18. 相关资料与来源文件

- `frontend/src/views/ai/AIProviderConfigView.vue`
- `frontend/src/api/ai.js`
- `frontend/src/config/pageHelp.js`
- `frontend/src/router/index.js`
- `frontend/src/constants/modules.js`
- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig/controller/AiProviderConfigController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig/service/impl/AiProviderConfigServiceImpl.java`
- `backend/src/main/resources/mapper/aiconfig/ProviderConfigMapper.xml`
- `backend/src/main/resources/mapper/aiconfig/ProviderModelMapper.xml`
- `ai-memory/AI_ACCESS_PERMISSION_PROGRESS_20260329.md`
- `ai_center/progress.md`
- `ai_center/tasks.md`

## 19. 关联功能

- AI接入区 ↔ AI权限配置 ↔ 个人中心与强制改密 ↔ AI工作台
- AI接入区 ↔ Skills中心 ↔ 专家台账 ↔ 咨询台账 ↔ 月度报表
- AI接入区 ↔ 参数管理 ↔ 日志中台
