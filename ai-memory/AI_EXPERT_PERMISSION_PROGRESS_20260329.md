# AI Expert And Skill Permission Progress - 2026-03-29

## Completed in this round
- Added user-to-skill permission management in `aipermission`.
- Extended current permission payload with `skillPermissions`.
- Integrated skill-level authorization into `skill` module and `agent` module.
- Added `expert` module for expert ledger and skill owner binding.
- Added frontend skill permission section in AI permission config page.
- Added frontend expert ledger page and menu entry.
- Updated skill center page so training/publishing UI responds to skill-level permission instead of only global AI permission.

## Backend details
- `AiPermissionServiceImpl` now supports list/save/check for user skill permissions.
- `SkillServiceImpl` now allows skill-specific train/publish checks.
- `AgentServiceImpl` now checks `canUseSkill(userId, skillId)` before creating and using sessions.
- New expert module:
  - `modules/expert/controller/ExpertController.java`
  - `modules/expert/service/ExpertService.java`
  - `modules/expert/service/impl/ExpertServiceImpl.java`
  - `modules/expert/mapper/SkillOwnerMapper.java`
  - `resources/mapper/expert/SkillOwnerMapper.xml`

## Frontend details
- `frontend/src/views/ai/AIPermissionConfigView.vue` now manages AI, knowledge-base, and skill permissions in one screen.
- `frontend/src/views/skill/SkillListView.vue` now respects `skillPermissions` from current permission API.
- `frontend/src/views/expert/ExpertListView.vue` shows expert ledger and admin maintenance form.
- Added route `/experts` and menu entry.

## Verification
- Backend compile passed: `mvn -q -DskipTests compile`
- Frontend build passed: `npm.cmd run build`

## Next recommended step
- Continue with expert identity display in more business pages and bind expert ownership to workflow/task assignment.
- Consider adding session title update persistence and skill version detail endpoint for draft version editing.