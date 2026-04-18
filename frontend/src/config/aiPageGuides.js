export const aiPageGuides = {
  aiMap: {
    feature: '这是 AI 全链路导航页，用来按流程查看当前链路是否已打通。',
    roles: '管理员、AI 运营、联调人员',
    steps: ['先看链路状态', '再点当前断点页面', '完成本页配置或排查', '按建议进入下一页'],
    next: { label: '从 AI接入区 开始配置', path: '/ai-provider' },
    faqs: ['节点显示“有风险”时，优先去日志中台看链路日志。', '普通用户若无权限，节点可见但进入后可能被拦到 403。', '本页重点是导航，不承载复杂配置。']
  },
  aiProvider: {
    feature: '配置智普等 AI Provider、模型和连通性，保证后续 AI 页面有可用模型可调。',
    roles: '管理员',
    steps: ['新增或编辑 Provider', '填写 Base URL、Token、默认模型', '先做连通测试', '成功后进入 AI权限配置'],
    next: { label: '去 AI权限配置', path: '/ai-permissions' },
    faqs: ['Provider 未启用或测试失败时，手机端咨询会走知识库兜底。', '默认模型为空时，AI 调用会直接失败。', '修改 Token 后建议立刻重新测试。']
  },
  aiPermissions: {
    feature: '给用户分配 AI、知识库和 Skills 的使用权限，决定谁能走完整 AI 链路。',
    roles: '管理员',
    steps: ['先选授权对象', '再配 AI 能力', '再配知识库和 Skills', '保存后去知识库或 Skills 验证'],
    next: { label: '去 知识库中心', path: '/knowledge' },
    faqs: ['只有开通 canUseAgent 的用户才能进入问答链路。', '用户能进页面但问答失败，通常是 Skill 或知识库权限没配齐。', '授权刚改完时，建议用户刷新页面重新取当前权限。']
  },
  knowledge: {
    feature: '维护知识库和文档内容，给 AI 工作台与手机端咨询提供可引用的知识来源。',
    roles: '管理员、知识库维护人',
    steps: ['先选或新建知识库', '导入文档或网页快照', '确认知识库启用状态', '再到 Skills 绑定知识库'],
    next: { label: '去 Skills中心', path: '/skills' },
    faqs: ['知识库没启用时，Skill 即使发布也拿不到有效上下文。', '知识块搜索不到时，优先检查文档是否导入成功。', '切换当前知识库后，建议重新发起新会话验证。']
  },
  skills: {
    feature: '配置、训练、发布 Skill，并把 Skill 绑定到知识库和模型。',
    roles: '管理员、Skill 训练人员',
    steps: ['先选 Skill 或新建 Skill', '配置版本与 Prompt', '绑定知识库并发布版本', '再去 AI工作台或手机端验证'],
    next: { label: '去 AI工作台', path: '/ai-workbench' },
    faqs: ['未发布版本的 Skill 不能进入正式问答链路。', 'Skill 没绑定知识库时，只能依赖模型自身能力。', '手机端政策咨询默认优先使用政策咨询 Skill。']
  },
  aiWorkbench: {
    feature: '按 Skill 发起问答和分析，是后台验证 AI 主链路是否可用的核心工作台。',
    roles: '已授权用户、管理员',
    steps: ['先选 Skill', '创建或进入会话', '发起问题验证答案', '再去手机端或台账看结果承接'],
    next: { label: '去 手机端政策咨询', path: '/policy-consultant' },
    faqs: ['会话创建失败通常是 AI/Skill/知识库权限没配齐。', '无 AI Provider 时会退回到知识库兜底回答。', '如果命中引用为空，优先检查知识库内容是否匹配问题。']
  },
  policyConsultant: {
    feature: '面向最终用户的极简问答页，用户只管提问，系统自动选 Skill 和知识来源。',
    roles: '普通咨询用户、运营演示人员',
    steps: ['直接输入问题', '需要时用 @Skill 指定能力', '发送后查看回答与来源提示', '继续追问或去台账回看'],
    next: { label: '去 AI结果回流', path: '/ai-result-flow' },
    faqs: ['输入 @ 时会给出 Skill 提示，不用死记名称。', '不写 @ 时会优先走默认政策咨询 Skill 和知识库。', '若返回兜底答案，说明 AI Provider 可能不可用。']
  },
  aiResultFlow: {
    feature: '查看手机端与工作台的 AI 结果如何通过服务层回流到台账、月报和日志中台。',
    roles: '管理员、AI 运营、排障人员',
    steps: ['先看当前来源场景是否入库', '再看台账是否可查', '再看月报是否统计到', '最后去日志中台排查断点'],
    next: { label: '去 咨询台账', path: '/ai-ledger' },
    faqs: ['本页先做服务闭环，不要求一开始就有重页面。', '来源场景缺失时，台账和月报会很难定位移动端数据。', '排查失败时请联动日志中台的 AI_CHAIN 日志。']
  },
  aiLedger: {
    feature: '按会话维度查看咨询记录、来源场景、Skill、知识库和消息规模。',
    roles: '管理员、AI 运营',
    steps: ['先按来源场景筛选', '再看 Skill 与知识库命中', '按需要归档会话', '异常时跳到日志中台'],
    next: { label: '去 月度报表', path: '/ai-monthly-report' },
    faqs: ['手机端政策咨询建议用来源场景过滤查看。', '会话归档后不能继续聊天，但仍能被查询和统计。', '若会话存在但消息为空，优先排查消息落库链路。']
  },
  aiMonthlyReport: {
    feature: '按年度汇总 AI 咨询情况，查看来源场景、Skill、知识库和专家维度数据。',
    roles: '管理员、AI 运营',
    steps: ['先选年份和来源场景', '再看总体指标', '重点核对趋势和排名', '异常时回到台账或日志中台排查'],
    next: { label: '去 日志中台', path: '/log-center' },
    faqs: ['手机端数据已入库后，会自然进入月报统计口径。', '口径不一致时，先确认筛选条件和来源场景。', '月报导出适合做阶段复盘，不适合逐条排查。']
  },
  logCenter: {
    feature: '统一查看 AI 调用开始、成功、失败、命中信息和承接结果，是主链路排障入口。',
    roles: '管理员、排障人员',
    steps: ['先按模块筛到 AI_CHAIN', '再按 traceId 或 sessionId 搜索', '查看原始日志和诊断结论', '回到对应页面修复或复测'],
    next: { label: '回到 AI能力地图', path: '/ai-map' },
    faqs: ['AI_CHAIN 模块记录的是服务端关键节点，不是前端展示文案。', '权限失败和参数缺失优先看 ERROR/WARN 级别。', '若日志为空，通常说明链路还没真正走到后端。']
  }
}

export function getAiPageGuide(guideKey) {
  return aiPageGuides[guideKey] || null
}
