# AI能力平台详细接口设计书

## 1. 文档目的

本文档定义 AI 能力平台第一阶段相关模块的接口设计，供后端开发、前端联调、测试验证使用。

接口范围：

- AI接入区
- 权限中心
- 知识库中心

统一约定：

- 返回结构使用现有 `ApiResponse`
- 鉴权沿用现有 JWT 体系
- 权限不足返回业务错误或 403
- 所有查询接口预留数据权限过滤

## 2. 返回结构约定

返回结构：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

说明：

- `code = 0` 表示成功
- 非 `0` 表示失败

## 3. AI接入区接口

## 3.1 查询 AI 接入配置列表

### 接口

`POST /api/ai-config/provider/list`

### 说明

查询 AI 接入配置列表。

### 请求参数

```json
{
  "keywords": "openai",
  "status": 1
}
```

### 返回示例

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "providerCode": "OPENAI_MAIN",
      "providerName": "OpenAI Main",
      "apiBaseUrl": "https://api.openai.com/v1",
      "tokenMask": "sk-****abcd",
      "defaultModel": "gpt-5.4",
      "connectStatus": "SUCCESS",
      "status": 1,
      "updateTime": "2026-03-29T12:00:00"
    }
  ]
}
```

## 3.2 保存 AI 接入配置

### 接口

`POST /api/ai-config/provider/save`

### 说明

新增或编辑 AI 接入配置。

### 请求参数

```json
{
  "id": 1,
  "providerCode": "OPENAI_MAIN",
  "providerName": "OpenAI Main",
  "apiBaseUrl": "https://api.openai.com/v1",
  "apiToken": "sk-xxxxx",
  "defaultModel": "gpt-5.4",
  "remark": "主接入"
}
```

### 返回示例

```json
{
  "code": 0,
  "message": "ok",
  "data": 1
}
```

## 3.3 启停 AI 接入配置

### 接口

`POST /api/ai-config/provider/toggle-status`

### 请求参数

```json
{
  "id": 1,
  "status": 0
}
```

## 3.4 测试 AI 接入配置

### 接口

`POST /api/ai-config/provider/test-connection`

### 说明

用配置好的 Base URL、Token、Model 发起测试。

### 请求参数

```json
{
  "id": 1,
  "testModel": "gpt-5.4"
}
```

### 返回示例

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "success": true,
    "connectStatus": "SUCCESS",
    "message": "连接成功"
  }
}
```

## 3.5 查询模型列表

### 接口

`POST /api/ai-config/model/list`

### 请求参数

```json
{
  "providerConfigId": 1,
  "status": 1
}
```

## 3.6 保存模型配置

### 接口

`POST /api/ai-config/model/save`

### 请求参数

```json
{
  "id": 1,
  "providerConfigId": 1,
  "modelCode": "gpt-5.4",
  "modelName": "GPT-5.4",
  "modelType": "chat",
  "supportKnowledge": true,
  "supportSkillTrain": true,
  "supportAgentChat": true,
  "supportAnalysis": true,
  "status": 1
}
```

## 3.7 启停模型

### 接口

`POST /api/ai-config/model/toggle-status`

### 请求参数

```json
{
  "id": 1,
  "status": 0
}
```

## 4. 权限中心接口

## 4.1 查询 AI 能力授权列表

### 接口

`POST /api/ai-permission/ai/list`

### 请求参数

```json
{
  "keywords": "张三",
  "providerConfigId": 1,
  "status": 1
}
```

### 返回字段

- 用户ID
- 用户名
- 姓名
- AI接入
- 可管理接入
- 可使用AI
- 可训练技能
- 可发布技能
- 可使用工作台
- 可运行分析
- 状态

## 4.2 保存 AI 能力授权

### 接口

`POST /api/ai-permission/ai/save`

### 请求参数

```json
{
  "id": 1,
  "userId": 1001,
  "providerConfigId": 1,
  "canManageProvider": false,
  "canUseAi": true,
  "canTrainSkill": true,
  "canPublishSkill": false,
  "canUseAgent": true,
  "canRunAnalysis": true,
  "status": 1
}
```

## 4.3 查询知识库授权列表

### 接口

`POST /api/ai-permission/knowledge/list`

## 4.4 保存知识库授权

### 接口

`POST /api/ai-permission/knowledge/save`

### 请求参数

```json
{
  "id": 1,
  "userId": 1001,
  "baseId": 1,
  "canView": true,
  "canUpload": true,
  "canTrainSkill": true,
  "canAnalyze": true,
  "status": 1
}
```

