# 打卡统计与异常监控联调说明

## 1. 统计接口

- `POST /api/attendance/summary`

请求体与考勤列表查询复用同一组筛选参数：

```json
{
  "keywords": "张三",
  "unitName": "第一支部",
  "checkInStatus": "OUT_OF_RANGE",
  "dateFrom": "2026-03-01",
  "dateTo": "2026-03-18"
}
```

说明：

- 非超级管理员会自动按 `currentUser.tree_path` 注入数据范围
- 超级管理员可直接看全量统计，或通过传入 `treePathPrefix` 收敛到某个子树范围
- 统计口径与 `/api/attendance/query` 列表接口保持一致

## 2. 返回字段

```json
{
  "totalCount": 120,
  "successCount": 92,
  "abnormalCount": 28,
  "statusCounts": [
    { "checkInStatus": "CHECK_IN_SUCCESS", "count": 60 },
    { "checkInStatus": "CHECK_OUT_SUCCESS", "count": 32 },
    { "checkInStatus": "OUT_OF_RANGE", "count": 10 },
    { "checkInStatus": "LOCATION_NOT_CONFIGURED", "count": 8 }
  ]
}
```

字段含义：

- `totalCount`：当前筛选范围内的打卡记录总数
- `successCount`：成功记录数，当前合并 `CHECK_IN_SUCCESS` 与 `CHECK_OUT_SUCCESS`
- `abnormalCount`：异常记录数，按 `totalCount - successCount` 计算
- `statusCounts`：各打卡状态数量分布

## 3. 状态口径

当前统计复用已有打卡状态模型：

- `CHECK_IN_SUCCESS`
- `CHECK_OUT_SUCCESS`
- `OUT_OF_RANGE`
- `LOCATION_NOT_CONFIGURED`
- `LOCATION_DISABLED`
- `LOCATION_REQUIRED`
- `ALREADY_FINISHED`
- `LOCATION_NOT_BOUND`
- `LOCATION_READY`

前后端统一状态常量位置：

- 后端：`backend/src/main/java/com/example/lecturesystem/modules/attendance/support/AttendanceCheckInStatus.java`
- 前端：`frontend/src/constants/attendance.js`

## 4. 前端展示口径

当前考勤页已接入：

- 总记录数卡片
- 成功数卡片
- 异常数卡片
- 异常率
- 各状态数量标签

说明：

- 不引入复杂图表
- 统计卡片与异常明细列表共用同一套筛选条件
