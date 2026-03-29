# CODEX开发总提示词（最终版）

你正在 `D:\20.develop64\ZgwwWokingSystem` 项目中继续开发“AI能力平台”。

请严格遵守以下约束和设计结论，不要自行偏离。

## 一、项目总目标

在现有系统中建设一套完整的 AI 能力平台，实现：

1. AI接入区
2. 知识库中心
3. Skills技能中心
4. 权限中心
5. AI工作台
6. 专家台账

核心业务目标为：

- 输入知识
- 训练技能
- 辅助工作

参考业务域：

- 人才政策
- 讲师团政策

## 二、必须先读取的设计文档

开始开发前，必须先读取并遵循以下文档：

1. `docs/20260329_ai_overall_design.md`
2. `docs/20260329_ai_phase1_spec.md`
3. `docs/20260329_ai_api_design.md`
4. `docs/20260329_ai_table_definition.md`
5. `docs/20260329_ai_screen_design.md`
6. `ai-memory/AI_CAPABILITY_BLUEPRINT.md`
7. `ai-memory/AI_TASKS_20260329.md`
8. `ai-memory/AI_DECISIONS_20260329.md`
9. `ai-memory/AI_DEVELOPMENT_PROMPTS_20260329.md`

## 三、必须遵守的项目约束

1. 不要重构目录结构。
2. 后端代码只能写在 `backend/src/main/java/com/example/lecturesystem/modules` 对应模块目录下。
3. 前端接口统一写到 `frontend/src/api` 下。
4. 前端页面统一写到 `frontend/src/views` 对应目录下。
5. 先补 `entity / dto / mapper / xml / serviceImpl / vo`，再补 `controller`。
6. 所有查询必须预留数据权限控制。
7. 所有新增、编辑、启停、导入、发布、授权、测试连接操作都要预留 `operationlog` 审计。
8. 保持现有 `Spring Boot + MyBatis XML + PostgreSQL + Vue3 + Vite + Vant` 技术风格一致。
9. 保留中文注释，便于后续二开。
10. 不要在代码中硬编码 AI Token。

## 四、数据库脚本

开发时必须对齐以下数据库脚本：

1. `database/20260329_ai_knowledge_schema.sql`
2. `database/20260329_ai_skill_schema.sql`
3. `database/20260329_ai_expert_agent_schema.sql`
4. `database/20260329_ai_access_permission_schema.sql`

## 五、模块规划

后端新增模块：

- `knowledge`
- `skill`
- `expert`
- `agent`
- `aiconfig`
- `aipermission`

前端新增页面目录：

- `frontend/src/views/knowledge`
- `frontend/src/views/skill`
- `frontend/src/views/expert`
- `frontend/src/views/agent`
- `frontend/src/views/aiconfig`
- `frontend/src/views/aipermission`

前端新增 API 文件：

- `frontend/src/api/knowledge.js`
- `frontend/src/api/skill.js`
- `frontend/src/api/expert.js`
- `frontend/src/api/agent.js`
- `frontend/src/api/aiconfig.js`
- `frontend/src/api/aipermission.js`

## 六、架构原则

### 1. 一个 AI 接入区，多业务域复用

不要为每个政策域单独实现一套 AI 系统。

统一采用：

- 一个 AI接入区
- 多个知识库
- 多套技能
- 一套权限体系
- 一个工作台

### 2. 第一阶段不做模型微调

第一阶段采用：

- 知识库
- 检索
- 技能提示词
- 验证
- 引用输出

不要把第一阶段做成底层模型训练项目。

### 3. 权限必须三层控制

必须区分：

- AI能力权限
- 知识库权限
- 技能权限

### 4. 政策类输出必须支持引用

后续 AI 工作台和技能输出都必须支持来源引用。

## 七、第一阶段开发范围

第一阶段只做：

1. AI接入区
2. 知识库中心
3. 权限中心
4. 人才政策知识库 MVP

本阶段不做：

1. 技能正式验证引擎
2. AI工作台正式聊天能力
3. 专家台账完整页面

## 八、第一阶段开发顺序

必须按以下顺序推进：

1. 数据库脚本对齐
2. `knowledge` 模块
3. `aiconfig` 模块
4. `aipermission` 模块
5. 前端知识库页面
6. 前端 AI接入区页面
7. 前端权限中心页面
8. 联调与自测
9. 文档更新

## 九、第一阶段具体目标

### knowledge 模块

实现：

- 知识库 CRUD
- 分类树
- Word 导入
- 正文和表格解析
- 文档切片
- 文档列表
- Chunk 列表
- 关键词检索

### aiconfig 模块

实现：

- AI接入配置
- 模型配置
- Token 密文保存
- 连通性测试

### aipermission 模块

实现：

- AI能力授权
- 知识库授权
- 技能授权

## 十、业务示例要求

请以“人才政策知识库”为首个落地样例：

分类建议：

- 人才认定
- 人才补贴
- 落户政策
- 创业扶持
- 企业引才奖励
- 申报材料

后续扩展预留“讲师团政策知识库”，不要写死为单一业务。

## 十一、实现要求

### 后端

- 按模块分层实现
- 权限不足必须拦截
- 查询要支持后续扩展
- 不要把业务逻辑写进 Controller

### 前端

- 延续现有页面风格
- 列表页、详情页、导入页、授权页必须完整可用
- 查询、刷新、空态、禁用态要齐全

### 安全

- Token 不明文返回页面
- 测试连接只能管理员操作
- 未授权用户看不到菜单且无法访问接口

## 十二、联调与验收

必须至少完成以下联调：

1. 新建 AI 接入配置
2. 测试连接成功
3. 新建知识库
4. 新建知识分类
5. 导入一份人才政策 Word
6. 生成知识块
7. 检索命中知识块
8. 给指定用户授权知识库权限
9. 验证未授权用户无法访问

## 十三、每次开发后的输出要求

每完成一个小阶段，必须同步更新：

1. `ai-memory/AI_CAPABILITY_BLUEPRINT.md`
2. `ai-memory/AI_TASKS_20260329.md`
3. `ai-memory/AI_DECISIONS_20260329.md`
4. 必要时补充 `docs` 下的设计文档

## 十四、输出风格要求

开发时请始终按照：

- 先扫描现状
- 再列出本次修改文件
- 先做最小闭环
- 完成后汇报已完成、未完成、风险点、下一步

## 十五、最终开发目标总结

请围绕以下主线持续开发：

- AI 可以接入
- 知识可以沉淀
- 技能可以训练
- 权限可以控制
- 用户可以辅助工作

最终形成一套适用于人才政策、讲师团政策等多个领域的 AI 能力平台。
