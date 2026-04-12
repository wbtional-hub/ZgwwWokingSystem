const api = require('../../services/api');
const env = require('../../config/env');
const { ensureAuthenticated } = require('../../utils/auth');

const QUICK_PROMPTS = [
  '厦门市高层次人才可以申请哪些住房支持？',
  '企业想申请人才项目，通常先准备哪些材料？',
  '新引进人才常见补贴有哪些，适用条件怎么判断？'
];

function resolvePermissionFlags(permissionInfo) {
  const aiPermissions = Array.isArray(permissionInfo && permissionInfo.aiPermissions)
    ? permissionInfo.aiPermissions
    : [];
  return {
    canUseAi: Boolean(permissionInfo && (permissionInfo.admin || aiPermissions.some((item) => item.canUseAi))),
    canUseAgent: Boolean(permissionInfo && (permissionInfo.admin || aiPermissions.some((item) => item.canUseAgent)))
  };
}

function resolveCitations(message) {
  if (Array.isArray(message && message.citedTitles) && message.citedTitles.length) {
    return message.citedTitles;
  }
  if (Array.isArray(message && message.citedChunkIdList) && message.citedChunkIdList.length) {
    return message.citedChunkIdList.map((item) => `Chunk ${item}`);
  }
  return [];
}

Page({
  data: {
    quickPrompts: QUICK_PROMPTS,
    loading: false,
    asking: false,
    skill: null,
    sessionInfo: null,
    permissionInfo: null,
    canUseAi: false,
    canUseAgent: false,
    helperText: '准备连接政策咨询能力中...',
    question: '',
    messages: [],
    errorMessage: '',
    scrollIntoView: ''
  },

  onShow() {
    if (!ensureAuthenticated()) {
      return;
    }
    this.bootstrap();
  },

  async onPullDownRefresh() {
    if (!ensureAuthenticated()) {
      wx.stopPullDownRefresh();
      return;
    }
    await this.bootstrap();
    wx.stopPullDownRefresh();
  },

  async bootstrap() {
    this.setData({
      loading: true,
      errorMessage: ''
    });
    try {
      const [skillList, permissionInfo] = await Promise.all([
        api.querySkillList({
          keywords: env.skillCode,
          publishStatus: 'PUBLISHED',
          status: 1
        }),
        api.queryCurrentAiPermission()
      ]);
      const resolvedSkill = (Array.isArray(skillList) ? skillList : []).find((item) => item.skillCode === env.skillCode)
        || (Array.isArray(skillList) ? skillList[0] : null);
      if (!resolvedSkill) {
        throw new Error('系统中还没有可用的政策咨询 Skill');
      }
      const permissionFlags = resolvePermissionFlags(permissionInfo);
      this.setData({
        skill: resolvedSkill,
        permissionInfo,
        canUseAi: permissionFlags.canUseAi,
        canUseAgent: permissionFlags.canUseAgent
      });
      if (!permissionFlags.canUseAgent) {
        this.setData({
          helperText: '当前账号未开通咨询能力，请先在 AI 权限中心授权。',
          errorMessage: '当前账号未开通咨询能力，请先在系统后台授权后再试。'
        });
        return;
      }
      await this.ensureSession(resolvedSkill.id);
      this.updateHelperText();
    } catch (error) {
      this.setData({
        errorMessage: error.message || '政策咨询初始化失败',
        helperText: error.message || '政策咨询初始化失败'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  updateHelperText() {
    if (this.data.errorMessage) {
      this.setData({ helperText: this.data.errorMessage });
      return;
    }
    if (!this.data.skill) {
      this.setData({ helperText: '正在定位政策咨询 Skill...' });
      return;
    }
    if (this.data.canUseAi) {
      this.setData({
        helperText: `当前已连接 ${this.data.skill.skillName || '政策咨询'}，支持 AI 优先答复。`
      });
      return;
    }
    this.setData({
      helperText: `当前已连接 ${this.data.skill.skillName || '政策咨询'}，若 AI 不可用将自动切换知识库兜底。`
    });
  },

  async ensureSession(skillId) {
    const sessionList = await api.queryAgentSessions({
      skillId: Number(skillId),
      status: 'ACTIVE'
    });
    const latestSession = Array.isArray(sessionList) ? sessionList[0] : null;
    if (latestSession && latestSession.id) {
      this.setData({ sessionInfo: latestSession });
      await this.fetchMessages(latestSession.id);
      return;
    }
    const newSession = await api.createAgentSession({
      skillId: Number(skillId)
    });
    this.setData({
      sessionInfo: newSession,
      messages: [],
      scrollIntoView: ''
    });
  },

  async fetchMessages(sessionId) {
    const messageList = await api.queryAgentMessages(sessionId);
    const normalizedMessages = (Array.isArray(messageList) ? messageList : []).map((item, index) => ({
      id: item.id || `local-${index}`,
      roleText: item.messageRole === 'user' ? '我' : '政策顾问',
      bubbleClass: item.messageRole === 'user' ? 'message-bubble message-bubble--user' : 'message-bubble message-bubble--assistant',
      messageText: item.messageText || '',
      citations: resolveCitations(item),
      anchorId: `msg-${item.id || index}`
    }));
    const lastItem = normalizedMessages[normalizedMessages.length - 1];
    this.setData({
      messages: normalizedMessages,
      scrollIntoView: lastItem ? lastItem.anchorId : ''
    });
  },

  handleQuestionInput(event) {
    this.setData({
      question: event.detail.value
    });
  },

  async handleQuickPrompt(event) {
    const prompt = event.currentTarget.dataset.prompt;
    if (!prompt) {
      return;
    }
    this.setData({
      question: prompt
    });
    await this.handleSend();
  },

  async handleNewSession() {
    if (!this.data.skill || this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    try {
      const newSession = await api.createAgentSession({
        skillId: Number(this.data.skill.id)
      });
      this.setData({
        sessionInfo: newSession,
        messages: [],
        question: '',
        scrollIntoView: ''
      });
      this.updateHelperText();
    } catch (error) {
      wx.showModal({
        title: '创建失败',
        content: error.message || '新会话创建失败',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  async handleSend() {
    const questionText = (this.data.question || '').trim();
    if (!questionText) {
      wx.showToast({
        title: '请输入咨询问题',
        icon: 'none'
      });
      return;
    }
    if (!this.data.canUseAgent || !this.data.sessionInfo || !this.data.sessionInfo.id) {
      wx.showToast({
        title: '当前还不能开始咨询',
        icon: 'none'
      });
      return;
    }
    this.setData({ asking: true });
    try {
      await api.sendAgentQuestion({
        sessionId: this.data.sessionInfo.id,
        question: questionText
      });
      this.setData({ question: '' });
      await this.fetchMessages(this.data.sessionInfo.id);
    } catch (error) {
      wx.showModal({
        title: '发送失败',
        content: error.message || '政策咨询失败',
        showCancel: false
      });
    } finally {
      this.setData({ asking: false });
    }
  }
});
