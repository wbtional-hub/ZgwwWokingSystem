# 打卡记录筛选与导出联调说明

## 1. 打卡状态枚举

- `CHECK_IN_SUCCESS`：上班打卡成功
- `CHECK_OUT_SUCCESS`：下班打卡成功
- `LOCATION_NOT_CONFIGURED`：所属单位未配置打卡点
- `LOCATION_DISABLED`：所属单位打卡点未启用
- `LOCATION_REQUIRED`：未提供定位信息或定位失败
- `OUT_OF_RANGE`：当前位置超出单位打卡点半径范围
- `ALREADY_FINISHED`：当日打卡已完成
- `LOCATION_NOT_BOUND`：当前账号未绑定单位
- `LOCATION_READY`：已查询到可用打卡点，可发起打卡

前后端统一使用：

- 后端：`backend/src/main/java/com/example/lecturesystem/modules/attendance/support/AttendanceCheckInStatus.java`
- 前端：`frontend/src/constants/attendance.js`

## 2. 查询筛选参数

打卡记录查询接口：

- `POST /api/attendance/query`

新增筛选参数：

- `checkInStatus`

示例：

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

- 不传 `checkInStatus` 时，返回全部记录
- 传入状态值时，仅返回该状态的记录
- 列表与导出共用同一筛选口径

## 3. 导出字段

当前前端导出为 CSV，导出口径与当前列表完全一致。

导出字段包括：

- `id`
- `userId`
- `username`
- `realName`
- `unitName`
- `attendanceDate`
- `checkInTime`
- `checkOutTime`
- `check_in_status`
- `check_in_status_label`
- `distance_meters`
- `check_in_fail_reason`
- `latitude`
- `longitude`
- `checkInAddress`
- `checkOutAddress`

## 4. 异常场景说明

- `LOCATION_NOT_CONFIGURED`：员工所属单位尚未配置主打卡点，应前往“单位管理 > 打卡点设置”
- `LOCATION_DISABLED`：单位已配置打卡点，但状态未启用
- `LOCATION_REQUIRED`：浏览器定位失败、被拒绝，或接口请求未带经纬度
- `OUT_OF_RANGE`：当前位置与单位主打卡点距离大于 `radius_meters`
- `ALREADY_FINISHED`：当天已完成上下班打卡，不再重复生成成功记录

## 5. 页面联调说明

- 员工/管理端考勤页支持直接按“结果状态”筛选
- 列表中的状态标签与导出中的状态文案使用同一套映射
- 失败类状态会在列表中额外显示“异常”标识，便于快速验收异常记录
