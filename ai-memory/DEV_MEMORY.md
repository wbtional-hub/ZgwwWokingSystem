# DEV_MEMORY

## 1. 当前开发阶段

当前处于“组织架构测试修复完成，准备恢复接口真实验收”。

当前聚焦模块：
- 金字塔组织架构，基于 `sys_user`

当前项目状态：
- 健康检查已完成
- 登录认证与 JWT 已完成最小闭环
- 单位管理已完成最小闭环
- 组织架构树后端主链路已基本完成
- WSL 到 Windows 宿主机 PostgreSQL 已完成连接验证
- `sys_user` 组织树字段已在真实数据库确认
- 组织架构真实接口验收已完成
- 前端组织树最小闭环代码已具备
- 前端联调基础配置已补齐，可通过 Vite 代理转发 `/api`
- 组织树页面已补“刷新后保持当前操作节点选中”逻辑
- 组织树移动上级候选已在前端过滤自己及其下级节点
- 组织树移动上级时，前端已拦截“目标上级未变更”的无效提交
- 组织树移动上级弹层已补充不可提交原因提示
- 组织树上级链已支持点击节点后直接切换当前选中节点
- 组织树直属下级卡片已支持当前节点高亮
- 组织树直属下级与上级链区域已补充局部加载反馈
- 组织树关系区空态与加载态已改为互斥展示
- 组织树关系区标题已展示直属下级与上级链数量
- 组织树节点详情已展示上级姓名/账号摘要
- 组织树节点详情已展示直属下级数与上级链长度
- 组织树直属下级卡片已补点击切换提示文案
- 组织树上级链已补点击切换提示文案
- 用户详情页已开始展示 `parentUserId / levelNo / treePath` 状态
- 用户详情接口已补齐 `parentUserId / levelNo / treePath` 返回
- `UserServiceImplTest` 内存 Mapper 已补齐 `parentUserId / levelNo / treePath` 复制，组织树字段断言已恢复通过
- 用户详情弹层已补“组织关系摘要”和职责说明，明确树关系维护入口仍在 `orgtree`
- 用户管理页已接入“重置密码”按钮，复用现有 `/api/user/reset-password/{userId}` 接口
- 用户编辑弹层已展示只读 `parentUserId / levelNo / treePath`
- 用户表单已补手机号格式校验与保存按钮禁用态
- 用户页已补当前筛选条件摘要，便于判断列表是否处于过滤状态
- 用户页空态已提供“清空筛选 / 新增用户”两个直接入口
- 用户列表已开始展示组织树挂接状态与最近更新时间
- 用户详情弹层已补“编辑当前用户 / 重置当前用户密码”快捷入口
- 用户表单已补不可提交原因提示，与组织树页面交互反馈风格保持一致
- 用户页已补“共多少人 / 当前第几页”结果摘要
- 用户详情弹层已补显式关闭按钮，减少只依赖弹层手势关闭
- 用户新增后可直接进入新建用户详情，编辑后若仍停留详情也会刷新
- 用户表单输入框已补 clearable 与长度限制，减少脏输入
- 用户表单已补当前状态说明提示，帮助理解启用/停用结果
- 用户页头部已补“刷新”按钮，便于真实联调时快速重拉列表
- 用户状态筛选切换后已自动重新查询，减少一次额外点击
- 用户详情弹层已补“刷新当前详情”按钮，便于反复核对接口返回
- 用户关闭详情或切到新增时会清空旧详情，减少陈旧数据闪现
- 用户创建表单已补初始密码显隐切换，降低录入错误
- 用户筛选区已补显式“重置筛选”按钮，避免只能依赖搜索取消或空态入口
- 用户详情已补创建时间与更新时间，便于联调时核对记录变化
- 用户表单已补字数计数反馈，长度限制不再完全依赖用户体感
- 用户详情中的账号与树路径已支持点击复制，便于拿值去做接口或数据库核对
- 用户列表已补“最后同步时间”，更容易判断刷新结果是否生效
- 用户列表的删除/重置密码按钮已补按用户粒度的 loading 与互斥禁用，避免误触重复操作
- 用户翻页后会自动回顶，更适合移动端长列表联调
- 用户从详情进入编辑时，会在编辑弹层顶部保留当前用户摘要，减少上下文丢失
- 用户创建表单已补初始密码说明，和列表中的“重置密码”入口形成闭环说明
- 用户列表卡片标题已同时展示姓名和账号，降低重名用户辨识成本
- 当前正在查看详情的用户卡片已支持高亮，便于在列表里快速定位
- 用户卡片标题区域已支持直接打开详情，减少只依赖底部操作按钮
- 用户详情已支持在当前页内切到“上一位 / 下一位”，便于连续验收
- 用户编辑弹层已补“恢复原值”，便于反复试填后快速回退
- 用户列表卡片已展示当前分页内序号，更方便和详情切换顺序对应
- 列表摘要区已补“当前正在查看谁”的反馈，不用一直盯着详情弹层标题
- 用户详情中的上一位/下一位/刷新已和提交态互斥，减少联调时并发误点
- 用户创建表单已补“清空输入”，便于反复试填场景快速回到初始态
- 筛选摘要文案已统一为“字段：值”格式，读起来更稳定
- 删除与重置密码确认弹框已直接带上当前用户姓名/账号，减少误操作焦虑
- 用户详情已支持复制 `userId`，方便直接拿主键去做接口或数据库核对
- 用户详情标题已展示“姓名 / 账号”，连续切换用户时更不容易混淆
- 用户创建表单的状态说明已明确“新建默认建议启用”
- 用户详情中的手机号已支持点击复制，便于验收联系人字段
- 用户详情已支持复制 `unitId`，常用核对字段现在都能直接点取
- 当前查看摘要已补“本页第几位 / 共几位”，和上一位/下一位操作保持一致语义
- 用户编辑弹层标题已补当前姓名，和详情弹层来回切换时更容易对焦
- 用户详情摘要已明确提示“可使用上一位 / 下一位连续浏览”
- 用户详情摘要已补所属单位展示，当前关键信息在摘要区就能先看一眼
- 用户列表中的账号已支持直接点击复制，不必每次都打开详情再取值
- 用户详情操作区已补“回到列表顶部”，和分页回顶保持一致体验
- 用户创建/编辑表单的重置按钮旁已补用途说明，减少误解为删除数据
- 当前查看摘要已补姓名 / 账号联合展示，只看摘要也更容易定位用户
- 用户筛选重置已避免因状态 watcher 与手动查询叠加而重复请求
- 用户搜索关键词已统一在页面与 store 层做 trim，减少前后空格造成的伪筛选
- 用户列表在筛选或删除后若落到空页，会自动回退到最后一个有效页
- 删除当前正在查看的用户后会自动关闭详情，避免保留已失效数据
- 删除/重置密码确认框点击取消后不再提示错误 toast
- 用户创建表单已补单位ID正整数校验，拦截明显脏输入
- 用户页已禁止删除当前登录用户，避免自删后页面进入异常状态
- 用户页已禁止重置当前登录用户密码，降低联调时误改当前账号的风险
- 当前详情用户若因分页或筛选不在当前页列表中，会自动关闭旧详情
- 新增用户弹层会优先带入当前查看用户的单位ID，减少重复录入
- 重置密码成功提示已明确默认密码为 `123456`
- 用户列表标题已显式标记“本人”，更容易识别当前登录账号
- 用户详情标题与摘要已显式标记当前登录用户
- 当前登录用户卡片已补“不可删除 / 不可重置密码”提示
- 编辑当前登录用户时，状态选择与提交都会阻止“停用自己”
- 当前登录用户状态说明已补自停用限制提示
- 编辑用户时若未修改任何内容，保存按钮会禁用
- 编辑表单在未修改时会直接提示“尚未修改内容”
- 创建表单在初始态或恢复初始值后会禁用“清空输入”
- 编辑表单在原始态会禁用“恢复原值”
- 创建/编辑表单存在未保存修改时，关闭前会先做二次确认
- 页面忙碌时会统一禁用“新增用户”入口，减少并发打开弹层
- 页面忙碌时会统一禁用查询与重置筛选，避免重复拉取列表
- 页面忙碌时会禁用卡片上的详情与编辑入口，减少重复详情请求
- 页面忙碌时会禁用详情中的“编辑当前用户”入口
- 提交中会禁用详情关闭按钮，避免保存过程中误关弹层

