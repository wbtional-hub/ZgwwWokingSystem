# AI Development Prompts 2026-03-29

## Scan Prompt

Before continuing AI capability development, scan:

- `ai-memory/AI_CAPABILITY_BLUEPRINT.md`
- `ai-memory/AI_TASKS_20260329.md`
- `ai-memory/AI_DECISIONS_20260329.md`
- `database/20260329_ai_knowledge_schema.sql`
- `database/20260329_ai_skill_schema.sql`
- `database/20260329_ai_expert_agent_schema.sql`
- `database/20260329_ai_access_permission_schema.sql`

## Implementation Order Prompt

Develop in this order:

1. database alignment
2. knowledge module
3. AI access config module
4. AI permission module
5. skill module
6. agent workbench module
7. expert module

## Constraints Prompt

- do not restructure directories
- backend code must stay under `backend/src/main/java/com/example/lecturesystem/modules`
- frontend API code must stay under `frontend/src/api`
- frontend pages must stay under `frontend/src/views`
- keep MyBatis XML style
- reserve operation log audit points
- reserve permission checks in all new queries
- use dedicated AI permission tables instead of hard-coded role checks
- every workbench answer should support citations

## High-level Build Prompt

You are developing the AI capability platform in `D:\20.develop64\ZgwwWokingSystem`.

Target scope:

- AI access zone
- knowledge base center
- skills center
- permission center
- AI workbench
- expert binding

Primary business objective:

- input knowledge
- train skills
- assist work

Reference domains:

- talent policy
- lecture team policy

Phase 1 implementation strategy:

- use retrieval plus prompts
- do not start with model fine-tuning
- prioritize Word import, knowledge chunking, permission control, skill configuration, validation, and citation-based answering
