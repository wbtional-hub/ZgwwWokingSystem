# AI能力平台第一期开发说明书

## 1. 文档目的

本文档用于定义 AI 能力平台第一期开发范围、页面清单、接口清单、表清单、权限矩阵和开发任务分解。

第一期目标聚焦于：

- AI接入区
- 知识库中心
- 权限中心
- 人才政策知识库 MVP

## 2. 第一期范围

### 2.1 本期纳入范围

1. AI接入配置
2. AI模型配置
3. AI能力权限配置
4. 知识库管理
5. 知识分类管理
6. Word 文档导入
7. 文档切片入库
8. 文档查询与知识检索
9. 人才政策知识库示例链路

### 2.2 本期不纳入范围

1. 技能验证执行引擎
2. 技能发布审批流
3. AI工作台正式问答
4. 专家台账正式页面
5. 向量检索
6. 模型微调

## 3. 页面一览

## 3.1 画面列表

### 画面 A01 AI接入配置列表

路径建议：

- `/ai-config/providers`

功能：

- 查看 AI 接入配置
- 新增配置
- 编辑配置
- 启停配置
- 测试连接

### 画面 A02 AI接入配置编辑页

路径建议：

- `/ai-config/providers/edit`

功能：

- 录入 Provider 编码
- 录入 Provider 名称
- 录入 API Base URL
- 录入 Token
- 录入默认模型

### 画面 A03 AI模型配置页

路径建议：

- `/ai-config/models`

功能：

- 查看某接入下的模型列表
- 配置模型名称、类型、能力
- 启停模型

### 画面 P01 AI能力授权页

路径建议：

- `/ai-permission/ai`

功能：

- 给用户分配 AI 能力权限
- 控制是否可训练技能、是否可分析

### 画面 P02 知识库授权页

路径建议：

- `/ai-permission/knowledge`

功能：

- 给用户分配知识库查看、上传、训练、分析权限

### 画面 P03 技能授权页

路径建议：

- `/ai-permission/skill`

功能：

- 给用户分配技能查看、使用、训练、发布权限

### 画面 K01 知识库管理页

路径建议：

- `/knowledge`

功能：

- 查看知识库列表
- 新增知识库
- 编辑知识库
- 启停知识库

### 画面 K02 知识库详情页

路径建议：

- `/knowledge/:id`

功能：

- 查看分类树
- 查看文档列表
- 查看文档状态
- 查看知识块

### 画面 K03 文档导入页

路径建议：

- `/knowledge/import`

功能：

- 上传 Word
- 指定知识库
- 指定分类
- 填写地区、级别、关键词、摘要
- 查看导入结果

### 画面 K04 知识检索页

路径建议：

- `/knowledge/search`

功能：

- 按关键词、分类、地区检索
- 查看文档命中
- 查看知识块命中

## 3.2 页面字段一览

### A01 AI接入配置列表

字段：

- 接入编码
- 接入名称
- API地址
- Token掩码
- 默认模型
- 连接状态
- 启停状态
- 更新时间

操作：

- 新增
- 编辑
- 启停
- 测试连接

### A02 AI接入配置编辑页

字段：

- 接入编码
- 接入名称
- API Base URL
- API Token
- 默认模型
- 备注

### A03 AI模型配置页

字段：

- 所属接入
- 模型编码
- 模型名称
- 模型类型
- 是否支持知识库
- 是否支持技能训练
- 是否支持工作台问答
- 是否支持分析
- 状态

### P01 AI能力授权页

字段：

- 用户
- AI接入
- 可管理接入
- 可使用AI
- 可训练技能
- 可发布技能
- 可使用工作台
- 可运行分析
- 状态

### P02 知识库授权页

字段：

- 用户
- 知识库
- 可查看
- 可上传
- 可训练技能
- 可分析
- 状态

### P03 技能授权页

字段：

- 用户
- 技能
- 可查看
- 可使用
- 可训练
- 可发布
- 状态

### K01 知识库管理页

字段：

- 知识库编码
- 知识库名称
- 领域
- 描述
- 文档数
- 知识块数
- 状态
- 更新时间

### K02 知识库详情页

分类树字段：

- 分类编码
- 分类名称
- 排序号
- 状态

文档列表字段：

- 文档标题
- 文档类型
- 地区
- 政策级别
- 生效时间
- 失效时间
- 解析状态
- 状态

