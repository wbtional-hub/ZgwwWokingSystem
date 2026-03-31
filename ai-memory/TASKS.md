# TASKS

## 高优先级任务

### 任务名称
知识库页改成业主可维护工作面

说明：
已完成。知识库中心已改成更适合业主使用的一页式管理页：主表单不再手填编码，新增时由后端自动生成；列表中可直接看见名称、分类、简介、状态、当前使用标识与最近更新时间；支持新增、编辑、启停、删除确认和“设为当前使用”。当前工作知识库已从前端临时 `activeBaseId` 收口为后端持久化参数，避免刷新后丢失。

涉及文件：
- `frontend/src/views/knowledge/KnowledgeBaseListView.vue`
- `frontend/src/api/knowledge.js`
- `frontend/src/config/pageHelp.js`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/controller/KnowledgeController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/KnowledgeService.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/impl/KnowledgeServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/dto/SaveKnowledgeBaseRequest.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/dto/SetCurrentKnowledgeBaseRequest.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/entity/KnowledgeBaseEntity.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/vo/KnowledgeBaseListItemVO.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/mapper/KnowledgeBaseMapper.java`
- `backend/src/main/resources/mapper/knowledge/KnowledgeBaseMapper.xml`
- `database/20260330_ai_knowledge_base_owner_friendly.sql`

### 任务名称
超级管理员直属新增不继承单位

说明：
已完成。组织架构页新增下级时，只有“超级管理员在自己节点下新增直属下级”这一种情况不再继承超级管理员单位，而是要求手动选择所属单位；其他用户或其他新增场景仍继续继承上级节点单位。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/service/impl/OrgTreeServiceImpl.java`

### 任务名称
组织架构页恢复设置单位能力

说明：
已完成。组织架构页继续保持新增下级自动继承上级单位且不可手改；编辑节点时“所属单位”已恢复为可选择、可编辑，且保存时会同步当前节点与全部下级节点的单位，避免子树单位归属不一致。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/service/impl/OrgTreeServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/mapper/OrgTreeMapper.java`
- `backend/src/main/resources/mapper/orgtree/OrgTreeMapper.xml`

### 任务名称
补齐用户模块前端基础交互

说明：
已完成一批最小闭环：用户详情弹层已补组织关系摘要，列表已接入重置密码，编辑弹层已展示只读组织树字段，表单已补手机号校验与保存按钮禁用态。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`
- `frontend/src/stores/user-management.js`
- `frontend/src/api/user.js`

### 任务名称
继续收口用户页面验收反馈

说明：
已完成一批页面级小闭环：用户页已补筛选摘要、空态动作入口、列表组织树状态与更新时间、详情弹层快捷编辑/重置密码、表单不可提交原因提示。当前仍需做真实页面验收，不宜提前跨到权限或业务模块。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`
- `backend/src/main/java/com/example/lecturesystem/modules/user/vo/UserListItemVO.java`
- `backend/src/main/resources/mapper/user/UserMapper.xml`
- `backend/src/test/java/com/example/lecturesystem/modules/user/service/impl/UserServiceImplTest.java`

### 任务名称
继续收口用户页面结果反馈

说明：
已完成新一批小闭环：用户页已补结果摘要，详情弹层已补关闭入口，新增/编辑后已衔接到详情查看，表单已补 clearable、长度限制与状态说明提示。当前页面可读性更完整，但仍缺少真实浏览器验收。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面刷新与状态管理

说明：
已完成新一批小闭环：用户页头部已补刷新入口，状态筛选已自动查询，详情弹层已补主动刷新和关闭后状态清理，创建表单已补密码显隐切换。当前更接近真实页面验收，但仍未进入浏览器实操。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/stores/user-management.js`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面验收辅助能力

说明：
已完成新一批小闭环：筛选区已补重置按钮，详情已补时间字段，表单已补字数计数，详情账号与树路径已支持复制，列表已补最后同步时间。当前页面更适合真实联调取值和核对，但仍未实际打开浏览器验收。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面操作反馈

说明：
已完成新一批小闭环：列表操作已补按用户粒度反馈，翻页后自动回顶，详情进入编辑会保留摘要，创建表单补了初始密码说明，卡片标题改为姓名和账号联合展示。当前更适合真实浏览器验收，但仍未进入实际页面操作。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面定位与连续浏览

