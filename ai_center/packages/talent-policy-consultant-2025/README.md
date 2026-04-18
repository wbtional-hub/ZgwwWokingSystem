# 人才政策咨询 Skill 材料包

这份材料包当前对应 2 份政策文件：

- `D:\2.售前工作\4.智慧人才\人才政策文件\2025年厦门市人才.docx`
- `D:\2.售前工作\4.智慧人才\人才政策文件\2025年福建省人才.docx`

目标是把文档整理为一套可直接导入当前系统的配置：

- 1 个知识库
- 1 个专业 Skill
- 1 个已配置版本
- 6 个验证题
- 1 个默认专家绑定

## 当前状态

这批材料已经可以支撑“人才政策咨询专家”场景，但更适合按层级拆分组织：

- 厦门市级政策优先
- 福建省级政策作为上位政策补充
- 园区/专项政策建议单列

当前机器上的项目数据库暂未连通，所以这里先提供：

- `skill-package.json`
  统一描述知识库、Skill、版本、验证题、专家绑定
- `import-package.ps1`
  等后端和数据库可用后，一键导入到系统

## 默认设计

知识库：

- 名称：`人才政策知识库（2025汇编）`
- 领域：`人才政策`
- 推荐组织：`厦门市级优先 + 福建省级补充 + 厦门专项政策单列 + 总览材料单列`

Skill：

- 编码：`talent_policy_consultant`
- 名称：`人才政策咨询专家`
- 类型：`CONSULTANT`
- 当前推荐版本：`v1.1.0`

回答原则：

- 只基于知识库内容回答
- 优先回答厦门市级政策
- 福建省政策只能作为上位政策或补充参考
- 严禁把福建省政策直接表述成厦门市现行政策清单
- 必须给出政策依据
- 信息不足时明确说明并提示人工复核
- 不臆造申报条件、金额、时限、材料

## 使用方法

先确保：

- PostgreSQL 已启动
- 后端服务可访问，例如 `http://127.0.0.1:8080/api/health`
- 系统里可以使用管理员账号 `admin / admin123`

建议先导入厦门市文件，再把福建省文件补充进同一知识库。

第一步，导入厦门市文件（默认）：

```powershell
powershell -ExecutionPolicy Bypass -File .\ai_center\packages\talent-policy-consultant-2025\import-package.ps1
```

第二步，再把福建省文件导入到同一知识库作为上位政策补充：

```powershell
powershell -ExecutionPolicy Bypass -File .\ai_center\packages\talent-policy-consultant-2025\import-package.ps1 -DocumentPath "D:\2.售前工作\4.智慧人才\人才政策文件\2025年福建省人才.docx"
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
- 重点检查厦门市材料和福建省材料是否作为同一知识库中的不同文档存在
- 再在 Skills 页面检查版本、绑定和验证题
- 若系统内已配置 AI Provider，可手动执行一次“技能验证”
- 最后在 `AI 工作台` 中试问 3 到 5 个真实政策问题
- 回归问题建议至少包含：
  - `请问厦门现在有哪些人才政策`
  - `福建省高层次人才认定条件是什么`
  - `厦门和福建省人才政策有什么区别`
