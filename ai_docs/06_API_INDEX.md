已完成接口

GET /api/health  
POST /api/auth/login  
GET /api/auth/me  
POST /api/unit/create  
GET /api/unit/list  
POST /api/orgtree/create  
GET /api/orgtree/tree  
GET /api/orgtree/children/{userId}  
GET /api/users  
GET /api/users/{userId}  
POST /api/users  
PUT /api/users/{userId}  
DELETE /api/users/{userId}  
POST /api/user/reset-password/{userId}

当前用户模块前端已覆盖的接口入口

GET /api/users：列表查询、筛选、空态恢复、结果摘要展示、最后同步时间反馈、页内定位辅助、当前查看反馈、确认信息增强、列表快捷复制、当前页启停用统计、筛选空结果提示、本页显示人数、列表单位ID复制、当前页组织树挂接人数、当前页未挂接人数、列表手机号复制、当前查看状态反馈  
GET /api/users/{userId}：详情弹层、编辑弹层预加载、保存后详情回看、详情主动刷新、详情字段复制辅助、详情时间核对、页内连续浏览、详情操作状态互斥、详情标题增强、连续浏览提示、回顶辅助、详情摘要状态与更新时间前置展示、详情摘要岗位与联系方式状态展示、详情关键信息完成度展示  
PUT /api/users/{userId}：编辑保存、保存后详情刷新、详情邻近用户预览辅助  
DELETE /api/users/{userId}：列表删除  
POST /api/user/reset-password/{userId}：列表与详情弹层快捷重置密码  

当前用户模块表单侧已补充的验收辅助

POST /api/users：创建表单已补账号录入提示、手机号可后补说明、单位ID录入提示、编辑态组织树只读说明、姓名用途提示、岗位名称录入提示、创建状态默认值说明、编辑摘要状态展示