## 2. 已完成功能

- `GET /api/health` 已可用
- `POST /api/auth/login` 已可用
- `GET /api/auth/me` 已可用
- JWT 鉴权链路已可用
- 超级管理员角色通过 `sys_user_role` 判定
- `/api/auth/me` 已改为返回 `LoginVO`，与登录接口统一包含 `userId / username / realName / superAdmin`
- 单位管理最小闭环已完成
- 前端请求基地址已改为可配置，默认走相对路径 `/api`
- Vite 开发环境已补 `/api` 代理，默认指向 `http://127.0.0.1:8080`
- 组织树新增下级后会自动重新选中新建节点
- 组织树调整上级后会自动重新选中当前节点
- 组织树调整上级时，前端会先排除自己和自己的下级，减少无效提交
- 组织树调整上级时，若目标上级未变化，提交按钮会禁用并阻止发请求
- 组织树调整上级弹层会提示“无可选目标 / 未选择目标 / 目标未变更”等原因
- 组织树上级链节点可直接点击切换，当前节点会在链路中高亮
- 组织树直属下级列表在当前节点切换后会同步高亮对应卡片
- 组织树直属下级与上级链刷新时会显示局部 loading，而不只依赖全页 loading
- 组织树关系区在加载时不会同时显示“暂无数据”空态
- 组织树关系区标题会实时显示当前直属下级数和上级链节点数
- 组织树详情区会优先展示可读的上级信息，而不只显示上级 ID
- 组织树详情区会同步展示当前直属下级数和上级链长度，便于验证关系规模
- 组织树直属下级卡片会明确提示“点击切换”，降低误解为纯展示的可能
- 组织树上级链区域会明确提示“点击切换”，与直属下级交互提示保持一致
- `sys_user` 实体类已包含：
  - `parentUserId`
  - `levelNo`
  - `treePath`
