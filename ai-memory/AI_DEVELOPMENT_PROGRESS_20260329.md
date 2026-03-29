# AI 开发进度记忆 - 2026-03-29 17:40:53

## 当前状态
第一阶段已开始实际开发，已优先落地 knowledge 模块的最小可用版本，目标是先打通“知识输入 -> 文档导入 -> 切片入库 -> 检索查看”的基础链路，为后续 Skills、AI 接入区、权限中心和 AI 工作台提供底座。

## 已完成的后端代码
目录：`backend/src/main/java/com/example/lecturesystem/modules/knowledge`

### 控制器
- `KnowledgeController`

### DTO
- `ImportKnowledgeDocxRequest`
- `KnowledgeBaseQueryRequest`
- `KnowledgeChunkQueryRequest`
- `KnowledgeDocumentQueryRequest`
- `KnowledgeSearchRequest`
- `SaveKnowledgeBaseRequest`
- `ToggleKnowledgeBaseStatusRequest`

### Entity
- `KnowledgeBaseEntity`
- `KnowledgeCategoryEntity`
- `KnowledgeDocumentEntity`
- `KnowledgeChunkEntity`
- `DocumentImportJobEntity`

### Mapper
- `KnowledgeBaseMapper`
- `KnowledgeDocumentMapper`
- `KnowledgeChunkMapper`
- `DocumentImportJobMapper`

### Service
- `KnowledgeService`
- `KnowledgeServiceImpl`

### Support
- `DocxPolicyParser`
- `KnowledgeChunkBuilder`
- `ParsedDocResult`
- `ParsedDocSection`

### VO
- `KnowledgeBaseListItemVO`
- `KnowledgeDocumentListItemVO`
- `KnowledgeDocumentDetailVO`
- `KnowledgeChunkListItemVO`
- `KnowledgeImportResultVO`
- `KnowledgeSearchResultVO`

## 已完成的后端能力
- 知识库列表查询
- 知识库新增/编辑
- 知识库启停
- Word(docx) 上传导入
- Apache POI 解析标题、段落、表格
- 按段落与字数切片入库
- 文档列表查询
- 文档详情查询
- Chunk 列表查询
- 关键词检索
- 导入任务记录
- 操作日志记录
- 管理员权限限制（当前先按超级管理员控制）

## 已完成的前端代码
目录：`frontend/src/views/knowledge`
- `KnowledgeBaseListView.vue`

目录：`frontend/src/api`
- `knowledge.js`

同时已接入：
- `frontend/src/router/index.js`
- `frontend/src/layouts/AppLayout.vue`

## 已完成的前端能力
- 知识库查询与维护
- 知识库列表展示
- 切换当前知识库
- Word 文档导入
- 文档列表展示
- 文档详情查看
- Chunk 查看
- 知识检索

## 本次关键实现约束
- 沿用现有 Spring Boot + MyBatis XML + PostgreSQL + Vue3 + Vant 风格
- 不重构原目录结构
- 后端新增代码集中在 `modules/knowledge`
- 前端新增代码集中在 `views/knowledge` 与 `api/knowledge.js`
- 数据库脚本按日期命名，放在 `database/`
- 所有新增设计方案已同步在 `docs/` 与 `ai-memory/`

## 本次验证结果
### 后端验证
命令：`mvn -q -DskipTests compile`
结果：通过

### 前端验证
命令：`npm.cmd run build`
结果：通过
说明：Vite 构建成功，但存在默认 chunk 体积告警，当前不阻塞功能开发，后续可在性能优化阶段处理。

## 当前已知边界
- 知识分类树接口和页面本轮尚未补齐，当前版本先以知识库为主维度运行
- 权限目前只做了管理员访问控制，后续需要切到 AI 专属授权模型
- AI 接入区、技能训练、技能验证、专家绑定、AI 工作台尚未进入代码开发阶段
- 文档导入目前只支持 `.docx`

## 下一阶段建议开发顺序
1. 开发 `aiconfig` 模块：AI 接入配置、模型配置、Token 管理、连通测试
2. 开发 `aipermission` 模块：AI 能力授权、知识库授权、技能授权
3. 开发 `skill` 模块：技能定义、技能版本、验证题、发布流转
4. 开发 `agent/workbench` 模块：选择技能、召回知识、AI 分析、引用展示
5. 回补 `knowledge category` 分类树与更细粒度筛选

## 对后续 AI/CODEX 的开发提醒
- 当前 `knowledge` 模块已经是真实代码基线，后续开发请在此基础上扩展，不要重做
- 如果新增技能训练，优先复用 `knowledge/search` 的召回接口
- 如果新增 AI 工作台，会话日志建议复用现有设计文档中的 `ai_agent_session` 和 `ai_agent_message`
- 后续权限开发要从“adminOnly”升级为“AI接入权限 + 知识库权限 + 技能权限”的三层控制