说明：
已完成新一批小闭环：当前详情用户已支持列表高亮，卡片标题可直接进入详情，详情支持上一位/下一位连续浏览，编辑弹层可恢复原值，列表卡片展示分页内序号。当前页面更适合真实浏览器逐项验收。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面当前查看与创建态反馈

说明：
已完成新一批小闭环：列表摘要区已补当前查看用户反馈，详情中的上一位/下一位/刷新与提交态互斥，创建表单支持清空输入，筛选摘要文案已统一。当前页面已非常接近真实浏览器验收态。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面取值与确认提示

说明：
已完成新一批小闭环：删除/重置密码确认弹框已补用户信息，详情支持复制用户ID和手机号，详情标题已展示姓名/账号，创建表单已明确默认建议启用。当前页面已更适合真实浏览器验收与取值核对。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面连续浏览提示

说明：
已完成新一批小闭环：详情支持复制单位ID，当前查看摘要已补页内位次信息，编辑弹层标题已补当前姓名，详情摘要已补连续浏览提示。当前页面的浏览、核对和切换线索更完整。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面快捷取值与定位

说明：
已完成新一批小闭环：详情摘要已补所属单位，列表账号支持直接复制，详情已补回顶入口，表单重置说明更清楚，当前查看摘要已补姓名/账号联合展示。当前页面更接近真实浏览器逐项验收。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面筛选与危险操作反馈

说明：
已完成。用户页筛选重置已避免重复请求，关键词统一 trim，列表在筛选或删除后若落到空页会自动回退；删除当前查看用户后会自动关闭旧详情；删除/重置密码在确认框取消时不再误报错误；创建表单已补单位 ID 正整数校验。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`
- `frontend/src/stores/user-management.js`

### 任务名称
继续收口用户页面自操作防误触与上下文承接

说明：
已完成。用户页已禁止删除当前登录用户、禁止重置当前登录用户密码；当前详情用户若离开当前页会自动关闭；新增用户弹层会优先带入当前查看用户的单位ID；重置密码成功提示已明确默认密码为 `123456`。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
继续收口用户页面当前登录用户识别与自停用防护

说明：
已完成。用户列表与详情已显式标记当前登录用户；当前登录用户卡片已提示不可删除与重置密码；编辑当前登录用户时，状态选择和提交都会阻止“停用自己”，状态说明中也已补充限制提示。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户表单未保存保护与无效提交拦截

说明：
已完成。编辑用户时若未修改内容会直接禁用保存并提示“尚未修改内容”；创建表单初始态会禁用“清空输入”，编辑表单原始态会禁用“恢复原值”；创建/编辑表单存在未保存修改时，关闭前会先做二次确认。

涉及文件：
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
继续收口用户页面并发误触

说明：
已完成。页面忙碌时已统一禁用新增用户、查询、重置筛选、卡片详情/编辑以及详情中的编辑入口；提交中会禁用详情关闭按钮，减少并发点击导致的状态抖动。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
修复组织架构测试内存映射

说明：
已完成。`UserServiceImplTest` 中 `InMemoryUserMapper.cloneUser(...)` 已补齐 `parentUserId / levelNo / treePath` 复制，`mvn -Dtest=UserServiceImplTest test` 已通过，当前阶段为“组织架构测试修复完成，准备恢复接口真实验收”。

涉及文件：
- `backend/src/test/java/com/example/lecturesystem/modules/user/service/impl/UserServiceImplTest.java`

### 任务名称
启动用户模块前端闭环

说明：
已完成第一步。用户详情页已开始展示组织树字段状态，为后续用户管理与组织树职责边界梳理做前置铺垫。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充上级链切换提示

说明：
已完成。组织树上级链区域已补点击切换提示文案，便于真实页面验证时识别可交互区域。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充直属下级切换提示

说明：
已完成。组织树直属下级卡片已补点击切换提示文案，便于真实页面验证时识别可交互区域。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充详情关系计数

说明：
已完成。组织树节点详情区已补直属下级数和上级链长度，便于真实页面验证时快速核对关系规模。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充上级摘要展示

说明：
已完成。组织树节点详情区已展示更直观的上级信息，优先显示上级姓名与账号，便于页面验证。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充关系区数量提示

说明：
已完成。组织树页面的直属下级与上级链标题已展示数量，便于真实页面验证时快速比对返回结果。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
整理关系区状态展示

说明：
已完成。组织树页面的直属下级和上级链区域已将空态与 loading 改为互斥展示，避免同时出现冲突提示。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充关系区局部加载态

说明：
已完成。组织树页面的直属下级和上级链区域已支持局部 loading，切换节点或手动刷新时反馈更明确。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充直属下级高亮

说明：
已完成。组织树页面的直属下级卡片已支持当前节点高亮，便于真实页面验证时快速识别当前位置。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充上级链点击切换

说明：
已完成。组织树页面的上级链节点已支持点击切换，便于真实页面验证时沿链路快速定位节点。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
补充移动弹层提示

说明：
已完成。组织树页面调整上级时，弹层会直接展示当前不可提交的原因，便于真实页面验证时快速判断状态。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
拦截无效移动提交

说明：
已完成。组织树页面调整上级时，若目标上级未变化，前端会直接禁用提交并阻止无意义请求。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
过滤无效移动目标

说明：
已完成。组织树页面调整上级时，前端已过滤当前节点自身及其全部下级节点，避免把必然失败的目标暴露给用户。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
稳定组织树页面选中态

说明：
已完成。组织树新增下级和调整上级后，页面刷新会优先重新选中当前操作节点，便于真实页面验证结果。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`

