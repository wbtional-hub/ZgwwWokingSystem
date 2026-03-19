模块：attendance-abnormal

说明：
负责签到管理中的异常用户监控、风险分析、趋势判断、原因分布、用户画像与简单预警输出，主要服务于管理看板与领导汇报场景。

一、统计范围口径
1. 数据范围：复用 `attendance` 模块当前登录用户的数据权限与查询时间范围。
2. 异常口径：`check_in_result` 不属于 `CHECK_IN_SUCCESS / CHECK_OUT_SUCCESS` 的记录，均视为异常。
3. 榜单口径：异常用户 Top10 按异常次数倒序，其次按异常率倒序。
4. 趋势口径：最近7天与前7天均按“异常次数”聚合比较，不按自然周切分。
5. 原因口径：优先取 `check_in_fail_reason`，为空时回退到 `check_in_result`。

二、核心字段说明
1. `abnormalCount`
   含义：当前统计范围内的异常记录次数。
2. `totalCount`
   含义：当前统计范围内的总打卡记录数。
3. `abnormalRate`
   含义：`abnormalCount / totalCount * 100`，保留 1 位小数。
4. `riskScore`
   含义：异常风险分，基于异常次数与异常率计算，范围 0-100。
   当前公式：`异常率 * 0.5 + 异常次数 * 10`，最终限制在 `0-100`。
5. `riskLevel`
   含义：风险等级。
   规则：
   `HIGH`：`riskScore >= 75`
   `MEDIUM`：`45 <= riskScore < 75`
   `LOW`：`riskScore < 45`
6. `recent7DayAbnormalCount`
   含义：最近7天异常次数。
7. `previous7DayAbnormalCount`
   含义：前7天异常次数。
8. `trendDirection`
   含义：异常趋势方向。
   规则：
   `RISING`：最近7天 > 前7天
   `FALLING`：最近7天 < 前7天
   `STABLE`：最近7天 = 前7天
9. `mainReasonKey`
   含义：当前用户异常中出现次数最多的原因原始值。
10. `mainReasonLabel`
   含义：面向页面展示的主要异常原因中文名称。
11. `mainReasonTag`
   含义：原因标签。
   当前标签：
   `配置问题`
   `范围问题`
   `定位问题`
   `组织问题`
   `重复打卡`
   `其他`
12. `reasonDistributions`
   含义：当前统计范围内的异常原因 Top 分布列表。
   字段：
   `reasonKey / reasonLabel / reasonTag / count / rate`
13. `topLocation`
   含义：异常记录中出现次数最多的打卡地点。
14. `topLocationCount`
   含义：最高频地点出现次数。
15. `locationConcentrationRate`
   含义：`topLocationCount / 异常总次数 * 100`，反映地点集中度。
16. `morningAbnormalCount / afternoonAbnormalCount / eveningAbnormalCount`
   含义：按时间段统计的异常次数。
   当前切分：
   上午：`< 12:00`
   下午：`12:00 - 17:59`
   晚间：`>= 18:00`
17. `peakTimeSlot`
   含义：异常最高发时段。
18. `alertTriggered`
   含义：是否命中简单预警规则。
19. `alertRuleText`
   含义：命中的预警规则说明文本。

三、预警规则
1. 综合风险高
   触发条件：`riskLevel = HIGH`
2. 近7天异常偏高
   触发条件：`recent7DayAbnormalCount >= 3`
3. 持续上升且异常率高
   触发条件：`trendDirection = RISING` 且 `recent7DayAbnormalCount >= 2` 且 `abnormalRate >= 70`

四、前端展示层级
1. 异常用户 Top10
   展示用户排行、异常次数、异常率、风险等级、趋势、主要异常原因、预警标识。
2. 原因分布
   展示当前统计范围下的失败原因 Top 分布与标签。
3. 异常用户趋势
   展示按天异常次数、总打卡次数、异常率。
4. 用户摘要
   展示风险、趋势、原因、地点集中度、时间段画像、预警命中信息。

五、验收建议
1. 核对同一用户在“榜单卡片”和“用户摘要”中的风险等级、趋势方向、主要异常原因是否一致。
2. 核对无异常用户、无原因分布、无趋势数据三类空态是否明确可读。
3. 核对导出榜单中的风险、趋势、原因、预警字段与页面是否一致。

