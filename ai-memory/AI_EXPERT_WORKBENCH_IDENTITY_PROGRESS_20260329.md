# AI Expert Identity And Workbench Progress - 2026-03-29

## Completed in this round
- Persisted agent session title to database after first user question.
- Added expert identity and permission summary display to AI workbench.
- Added current AI/knowledge/skill authorization summary to profile page.
- Added current expert identity list to profile page.

## Backend changes
- `AgentSessionMapper` now supports `updateTitle`.
- `AgentSessionMapper.xml` now updates `ai_agent_session.session_title`.
- `AgentServiceImpl` now writes the first-question summary back to DB when the session title is still `New session`.

## Frontend changes
- `AIWorkbenchView.vue`
  - shows current AI capability summary
  - shows current expert identities
  - shows whether the selected skill is one of the user's expert skills
- `ProfileView.vue`
  - shows current AI permission counts
  - shows current expert identities
  - shows whether AI workbench is available to the current user

## Verification
- Backend compile passed: `mvn -q -DskipTests compile`
- Frontend build passed: `npm.cmd run build`

## Next recommended step
- Bind expert identity into business records such as consultation logs, task assignment, and statistics dashboards.
- Add dedicated session history list for AI workbench so users can revisit previous expert consultations.