### 任务名称
打通前端联调配置

说明：
已完成。前端请求地址已改为可配置，开发环境通过 Vite `/api` 代理转发到后端，前端构建已验证通过。

涉及文件：
- `frontend/src/utils/request.js`
- `frontend/vite.config.js`

### 任务名称
统一登录态返回结构

说明：
已完成。`/api/auth/me` 现已与 `POST /api/auth/login` 统一返回 `LoginVO` 结构，`superAdmin` 字段已通过单测固定。

涉及文件：
- `backend/src/main/java/com/example/lecturesystem/modules/auth/controller/AuthController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/auth/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/auth/vo/LoginVO.java`
- `backend/src/test/java/com/example/lecturesystem/modules/auth/service/impl/AuthServiceImplTest.java`

### 任务名称
确认组织树字段已落库

说明：
已完成。`sys_user.parent_user_id / level_no / tree_path` 已在真实 PostgreSQL 中确认存在，组织树 SQL 已执行。

涉及文件：
- `backend/database/init_orgtree_user_fields.sql`
- `database/01_schema.sql`
- `database/02_seed.sql`

### 任务名称
完成组织树真实接口联调

说明：
已完成。基于真实 JWT，已验证创建一级/二级/三级节点、查询组织树、查询直属下级、查询上级链、移动节点、防循环。

涉及文件：
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/controller/OrgTreeController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/service/impl/OrgTreeServiceImpl.java`
- `backend/src/main/resources/mapper/orgtree/OrgTreeMapper.xml`

### 任务名称
验证数据库账号与表结构

说明：
已完成。后端通过 `wsl` profile 成功连接宿主机 PostgreSQL，并完成字段确认与 SQL 执行。

涉及文件：
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-wsl.yml`
- `backend/database/init_orgtree_user_fields.sql`

### 任务名称
对齐前后端组织树 API

说明：
前端主组织树 API 已切到 `/api/orgtree`，并补齐 `children`、`ancestors` 调用；下一步是做真实页面联调。

涉及文件：
- `frontend/src/api/orgtree.js`
- `frontend/src/views/orgtree/OrgTreeView.vue`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/controller/OrgTreeController.java`

### 任务名称
完成组织树前端最小闭环

说明：
已完成代码实现，下一步是做真实浏览器页面联调。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`
- `frontend/src/api/orgtree.js`
- `frontend/src/components/orgtree/OrgTreeNodeItem.vue`

## 中优先级任务

### 任务名称
补数据库序列稳定化

说明：
本次真实联调发现 `sys_user_id_seq` 与种子数据不一致，已手工修复。后续可补最小自检/修复脚本，避免再次出现主键重复。

涉及文件：
- `database/02_seed.sql`
- `backend/database/init_orgtree_user_fields.sql`

### 任务名称
明确用户管理与组织树职责边界