- `UserMapper.xml` 已映射：
  - `parent_user_id`
  - `level_no`
  - `tree_path`
- 组织架构接口已存在：
  - `POST /api/orgtree/create`
  - `POST /api/orgtree/move`
  - `GET /api/orgtree/tree`
  - `GET /api/orgtree/children/{userId}`
  - `GET /api/orgtree/ancestors/{userId}`
- 创建下级用户时已自动计算：
  - `parent_user_id`
  - `level_no`
  - `tree_path`
- 节点移动时已支持：
  - 防止移动到自己下面
  - 防止移动到自己的下级下面
  - 同步更新当前节点及全部子孙的 `tree_path` 和 `level_no`
- 权限范围已开始基于 `tree_path` 查询数据范围
- 组织架构服务层单测已通过：`OrgTreeServiceImplTest`
- 真实数据库已执行组织树字段 SQL：
  - `parent_user_id`
  - `level_no`
  - `tree_path`
- 已修复真实库 `sys_user_id_seq` 与种子数据不一致问题
- 已使用真实 JWT 完成接口验收：
  - 创建一级节点
  - 创建二级节点
  - 创建三级节点
  - 查询整棵树
  - 查询直属下级
  - 查询上级链
  - 移动节点
  - 防循环校验
- 已修复 `OrgTreeController` 中 `children / ancestors` 的 `@PathVariable` 绑定问题

## 3. 未完成功能

- 当前数据库连接已调整为可配置模式：
  - 默认环境使用 `127.0.0.1`
  - `wsl` profile 默认使用 `172.24.16.1`
- 已验证 WSL 到 Windows 宿主机 `172.24.16.1:5432` 可由后端真实连接
- 通用用户管理模块 `user` 的新增/编辑逻辑仍未全面接入树字段维护
- 通用用户管理前端已开始展示树字段，但创建与编辑表单仍未承载这些字段
- 前端组织树页面已改为最小可用页面，覆盖：
  - 组织树展示
  - 新增下级
  - 调整上级
  - 查看直属下级
  - 查看上级链
- 用户管理前端 `frontend/src/components/user/UserFormDialog.vue` 仍使用 `unitId` 录入，不支持从通用用户管理入口选择上级用户
- 通用用户管理前端当前走 `/api/users`，与组织树新增节点链路分离
- 前端页面尚未做真实浏览器操作联调

## 4. 当前任务

用户模块最小前端闭环继续推进，当前已完成一批页面级小闭环并保持可构建。

当前附加任务：
- 保持 WSL / Windows 宿主机 PostgreSQL 连接配置可切换

## 5. 下一步任务

- 做用户页面真实操作联调
- 明确 `/api/users` 与 `/api/orgtree` 的职责边界
- 如需，补数据库序列自检脚本，避免种子数据后再次出现主键序列错位

