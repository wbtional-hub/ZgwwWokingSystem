# AI能力平台详细表定义书

## 1. 文档目的

本文档说明 AI 能力平台新增数据表的用途、主要字段、索引建议和使用关系。

本次覆盖数据表：

- AI接入区
- 知识库中心
- Skills中心
- 权限中心
- AI工作台
- 专家体系

## 2. AI接入区表定义

## 2.1 `ai_provider_config`

### 用途

保存外部 AI 提供方接入配置。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `provider_code` | VARCHAR(64) | 接入编码，唯一 |
| `provider_name` | VARCHAR(128) | 接入名称 |
| `api_base_url` | VARCHAR(500) | API地址 |
| `api_token_cipher` | TEXT | 加密或密文化后的 Token |
| `token_mask` | VARCHAR(64) | 页面展示用掩码 |
| `default_model` | VARCHAR(128) | 默认模型 |
| `connect_status` | VARCHAR(32) | 连接状态 |
| `status` | SMALLINT | 启停状态 |
| `remark` | TEXT | 备注 |
| `create_time` | TIMESTAMP | 创建时间 |
| `update_time` | TIMESTAMP | 更新时间 |
| `create_user` | VARCHAR(64) | 创建人 |
| `update_user` | VARCHAR(64) | 更新人 |
| `is_deleted` | BOOLEAN | 逻辑删除 |

### 索引

- `provider_code` 唯一
- `status + connect_status + update_time`

## 2.2 `ai_provider_model`

### 用途

保存某个 AI 接入配置下的模型能力定义。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `provider_config_id` | BIGINT | 接入配置ID |
| `model_code` | VARCHAR(128) | 模型编码 |
| `model_name` | VARCHAR(128) | 模型名称 |
| `model_type` | VARCHAR(64) | 模型类型 |
| `support_knowledge` | BOOLEAN | 是否支持知识检索使用 |
| `support_skill_train` | BOOLEAN | 是否支持技能训练 |
| `support_agent_chat` | BOOLEAN | 是否支持工作台问答 |
| `support_analysis` | BOOLEAN | 是否支持分析 |
| `status` | SMALLINT | 状态 |
| `create_time` | TIMESTAMP | 创建时间 |

### 索引

- `(provider_config_id, model_code)` 唯一

## 3. 权限中心表定义

## 3.1 `ai_user_ai_permission`

### 用途

控制用户级 AI 能力。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `user_id` | BIGINT | 用户ID |
| `provider_config_id` | BIGINT | 接入配置ID |
| `can_manage_provider` | BOOLEAN | 可管理接入 |
| `can_use_ai` | BOOLEAN | 可使用AI |
| `can_train_skill` | BOOLEAN | 可训练技能 |
| `can_publish_skill` | BOOLEAN | 可发布技能 |
| `can_use_agent` | BOOLEAN | 可使用工作台 |
| `can_run_analysis` | BOOLEAN | 可运行分析 |
| `status` | SMALLINT | 状态 |
| `grant_time` | TIMESTAMP | 授权时间 |
| `grant_user` | VARCHAR(64) | 授权人 |

### 说明

该表回答的是“某个用户对某个 AI 接入是否拥有 AI 能力”。

## 3.2 `ai_user_knowledge_permission`

### 用途

控制用户对知识库的权限。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `user_id` | BIGINT | 用户ID |
| `base_id` | BIGINT | 知识库ID |
| `can_view` | BOOLEAN | 可查看 |
| `can_upload` | BOOLEAN | 可上传 |
| `can_train_skill` | BOOLEAN | 可训练技能 |
| `can_analyze` | BOOLEAN | 可分析 |
| `status` | SMALLINT | 状态 |
| `grant_time` | TIMESTAMP | 授权时间 |
| `grant_user` | VARCHAR(64) | 授权人 |

## 3.3 `ai_user_skill_permission`

### 用途

控制用户对技能的权限。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `user_id` | BIGINT | 用户ID |
| `skill_id` | BIGINT | 技能ID |
| `can_view` | BOOLEAN | 可查看 |
| `can_use` | BOOLEAN | 可使用 |
| `can_train` | BOOLEAN | 可训练 |
| `can_publish` | BOOLEAN | 可发布 |
| `status` | SMALLINT | 状态 |
| `grant_time` | TIMESTAMP | 授权时间 |
| `grant_user` | VARCHAR(64) | 授权人 |

## 4. 知识库中心表定义

## 4.1 `ai_knowledge_base`

