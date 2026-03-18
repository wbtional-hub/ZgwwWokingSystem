# 讲师团日志管理系统

## 项目说明

当前版本已经从“可用”推进到“最小可交付”阶段，包含用户、组织、周报、考勤、评分、统计、日志和最小权限控制。

## 环境要求

- Java 17
- Maven 3.9+
- Node.js 20
- PostgreSQL 14+

## 环境配置

后端读取 [application.yml](/mnt/d/20.develop64/ZgwwWokingSystem/backend/src/main/resources/application.yml)：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `app.jwt.secret`

前端默认通过 Vite 启动，接口前缀走 `/api` 代理。

## 数据初始化

推荐使用：

```sql
\i database/00_init_delivery.sql
```

该脚本会依次执行基础表、种子数据、考勤升级、评分表、状态字段、操作日志、RBAC Lite 和单位兼容表。

## 启动方式

后端：

```bash
cd backend
mvn -Dspring-boot.run.profiles=wsl spring-boot:run
```

前端：

```bash
cd frontend
npm run dev
```

## 演示路径

1. 登录：`admin / admin123`
2. 用户管理：新增、编辑、启停、删除
3. 周报管理：新建、提交、审核
4. 考勤管理：打卡、补录、删除
5. 工作评分：计算评分、查看排行榜、导出通报
6. 统计分析：看概览、组织排名、红黄牌、趋势
7. 操作日志：查看登录、删除、导出、权限拒绝等关键日志

## 当前交付边界

- 权限系统为 `ADMIN / USER` 两角色最小实现
- 日志系统覆盖关键操作，不是全链路审计
- 统计页采用表格，不含图表大屏
- 评分规则为最小业务规则，不包含复杂人工干预