## 6. 最近修改文件

- `backend/src/main/java/com/example/lecturesystem/modules/auth/controller/AuthController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/auth/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/auth/vo/LoginVO.java`
- `backend/src/test/java/com/example/lecturesystem/modules/auth/service/impl/AuthServiceImplTest.java`
- `frontend/src/utils/request.js`
- `frontend/vite.config.js`
- `frontend/src/views/orgtree/OrgTreeView.vue`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/controller/OrgTreeController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/service/OrgTreeService.java`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/service/impl/OrgTreeServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/mapper/OrgTreeMapper.java`
- `backend/src/main/resources/mapper/orgtree/OrgTreeMapper.xml`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/vo/OrgTreeNodeVO.java`
- `backend/src/main/java/com/example/lecturesystem/modules/user/entity/UserEntity.java`
- `backend/src/main/resources/mapper/user/UserMapper.xml`
- `backend/src/test/java/com/example/lecturesystem/modules/user/service/impl/UserServiceImplTest.java`
- `backend/src/main/resources/mapper/permission/PermissionMapper.xml`
- `backend/src/test/java/com/example/lecturesystem/modules/orgtree/service/impl/OrgTreeServiceImplTest.java`
- `backend/database/init_orgtree_user_fields.sql`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-wsl.yml`
- `backend/src/main/java/com/example/lecturesystem/modules/orgtree/controller/OrgTreeController.java`
- `frontend/src/api/orgtree.js`
- `frontend/src/views/orgtree/OrgTreeView.vue`
- `frontend/src/components/orgtree/OrgTreeNodeItem.vue`
- `database/01_schema.sql`
- `database/02_seed.sql`

## 7. 风险点

- 如果启动时未显式指定 `wsl` profile，WSL 环境会回退到默认 `127.0.0.1`
- `database/01_schema.sql` 仍保留 `sys_org_node` 表定义，容易让后续开发误以为组织树仍依赖旧表
- `frontend/src/api/orgtree.js` 仍使用旧别名路径，虽然当前后端兼容，但后续统一时可能产生偏差
- `backend/src/main/java/com/example/lecturesystem/modules/user/service/impl/UserServiceImpl.java` 并未承担树维护职责，若误走用户通用新增接口，层级关系可能不完整
- `frontend/src/components/user/UserFormDialog.vue` 没有 `parentUserId` 字段，无法从用户管理入口承载“选择上级”
- 当前缺少的是前端浏览器级联调，不再是后端主链路问题
- 本次仅通过单测确认 `/api/auth/me` 返回结构一致，尚未重新做真实登录接口复验
- 本次已通过 `npm run build` 验证前端配置改动可构建，但尚未实际打开浏览器操作组织树页面
- 本轮用户页新增闭环已再次通过 `npm run build`
- 本轮最新一批用户页闭环也已通过 `npm run build`
- 本轮“当前登录用户识别与自停用防护”也已通过 `npm run build`
- 本轮“表单未保存保护”也已通过 `npm run build`
- 本轮“页面并发误触收口”也已通过 `npm run build`

## 当前扫描结论

### 数据库字段进度

- `parent_user_id`：真实数据库已确认
- `level_no`：真实数据库已确认
- `tree_path`：真实数据库已确认

### 组织架构功能进度

- 实体类字段：已完成
- 创建用户自动计算层级：已完成
- 查询组织树：已完成
- 查询下级：已完成
- 查询上级链：已完成
- 防循环关系：已完成
- 修改用户上级并级联更新子孙：已完成

### 当前联调结论

- 后端组织树接口：真实联调已通过
- 数据库实例：宿主机 PostgreSQL 已通过后端真实连接与 SQL 执行验证
- 前端组织树页面：已完成最小闭环代码
- 前端新增/改上级交互：已完成于 `orgtree` 页面
- 前端直属下级/上级链查询：已完成于 `orgtree` 页面

## 最近一批 user 闭环