知识块字段：

- Chunk序号
- 标题路径
- 内容
- 关键词
- 长度

### K03 文档导入页

字段：

- 知识库
- 分类
- 上传文件
- 地区
- 政策级别
- 生效时间
- 失效时间
- 摘要
- 关键词

结果字段：

- 导入任务ID
- 文件名
- 状态
- 总Chunk数
- 成功Chunk数
- 错误信息

### K04 知识检索页

字段：

- 知识库
- 分类
- 关键词
- 地区
- 是否仅有效期内
- TopN

结果字段：

- 文档标题
- 地区
- 标题路径
- 片段摘要
- Chunk序号

## 4. 接口一览

## 4.1 AI接入区接口

### `/api/ai-config/provider/list`

用途：

- 查询 AI 接入配置列表

### `/api/ai-config/provider/save`

用途：

- 新增/编辑 AI 接入配置

### `/api/ai-config/provider/toggle-status`

用途：

- 启停 AI 接入配置

### `/api/ai-config/provider/test-connection`

用途：

- 测试 AI 接入是否连通

### `/api/ai-config/model/list`

用途：

- 查询模型列表

### `/api/ai-config/model/save`

用途：

- 新增/编辑模型

### `/api/ai-config/model/toggle-status`

用途：

- 启停模型

## 4.2 权限中心接口

### `/api/ai-permission/ai/list`

用途：

- 查询 AI 能力授权列表

### `/api/ai-permission/ai/save`

用途：

- 新增/编辑 AI 能力授权

### `/api/ai-permission/knowledge/list`

用途：

- 查询知识库授权列表

### `/api/ai-permission/knowledge/save`

用途：

- 新增/编辑知识库授权

### `/api/ai-permission/skill/list`

用途：

- 查询技能授权列表

### `/api/ai-permission/skill/save`

用途：

- 新增/编辑技能授权

## 4.3 知识库中心接口

### `/api/knowledge/base/list`

用途：

- 查询知识库列表

### `/api/knowledge/base/save`

用途：

- 新增/编辑知识库

### `/api/knowledge/base/toggle-status`

用途：

- 启停知识库

### `/api/knowledge/category/tree`

用途：

- 查询分类树

### `/api/knowledge/category/save`

用途：

- 新增/编辑分类

### `/api/knowledge/document/list`

用途：

- 查询文档列表

### `/api/knowledge/document/import-docx`

用途：

- 上传 Word 并导入知识库

### `/api/knowledge/document/{id}`

用途：

- 查询文档详情

### `/api/knowledge/chunk/list`

用途：

- 查询指定文档的知识块

### `/api/knowledge/search`

用途：

- 知识检索

## 5. 表定义一览

## 5.1 AI接入区相关表

### `ai_provider_config`

用途：

- 保存 AI 接入基础配置

主要字段：

- `provider_code`
- `provider_name`
- `api_base_url`
- `api_token_cipher`
- `token_mask`
- `default_model`
- `connect_status`
- `status`

### `ai_provider_model`

用途：

- 保存某 AI 接入下可用模型

主要字段：

- `provider_config_id`
- `model_code`
- `model_name`
- `model_type`
- `support_knowledge`
- `support_skill_train`
- `support_agent_chat`
- `support_analysis`
- `status`

## 5.2 权限相关表

### `ai_user_ai_permission`

用途：

- 控制用户是否可以用 AI、训练技能、分析等

### `ai_user_knowledge_permission`

用途：

- 控制用户是否能使用指定知识库

### `ai_user_skill_permission`

用途：

- 控制用户是否能使用指定技能

## 5.3 知识库相关表

### `ai_knowledge_base`

用途：

- 知识库主表

### `ai_knowledge_category`

用途：

- 分类树

### `ai_knowledge_document`

用途：

- 文档主表

### `ai_knowledge_chunk`

用途：

- 文档切片表

### `ai_document_import_job`

用途：

- 导入任务记录表

## 6. 权限矩阵

## 6.1 角色说明

本期按系统管理员、知识管理员、普通授权用户、未授权用户四类考虑。

## 6.2 权限矩阵