### 用途

知识库主表。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `base_code` | VARCHAR(64) | 知识库编码 |
| `base_name` | VARCHAR(128) | 知识库名称 |
| `domain_type` | VARCHAR(64) | 领域类型 |
| `description` | TEXT | 描述 |
| `status` | SMALLINT | 状态 |
| `owner_user_id` | BIGINT | 负责人 |
| `create_time` | TIMESTAMP | 创建时间 |
| `update_time` | TIMESTAMP | 更新时间 |
| `create_user` | VARCHAR(64) | 创建人 |
| `update_user` | VARCHAR(64) | 更新人 |
| `is_deleted` | BOOLEAN | 逻辑删除 |

## 4.2 `ai_knowledge_category`

### 用途

知识库分类树。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `base_id` | BIGINT | 知识库ID |
| `parent_id` | BIGINT | 父分类ID |
| `category_code` | VARCHAR(64) | 分类编码 |
| `category_name` | VARCHAR(128) | 分类名称 |
| `sort_no` | INT | 排序号 |
| `status` | SMALLINT | 状态 |
| `create_time` | TIMESTAMP | 创建时间 |
| `update_time` | TIMESTAMP | 更新时间 |
| `is_deleted` | BOOLEAN | 逻辑删除 |

## 4.3 `ai_knowledge_document`

### 用途

知识文档主表。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `base_id` | BIGINT | 知识库ID |
| `category_id` | BIGINT | 分类ID |
| `doc_title` | VARCHAR(255) | 文档标题 |
| `source_type` | VARCHAR(32) | 来源类型 |
| `file_name` | VARCHAR(255) | 原始文件名 |
| `file_path` | VARCHAR(500) | 文件保存路径 |
| `doc_type` | VARCHAR(32) | 文档类型 |
| `policy_region` | VARCHAR(128) | 地区 |
| `policy_level` | VARCHAR(64) | 政策级别 |
| `effective_date` | DATE | 生效日期 |
| `expire_date` | DATE | 失效日期 |
| `keywords` | TEXT | 关键词 |
| `summary` | TEXT | 摘要 |
| `parse_status` | VARCHAR(32) | 解析状态 |
| `status` | SMALLINT | 状态 |
| `create_time` | TIMESTAMP | 创建时间 |
| `update_time` | TIMESTAMP | 更新时间 |
| `create_user` | VARCHAR(64) | 创建人 |
| `update_user` | VARCHAR(64) | 更新人 |
| `is_deleted` | BOOLEAN | 逻辑删除 |

## 4.4 `ai_knowledge_chunk`

### 用途

文档切片数据，用于检索和后续问答引用。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `document_id` | BIGINT | 文档ID |
| `base_id` | BIGINT | 知识库ID |
| `category_id` | BIGINT | 分类ID |
| `chunk_no` | INT | 切片序号 |
| `chunk_type` | VARCHAR(32) | 切片类型 |
| `heading_path` | VARCHAR(500) | 标题路径 |
| `content_text` | TEXT | 内容 |
| `keyword_text` | TEXT | 关键词文本 |
| `content_length` | INT | 内容长度 |
| `sort_no` | INT | 排序号 |
| `create_time` | TIMESTAMP | 创建时间 |

## 4.5 `ai_document_import_job`

### 用途

记录导入任务执行结果。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `base_id` | BIGINT | 知识库ID |
| `category_id` | BIGINT | 分类ID |
| `file_name` | VARCHAR(255) | 文件名 |
| `job_status` | VARCHAR(32) | 任务状态 |
| `total_chunks` | INT | 总切片数 |
| `success_chunks` | INT | 成功切片数 |
| `error_message` | TEXT | 错误信息 |
| `start_time` | TIMESTAMP | 开始时间 |
| `finish_time` | TIMESTAMP | 结束时间 |
| `create_time` | TIMESTAMP | 创建时间 |
| `create_user` | VARCHAR(64) | 创建人 |

## 5. Skills中心表定义

## 5.1 `ai_skill`

### 用途

技能主表。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `skill_code` | VARCHAR(64) | 技能编码 |
| `skill_name` | VARCHAR(128) | 技能名称 |
| `domain_type` | VARCHAR(64) | 领域类型 |
| `skill_type` | VARCHAR(64) | 技能类型 |
| `description` | TEXT | 描述 |
| `status` | SMALLINT | 状态 |
| `owner_user_id` | BIGINT | 负责人 |
| `create_time` | TIMESTAMP | 创建时间 |
| `update_time` | TIMESTAMP | 更新时间 |
| `create_user` | VARCHAR(64) | 创建人 |
| `update_user` | VARCHAR(64) | 更新人 |
| `is_deleted` | BOOLEAN | 逻辑删除 |

