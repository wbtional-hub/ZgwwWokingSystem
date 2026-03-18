# 单位打卡点联调说明

## 功能范围

当前仅覆盖“单位管理 -> 打卡点设置”能力：

- 查询某单位主打卡点
- 保存或更新某单位主打卡点
- 地图选点与地址回填

当前阶段不包含：

- 员工打卡页面
- GPS 打卡范围校验
- 考勤业务扩展逻辑

## 前端地图配置

前端环境变量示例见 [frontend/.env.example](/mnt/d/20.develop64/ZgwwWokingSystem/frontend/.env.example)。

需要的变量：

- `VITE_AMAP_KEY`
- `VITE_AMAP_SECURITY_JS_CODE`

说明：

- 配置 `VITE_AMAP_KEY` 后，单位管理页会进入正式地图 SDK 模式
- 未配置 `VITE_AMAP_KEY` 时，页面会自动回退到轻量选点模式
- 轻量选点模式不影响经纬度回填和打卡点保存

## 接口 1：查询单位打卡点

- 方法：`GET`
- 路径：`/api/unit/{id}/attendance-location`

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 11,
    "unitId": 1,
    "locationName": "平台管理单位主打卡点",
    "latitude": 39.90923,
    "longitude": 116.397428,
    "radiusMeters": 200,
    "address": "北京市东城区东华门街道",
    "status": 1,
    "createTime": "2026-03-18T14:20:00",
    "updateTime": "2026-03-18T15:20:00"
  }
}
```

## 接口 2：保存单位打卡点

- 方法：`POST`
- 路径：`/api/unit/attendance-location/save`

请求示例：

```json
{
  "unitId": 1,
  "locationName": "平台管理单位主打卡点",
  "longitude": 116.397428,
  "latitude": 39.90923,
  "radiusMeters": 200,
  "address": "北京市东城区东华门街道",
  "status": 1
}
```

说明：

- 当前按“一个单位一个主打卡点”实现
- 若该单位已有记录，则更新
- 若该单位没有记录，则新增

## 正式地图模式与轻量模式差异

正式地图模式：

- 依赖 `VITE_AMAP_KEY`
- 支持正式地图底图
- 支持地图点击选点
- 支持逆地理地址回填
- 优先走地图 SDK 定位

轻量选点模式：

- 不依赖地图 Key
- 使用页面内轻量选点区域
- 仍支持浏览器定位
- 仍支持经纬度与地址回填
- 仍可正常保存打卡点