六、当前提交边界建议（闭环 477-481）
1. 后端分析能力
   文件：`backend/src/main/java/com/example/lecturesystem/modules/attendance/mapper/AttendanceMapper.java`、`backend/src/main/resources/mapper/attendance/AttendanceMapper.xml`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImpl.java`
2. 后端 VO
   文件：`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalMonitorVO.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalUserRankVO.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalUserSummaryVO.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalReasonDistributionVO.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalTrendComparisonVO.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalUserBehaviorPointVO.java`
3. 前端最小展示
   文件：`frontend/src/views/attendance/AttendanceCheckInView.vue`
4. 测试适配
   文件：`backend/src/test/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImplTest.java`、`backend/src/test/java/com/example/lecturesystem/modules/statistics/service/impl/StatisticsServiceImplTest.java`
5. 文档与进度
   文件：`ai_docs/modules/module_attendance_abnormal.md`、`ai_center/progress.md`、`ai_center/tasks.md`

七、提交顺序建议
1. 第一提交：后端分析能力
   说明：先提交 Mapper / SQL / Service，固定异常分析口径。
2. 第二提交：VO 与测试适配
   说明：再提交异常分析 VO、`AttendanceServiceImplTest`、`StatisticsServiceImplTest`，确保接口结构与测试同步。
3. 第三提交：前端最小展示
   说明：最后提交 `AttendanceCheckInView.vue`，把风险、趋势、原因、画像、预警展示接到页面。
4. 第四提交：文档与进度
   说明：最后单独提交 `module_attendance_abnormal.md`、`progress.md`、`tasks.md`，方便回溯演示口径。

八、演示顺序建议
1. 先看异常用户 Top10
   重点：异常次数、异常率、风险等级、预警人数。
2. 再看异常原因分布
   重点：当前阶段最主要的异常成因和标签。
3. 再点选单个异常用户
   重点：最近7天趋势、是否上升、用户摘要。
4. 最后看用户画像
   重点：地点集中度、时间段分析、是否命中预警规则。

九、分组提交边界确认（闭环 477-482）
1. 第一组：后端分析能力
   应包含：
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/mapper/AttendanceMapper.java`
   `backend/src/main/resources/mapper/attendance/AttendanceMapper.xml`
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImpl.java`
   提交说明建议：
   `feat(attendance): add abnormal analysis metrics and alert rules`
2. 第二组：VO 与测试适配
   应包含：
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalMonitorVO.java`
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalUserRankVO.java`
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalUserSummaryVO.java`
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalReasonDistributionVO.java`
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalTrendComparisonVO.java`
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendanceAbnormalUserBehaviorPointVO.java`
   `backend/src/test/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImplTest.java`
   `backend/src/test/java/com/example/lecturesystem/modules/statistics/service/impl/StatisticsServiceImplTest.java`
   提交说明建议：
   `test(attendance): align abnormal analysis vo and test stubs`
3. 第三组：前端最小展示
   应包含：
   `frontend/src/views/attendance/AttendanceCheckInView.vue`
   提交说明建议：
   `feat(attendance-ui): surface abnormal risk trend reason portrait and alert`
4. 第四组：文档与进度
   应包含：
   `ai_docs/modules/module_attendance_abnormal.md`
   `ai_center/progress.md`
   `ai_center/tasks.md`
   提交说明建议：
   `docs(attendance): document abnormal analysis delivery and commit plan`

十、当前工作区排除项确认
1. 以下文件属于本轮异常分析提交之外的既有改动，不应混入：
   `frontend/src/utils/amap.js`
   `frontend/src/views/unit/UnitListView.vue`
   `backend/pom.xml`
   `backend/src/main/java/com/example/lecturesystem/common/GlobalExceptionHandler.java`
   `backend/src/main/java/com/example/lecturesystem/modules/attendance/dto/AttendanceQueryRequest.java`
   `backend/src/main/resources/application.yml`
   `ai_docs/modules/module_attendance.md`
2. `backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendancePageVO.java` 仍是分页闭环相关文件，也不应并入异常分析提交。
3. 若执行 `git add`，应按上方四组显式挑文件，不要直接使用全量暂存。
