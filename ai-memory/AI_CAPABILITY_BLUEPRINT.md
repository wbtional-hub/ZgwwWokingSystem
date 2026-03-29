# AI Capability Blueprint

## Goal

Build an AI capability platform on top of the current system so that:

- admins can configure external AI access by token through the UI
- domain documents can be imported into knowledge bases
- authorized users can train skills from authorized knowledge bases
- authorized users can use skills plus knowledge retrieval in an AI workbench
- unauthorized users cannot see or use these functions

Primary business objective:

- input knowledge
- train skills
- assist work

Reference business domains:

- talent policy
- lecture team policy

## Product Model

The target solution has five layers:

1. AI access layer
2. Knowledge base layer
3. Skills layer
4. Permission layer
5. Workbench application layer

### AI access layer

Purpose:

- configure provider access through UI
- save token securely
- select default model
- test connection

Core objects:

- provider config
- provider model

### Knowledge base layer

Purpose:

- create domain knowledge bases
- import Word documents
- classify and chunk content
- support retrieval for AI answering and analysis

### Skills layer

Phase 1 skill training is not model fine-tuning.
It is a governed skill package made of:

- role definition
- task boundary
- knowledge base binding
- output template
- forbidden rules
- citation rules
- validation cases
- validation result
- published version

### Permission layer

Permissions must be enforced in frontend and backend.

Permission dimensions:

- AI capability permission
- knowledge base permission
- skill permission

### Workbench layer

Authorized user flow:

1. choose knowledge base
2. choose skill
3. choose AI provider and model if allowed
4. ask question
5. retrieve knowledge chunks
6. generate answer with citations
7. persist session and trace

## Core Decisions

### One AI access zone, many domains

Use one shared AI access zone for all domains.
Do not build one AI subsystem per policy domain.

Reuse pattern:

- one AI access zone
- many knowledge bases
- many skills
- one permission system
- one workbench

### Knowledge first, AI second

Skill training has two stages:

Stage A, non-AI training:

- import documents
- classify content
- define skill boundary
- define output template
- define forbidden rules

Stage B, AI-assisted training:

- generate draft prompts
- generate validation cases
- run validation
- answer questions
- perform analysis

### Phase 1 avoids fine-tuning

Phase 1 uses:

- knowledge retrieval
- governed prompts
- validation
- versioned publishing

Do not choose model fine-tuning as the first implementation path.

### Traceable policy answers

Every policy-answering and analysis feature should support citations.

Reason:

- policy consultation requires traceability
- users need source confirmation
- validation quality depends on source visibility

## Database Scripts Already Added

The following SQL files have been added into the repository:

- `database/20260329_ai_knowledge_schema.sql`
- `database/20260329_ai_skill_schema.sql`
- `database/20260329_ai_expert_agent_schema.sql`
- `database/20260329_ai_access_permission_schema.sql`

## Database Scope

### Knowledge tables

- `ai_knowledge_base`
- `ai_knowledge_category`
- `ai_knowledge_document`
- `ai_knowledge_chunk`
- `ai_document_import_job`

### Skill tables

- `ai_skill`
- `ai_skill_version`
- `ai_skill_kb_binding`
- `ai_skill_test_case`
- `ai_skill_validation_run`
- `ai_skill_validation_result`

### Expert and workbench tables

- `ai_skill_owner`
- `ai_agent_session`
- `ai_agent_message`

### AI access and permission tables

- `ai_provider_config`
- `ai_provider_model`
- `ai_user_ai_permission`
- `ai_user_knowledge_permission`
- `ai_user_skill_permission`

### Provider/model linkage fields

The schema also reserves real AI execution linkage by adding:

- `ai_skill_version.provider_config_id`
- `ai_skill_version.model_code`
- `ai_skill_validation_run.provider_config_id`
- `ai_skill_validation_run.model_code`
- `ai_agent_session.provider_config_id`
- `ai_agent_session.model_code`

## Backend Module Plan

Create new backend modules under:

- `backend/src/main/java/com/example/lecturesystem/modules/knowledge`
- `backend/src/main/java/com/example/lecturesystem/modules/skill`
- `backend/src/main/java/com/example/lecturesystem/modules/expert`
- `backend/src/main/java/com/example/lecturesystem/modules/agent`
- `backend/src/main/java/com/example/lecturesystem/modules/aiconfig`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission`

Implementation style must follow current project conventions:

- Controller
- Service
- Mapper
- XML
- DTO
- Entity
- VO

## Frontend Module Plan

Create new frontend pages under:

- `frontend/src/views/knowledge`
- `frontend/src/views/skill`
- `frontend/src/views/agent`
- `frontend/src/views/expert`
- `frontend/src/views/aiconfig`
- `frontend/src/views/aipermission`

Create new API files:

- `frontend/src/api/knowledge.js`
- `frontend/src/api/skill.js`
- `frontend/src/api/agent.js`
- `frontend/src/api/expert.js`
- `frontend/src/api/aiconfig.js`
- `frontend/src/api/aipermission.js`

## Key Pages

### AI access zone

- provider config list
- provider config edit form
- model config list
- connection test

### Knowledge center

- knowledge base list
- category tree
- document import page
- document list
- chunk detail
- search page

### Skills center

- skill list
- skill train page
- skill validation page
- skill publish page

### Permission center

- AI capability authorization
- knowledge base authorization
- skill authorization

### Workbench

- choose knowledge base
- choose skill
- choose AI model
- ask question
- show citations
- show hit documents and chunks
- show session history

## Permission Rules

### Frontend

The UI must hide menus, pages, actions, and buttons for unauthorized users.

### Backend

All APIs must enforce authorization even if frontend checks already exist.

### Admin behavior

Only admin users should be able to:

- manage AI provider configs
- test provider access
- grant AI permissions
- grant knowledge base permissions
- grant skill permissions
- publish sensitive skills if policy requires admin approval

## Domain Examples

### Talent policy domain

Knowledge base examples:

- talent recognition
- settlement policy
- living subsidy
- entrepreneurship support
- employer talent incentive
- application material list
- common rejection reasons

Skill examples:

- talent policy consultation expert
- talent policy matching assistant
- talent application material assistant
- subsidy calculation assistant

### Lecture team policy domain

Knowledge base examples:

- lecture team management policy
- lecture team assessment rules
- activity support policy
- reimbursement rules
- appointment rules

Skill examples:

- lecture team policy interpretation expert
- lecture team application assistant
- lecture team assessment assistant

## Phase Plan

### Phase 1

- AI access zone
- knowledge base center
- Word import
- document chunking
- retrieval

### Phase 2

- skill definition
- skill versioning
- validation cases
- validation runs
- publish flow

### Phase 3

- workbench
- citations
- session logs
- expert binding

### Phase 4

- dashboards
- optimization loop
- stronger retrieval and ranking

## Development Rules

- do not restructure directories
- backend code must stay under `backend/src/main/java/com/example/lecturesystem/modules`
- frontend API code must stay under `frontend/src/api`
- frontend pages must stay under `frontend/src/views`
- keep MyBatis XML style
- reserve operation log audit points
- reserve permission checks in all new queries
- use dedicated AI permission tables instead of hard-coded role checks
- prefer retrieval plus prompts over fine-tuning in phase 1

## Immediate Development Order

1. knowledge module
2. aiconfig module
3. aipermission module
4. skill module
5. agent module
6. expert module