说明：
当前通用 `user` 模块与 `orgtree` 模块都有用户相关操作，需要确认后续新增用户是否一律通过组织树链路进入。当前 `frontend/src/components/user/UserFormDialog.vue` 与 `/api/users` 仍不支持 `parentUserId`。

涉及文件：
- `backend/src/main/java/com/example/lecturesystem/modules/user/controller/UserController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/user/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/service/impl/OrgTreeServiceImpl.java`

### 任务名称
补充组织树前端能力

说明：
前端需继续做真实页面操作验证，重点确认直属下级、上级链、节点移动后的结果展示是否与后端一致，并继续检查高亮反馈、点击切换、交互提示、禁用态以及局部状态展示是否足够清晰。

涉及文件：
- `frontend/src/views/orgtree/OrgTreeView.vue`
- `frontend/src/api/orgtree.js`

### 任务名称
补充统计与权限联调

说明：
组织树切到 `sys_user` 后，需要继续验证统计和权限边界是否完全建立在新树字段之上。

涉及文件：
- `backend/src/main/resources/mapper/permission/PermissionMapper.xml`
- `backend/src/main/resources/mapper/statistics/StatisticsMapper.xml`
- `backend/src/main/java/com/example/lecturesystem/modules/permission/service/impl/PermissionServiceImpl.java`

## 低优先级任务

### 任务名称
清理旧组织树概念残留

说明：
当前库初始化脚本仍保留 `sys_org_node` 表，命名上也保留 `OrgNodeEntity`，后续可在确认稳定后清理旧概念。

涉及文件：
- `database/01_schema.sql`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/entity/OrgNodeEntity.java`

### 任务名称
补充 AI 开发记忆更新流程

说明：
每次闭环开发后更新 `DEV_MEMORY.md`、`TASKS.md`、`DECISIONS.md`，形成持续开发上下文。

涉及文件：
- `ai-memory/DEV_MEMORY.md`
- `ai-memory/TASKS.md`
- `ai-memory/DECISIONS.md`
- `ai-memory/PROMPTS.md`

## 本批已完成

### 任务名称
补充用户页当前页状态统计与空结果提示

说明：
用户列表已能直接展示当前页启用/停用人数，并在有筛选条件但无结果时提示“当前筛选无结果”，更适合真实页面验收。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户详情邻近用户预览

说明：
详情摘要中已可直接看到上一位/下一位用户的姓名账号，方便连续浏览验收。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户创建与编辑表单录入说明

说明：
表单已明确手机号可后补，创建时也已补账号录入建议，减少无效试填。

涉及文件：
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
补充用户页本页显示人数与单位ID快捷取值

说明：
已完成。用户页摘要区已补本页显示人数，列表中的单位 ID 也已支持直接复制，方便真实验收快速核对。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户详情摘要状态反馈

说明：
已完成。用户详情摘要已前置展示当前状态与最近更新时间，减少进入详情后重复扫字段。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户表单单位与组织树说明

说明：
已完成。创建表单已补单位 ID 录入提示，编辑态已补组织树字段只读说明，职责边界更明确。

涉及文件：
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
补充用户页组织树挂接统计与手机号快捷取值

说明：
已完成。用户页摘要区已补本页组织树挂接人数，列表中的手机号也已支持直接复制，方便页内联调核对。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户详情摘要信息完整度提示

说明：
已完成。用户详情摘要已补岗位与联系方式状态，减少进入详情后反复扫字段。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户表单姓名与岗位录入说明

说明：
已完成。表单已明确姓名会用于页面展示，岗位名称可留空但可帮助区分同名用户。

涉及文件：
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
补充用户页未挂接统计与当前查看状态

说明：
已完成。用户页摘要区已补本页未挂接组织树人数，当前查看摘要也已带上状态反馈，页内核对更直接。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户详情关键信息完成度

说明：
已完成。用户详情摘要已可直接显示关键信息完成度，便于判断岗位和手机号是否仍待补录。

涉及文件：
- `frontend/src/views/user/UserEditView.vue`

### 任务名称
补充用户表单状态说明与编辑摘要状态

说明：
已完成。编辑摘要已带上当前状态，创建状态说明也已明确“后续可编辑调整”。

涉及文件：
- `frontend/src/components/user/UserFormDialog.vue`

### 任务名称
补充统一鉴权失效联调处理

说明：
已完成。前端请求层已统一处理 `401 / 403`，遇到登录态失效会自动清理本地 token 与用户信息，并跳回登录页，避免页面停留在半失效状态。

涉及文件：
- `frontend/src/utils/request.js`

### 任务名称
对齐前端 /api/auth/login 与 Vite 代理 / 后端登录放行配置

说明：
已完成。排查确认后端 `POST /api/auth/login` 已放行且 CSRF 已禁用，403 的根因在于前端请求基地址与代理链路不一致。现已统一为 `/api + /auth/login + vite proxy + /api/auth/login`，并让登录请求跳过旧 token 注入。

涉及文件：
- `frontend/src/utils/request.js`
- `frontend/src/api/auth.js`
- `frontend/vite.config.js`
- `backend/src/main/java/com/example/lecturesystem/config/SecurityConfig.java`
- `backend/src/main/java/com/example/lecturesystem/modules/auth/controller/AuthController.java`

### 任务名称
登录 403 强制定位闭环

说明：
已完成。强制打印并核对前端请求路径、Vite 代理、后端登录接口和 Security 放行配置后，确认后端真实接口与代理链路当前均返回 `200`，问题不在 Controller / Security / CSRF / 当前 Vite 代理，而在此前前端请求链路不一致与浏览器旧脚本缓存。

涉及文件：
- `frontend/src/api/auth.js`
- `frontend/src/utils/request.js`
- `frontend/vite.config.js`
- `backend/src/main/java/com/example/lecturesystem/modules/auth/controller/AuthController.java`
- `backend/src/main/java/com/example/lecturesystem/config/SecurityConfig.java`

### 任务名称
执行前端缓存清理与登录重新验证

说明：
已完成。已停止旧前端 dev 服务，删除 `node_modules/.vite`，重新启动 Vite，并再次验证 `POST http://127.0.0.1:5173/api/auth/login` 返回 `200 OK`，不再沿用旧的 403 结果。

