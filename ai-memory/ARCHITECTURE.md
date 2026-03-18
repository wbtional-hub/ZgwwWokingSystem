# ARCHITECTURE

## 1. 技术栈

- 后端：Java 17
- 框架：Spring Boot
- 安全：Spring Security
- 认证：JWT
- 数据访问：MyBatis XML Mapper
- 数据库：PostgreSQL
- 构建：Maven
- 前端：Vue

## 2. 目录结构

### 仓库根目录

- `backend`：后端服务
- `frontend`：前端页面
- `database`：全量初始化脚本
- `ai-memory`：AI 开发记忆系统

### backend 结构

- `src/main/java/com/example/lecturesystem/config`：全局配置
- `src/main/java/com/example/lecturesystem/controller`：通用控制器
- `src/main/java/com/example/lecturesystem/modules/auth`：认证模块
- `src/main/java/com/example/lecturesystem/modules/unit`：单位管理
- `src/main/java/com/example/lecturesystem/modules/user`：用户管理
- `src/main/java/com/example/lecturesystem/modules/orgtree`：组织架构树
- `src/main/java/com/example/lecturesystem/modules/permission`：数据权限
- `src/main/java/com/example/lecturesystem/modules/weeklywork`：周工作
- `src/main/java/com/example/lecturesystem/modules/attendance`：考勤
- `src/main/java/com/example/lecturesystem/modules/statistics`：统计
- `src/main/java/com/example/lecturesystem/modules/param`：参数管理
- `src/main/java/com/example/lecturesystem/modules/operationlog`：操作日志
- `src/main/resources/mapper`：MyBatis XML
- `database`：后端增量修复 SQL

## 3. 数据模型

### 核心表

- `sys_unit`：单位信息
- `sys_user`：用户主表
- `sys_user_role`：用户角色
- `attendance_record`：考勤记录
- `weekly_work`：周工作

### 组织架构模型

当前组织树以 `sys_user` 为核心承载层级关系：

- `parent_user_id`：上级用户 ID
- `level_no`：层级
- `tree_path`：树路径，格式如 `/1/2/3/`

说明：
- 超级管理员权限来自 `sys_user_role`
- 数据权限依赖 `parent_user_id + level_no + tree_path`

## 4. 核心模块

- `auth`：登录、JWT、当前用户信息
- `unit`：单位创建与单位列表
- `user`：用户基础信息维护
- `orgtree`：树形组织结构创建、移动、查询
- `permission`：超级管理员、单位管理员、组织用户的数据范围
- `weeklywork`：周工作草稿、提交、列表
- `attendance`：上下班打卡、记录查询
- `statistics`：团队人数、考勤率、周工作提交率
- `param`：系统参数
- `operationlog`：操作日志

## 5. 设计模式

- 按模块分层：
  - Controller
  - Service
  - Mapper
  - XML
  - DTO
  - Entity
  - VO
- 认证采用无状态 JWT
- 数据权限通过角色与树路径组合实现
- 组织架构采用物化路径模型
- SQL 主要通过 MyBatis XML 明确维护

## 6. API 结构

### 已有主要接口前缀

- `/api/health`
- `/api/auth`
- `/api/unit`
- `/api/users`
- `/api/orgtree`
- `/api/org-tree`
- `/api/attendance`
- `/api/weekly-work`
- `/api/statistics`
- `/api/params`
- `/api/operation-log`

### 组织树相关接口

- `POST /api/orgtree/create`
- `POST /api/orgtree/move`
- `GET /api/orgtree/tree`
- `GET /api/orgtree/children/{userId}`
- `GET /api/orgtree/ancestors/{userId}`

