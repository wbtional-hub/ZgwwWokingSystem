系统模块

auth 登录认证  
unit 单位管理  
user 用户管理  
orgtree 组织架构  
permission 权限控制  
weeklywork 周工作  
attendance 考勤  
statistics 统计  
operationlog 审计日志  
param 系统参数  

设计原则

1 Controller 不写业务逻辑  
2 Service 实现业务逻辑  
3 Mapper 负责数据库访问  
4 所有查询必须考虑数据权限  