涉及文件：
- `frontend/node_modules/.vite`
- `ai-memory/DEV_MEMORY.md`
- `ai-memory/TASKS.md`

### 任务名称
知识库模块新增网页导入与折叠反馈信息

说明：
已完成。知识库管理页已新增“网页导入”面板，支持选择知识库、输入单个网页地址、先抓取预览再确认导入；抓取成功后可回填并人工修正标题、摘要、正文，反馈信息区默认折叠，仅在需要排查时展开并可一键复制给 AI。后端已新增网页抓取预览与确认导入接口，使用 `HttpClient + jsoup` 做静态/半静态页面正文提取，并把导入结果复用到现有知识文档与切片模型；同时新增 SQL 以补充 `ai_knowledge_document.source_url / fetch_time` 字段。

涉及文件：
- `frontend/src/views/knowledge/KnowledgeBaseListView.vue`
- `frontend/src/api/knowledge.js`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/controller/KnowledgeController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/KnowledgeService.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/impl/KnowledgeServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/dto/WebKnowledgePreviewRequest.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/dto/WebKnowledgeImportRequest.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/vo/WebKnowledgePreviewVO.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/vo/WebKnowledgeImportResultVO.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/entity/KnowledgeDocumentEntity.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/vo/KnowledgeDocumentDetailVO.java`
- `backend/src/main/resources/mapper/knowledge/KnowledgeDocumentMapper.xml`
- `backend/pom.xml`
- `database/20260330_ai_knowledge_web_import.sql`

### 任务名称
知识库创建人默认权限与同单位授权规则收口

说明：
已完成。知识库权限不再是“只有管理员才能看到和维护”。当前规则已收口为：知识库创建人默认拥有该库的查看、上传、训练、分析权限；创建人可在知识库管理页直接给其他用户授权，但只能授权本单位人员；超级管理员可跨单位设置所有人的知识库授权。知识库列表、当前权限摘要和知识库页“使用授权”区均已兼容这套规则，且创建人默认权限会作为不可编辑的默认项展示，避免被旧授权记录覆盖。

涉及文件：
- `frontend/src/views/knowledge/KnowledgeBaseListView.vue`
- `frontend/src/api/ai.js`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/controller/AiPermissionController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/service/AiPermissionService.java`
- `backend/src/main/java/com/example/lecturesystem/modules/aipermission/service/impl/AiPermissionServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/mapper/KnowledgeBaseMapper.java`
- `backend/src/main/resources/mapper/knowledge/KnowledgeBaseMapper.xml`

