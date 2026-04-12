# 人才政策咨询小程序 MVP

这个目录是基于现有后端接口补出的原生微信小程序首版工程，目标是尽快打通：

- 微信授权登录
- 移动工作台
- 政策咨询
- 考勤签到
- 周报查看与提交
- 个人中心

## 启动前要做的事

1. 打开 [config/env.js](./config/env.js) 把 `apiBaseUrl` 改成小程序可访问的后端地址。
2. 微信公众平台里把后端域名加入“小程序服务器域名”白名单。
3. 后端参数里配置好：
   - `wechatMiniAppId`
   - `wechatMiniAppSecret`
4. 在系统用户里维护好微信绑定信息 `openId / unionId`。
