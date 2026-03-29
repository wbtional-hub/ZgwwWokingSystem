# AI Monthly Report Progress - 2026-03-29

## 本轮完成内容
- 新增 AI 咨询月度报表接口：`POST /api/agent/session/monthly-report`
- 月报统计口径按年份汇总，支持按用户、技能、关键字筛选
- 月报输出内容包括：
  - 年度会话总数
  - 年度消息总数
  - 活跃月份数
  - 月均会话数
  - TOP 技能
  - TOP 知识库
  - 月度趋势
  - 技能排行
  - 知识库排行
  - 用户排行
- 新增前端页面：`AIConsultationMonthlyReportView.vue`
- 新增导航入口：`/ai-monthly-report`
- 重写路由和布局文件为干净 UTF-8 版本，新增“月度报表”菜单项

## 涉及文件
- `backend/src/main/java/com/example/lecturesystem/modules/agent/dto/AgentSessionQueryRequest.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/controller/AgentController.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/service/AgentService.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/service/impl/AgentServiceImpl.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/mapper/AgentSessionMapper.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/vo/AgentMonthlySummaryVO.java`
- `backend/src/main/java/com/example/lecturesystem/modules/agent/vo/AgentMonthlyReportVO.java`
- `backend/src/main/resources/mapper/agent/AgentSessionMapper.xml`
- `frontend/src/api/agent.js`
- `frontend/src/views/agent/AIConsultationMonthlyReportView.vue`
- `frontend/src/router/index.js`
- `frontend/src/layouts/AppLayout.vue`

## 验证结果
- 后端：`mvn -q -DskipTests compile` 通过
- 前端：`npm.cmd run build` 通过

## 当前状态
- 咨询能力已形成三层展示：
  - AI 工作台
  - 咨询台账
  - 月度报表
- 月度报表当前是查询页，不含导出和截图能力

## 后续建议
- 增加月报导出（Excel / PDF）
- 增加年度对比和同比环比
- 增加知识库命中率、专家使用率等经营指标