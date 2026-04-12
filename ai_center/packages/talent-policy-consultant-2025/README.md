# 人才政策咨询 Skill 材料包

这份材料包对应用户提供的文档：

- `D:\2.售前工作\4.智慧人才\99.人才推广\人才政策汇编.docx`

目标是把文档整理为一套可直接导入当前系统的配置：

- 1 个知识库
- 1 个专业 Skill
- 1 个已配置版本
- 6 个验证题
- 1 个默认专家绑定

## 当前状态

文档已经确认可用于“人才政策咨询专家”场景。

当前机器上的项目数据库暂未连通，所以还不能直接把数据真正落进运行中的系统库。
为避免重复工作，这里先提供：

- `skill-package.json`
  统一描述知识库、Skill、版本、验证题、专家绑定
- `import-package.ps1`
  等后端和数据库可用后，一键导入到系统

## 默认设计

知识库：

- 名称：`人才政策知识库（2025汇编）`
- 领域：`人才政策`

Skill：

- 编码：`talent_policy_consultant`
- 名称：`人才政策咨询专家`
- 类型：`CONSULTANT`

回答原则：

- 只基于知识库内容回答
- 结论先行，依据后置
- 必须给出政策依据
- 信息不足时明确说明并提示人工复核
- 不臆造申报条件、金额、时限、材料

## 使用方法

先确保：

- PostgreSQL 已启动
- 后端服务可访问，例如 `http://127.0.0.1:8080/api/health`
- 系统里可以使用管理员账号 `admin / admin123`

然后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\ai_center\packages\talent-policy-consultant-2025\import-package.ps1
```

如果后端不是默认地址：

```powershell
powershell -ExecutionPolicy Bypass -File .\ai_center\packages\talent-policy-consultant-2025\import-package.ps1 -BackendBaseUrl "http://127.0.0.1:18080/api"
```

如果要指定别的文档路径：

```powershell
powershell -ExecutionPolicy Bypass -File .\ai_center\packages\talent-policy-consultant-2025\import-package.ps1 -DocumentPath "D:\你的文档.docx"
```

## 导入完成后建议

- 先在知识库页面确认文档切片是否正常
- 再在 Skills 页面检查版本、绑定和验证题
- 若系统内已配置 AI Provider，可手动执行一次“技能验证”
- 最后在 `AI 工作台` 中试问 3 到 5 个真实政策问题
