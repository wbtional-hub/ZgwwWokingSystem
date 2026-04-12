const TEST_LOGIN_ORIGIN = 'http://124.220.158.213:9090'
const TEST_LOGIN_HOST = '124.220.158.213:9090'
const TEST_LOGIN_HOSTNAME = '124.220.158.213'
const TEST_LOGIN_PORT = '9090'

function resolveCurrentOrigin() {
  if (typeof window === 'undefined' || !window.location) {
    return ''
  }
  return window.location.origin || ''
}

function resolveCurrentHost() {
  if (typeof window === 'undefined' || !window.location) {
    return ''
  }
  return window.location.host || ''
}

function resolveCurrentHostname() {
  if (typeof window === 'undefined' || !window.location) {
    return ''
  }
  return window.location.hostname || ''
}

function resolveCurrentPort() {
  if (typeof window === 'undefined' || !window.location) {
    return ''
  }
  return window.location.port || ''
}

export function isTestIpLoginEnv() {
  return resolveCurrentOrigin() === TEST_LOGIN_ORIGIN
    || resolveCurrentHost() === TEST_LOGIN_HOST
    || (resolveCurrentHostname() === TEST_LOGIN_HOSTNAME && resolveCurrentPort() === TEST_LOGIN_PORT)
}

export function isTestLoginOrigin() {
  return isTestIpLoginEnv()
}

export function getCurrentOrigin() {
  return resolveCurrentOrigin()
}