- 用户列表已补当前页启用/停用人数统计，便于页内验收
- 用户筛选空态已补“当前筛选无结果”提示，区分空库与筛选无命中
- 用户详情摘要已补上一位/下一位用户名预览，减少连续浏览中的确认成本
- 用户表单已补手机号可后补说明
- 用户创建表单已补账号录入建议
- 用户列表摘要已补本页显示人数，验收时不用手动数卡片
- 用户列表中的单位 ID 已支持直接复制
- 用户详情摘要已补当前状态与最近更新时间
- 用户创建表单已补单位 ID 用途提示
- 用户编辑态已补组织树字段只读说明，进一步强调职责边界
- 用户列表摘要已补本页组织树挂接人数，便于快速核对树字段接入情况
- 用户列表中的手机号已支持直接复制
- 用户详情摘要已补岗位与联系方式状态，进入详情后更容易判断信息完整度
- 用户表单已补姓名用途提示
- 用户表单已补岗位名称录入提示
- 用户列表摘要已补本页未挂接组织树人数，挂接/未挂接现在可成对核对
- 当前查看摘要已补状态反馈，列表与详情之间切换时更容易判断启停用
- 用户详情摘要已补关键信息完成度
- 用户编辑摘要已补当前状态
- 用户创建状态说明已明确“后续可编辑调整”
- 用户筛选重置已避免重复请求，减少一次无意义列表刷新
- 用户搜索关键词已统一 trim，筛选摘要与真实查询参数保持一致
- 用户列表空页已支持自动回退，删除或收窄筛选后不再停留空白页
- 删除当前查看用户后会自动清理详情态
- 删除/重置密码取消不再弹错误提示
- 用户创建表单已补单位ID正整数校验
- 用户页已禁止删除当前登录用户
- 用户页已禁止重置当前登录用户密码
- 当前详情用户离开当前页后会自动关闭
- 新增用户弹层已默认带入当前单位ID
- 重置密码成功提示已明确默认密码
- 用户列表已标记当前登录用户
- 用户详情已标记当前登录用户
- 当前登录用户卡片已提示不可删除与重置密码
- 编辑当前登录用户时已禁止停用自己
- 当前登录用户状态提示已补自停用限制
- 编辑用户未改内容时已禁止保存
- 编辑表单已补“尚未修改内容”提示
- 创建表单初始态已禁用清空输入
- 编辑表单初始态已禁用恢复原值
- 表单存在未保存修改时关闭前会二次确认
- 页面忙碌时已禁用新增用户入口
- 页面忙碌时已禁用查询与重置筛选
- 页面忙碌时已禁用卡片详情与编辑入口
- 页面忙碌时已禁用详情中的编辑入口
- 提交中已禁用详情关闭入口

## 当前阶段

- 用户模块仍处于前端验收辅助收尾阶段，已非常接近“待真实浏览器验收”
- 已停止在 `user` 模块继续堆叠低收益摘要/说明类改动，转入真正的联调辅助闭环
- 当前又完成一批偏真实操作链路的小闭环，下一步更适合直接做浏览器实操验收
- 当前又完成一批围绕自操作防误触与上下文延续的小闭环
- 当前又完成一批围绕当前登录用户识别与自停用防护的小闭环
- 当前又完成一批围绕表单未保存保护与无效提交拦截的小闭环
- 当前又完成一批围绕页面并发误触收口的小闭环
- 前端请求层已补统一 `401 / 403` 处理：自动清理本地登录态并跳回登录页，便于真实接口联调暴露鉴权失效问题
- 登录 403 阻塞点已定位为前端请求基地址与 Vite 代理链路不一致，不是后端 `permitAll` 或 CSRF 问题
- 当前已重新对齐为：`request baseURL=/api` + `auth.js=/auth/login` + `vite proxy /api -> http://127.0.0.1:8080` + 后端 `POST /api/auth/login`
- 登录请求已跳过旧 token 注入，避免历史登录态干扰登录接口联调
- 已用 `curl` 强制验证两条链路：
  - `POST http://127.0.0.1:8080/api/auth/login` -> `200`
  - `POST http://127.0.0.1:5173/api/auth/login` -> `200`
- 结论：403 不在后端 Controller、Security permitAll、CSRF，也不在当前 Vite 代理层；当前代码链路已修复，若浏览器仍见 403，最小阻塞点为浏览器端旧脚本缓存
- 已执行前端缓存清理闭环：停止旧 dev server、删除 `node_modules/.vite`、重新启动前端
- 重启后再次验证 `POST http://127.0.0.1:5173/api/auth/login` -> `200 OK`
- 当前阶段已进入“登录链路代码已修复，进入前端缓存清理与重新验证阶段”
