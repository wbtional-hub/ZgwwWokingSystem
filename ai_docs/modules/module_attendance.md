模块：attendance

说明：
负责上下班打卡、考勤记录查询、补录、异常监控与趋势查看。

验收前已完成能力：
1. 正常定位打卡，支持上班打卡与下班补齐。
2. 当前打卡点卡片展示单位打卡点名称、地址、经纬度、半径与启用状态。
3. 本次打卡结果区展示结果、动作、距离、允许打卡、失败原因。
4. 考勤查询支持日期范围、快捷时间范围、状态筛选、分页、导出。
5. 考勤补录支持按姓名或手机号搜索用户，并自动回填必要信息。
6. 异常监控与趋势图继续复用当前筛选日期范围。
7. 地图打卡点配置支持默认中心点、默认缩放、默认半径、搜索、拖拽、手工改经纬度与半径。
8. 打卡异常链路已收口 UTF-8 中文提示，避免前端乱码和误报“定位失败”。

真实验收场景清单：
1. 正常定位并成功打卡
   预期：浏览器定位成功后可完成打卡；结果区显示成功动作；列表自动回刷；当前打卡点卡片保持正确。
2. 浏览器定位失败
   预期：提示“当前环境不支持定位”“定位权限被拒绝”或“定位失败，请重试”；不误报后端异常；页面不乱码。
3. 单位打卡点未配置、未启用或配置不完整
   预期：结果区与提示文案准确反映 `LOCATION_NOT_CONFIGURED`、`LOCATION_DISABLED` 等真实状态；列表与当前打卡点卡片同步回刷。
4. 超出单位打卡范围
   预期：结果区显示失败、允许打卡为“否”、原因包含超出范围；列表写入真实失败记录。
5. 后端普通异常或网络异常
   预期：前端不乱码，不误提示成定位失败；结果区失败原因与 Network / 后端日志一致；如存在服务端响应，列表和当前打卡点卡片仍会回刷。
6. 连续多次打开地图弹窗
   预期：底图稳定显示；默认中心点、默认缩放、默认半径稳定；搜索、拖拽、手工修改经纬度/半径后 marker、半径圆、标签状态一致。

打卡三类分支一致性结论：
1. 打卡成功
   现状：会写入结果区，并统一执行列表与当前打卡点卡片回刷。
2. 业务失败
   现状：后端正常返回 `success=false` 时，结果区展示真实状态和原因，并统一执行列表与当前打卡点卡片回刷。
3. 接口异常
   现状：存在后端响应时，会优先展示真实中文 message，并反推出更接近业务语义的状态值，同时统一执行列表与当前打卡点卡片回刷；仅浏览器定位失败或纯网络异常时不会伪造服务端结果。

当前工作区未提交改动梳理（截至 2026-03-19）：
1. 考勤分页与日期范围校验
   文件：`backend/src/main/java/com/example/lecturesystem/modules/attendance/dto/AttendanceQueryRequest.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendancePageVO.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImpl.java`、`backend/src/main/resources/mapper/attendance/AttendanceMapper.xml`、`backend/src/test/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImplTest.java`
2. 打卡异常与 UTF-8 编码修复
   文件：`backend/pom.xml`、`backend/src/main/java/com/example/lecturesystem/common/GlobalExceptionHandler.java`、`backend/src/main/resources/application.yml`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImpl.java`、`frontend/src/views/attendance/AttendanceCheckInView.vue`
3. 地图定位打卡稳定性专项
   文件：`frontend/src/utils/amap.js`、`frontend/src/views/unit/UnitListView.vue`
4. 进度记录
   文件：`ai_center/progress.md`、`ai_center/tasks.md`

当前验收建议：
1. 先按“成功打卡 -> 业务失败 -> 接口异常”三条链路走一遍。
2. 再补走地图弹窗“首次打开 -> 连续开关 -> 搜索/拖拽/手工修改 -> 保存”的稳定性清单。
3. 验收通过后，再统一整理本轮 attendance 相关改动并提交，避免把分页、地图、编码、验收说明混成一批无说明提交。

最终验收结果（2026-03-19）：
1. 真实浏览器人工验收
   结果：当前 CLI 环境未接入真实浏览器人工操作能力，仓库内也没有 Playwright、Cypress、Puppeteer 等现成自动化脚手架，因此本轮无法诚实标记为“已逐项完成真实浏览器验收”。
2. 代码层主链路复核
   结果：已复核打卡成功、业务失败、接口异常三类分支；其中成功分支、业务失败分支，以及“存在服务端响应”的接口异常分支，都会统一回刷结果区、考勤列表和当前打卡点卡片对应数据链路。
3. 可执行验证
   结果：保留 `AttendanceServiceImplTest` 与前端 `npm run build` 作为本轮可在当前环境直接完成的主链路验证依据。
4. 验收结论
   结果：签到模块代码层主链路当前未发现新增阻塞问题；“真实浏览器人工验收已完成”这一结论仍待后续在可操作浏览器环境中逐项勾验后再确认。

提交边界建议（按当前 4 组改动整理）：
1. 第一组：考勤分页与日期范围校验
   建议文件：`backend/src/main/java/com/example/lecturesystem/modules/attendance/dto/AttendanceQueryRequest.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/vo/AttendancePageVO.java`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImpl.java`、`backend/src/main/resources/mapper/attendance/AttendanceMapper.xml`、`backend/src/test/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImplTest.java`
2. 第二组：打卡异常与 UTF-8 编码修复
   建议文件：`backend/pom.xml`、`backend/src/main/java/com/example/lecturesystem/common/GlobalExceptionHandler.java`、`backend/src/main/resources/application.yml`、`backend/src/main/java/com/example/lecturesystem/modules/attendance/service/impl/AttendanceServiceImpl.java`、`frontend/src/views/attendance/AttendanceCheckInView.vue`
3. 第三组：地图定位打卡稳定性专项
   建议文件：`frontend/src/utils/amap.js`、`frontend/src/views/unit/UnitListView.vue`
4. 第四组：验收与进度文档
   建议文件：`ai_docs/modules/module_attendance.md`、`ai_center/progress.md`、`ai_center/tasks.md`
