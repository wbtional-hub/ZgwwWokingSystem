export const AGENT_FUNCTION_BINDINGS = Object.freeze({
  talent_policy_consult: Object.freeze({
    functionCode: 'talent_policy_consult',
    functionName: '人才政策咨询',
    expertName: '人才政策咨询专家',
    defaultSkillCode: 'talent_policy_consultant',
    defaultBaseId: 1,
    allowSkillSwitch: false
  })
})

export function getAgentFunctionBinding(functionCode) {
  return AGENT_FUNCTION_BINDINGS[functionCode] || null
}