## 4.5 查询技能授权列表

### 接口

`POST /api/ai-permission/skill/list`

## 4.6 保存技能授权

### 接口

`POST /api/ai-permission/skill/save`

### 请求参数

```json
{
  "id": 1,
  "userId": 1001,
  "skillId": 1,
  "canView": true,
  "canUse": true,
  "canTrain": true,
  "canPublish": false,
  "status": 1
}
```

## 5. 知识库中心接口

## 5.1 查询知识库列表

### 接口

`POST /api/knowledge/base/list`

### 请求参数

```json
{
  "keywords": "人才",
  "domainType": "talent-policy",
  "status": 1
}
```

### 返回字段

- `id`
- `baseCode`
- `baseName`
- `domainType`
- `description`
- `status`
- `ownerUserId`
- `documentCount`
- `chunkCount`
- `updateTime`

## 5.2 保存知识库

### 接口

`POST /api/knowledge/base/save`

### 请求参数

```json
{
  "id": 1,
  "baseCode": "TALENT_POLICY",
  "baseName": "人才政策知识库",
  "domainType": "talent-policy",
  "description": "人才政策相关知识库",
  "status": 1
}
```

## 5.3 启停知识库

### 接口

`POST /api/knowledge/base/toggle-status`

## 5.4 查询分类树

### 接口

`POST /api/knowledge/category/tree`

### 请求参数

```json
{
  "baseId": 1,
  "status": 1
}
```

## 5.5 保存分类

### 接口

`POST /api/knowledge/category/save`

### 请求参数

```json
{
  "id": 1,
  "baseId": 1,
  "parentId": null,
  "categoryCode": "TALENT_SUBSIDY",
  "categoryName": "人才补贴",
  "sortNo": 10,
  "status": 1
}
```

## 5.6 查询文档列表

### 接口

`POST /api/knowledge/document/list`

### 请求参数

```json
{
  "baseId": 1,
  "categoryId": 10,
  "keywords": "补贴",
  "policyRegion": "杭州",
  "status": 1,
  "parseStatus": "SUCCESS"
}
```

## 5.7 导入 Word 文档

### 接口

`POST /api/knowledge/document/import-docx`

### 请求方式

`multipart/form-data`

### 表单字段

- `baseId`
- `categoryId`
- `policyRegion`
- `policyLevel`
- `effectiveDate`
- `expireDate`
- `summary`
- `keywords`
- `file`

### 返回字段

- `jobId`
- `documentId`
- `fileName`
- `jobStatus`
- `totalChunks`
- `successChunks`
- `errorMessage`

## 5.8 查询文档详情

### 接口

`GET /api/knowledge/document/{id}`

## 5.9 查询知识块列表

### 接口

`POST /api/knowledge/chunk/list`

### 请求参数

```json
{
  "documentId": 100
}
```

## 5.10 知识检索

### 接口

`POST /api/knowledge/search`

### 请求参数

```json
{
  "baseId": 1,
  "categoryId": 10,
  "keywords": "高层次人才补贴",
  "policyRegion": "杭州",
  "effectiveOnly": true,
  "topN": 10
}
```

### 返回字段

- `documentId`
- `chunkId`
- `docTitle`
- `policyRegion`
- `headingPath`
- `snippet`
- `keywordText`
- `chunkNo`

## 6. 后端校验要求

## 6.1 AI接入区

- 仅管理员可访问
- Token 保存时必须加密或密文化
- 返回列表时不可返回明文 Token

## 6.2 权限中心

- 仅管理员可访问
- 保存授权时必须校验目标用户存在

## 6.3 知识库中心

- 知识库管理默认管理员或知识管理员可用
- 普通用户必须按授权访问
- 导入文档需校验上传权限
- 检索需校验查看权限

## 7. 审计要求

以下接口要记录审计日志：

- provider/save
- provider/toggle-status
- provider/test-connection
- model/save
- model/toggle-status
- ai-permission/*/save
- knowledge/base/save
- knowledge/base/toggle-status
- knowledge/category/save
- knowledge/document/import-docx

## 8. 开发注意事项

- 接口命名风格与现有项目保持一致
- 优先使用 `POST /list` 风格做查询
- Controller 不写业务逻辑
- Service 做权限和流程控制
- Mapper XML 明确维护 SQL
- 文档导入接口要兼容后续扩展 PDF、TXT 的可能性
