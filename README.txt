讲师团考勤与周工作管理系统 - 开发框架说明

一、项目目标
本框架根据《讲师团考勤与周工作管理系统_日本规格详细设计说明书_DDS_V5》整理，
用于后续由程序员或 CODEX 继续自动开发。

二、技术栈
1. 后端：Spring Boot 3 + MyBatis + PostgreSQL + JWT
2. 前端：Vue 3 + Vite + Pinia + Vue Router + Axios + Vant
3. 数据库：PostgreSQL
4. 结构原则：每个功能模块单独一个文件夹，互不混写，方便二开和排查

三、当前状态
1. 已生成完整目录结构
2. 已生成核心模块的代码骨架
3. 已为每个功能模块生成“功能说明.txt”
4. 已预留 DTO / VO / Entity / Mapper / Controller / Service / Page / API / Store 文件
5. 当前代码为“可继续开发的脚手架”，不是完整业务实现

四、开发建议顺序
1. common 公共模块
2. auth 登录认证
3. unit 单位管理
4. user 用户管理
5. org-tree 组织架构
6. permission 权限
7. attendance 考勤
8. weekly-work 周工作
9. statistics 统计
10. operation-log 日志审计
11. param 参数管理
12. 前端联调

五、打包时间
2026-03-14 07:17:19