### 任务名称
网页导入标题与正文抽取增强

说明：
已完成。网页导入的抓取预览不再只依赖一套通用规则，而是升级为多级标题策略、多个正文候选容器评分择优、低质量候选自动重试和更细的反馈文本。当前会依次尝试 `h1 / 常见标题类名 / og:title / title / 正文首行` 提取标题，并过滤导航词、站点名和栏目名；正文会尝试常见详情页容器与文本密度候选，对每个候选记录文本长度、评分和低质量原因，最终选择质量最高的候选。反馈信息已补充 HTML 长度、原始 title、发布时间、标题尝试过程、正文候选过程、最终采用策略和导入建议，便于继续复制给 AI 调优。

涉及文件：
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/impl/KnowledgeServiceImpl.java`
- `frontend/src/views/knowledge/KnowledgeBaseListView.vue`

### 任务名称
网页导入动态页诊断与浏览器渲染兜底

说明：
已完成。针对“HTTP 200 但 HTML 很短、标题抓不到、正文为空”的网页导入场景，知识库网页抓取已新增疑似动态页诊断与浏览器渲染兜底。普通模式会补齐更接近浏览器的请求头，并检测 `fetch / axios / XMLHttpRequest / iframe / rowId / detailId / contentId` 等异步加载特征；若同时伴随 HTML 偏短、标题缺失、正文极短，则自动触发无头浏览器 `dump-dom` 兜底，再复用现有标题/正文评分逻辑择优返回结果。反馈信息也已新增抓取模式、是否触发兜底、异步特征检测、普通/渲染两种正文长度、最终采用模式、失败类型和建议，便于复制给 AI 继续分析。

涉及文件：
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/impl/KnowledgeServiceImpl.java`

### 任务名称
网页导入请求阶段失败诊断与兜底触发修复

说明：
已完成。网页导入普通 HTTP 抓取在请求阶段抛异常时，不再被直接吞掉后返回空反馈，而是会生成结构化请求阶段诊断，并继续判断是否触发浏览器渲染兜底。当前反馈信息已能展示请求客户端、请求开始时间、目标地址、最终地址、是否重定向、HTML 长度、HTML 预览、异常类型、异常消息，以及是否超时 / SSL 异常 / DNS 解析异常 / 连接被拒绝；失败类型也已收口为 `DNS解析失败 / 连接超时 / 读取超时 / SSL证书或握手异常 / 连接被拒绝 / 重定向异常 / 返回4xx / 返回5xx / HTML为空 / 其他未知请求异常`。兜底触发条件同步放宽，只要普通模式失败、状态未获取、HTML 为空或过短、标题未获取、正文为空或疑似动态页，就会继续尝试浏览器渲染；若渲染能力未接通，也会在反馈信息里明确说明。

涉及文件：
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/impl/KnowledgeServiceImpl.java`
- `frontend/src/views/knowledge/KnowledgeBaseListView.vue`

### 任务名称
网页快照导入闭环

说明：
已完成。知识库模块在现有“网页地址导入”基础上新增了“网页快照导入”子模式，支持上传单个 `.mhtml / .html / .htm` 文件进行解析预览、手工修正标题/摘要/正文后再确认导入。`.html/.htm` 会直接按 HTML 解析；`.mhtml` 会按 MIME multipart 结构提取主 HTML，并兼容 `base64 / quoted-printable / 8bit` 常见正文编码。解析出 HTML 后，会继续复用现有标题提取、正文候选择优、摘要生成和默认折叠反馈信息能力。入库则继续复用 `ai_knowledge_document + ai_knowledge_chunk` 模型，不新增表结构，仅把 `sourceType` 记为 `WEB_SNAPSHOT` 并保留上传的快照文件名与存储路径。

涉及文件：
- `frontend/src/views/knowledge/KnowledgeBaseListView.vue`
- `frontend/src/api/knowledge.js`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/controller/KnowledgeController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/KnowledgeService.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/service/impl/KnowledgeServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/knowledge/vo/WebKnowledgePreviewVO.java`