## 5.2 `ai_skill_version`

### 用途

技能版本表。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `skill_id` | BIGINT | 技能ID |
| `version_no` | VARCHAR(32) | 版本号 |
| `provider_config_id` | BIGINT | 默认 AI 接入配置 |
| `model_code` | VARCHAR(128) | 默认模型 |
| `system_prompt` | TEXT | 系统提示词 |
| `task_prompt` | TEXT | 任务提示词 |
| `output_template` | TEXT | 输出模板 |
| `forbidden_rules` | TEXT | 禁答规则 |
| `citation_rules` | TEXT | 引用规则 |
| `validation_status` | VARCHAR(32) | 验证状态 |
| `publish_status` | VARCHAR(32) | 发布状态 |
| `score` | NUMERIC(5,2) | 评分 |
| `create_time` | TIMESTAMP | 创建时间 |
| `create_user` | VARCHAR(64) | 创建人 |

## 5.3 `ai_skill_kb_binding`

### 用途

技能版本与知识库绑定关系。

## 5.4 `ai_skill_test_case`

### 用途

技能验证题集。

## 5.5 `ai_skill_validation_run`

### 用途

技能验证运行记录。

### 说明

记录某个技能版本使用某个接入和模型执行验证的结果。

## 5.6 `ai_skill_validation_result`

### 用途

技能验证明细结果。

## 6. 专家与工作台表定义

## 6.1 `ai_skill_owner`

### 用途

记录用户拥有某技能的关系，形成专家身份。

## 6.2 `ai_agent_session`

### 用途

AI工作台会话主表。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `user_id` | BIGINT | 用户ID |
| `skill_id` | BIGINT | 技能ID |
| `skill_version_id` | BIGINT | 技能版本ID |
| `provider_config_id` | BIGINT | 接入配置ID |
| `model_code` | VARCHAR(128) | 模型 |
| `base_id` | BIGINT | 知识库ID |
| `session_title` | VARCHAR(255) | 会话标题 |
| `status` | VARCHAR(32) | 状态 |
| `create_time` | TIMESTAMP | 创建时间 |

## 6.3 `ai_agent_message`

### 用途

保存会话消息和引用关系。

### 主要字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | BIGSERIAL | 主键 |
| `session_id` | BIGINT | 会话ID |
| `message_role` | VARCHAR(32) | 角色 |
| `message_text` | TEXT | 消息内容 |
| `cited_chunk_ids` | TEXT | 引用知识块ID列表 |
| `create_time` | TIMESTAMP | 创建时间 |

## 7. 表间关系说明

## 7.1 AI接入关系

- `ai_provider_config` 1 对多 `ai_provider_model`

## 7.2 权限关系

- `sys_user` 1 对多 `ai_user_ai_permission`
- `sys_user` 1 对多 `ai_user_knowledge_permission`
- `sys_user` 1 对多 `ai_user_skill_permission`

## 7.3 知识库关系

- `ai_knowledge_base` 1 对多 `ai_knowledge_category`
- `ai_knowledge_base` 1 对多 `ai_knowledge_document`
- `ai_knowledge_document` 1 对多 `ai_knowledge_chunk`

## 7.4 技能关系

- `ai_skill` 1 对多 `ai_skill_version`
- `ai_skill_version` 1 对多 `ai_skill_test_case`
- `ai_skill_version` 1 对多 `ai_skill_validation_run`

## 7.5 工作台关系

- `ai_agent_session` 1 对多 `ai_agent_message`

## 8. 索引与性能说明

首期策略：

- 使用常规索引 + PostgreSQL 文本检索思路
- 不先做向量检索

后续扩展：

- 可接 `pgvector`
- 可接 Elasticsearch

## 9. 数据安全说明

### Token

- 不允许明文返回前端列表
- 前端仅显示掩码
- 后端负责解密调用

### 会话

- 会话和引用需要保留
- 敏感场景可补充脱敏策略

## 10. 建议开发顺序

1. 先实现 `ai_provider_config`
2. 再实现 `ai_provider_model`
3. 再实现知识库五张表相关模块
4. 再实现三张权限表相关模块
5. 最后再进入 skill / workbench / expert