| 功能 | 系统管理员 | 知识管理员 | 授权用户 | 未授权用户 |
|---|---|---|---|---|
| 配置AI接入 | 是 | 否 | 否 | 否 |
| 测试AI连接 | 是 | 否 | 否 | 否 |
| 管理AI模型 | 是 | 否 | 否 | 否 |
| AI能力授权 | 是 | 否 | 否 | 否 |
| 创建知识库 | 是 | 是 | 否 | 否 |
| 编辑知识库 | 是 | 是 | 否 | 否 |
| 导入Word文档 | 是 | 是 | 按授权 | 否 |
| 查看知识库 | 是 | 是 | 按授权 | 否 |
| 检索知识库 | 是 | 是 | 按授权 | 否 |
| 训练技能 | 是 | 按授权 | 按授权 | 否 |
| 使用技能 | 是 | 按授权 | 按授权 | 否 |
| 发布技能 | 是 | 按授权 | 否 | 否 |
| 使用AI分析 | 是 | 按授权 | 按授权 | 否 |

## 7. 后端模块划分

## 7.1 `aiconfig`

建议类：

- `AiProviderConfigController`
- `AiProviderModelController`
- `AiProviderConfigService`
- `AiProviderModelService`

## 7.2 `aipermission`

建议类：

- `AiCapabilityPermissionController`
- `AiKnowledgePermissionController`
- `AiSkillPermissionController`
- `AiPermissionService`

## 7.3 `knowledge`

建议类：

- `KnowledgeBaseController`
- `KnowledgeCategoryController`
- `KnowledgeDocumentController`
- `KnowledgeSearchController`

## 8. 开发任务分解

## 8.1 任务包 T01 数据库脚本对齐

内容：

- 核对并执行四份 AI 相关 SQL

输出：

- 表创建完成

涉及文件：

- `database/20260329_ai_knowledge_schema.sql`
- `database/20260329_ai_skill_schema.sql`
- `database/20260329_ai_expert_agent_schema.sql`
- `database/20260329_ai_access_permission_schema.sql`

## 8.2 任务包 T02 AI接入区后端开发

内容：

- 完成 provider config 和 model config 的 entity/dto/mapper/xml/service/controller

输出：

- 接入配置接口
- 模型配置接口
- 连接测试接口

## 8.3 任务包 T03 权限中心后端开发

内容：

- 完成 AI 权限、知识库权限、技能权限三个授权模块

输出：

- 授权查询接口
- 授权保存接口
- 权限校验服务

## 8.4 任务包 T04 知识库中心后端开发

内容：

- 完成知识库、分类、文档、Chunk、导入任务模块
- 完成 Word 导入解析

输出：

- CRUD 接口
- 导入接口
- 检索接口

## 8.5 任务包 T05 前端 AI接入区页面

内容：

- 接入配置页
- 模型配置页

## 8.6 任务包 T06 前端 权限中心页面

内容：

- AI能力授权页
- 知识库授权页
- 技能授权页

## 8.7 任务包 T07 前端 知识库页面

内容：

- 知识库管理页
- 知识库详情页
- 文档导入页
- 检索页

## 8.8 任务包 T08 联调与自测

内容：

- 跑通“人才政策知识库”导入链路
- 跑通权限限制链路
- 验证未授权用户无法使用

## 9. 审计要求

以下操作必须预留 operationlog：

- AI接入新增/修改/启停
- 模型新增/修改/启停
- 权限授权
- 知识库新增/修改/启停
- 分类新增/修改
- 文档导入

## 10. 自测清单

### 自测项 1

管理员可新增 AI 接入配置。

### 自测项 2

管理员可测试 AI 接入。

### 自测项 3

管理员可新增知识库和分类。

### 自测项 4

管理员可导入 Word 并生成知识块。

### 自测项 5

管理员可给指定用户授权知识库权限。

### 自测项 6

授权用户可查询知识库。

### 自测项 7

未授权用户无法访问相关页面和接口。

## 11. 第一期开发表达建议

开发顺序建议固定为：

1. 数据库
2. `knowledge`
3. `aiconfig`
4. `aipermission`
5. 前端页面
6. 联调
7. 自测
8. 文档补充

## 12. 结论

第一期重点不是把 AI 全量问答全部做完，而是先把 AI 平台底座搭起来：

- AI接入可配
- 知识可导
- 权限可控
- 人才政策知识库可用

第一期完成后，第二期即可顺利进入 Skills 中心和 AI工作台建设。
